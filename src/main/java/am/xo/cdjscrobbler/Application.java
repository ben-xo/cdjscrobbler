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

import org.deepsymmetry.beatlink.DeviceFinder;
import org.deepsymmetry.beatlink.VirtualCdj;
import org.deepsymmetry.beatlink.data.MetadataFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
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
public class Application
{
    static final Logger logger = LoggerFactory.getLogger(Application.class);
    static final ComboConfig config = new ComboConfig();
    static final Application theApplication = new Application();

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

    public void start() throws Exception
    {
        LastFmClient lfm = null;
        if(lfmEnabled) {
            logger.info("Starting Last.fm Scrobbler…");
            lfm = new LastFmClient(new LastFmClientConfig(config));
            try {
                lfm.ensureUserIsConnected();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        TwitterClient twitter = null;
        if(twitterEnabled) {
            logger.info("Starting Twitter bot…");
            twitter = new TwitterClient(new TwitterClientConfig(config));
            try {
                twitter.ensureUserIsConnected();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
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
        if(lfmEnabled)     queueProcessor.setLfm(lfm);
        if(twitterEnabled) queueProcessor.setTwitter(twitter);
        queueProcessor.start(); // this doesn't return until shutdown (or exception)

        // TODO: add a Lifecycle handler that shuts down when everything else shuts down
    }

    private static void loadConfig(String[] args) throws IOException {

        // TODO: make fewer assumptions here, but this'll do for now!

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
            ConfigFileUtil.load(config, localConfigFile);
        } catch (IOException ioe) {
            logger.error("Error loading config properties from {}", localConfigFile, ioe);
            throw ioe;
        }

        String nowPlayingPoint = config.getProperty("cdjscrobbler.model.nowPlayingPointMs", "");
        String lfmEnabled = config.getProperty("cdjscrobbler.enable.lastfm", "false");
        String twitterEnabled = config.getProperty("cdjscrobbler.enable.twitter", "false");

        if(nowPlayingPoint != null && !nowPlayingPoint.isEmpty()) {
            logger.info("Loaded Now Playing Point of {} ms", nowPlayingPoint);
            SongModel.setNowPlayingPoint(Integer.parseInt(nowPlayingPoint));
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
}
