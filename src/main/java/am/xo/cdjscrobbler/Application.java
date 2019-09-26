package am.xo.cdjscrobbler;

import am.xo.cdjscrobbler.SongEvents.NowPlayingEvent;
import org.deepsymmetry.beatlink.*;
import org.deepsymmetry.beatlink.data.MetadataFinder;

import org.deepsymmetry.beatlink.data.TrackMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Hello world!
 *
 */
public class Application
{
    final Logger logger = LoggerFactory.getLogger(Application.class);

    protected LinkedBlockingQueue<SongEvent> songEventQueue;
    protected UpdateListener updateListener;
//    protected QueueProcessor queueProcessor;

    public static void main( String[] args ) throws Exception
    {
        Application theApplication = new Application();
        theApplication.start(args);
    }

    public void start( String[] args ) throws Exception
    {
        logger.info( "Starting CDJ Scrobbler" );

        logger.info("Starting DeviceFinder…");
        DeviceFinder.getInstance().start();
        while(DeviceFinder.getInstance().getCurrentDevices().isEmpty()) {
            logger.info("Waiting for devices…");
            Thread.sleep(1000);
        }

        VirtualCdj virtualCdj = VirtualCdj.getInstance();

        logger.info("Starting VirtualCDJ…");
        while(!virtualCdj.start()) {
            logger.info("Retrying…");
        }

        // MediaFinder fails if there's only 1 CDJ on the network, because it can't impersonate an active device.
        if(virtualCdj.getDeviceNumber() > 4 && DeviceFinder.getInstance().getCurrentDevices().size() == 1) {
            virtualCdj.setDeviceNumber((byte) 4);
        }


//        System.exit(-1);

        logger.info("Starting MetadataFinder…");
        MetadataFinder.getInstance().start();

        songEventQueue = new LinkedBlockingQueue<SongEvent>();

        // start two threads with a shared queue
        // TODO: dynamically add and remove UpdateListeners as devices are announced
        logger.info( "Starting UpdateListener…" );
        updateListener = new UpdateListener(songEventQueue);
        VirtualCdj.getInstance().addUpdateListener(updateListener);

        logger.info( "Starting QueueProcessor…" );
//        queueProcessor = new QueueProcessor(songEventQueue);

        while(true) {
            SongEvent e = songEventQueue.take();
            logger.info("Received event " + e);

            if(e instanceof NowPlayingEvent) {
                NowPlayingEvent npe = (NowPlayingEvent) e;
                TrackMetadata metadata = MetadataFinder.getInstance().requestMetadataFrom(npe.cdjStatus);
                logger.info("Song: " + metadata);
                npe.model.song = new SongDetails(metadata);
            }
        }

        // TODO: add a Lifecycle handler that shuts down when everything else shuts down

        // TODO: create a scrobbling adaptor
        // TODO: create a state machine

        // TODO: model "more than half way" ? Adjustable? How much needs to be played to count?
    }
}
