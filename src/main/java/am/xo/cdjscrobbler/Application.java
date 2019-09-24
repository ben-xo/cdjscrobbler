package am.xo.cdjscrobbler;

import org.deepsymmetry.beatlink.*;
import org.deepsymmetry.beatlink.data.MetadataFinder;

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



        // TODO: create a scrobbling adaptor
        // TODO: create a state machine

        // TODO: model is playing / is not playing
        // TODO: model tempo influence on play time
        // TODO: model "more than half way" ? Adjustable? How much needs to be played to count?
    }
}
