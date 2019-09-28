package am.xo.cdjscrobbler;

import am.xo.cdjscrobbler.SongEvents.NowPlayingEvent;
import am.xo.cdjscrobbler.SongEvents.ScrobbleEvent;
import org.deepsymmetry.beatlink.data.MetadataFinder;
import org.deepsymmetry.beatlink.data.TrackMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

public class QueueProcessor {

    static final Logger logger = LoggerFactory.getLogger(QueueProcessor.class);

    private LinkedBlockingQueue<SongEvent> songEventQueue;

    // TODO: generify this to decouple.
    private LastFmClient lfm;

    public QueueProcessor(LinkedBlockingQueue<SongEvent> songEvents) {
        this.songEventQueue = songEvents;
    }

    public void start() throws InterruptedException {
        while(true) {
            SongEvent songEvent = songEventQueue.take(); // this blocks until an event is ready.
            logger.info("Received event " + songEvent);

            if(songEvent instanceof NowPlayingEvent) {

                // NowPlaying events indicate that we've played enough of the song to start caring about
                // what it actually is. (The next state, Scrobbling, depends on knowing the song length)
                NowPlayingEvent nowPlayingEvent = (NowPlayingEvent) songEvent;
                TrackMetadata metadata = MetadataFinder.getInstance().requestMetadataFrom(nowPlayingEvent.cdjStatus);
                logger.info("Song: " + metadata);

                // save it back to the model so it can be used to determine the scrobble point
                nowPlayingEvent.model.song = new SongDetails(metadata);

                lfm.updateNowPlaying(nowPlayingEvent);

            } else if(songEvent instanceof ScrobbleEvent) {

                ScrobbleEvent scrobbleEvent = (ScrobbleEvent) songEvent;
                lfm.scrobble(scrobbleEvent);
            }
        }
    }

    public void setLfm(LastFmClient lfm) {
        this.lfm = lfm;
    }
}
