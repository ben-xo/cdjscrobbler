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

import am.xo.cdjscrobbler.SongEventListeners.NewSongLoadedListener;
import am.xo.cdjscrobbler.SongEventListeners.NowPlayingListener;
import am.xo.cdjscrobbler.SongEventListeners.ScrobbleListener;
import am.xo.cdjscrobbler.SongEvents.NewSongLoadedEvent;
import am.xo.cdjscrobbler.SongEvents.NowPlayingEvent;
import am.xo.cdjscrobbler.SongEvents.ResetEvent;
import am.xo.cdjscrobbler.SongEvents.ScrobbleEvent;
import am.xo.cdjscrobbler.SongEvents.TransitionEvent;
import org.deepsymmetry.beatlink.data.MetadataFinder;
import org.deepsymmetry.beatlink.data.TrackMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class takes SongEvents from a thread-safe queue, forever, and then deals with them (e.g. by scrobbling).
 *
 * Currently we're only interested in NowPlayingEvent and ScrobbleEvent (both of which only happen once per song), but
 * we will also receive other events.
 *
 * When receiving NowPlaying events, we treat this as a signal to look up the track metadata and import this back into
 * the SongModel. (SongModel needs to know the track length in order to know where the half-way point is for scrobbling)
 *
 * TODO: make this pluginable
 */
public class QueueProcessor implements SongEventVisitor {

    static final Logger logger = LoggerFactory.getLogger(QueueProcessor.class);

    private final LinkedBlockingQueue<SongEvent> songEventQueue;

    private final List<NewSongLoadedListener> newSongLoadedListeners = new ArrayList<>();
    private final List<NowPlayingListener> nowPlayingListeners = new ArrayList<>();
    private final List<ScrobbleListener> scrobbleListeners = new ArrayList<>();

    public QueueProcessor(LinkedBlockingQueue<SongEvent> songEvents) {
        this.songEventQueue = songEvents;
    }

    /**
     * Infinite loop where scrobbles and tweets come out.
     *
     * @throws InterruptedException
     */
    @SuppressWarnings("InfiniteLoopStatement")
    public void start() throws InterruptedException {
        while(true) {
            SongEvent songEvent = songEventQueue.take(); // this blocks until an event is ready.
            info("Received event {}", songEvent);

            // Visitor pattern. Go visit the event, to have it call back to the right handler on this class.
            // (One wonders if it's worth it, just to avoid smelly "instanceof")
            songEvent.accept(this);
        }
    }

    @Override
    public void visit(NowPlayingEvent event) {

        if(event.model.song == null) {
            // Second, and final chance to work out what song it is (in case MetadataFinder is being slow).
            // The next state, Scrobbling, depends on knowing the song length, so if we can't get this here
            // then the song will remain anonymous and will never scrobble or tweet.
            TrackMetadata metadata = MetadataFinder.getInstance().requestMetadataFrom(event.cdjStatus);
            info("Song: {} - {}", metadata.getArtist().label, metadata.getTitle());
            logger.debug(metadata.toString());

            if (metadata != null) {
                // save it back to the model so it can be used to determine the scrobble point
                event.model.song = new SongDetails(metadata);
            }
        }

        // This sends the event to the Dmca Accountant, LastFM Client and Twitter Client etc (if configured)
        nowPlayingListeners.forEach((listener) -> listener.nowPlaying(event));
    }

    @Override
    public void visit(ResetEvent event) {
        // noop
    }

    @Override
    public void visit(ScrobbleEvent event) {
        // This sends the event to the LastFM Client etc (if configured)
        scrobbleListeners.forEach((listener) -> listener.scrobble(event));
    }

    @Override
    public void visit(TransitionEvent event) {
        // noop
    }

    @Override
    public void visit(NewSongLoadedEvent event) {
        // First attempt to look up the song length. Opportunity for an early warning that you shouldn't play the song.
        TrackMetadata metadata = MetadataFinder.getInstance().requestMetadataFrom(event.cdjStatus);
        info("Song: {} - {}", metadata.getArtist().label, metadata.getTitle());
        logger.debug(metadata.toString());

        if(metadata != null) {
            // save it back to the model so it can be used to determine the scrobble point
            event.model.song = new SongDetails(metadata);
        }

        // This sends the event to the DmcaAccountant, etc (if configured).
        // Main use is to warn if playing the song might make the mix unstreamable on radio or on Mixcloud etc
        newSongLoadedListeners.forEach((listener) -> listener.newSongLoaded(event));
    }

    public void addNewSongLoadedListener(NewSongLoadedListener l) {
        newSongLoadedListeners.add(l);
    }
    public void removeNewSongLoadedListener(NewSongLoadedListener l) {
        newSongLoadedListeners.remove(l);
    }

    public void addNowPlayingListener(NowPlayingListener l) {
        nowPlayingListeners.add(l);
    }

    public void removeNowPlayingListener(NowPlayingListener l) {
        nowPlayingListeners.remove(l);
    }

    public void addScrobbleListener(ScrobbleListener l) {
        scrobbleListeners.add(l);
    }
    public void removeScrobbleListener(ScrobbleListener l) {
        scrobbleListeners.remove(l);
    }

    private final List<OrchestratorListener> orchestratorListeners = new ArrayList<>();
    public void addMessageListener(OrchestratorListener l) {
        orchestratorListeners.add(l);
    }

    protected void info(String m, Object... args) {
        logger.info(m, args);
        orchestratorListeners.forEach((listener) -> listener.cdjScrobblerMessage(
                MessageFormatter.arrayFormat(m, args).getMessage()
        ));
    }

    public void warn(String m, Object... args) {
        logger.warn(m, args);
        orchestratorListeners.forEach((listener) -> listener.cdjScrobblerMessage(
                MessageFormatter.arrayFormat(m, args).getMessage()
        ));
    }

    protected void error(String m, Object... args) {
        logger.error(m, args);
        orchestratorListeners.forEach((listener) -> listener.cdjScrobblerMessage(
                MessageFormatter.arrayFormat(m, args).getMessage()
        ));
    }
}
