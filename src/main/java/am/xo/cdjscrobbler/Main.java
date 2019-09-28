package am.xo.cdjscrobbler;

import am.xo.cdjscrobbler.SongEvents.NowPlayingEvent;
import am.xo.cdjscrobbler.SongEvents.ScrobbleEvent;
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
 * TODO: sort out the configuration mess
 *
 */
public class Main
{
    static final Logger logger = LoggerFactory.getLogger(Main.class);
    static final Properties config = new Properties();
    static final Main theApplication = new Main();

    static final String localConfigFile = System.getProperty("user.home") + File.separator + "cdjscrobbler.properties";
    static final String localSessionFile = System.getProperty("user.home") + File.separator + "cdjscrobbler-session.properties";

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
        logger.info("Starting Last.fm Scrobbler…");
        LastFmClient lfm = new LastFmClient(new LastFmClientConfig(config));
        try {
            lfm.ensureUserIsConnected();
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        logger.info("Starting Twitter bot…");
        //TwitterClient twitter = new TwitterClient(new TwitterClientConfig(config));
        try {
            lfm.ensureUserIsConnected();
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

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

        // TODO: add a Lifecycle handler that shuts down when everything else shuts down

        // TODO: create a scrobbling adaptor
        // TODO: create a state machine

        // TODO: model "more than half way" ? Adjustable? How much needs to be played to count?
    }

    private static void loadConfig(String[] args) throws IOException {

        // TODO: make fewer assumptions here, but this'll do for now!

        // load default (internal) config
        config.load(Main.class.getClassLoader().getResourceAsStream("config.properties"));

        // load e.g. Last.fm api key and secret
        logger.info("Loading local client configuration");
        loadConfigFromFile(localConfigFile, false);

        // load e.g. Last.fm session key
        logger.info("Loading local session configuration");
        loadConfigFromFile(localSessionFile, false);

        // load any config specified on the command line.
        if (args.length > 0) {
            loadConfigFromFile(args[0], true);
        }
    }

    private static void loadConfigFromFile(String configPath, boolean isVital) throws IOException {
        logger.info("Loading config from " + configPath);
        try (InputStream is = Files.newInputStream(Paths.get(configPath))) {
            config.load(is);
        } catch (IOException ioe) {
            if(isVital) {
                logger.error("Error loading config properties from {}", configPath, ioe);
                throw ioe;
            }
        }
    }
}
