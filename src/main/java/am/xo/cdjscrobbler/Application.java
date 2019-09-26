package am.xo.cdjscrobbler;

import am.xo.cdjscrobbler.SongEvents.NowPlayingEvent;
import org.deepsymmetry.beatlink.*;
import org.deepsymmetry.beatlink.data.MetadataFinder;

import org.deepsymmetry.beatlink.data.TrackMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Hello world!
 *
 */
public class Application
{
    static final Logger logger = LoggerFactory.getLogger(Application.class);
    static final Properties config = new Properties();
    static final Application theApplication = new Application();

    static String userCredsFile;

    protected LinkedBlockingQueue<SongEvent> songEventQueue;
    protected UpdateListener updateListener;
//    protected QueueProcessor queueProcessor;

    public static void main( String[] args ) throws Exception
    {
        loadConfig(args);

        logger.info( "💿📀💿📀 CDJ Scrobbler v{} by Ben XO", config.getProperty("cdjscrobbler.version"));
        logger.info( "💿📀💿📀 https://github.com/ben-xo/cdjscrobbler");

        theApplication.start();
    }

    public void start() throws Exception
    {
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
            // TODO: this is the body of QueueProcessor.
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

    private static void loadConfig(String[] args) throws IOException {

        // TODO: make fewer assumptions here, but this'll do for now!

        // load default config.
        config.load(Application.class.getClassLoader().getResourceAsStream("config.properties"));

        // load e.g. Last.fm credentials.
        logger.info("Loading user credentials");
        userCredsFile = System.getProperty("user.home") + File.separator + "cdjscrobbler.properties";
        try (InputStream is = Files.newInputStream(Paths.get(userCredsFile))) {
            config.load(is);
        } catch (IOException ioe) {
            logger.warn("No saved credentials found in {}", userCredsFile);
            // move on.
        }

        // load any config specified on the command line.
        if (args.length > 0) {
            String configPath = args[0];
            logger.info("Loading config from " + configPath);

            try (InputStream is = Files.newInputStream(Paths.get(configPath))) {
                config.load(is);
            } catch (IOException ioe) {
                logger.error("Error loading config properties from {}", configPath, ioe);
                throw ioe;
            }
        }
    }
}
