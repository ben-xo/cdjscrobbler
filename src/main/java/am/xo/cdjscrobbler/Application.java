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

import am.xo.cdjscrobbler.Plugins.*;
import com.github.scribejava.core.exceptions.OAuthException;
import de.umass.lastfm.CallException;
import org.deepsymmetry.beatlink.*;
import org.deepsymmetry.beatlink.data.MetadataFinder;
import org.deepsymmetry.beatlink.dbserver.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This is where it all starts.
 *
 * The Application class is responsible for loading the configuration, creating Twitter and Last.fm clients, and
 * then hooking them up to the CDJ lifecycle through beat-link's VirtualCdj and MediaFinder.
 *
 * beat-link requires low latency (but delivers DeviceUpdates on the same thread), so the architecture is to
 * model the playing songs in one thread with the UpdateListener, and deliver events to the QueueProcessor
 * so that actions such as Tweeting or Scrobbling happen in a different thread.
 *
 * During set up, if configuration is missing for either Last.fm or Twitter, you will be prompted to authenticate.
 *
 */
public class Application implements LifecycleListener
{
    static final Logger logger = LoggerFactory.getLogger(Application.class);
    static final ApplicationConfig config = new ApplicationConfig();
    static final Application theApplication = new Application();

    static int retryDelay = 500; // override with setting cdjscrobbler.retryDelayMs

    static boolean lfmEnabled;
    static boolean twitterEnabled;

    static String localConfigFile = System.getProperty("user.home") + File.separator + "cdjscrobbler.properties";

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

    public static void setRetryDelay(int delay) {
        retryDelay = delay;
    }

    public static LastFmClient getLfmClient(boolean enabled) throws IOException {
        LastFmClient lfm = null;
        if(enabled) {
            logger.info("Starting Last.fm Scrobbler…");
            lfm = new LastFmClient(new LastFmClientConfig(config));
            try {
                lfm.ensureUserIsConnected();
            } catch (CallException e) {
                if(e.getCause() instanceof UnknownHostException) {
                    logger.warn("** Looks like we're offline. Scrobbling disabled. **");
                } else {
                    throw e;
                }
            }
        }
        return lfm;
    }

    public static TwitterClient getTwitterClient(boolean enabled) throws IOException {
        TwitterClient twitter = null;
        if(enabled) {
            logger.info("Starting Twitter bot…");
            twitter = new TwitterClient(new TwitterClientConfig(config));
            try {
                twitter.ensureUserIsConnected();
            } catch (OAuthException e) {
                if(e.getCause() instanceof UnknownHostException) {
                    logger.warn("** Looks like we're offline. Tweeting disabled. **");
                } else {
                    throw e;
                }
            }
        }
        return twitter;
    }

    public void start() throws Exception
    {
        LastFmClient lfm = getLfmClient(lfmEnabled);
        TwitterClient twitter = getTwitterClient(twitterEnabled);
        DmcaAccountant dmcaAccountant = new DmcaAccountant();

        songEventQueue = new LinkedBlockingQueue<>();

        // start two threads with a shared queue
        // TODO: dynamically add and remove UpdateListeners as devices are announced
        logger.info( "Starting UpdateListener…" );
        updateListener = new UpdateListener(songEventQueue);
        VirtualCdj.getInstance().addUpdateListener(updateListener);

        startVirtualCdj();

        logger.info( "Starting QueueProcessor…" );
        queueProcessor = new QueueProcessor(songEventQueue);

        queueProcessor.addNewSongLoadedListener(dmcaAccountant);
        queueProcessor.addNowPlayingListener(dmcaAccountant);

        if(lfmEnabled)     {
            queueProcessor.addNowPlayingListener(lfm);
            queueProcessor.addScrobbleListener(lfm);
        }

        if(twitterEnabled) {
            queueProcessor.addNowPlayingListener(twitter);
        }

        queueProcessor.start(); // this doesn't return until shutdown (or exception)

        // TODO: queue processor should probably have its own thread.
    }

    private void startVirtualCdj() throws InterruptedException {
        ConnectionManager connectionManager = ConnectionManager.getInstance();
        VirtualCdj virtualCdj = VirtualCdj.getInstance();
        MetadataFinder metadataFinder = MetadataFinder.getInstance();

        // default is 10s, which is quite high when recovering from a network outage
        connectionManager.setSocketTimeout(3000);
        metadataFinder.addLifecycleListener(this);

        boolean started;

        logger.info("Starting VirtualCDJ…");

        started = false;
        do {
            try {
                started = virtualCdj.start();
            } catch(Exception e) {
                logger.warn("Failed to start.", e);
            }
            if(!started) {
                logger.info("Retrying VirtualCdj…");
                Thread.sleep(retryDelay);
            }
        } while(!started);

        // MediaFinder fails if there's only 1 CDJ on the network, because it can't impersonate an active device.
        // It also fails if there are two on the network and they're both using Pro Link at the same time.
        if(virtualCdj.getDeviceNumber() > 4) {
            try {
                byte newDeviceNumber = getFreeLowDeviceNumber();
                virtualCdj.setDeviceNumber(newDeviceNumber);
                logger.info("Set virtual CDJ device number to {}", newDeviceNumber);
            } catch(IllegalStateException e) {
                logger.error("Looks like metadata finder isn't going to work: no free low device numbers.");
            }
        }

        logger.info("Starting MetadataFinder…");

        started = false;
        do {
            try {
                metadataFinder.start();
                started = metadataFinder.isRunning();
            } catch(Exception e) {
                logger.warn("Failed to start.", e);
            }
            if(!started) {
                logger.info("Retrying MetadataFinder…");
                Thread.sleep(retryDelay);
            }
        } while(!started);

    }

    /**
     * Looks for a device number <= 4 that we can use for the MetadataFinder.
     *
     * The MetadataFinder only works if it can use the ID of an unused "real" CDJ (1-4 - Rekordbox can use higher IDs)
     * because it emulates a Pro-Link media browser. This means we either have to pick the ID of a CDJ that's not
     * present. If you happen to have 4 real CDJs, it will automatically try to "borrow" an ID from one for a lookup -
     * but that only works if there is one on the network that is not using Pro-Link right now. (If all 4 are using
     * Pro-Link then sorry - you're out of luck!)
     *
     * @return byte a safe device number
     */
    private byte getFreeLowDeviceNumber() {
        boolean[] taken = {false, false, false, false, false};
        for (DeviceAnnouncement a : DeviceFinder.getInstance().getCurrentDevices()) {
            int deviceNumber = a.getNumber();
            if(deviceNumber <= 4) {
                taken[deviceNumber] = true;
            }
        }

        // try for 4 first.
        for(byte t = 4; t > 0; t--) {
            if(!taken[t]) {
                return t;
            }
        }
        throw new IllegalStateException("No free low device numbers");
    }

    private static void loadConfig(String[] args) throws IOException {

        // TODO: make fewer assumptions here, but this'll do for now!
        // We could reasonably have multiple instances of LastFmClient or TwitterClient, but there is currently
        // no way to configure multiple instances without doing it in code.

        // load any config specified on the command line.
        if (args.length > 0) {
            localConfigFile = args[0];
            logger.info("Config file set to " + localConfigFile);
        }

        // load default (internal) config
        config.load(Application.class.getClassLoader().getResourceAsStream("config.properties"));

        try {
            // load e.g. Last.fm and Twitter keys and tokens
            logger.info("Loading local client configuration");
            config.load();
        } catch (IOException ioe) {
            logger.error("Error loading config properties from {}", localConfigFile, ioe);
            throw ioe;
        }

        String nowPlayingPoint = config.getProperty("cdjscrobbler.model.nowPlayingPointMs", "");
        String retryDelay = config.getProperty("cdjscrobbler.retryDelayMs", "500");
        String lfmEnabled = config.getProperty("cdjscrobbler.enable.lastfm", "false");
        String twitterEnabled = config.getProperty("cdjscrobbler.enable.twitter", "false");

        if(nowPlayingPoint != null && !nowPlayingPoint.isEmpty()) {
            logger.info("Loaded Now Playing Point of {} ms", nowPlayingPoint);
            SongModel.setNowPlayingPoint(Integer.parseInt(nowPlayingPoint));
        }

        if(retryDelay != null && !retryDelay.isEmpty()) {
            logger.info("Loaded Retry Delay of {} ms", retryDelay);
            setRetryDelay(Integer.parseInt(nowPlayingPoint));
        }

        if(Boolean.parseBoolean(lfmEnabled)) {
            Application.lfmEnabled = true;
        } else {
            logger.warn("**************************************************************************************");
            logger.warn("* Scrobbling to Last.fm disabled. set cdjscrobbler.enable.lastfm=true in your config *");
            logger.warn("**************************************************************************************");
        }

        if(Boolean.parseBoolean(twitterEnabled)) {
            Application.twitterEnabled = true;
        } else {
            logger.warn("*********************************************************************************");
            logger.warn("* Tweeting tracks disabled. set cdjscrobbler.enable.twitter=true in your config *");
            logger.warn("*********************************************************************************");
        }
    }

    @Override
    public void started(LifecycleParticipant sender) {
        // jolly good!
    }

    @Override
    public void stopped(LifecycleParticipant sender) {
        logger.info("Attempting to restart because {} stopped", sender);
        try {
            startVirtualCdj();
        } catch(InterruptedException e) {
            // looks like the app was shutting down. Accept fate.
        }
    }

}
