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
public class Application
{
    static final Logger logger = LoggerFactory.getLogger(Application.class);
    static final ComboConfig config = new ComboConfig();
    static final Application theApplication = new Application();

    static final String localConfigFile = System.getProperty("user.home") + File.separator + "cdjscrobbler.properties";

    protected LinkedBlockingQueue<SongEvent> songEventQueue;
    protected UpdateListener updateListener;
    protected QueueProcessor queueProcessor;

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
        TwitterClient twitter = new TwitterClient(new TwitterClientConfig(config));
        try {
            twitter.ensureUserIsConnected();
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
        queueProcessor = new QueueProcessor(songEventQueue);
        queueProcessor.setLfm(lfm);
        queueProcessor.setTwitter(twitter);
        queueProcessor.start(); // this doesn't return until shutdown (or exception)

        // TODO: add a Lifecycle handler that shuts down when everything else shuts down

        // TODO: create a scrobbling adaptor
        // TODO: create a state machine

        // TODO: model "more than half way" ? Adjustable? How much needs to be played to count?
    }

    private static void loadConfig(String[] args) throws IOException {

        // TODO: make fewer assumptions here, but this'll do for now!

        // load default (internal) config
        config.load(Application.class.getClassLoader().getResourceAsStream("config.properties"));

        // load e.g. Last.fm and Twitter keys and tokens
        logger.info("Loading local client configuration");
        ConfigFileUtil.maybeLoad(config, localConfigFile);

        // load any config specified on the command line.
        if (args.length > 0) {
            String extraConfigFile =  args[0];
            logger.info("Loading config from " + extraConfigFile);
            try {
                ConfigFileUtil.load(config, extraConfigFile);
            } catch (IOException ioe) {
                logger.error("Error loading config properties from {}", extraConfigFile, ioe);
                throw ioe;
            }
        }

        String nowPlayingPoint = config.getProperty("cdjscrobbler.model.nowPlayingPointMs", "");
        if(nowPlayingPoint != null && !nowPlayingPoint.isEmpty()) {
            logger.info("Loaded Now Playing Point of {} ms", nowPlayingPoint);
            SongModel.setNowPlayingPoint(Integer.parseInt(nowPlayingPoint));
        }
    }
}
