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

import am.xo.cdjscrobbler.Plugins.CsvLogger;
import am.xo.cdjscrobbler.Plugins.DmcaAccountant;
import am.xo.cdjscrobbler.Plugins.LastFmClient;
import am.xo.cdjscrobbler.Plugins.TwitterClient;
import com.github.scribejava.core.exceptions.OAuthException;
import de.umass.lastfm.CallException;
import org.deepsymmetry.beatlink.*;
import org.deepsymmetry.beatlink.data.CrateDigger;
import org.deepsymmetry.beatlink.data.MetadataFinder;
import org.deepsymmetry.beatlink.dbserver.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This is the main thread.
 *
 * The Orchestrator class is responsible for creating Twitter and Last.fm clients, and
 * then hooking them up to the CDJ lifecycle through beat-link's VirtualCdj and MediaFinder.
 *
 * beat-link requires low latency (but delivers DeviceUpdates on the same thread), so the architecture is to
 * model the playing songs in one thread with the UpdateListener, and deliver events to the QueueProcessor
 * so that actions such as Tweeting or Scrobbling happen in a different thread.
 *
 * During set up, if configuration is missing for either Last.fm or Twitter, you will be prompted to authenticate.
 *
 */
public class Orchestrator implements LifecycleListener, Runnable, DeviceAnnouncementListener {
    static final Logger logger = LoggerFactory.getLogger(Orchestrator.class);
    static OrchestratorConfig config;

    static byte oldDeviceNumber; // used for VirtualCdj / MetadataFinder compat with only 1 CDJ

    public Orchestrator(OrchestratorConfig c) {
        config = c;
    }

    public static LastFmClient getLfmClient() throws IOException {
        logger.info("Starting Last.fm Scrobbler…");
        LastFmClient lfm = new LastFmClient(config.getLastFmClientConfig());
        try {
            lfm.ensureUserIsConnected();
        } catch (CallException e) {
            if (e.getCause() instanceof UnknownHostException) {
                logger.warn("** Looks like we're offline. Scrobbling disabled. **");
            } else {
                throw e;
            }
        }
        return lfm;
    }

    public static TwitterClient getTwitterClient() throws IOException {
        logger.info("Starting Twitter bot…");
        TwitterClient twitter = new TwitterClient(config.getTwitterClientConfig());
        try {
            twitter.ensureUserIsConnected();
        } catch (OAuthException e) {
            if (e.getCause() instanceof UnknownHostException) {
                logger.warn("** Looks like we're offline. Tweeting disabled. **");
            } else {
                throw e;
            }
        }
        return twitter;
    }

    public static DmcaAccountant getDmcaAccountant() throws IOException {
        logger.info("Starting DMCA Accountant…");
        return new DmcaAccountant();
    }

    public static CsvLogger getCsvLogger() throws IOException {
        logger.info("Logging the tracklist to {}", config.getCsvLoggerFilename());
        CsvLogger csvLogger = new CsvLogger(config.getCsvLoggerFilename());
        return csvLogger;
    }

    protected LinkedBlockingQueue<SongEvent> songEventQueue;
    protected UpdateListener updateListener;
    protected QueueProcessor queueProcessor;

    @Override
    public void run() {

        logger.info("💿📀💿📀 CDJ Scrobbler v{} by Ben XO", config.getVersion());
        logger.info("💿📀💿📀 https://github.com/ben-xo/cdjscrobbler");

        try {
            songEventQueue = new LinkedBlockingQueue<>();

            // start two threads with a shared queue
            // TODO: dynamically add and remove UpdateListeners as devices are announced
            logger.info("Starting UpdateListener…");
            updateListener = new UpdateListener(songEventQueue);
            VirtualCdj.getInstance().addUpdateListener(updateListener);

            startVirtualCdj();


            logger.info("Starting QueueProcessor…");
            queueProcessor = new QueueProcessor(songEventQueue);

            // this must happen after startVirtualCdj() because ArtFinder starts MetadataFinder
//            final ArtworkPopup artworkPopup = new ArtworkPopup();
//            queueProcessor.addNowPlayingListener(artworkPopup);

            if(config.isCsvLoggerEnabled()) {
                CsvLogger csvLogger = getCsvLogger();
                queueProcessor.addScrobbleListener(csvLogger);
            }

            if(config.isDmcaAccountantEnabled()) {
                final DmcaAccountant dmcaAccountant = getDmcaAccountant();

                // start the on air warning
                dmcaAccountant.start();
                queueProcessor.addNewSongLoadedListener(dmcaAccountant);
                queueProcessor.addNowPlayingListener(dmcaAccountant);
            }

            if (config.isLfmEnabled()) {
                final LastFmClient lfm = getLfmClient();
                queueProcessor.addNowPlayingListener(lfm);
                queueProcessor.addScrobbleListener(lfm);
            }

            if (config.isTwitterEnabled()) {
                final TwitterClient twitter = getTwitterClient();
                queueProcessor.addNowPlayingListener(twitter);
            }

            queueProcessor.start(); // this doesn't return until shutdown (or exception)

            // TODO: queue processor should probably have its own thread.

        } catch(Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void startVirtualCdj() throws InterruptedException {
        ConnectionManager connectionManager = ConnectionManager.getInstance();
        DeviceFinder deviceFinder = DeviceFinder.getInstance();
        VirtualCdj virtualCdj = VirtualCdj.getInstance();
        MetadataFinder metadataFinder = MetadataFinder.getInstance();
        CrateDigger crateDigger = CrateDigger.getInstance();

        // default is 10s, which is quite high when recovering from a network outage
        connectionManager.setSocketTimeout(3000);
        metadataFinder.addLifecycleListener(this);

        boolean started;

        logger.info("Starting VirtualCDJ…");

        started = false;
        do {
            try {
                started = virtualCdj.start();
                oldDeviceNumber = virtualCdj.getDeviceNumber();
            } catch (Exception e) {
                logger.warn("Failed to start.", e);
            }
            if (!started) {
                logger.info("Retrying VirtualCdj…");
                Thread.sleep(config.getRetryDelay());
            }
        } while (!started);

        // compat with 1 CDJ mode for MetadataFinder. Depends on oldDeviceNumber.
        updateVirtualCdjNumber();
        deviceFinder.addDeviceAnnouncementListener(this);

        logger.info("Starting MetadataFinder…");

        started = false;
        do {
            try {
                metadataFinder.start();
                started = metadataFinder.isRunning();
            } catch (Exception e) {
                logger.warn("Failed to start.", e);
            }
            if (!started) {
                logger.info("Retrying MetadataFinder…");
                Thread.sleep(config.getRetryDelay());
            }
        } while (!started);

        do {
            try {
                crateDigger.start();
            } catch(Exception e) {
                logger.error("CrateDigger error (retrying):", e);
                Thread.sleep(config.getRetryDelay());
            }
        } while(!crateDigger.isRunning());
    }

    /**
     * Looks for a device number <= 4 that we can use for the MetadataFinder.
     * <p>
     * The MetadataFinder only works if it can use the ID of an unused "real" CDJ (1-4 - Rekordbox can use higher IDs)
     * because it emulates a Pro-Link media browser. This means we have to pick the ID of a CDJ that's not present.
     * If you happen to have 4 real CDJs, it will automatically try to "borrow" an ID from one for a lookup -
     * but that only works if there is one on the network that is not using Pro-Link right now. (If all 4 are using
     * Pro-Link then sorry - you're out of luck!)
     *
     * @return byte a safe device number
     */
    private byte getFreeLowDeviceNumber() {
        boolean[] taken = {false, false, false, false, false};
        for (DeviceAnnouncement a : DeviceFinder.getInstance().getCurrentDevices()) {
            int deviceNumber = a.getNumber();
            if (deviceNumber <= 4) {
                taken[deviceNumber] = true;
            }
        }

        // try for 4 first.
        for (byte t = 4; t > 0; t--) {
            if (!taken[t]) {
                return t;
            }
        }
        throw new IllegalStateException("No free low device numbers");
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
        } catch (InterruptedException e) {
            // looks like the app was shutting down. Accept fate.
        }
    }

    @Override
    public void deviceFound(DeviceAnnouncement deviceAnnouncement) {
        updateVirtualCdjNumber();
    }

    @Override
    public void deviceLost(DeviceAnnouncement deviceAnnouncement) {
        updateVirtualCdjNumber();
    }

    /**
     * MediaFinder throws a bunch of ugly exceptions if you don't try to give it a virtual CDJ number when there's
     * only 1 virtual CDJ on the network.
     */
    void updateVirtualCdjNumber() {
        if(DeviceFinder.getInstance().getCurrentDevices().size() == 1) {
            try {
                byte newDeviceNumber = getFreeLowDeviceNumber();
                VirtualCdj.getInstance().setDeviceNumber(newDeviceNumber);
                logger.info("Set virtual CDJ device number to {}", newDeviceNumber);
            } catch (IllegalStateException e) {
                logger.error("only 1 device no free devices?");
            }
        } else {
            VirtualCdj.getInstance().setDeviceNumber(oldDeviceNumber);
        }
    }
}
