/*
 * Copyright (c) 2019, Ben XO.
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package am.xo.cdjscrobbler;

import org.deepsymmetry.beatlink.CdjStatus;
import org.deepsymmetry.beatlink.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * State machine of a song as currently playing on an individual CDJ. You instantiate one of these per CDJ.
 *
 * Songs follow the state machine diagrammed in README.md.
 *
 * CDJs generally emit several updates a second, and update() is called for each one (if it has the same device number).
 * The updates are used to work out how long the song has been playing for, so that we can work out whether it's "Now
 * Playing", or has been playing for long enough to scrobble. Transitions between certain states will emit a SongEvent
 * that can be handled in the QueueProcessor to trigger tweets or scrobbles.
 *
 * We take account of whether or not the platter is stopped, and we adjust for the pitch you are playing at, but
 * what we're really modelling is the length of time you've been "listening" to the song - even if that's looping or
 * rewinding.
 *
 * The first (by default) ten seconds of playback of a song is treated as "cueing time", where every time you stop,
 * scratch or pause the track the play time is reset to 0. After that, play time is accumulated until the end.
 *
 * Events such as NowPlayingEvent and ScrobbleEvent are emitted by the state machine transitions.
 */
public class SongModel {

    /**
     * After NOW_PLAYING_POINT_MS of continuous playback, we start recording the song playtime.
     */
    public static int NOW_PLAYING_POINT_MS = 10000; // milliseconds (e.g. 30000 = 30 seconds)

    final Logger logger = LoggerFactory.getLogger(SongModel.class);

    /**
     * This field is written directly by the QueueProcessor when the song's details have been resolved (based on the
     * rekordboxId field). This happens in a different thread to the rest of the processing of the model.
     */
    protected SongDetails song = null;

    protected int deviceNumber;
    protected long totalPlayTime = 0;
    protected long startedAt = 0;
    protected SongState currentState = SongState.STARTED;
    protected long lastUpdate = 0;
    protected int rekordboxId = 0;

    public SongModel(int deviceNumber) {
        this.deviceNumber = deviceNumber;
    }

    /**
     * The main event receiver, fed with events by UpdateListener.
     *
     * Note that it's possible for a CdjStatus update to transition us through more than one state with a single
     * message (e.g. PLAYINGPAUSED => PLAYING and then PLAYING => SCROBBLING in a single update), and so we run the
     * update until no more events are yielded.
     *
     * @param update
     * @return
     */
    public ArrayList<SongEvent> update(CdjStatus update) {
        return update(update, new ArrayList<>());
    }

    protected ArrayList<SongEvent> update(CdjStatus update, ArrayList<SongEvent> returnedEvents) {
        SongState prevState = currentState;
        SongEvent yieldedEvent = currentState.applyNext(this, update);

        if(prevState != currentState || currentState.isMoving() ) {
            // only log transitions and playing states - a lot of boring STOPPED messages otherwise

            logger.info("Device {} rekordbox ID {} {} -> {}", deviceNumber, rekordboxId, prevState.name(), this);
        }

        lastUpdate = update.getTimestamp();
        if(yieldedEvent != null) {
            logger.debug("yielded " + yieldedEvent.getClass().getSimpleName());
            // must apply continuously to catch all events in sequence.
            // this could happen if a single event increments time by a large amount.
            returnedEvents.add(yieldedEvent);
            return update(update, returnedEvents);
        }
        return returnedEvents;
    }

    /**
     * Re-initialise. This is usually called when transitioning from CUEING to STARTED.
     */
    public void resetPlay() {
        totalPlayTime = 0;
        startedAt = 0;
    }

    /**
     * Adds the time between this update and the last, adjusted for the current pitch.
     * It's not perfect, as we only receive a few updates a second, but it's close enough!
     *
     * @param update
     */
    public void addPlaytimeFrom(CdjStatus update) {
        if(lastUpdate != 0 && isPlayingForward(update)) {
            long nanosToAdd = update.getTimestamp() - lastUpdate;
            double tempo = Util.pitchToMultiplier(update.getPitch());
            totalPlayTime += tempo * nanosToAdd / 1000000;
        }
    }

    public boolean isPastNowPlaying() {
        return totalPlayTime >= NOW_PLAYING_POINT_MS;
    }

    /**
     * If we have the song metadata, then this will return if we're past the scrobble point (as defined at
     * https://www.last.fm/api/scrobbling). Usually, half way.
     *
     * If we couldn't (or have not yet) retrieved the metadata, then it will never be reached.
     * @return
     */
    public boolean isPastScrobblePoint() {
        return song != null && (totalPlayTime / 1000) > song.getScrobblePoint();
    }

    /**
     * This returns true if the CDJ is "playing" (not just "cue listening), and is not paused, and does not have the
     * platter held down for scratching.
     *
     * @param update
     * @return
     */
    public boolean isPlayingForward(CdjStatus update) {
        return update.isPlaying() && update.getPlayState2() == CdjStatus.PlayState2.MOVING;
    }

    public String toString() {
        return "Device " + Integer.toString(deviceNumber) + " " + currentState.name()
                + " song: " + (song == null ? "<unknown>" : song)
                + " playtime: " + Long.toString(totalPlayTime) + " ms";
    }

    public SongDetails getSong() {
        return song;
    }

    public long getStartedAt() {
        return startedAt;
    }

    /**
     * For those times when the default of 10s is too soon and you want to override this in config.
     *
     * @param point
     */
    public static void setNowPlayingPoint(int point) {
        NOW_PLAYING_POINT_MS = point;
    }
}
