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

import am.xo.cdjscrobbler.SongEvents.*;
import org.deepsymmetry.beatlink.data.MetadataFinder;
import org.deepsymmetry.beatlink.data.TrackMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private LinkedBlockingQueue<SongEvent> songEventQueue;

    // TODO: generify this to decouple.
    private LastFmClient lfm;
    private TwitterClient twitter;

    public QueueProcessor(LinkedBlockingQueue<SongEvent> songEvents) {
        this.songEventQueue = songEvents;
    }

    /**
     * Infinite loop where scrobbles and tweets come out.
     *
     * @throws InterruptedException
     */
    public void start() throws InterruptedException {
        while(true) {
            SongEvent songEvent = songEventQueue.take(); // this blocks until an event is ready.
            logger.info("Received event " + songEvent);

            // Visitor pattern. Go visit the event, to have it call back to the right handler on this class.
            // (One wonders if it's worth it, just to avoid smelly "instanceof")
            songEvent.accept(this);
        }
    }

    @Override
    public void visit(NowPlayingEvent event) {

        // TODO: tell the DMCA song-warning plugin

        if(lfm != null) {
            lfm.updateNowPlaying(event);
        }

        if(twitter != null) {
            twitter.sendNowPlaying(event);
        }
    }

    @Override
    public void visit(ResetEvent event) {
        // noop
    }

    @Override
    public void visit(ScrobbleEvent event) {
        if(lfm != null) {
            lfm.scrobble(event);
        }
    }

    @Override
    public void visit(TransitionEvent event) {
        // noop
    }

    @Override
    public void visit(NewSongLoadedEvent event) {
        // NowPlaying events indicate that we've played enough of the song to start caring about
        // what it actually is. (The next state, Scrobbling, depends on knowing the song length)
        TrackMetadata metadata = MetadataFinder.getInstance().requestMetadataFrom(event.cdjStatus);
        logger.info("Song: " + metadata);

        // save it back to the model so it can be used to determine the scrobble point
        event.model.song = new SongDetails(metadata);

        // TODO: tell the DMCA song-warning plugin
    }

    public void setLfm(LastFmClient lfm) {
        this.lfm = lfm;
    }

    public void setTwitter(TwitterClient t) {
        this.twitter = t;
    }
}
