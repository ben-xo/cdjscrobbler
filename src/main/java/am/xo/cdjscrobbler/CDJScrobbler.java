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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import static picocli.CommandLine.Command;

/**
 * This is where it all starts.
 *
 * The CDJScrobbler class is responsible for loading the configuration (from configuration files and the command line)
 * and then starting the Orchestrator to do all of the rest of the work.
 *
 */
@Command(versionProvider = CDJScrobbler.VersionProvider.class,
        header = {
            "@|fg(124)  .--. .---.    .-.   .--.                  .-.   .-.   .-.              |@",
            "@|fg(125) : .--': .  :   : :  : .--'                 : :   : :   : :              |@",
            "@|fg(126) : :   : :: : _ : :  `. `.  .--. .--.  .--. : `-. : `-. : :   .--. .--.  |@",
            "@|fg(127) : :__ : :; :: :; :   _`, :'  ..': ..'' .; :' .; :' .; :: :_ ' '_.': ..' |@",
            "@|fg(128) `.__.':___.'`.__.'  `.__.'`.__.':_;  `.__.'`.__.'`.__.'`.__;`.__.':_;   |@",
            ""},
        usageHelpWidth = 120,
        usageHelpAutoWidth = true,
        name = "cdjscrobbler",
        description = "Scrobbles tracks from Pioneer CDJ-2000 pro-link network.%n",
        mixinStandardHelpOptions = true,
        sortOptions = false
)
public class CDJScrobbler implements Runnable {
    static final Logger logger = LoggerFactory.getLogger(CDJScrobbler.class);

    // This is a composite config file loaded from the internal config, with the local --config overlaid.
    // The local config file is used for saving things like the Twitter and Last.fm API credentials.
    static final CDJScrobblerConfig config = new CDJScrobblerConfig();

    // This is the parsed config for the Orchestrator, derived from the config above.
    static final OrchestratorConfig oconfig = new OrchestratorConfig();

    static final CDJScrobbler theApplication = new CDJScrobbler();

    @Option(names = {"-L", "--lfm"}, description = "Enable Last.fm scrobbling")
    static boolean lfmEnabled = false;

    @Option(names = {"-T", "--twitter"}, description = "Enable tweeting the tracklist")
    static boolean twitterEnabled = false;

    @Option(names = {"--config"},
            paramLabel = "<filename>",
            description = "Which config file to use. Defaults to cdjscrobbler.properties in your home directory")
    static String confFile = System.getProperty("user.home") + File.separator + "cdjscrobbler.properties";

    @Option(names = {"--no-dmca-warning"},
            description = "Disable flashing the platter red if the loaded track would break DMCA rules")
    static boolean dmcOnAirWarningDisabled = false;

    public static void main(String[] args) throws Exception {

        // this stuff is necessary for the Last FM client
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        java.util.logging.Logger.getLogger("").setLevel(Level.FINEST);

        loadStaticConfig();
        new CommandLine(theApplication).execute(args);
    }

    private static void loadStaticConfig() throws IOException {
        // load default (internal) config
        config.load(CDJScrobbler.class.getClassLoader().getResourceAsStream("config.properties"));
    }

    private static void loadExternalConfig() throws IOException {

        // TODO: make fewer assumptions here, but this'll do for now!
        // We could reasonably have multiple instances of LastFmClient or TwitterClient, but there is currently
        // no way to configure multiple instances without doing it in code.

        try {
            // load e.g. Last.fm and Twitter keys and tokens
            logger.info("Loading local client configuration");
            config.load(); // from CDJScrobbler.confFile
        } catch (IOException ioe) {
            logger.error("Error loading config properties from {}", confFile, ioe);
            throw ioe;
        }

        String nowPlayingPoint = config.getProperty("cdjscrobbler.model.nowPlayingPointMs", "");
        String retryDelay = config.getProperty("cdjscrobbler.retryDelayMs", "500");


        String lfmEnabled = config.getProperty("cdjscrobbler.enable.lastfm", "false");
        String twitterEnabled = config.getProperty("cdjscrobbler.enable.twitter", "false");
        String dmcOnAirWarningEnabled = config.getProperty("dmcaaccountant.onairwarning.enabled", "true");

        if (nowPlayingPoint != null && !nowPlayingPoint.isEmpty()) {
            logger.info("Loaded Now Playing Point of {} ms", nowPlayingPoint);

            // TODO: move this into OrchestratorConfig?
            SongModel.setNowPlayingPoint(Integer.parseInt(nowPlayingPoint));
        }

        if (retryDelay != null && !retryDelay.isEmpty()) {
            logger.info("Loaded Retry Delay of {} ms", retryDelay);
            oconfig.setRetryDelay(Integer.parseInt(nowPlayingPoint));
        }

        if (Boolean.parseBoolean(lfmEnabled)) {
            oconfig.setLfmEnabled(true);
        } else {
            logger.warn("**************************************************************************************");
            logger.warn("* Scrobbling to Last.fm disabled. set cdjscrobbler.enable.lastfm=true in your config *");
            logger.warn("**************************************************************************************");
        }

        if (Boolean.parseBoolean(twitterEnabled)) {
            oconfig.setTwitterEnabled(true);
        } else {
            logger.warn("*********************************************************************************");
            logger.warn("* Tweeting tracks disabled. set cdjscrobbler.enable.twitter=true in your config *");
            logger.warn("*********************************************************************************");
        }

        if (Boolean.parseBoolean(dmcOnAirWarningEnabled)) {
            oconfig.setDmcaAccountantEnabled(true);
        } else {
            logger.warn("DMCA On Air Warning disabled in config. You will still see warnings in the log.");
        }
    }

    @Override
    public void run() {

        try {
            loadExternalConfig();
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // TODO: clean up this API by passing in LFM and Twitter configs directly

        oconfig.setFromProperties(config);

        // saved configuration is overridden by command line configuration

        // --lfm
        if(lfmEnabled)              oconfig.setLfmEnabled(true);

        // --twitter
        if(twitterEnabled)          oconfig.setTwitterEnabled(true);

        // --disable-dmca-warning
        if(dmcOnAirWarningDisabled) oconfig.setDmcaAccountantEnabled(false);

        Orchestrator o = new Orchestrator(oconfig);
        o.run();
    }


    static class VersionProvider implements CommandLine.IVersionProvider {

        @Override
        public String[] getVersion() throws Exception {
            String[] v = new String[1];
            v[0] = config.getProperty("cdjscrobbler.version");
            return v;
        }
    }
}
