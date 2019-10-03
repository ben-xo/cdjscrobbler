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

import am.xo.cdjscrobbler.SongEvents.NowPlayingEvent;
import am.xo.cdjscrobbler.SongEvents.ScrobbleEvent;
import de.umass.lastfm.*;
import de.umass.lastfm.scrobble.ScrobbleData;
import de.umass.lastfm.scrobble.ScrobbleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Component that is responsible for authenticating to Last.fm, and then scrobbling (as well as issuing
 * Now Playing updates).
 *
 * Call ensureUserIsConnected() to make sure we have a valid session, then updateNowPlaying() or scrobble() to
 * take action!
 *
 * Scrobbling has various rules about when it should be done - see https://www.last.fm/api/scrobbling
 */
public class LastFmClient {

    static final Logger logger = LoggerFactory.getLogger(LastFmClient.class);

    private Session theSession;
    private LastFmClientConfig config;

    public LastFmClient(LastFmClientConfig config) {
        this.config = config;
        Caller.getInstance().setUserAgent(config.getUserAgent());
    }

    /**
     * Gets us a valid session, or dies trying.
     *
     * If No API Key or API Secret (from https://www.last.fm/api) are configured, throws an exception. You will need
     * to put these in your cdjscrobbler.properties file.
     *
     * If a Session Key is loaded, it is tested for validity making one call to User.getInfo. If it not valid or none
     * was found, then the user is prompted to authorize against Last.fm. (If you wait too long this will eventually
     * time out).
     *
     * @throws IOException
     */
    public void ensureUserIsConnected() throws IOException {

        String apiKey = config.getApiKey();
        String apiSecret = config.getApiSecret();
        String apiSk = config.getApiSk();

        do {
            try {
                config.assertConfigured();
            } catch(IOException ioe) {
                logger.error("Connection to Last.fm failed: {}", ioe.getMessage());
                throw ioe;
            }

            if (apiSk.isEmpty()) {
                // trigger auth flow
                theSession = authorize(apiKey, apiSecret);
                apiSk = theSession.getKey();
                saveCredentials(apiSk);
            } else {
                logger.info("Restored Last.fm session from saved config");
                theSession = Session.createSession(apiKey, apiSecret, apiSk);
            }
        } while(!isSessionValid(theSession));
    }

    public Session authorize(String apiKey, String apiSecret) {
        String token = Authenticator.getToken(apiKey);

        Session session;
        int waitLoopCount = 0;
        while(waitLoopCount < 300) {
            // wait for user to authorize.

            // repeated in the loop because otherwise lastfm-java's logging scrolls it out of view.
            logger.info("💯 You must now visit https://www.last.fm/api/auth/?api_key={}&token={}", apiKey, token);

            try {
                logger.info("❓ Waiting for authorization…");
                Thread.sleep(5000);
            } catch(InterruptedException e) {
                // so what.
            }

            session = Authenticator.getSession(token, apiKey, apiSecret);
            if(session != null) {
                logger.info("✅ authorized to scrobble to https://www.last.fm/user/{}", session.getUsername());
                return session;
            }

            waitLoopCount++;
        }
        throw new RuntimeException("Gave up waiting for you to authorize to Last.fm.");
    }

    public void saveCredentials(String apiSk) {
        try {
            Application.config.setProperty("lastfm.api.sk", apiSk);
            ConfigFileUtil.save(Application.config, Application.localConfigFile);
        } catch (IOException ex) {
            logger.error("🚫 Saving credentials failed!", ex);
            // carry on anyway
        }
    }

    public boolean isSessionValid(Session theSession) throws IOException {
        User u = User.getInfo(theSession);
        if(u == null) {
            logger.warn("Invalid Last.fm session. Try again.");
            config.setApiSk(""); // bin-it
            return false;
        }
        logger.info("💃 Logged in to Last.fm as {}", u.getName());
        return true;
    }

    protected ScrobbleData getScrobbleDataFor(SongModel model) {
        SongDetails song = model.getSong();
        if(song == null) {
            // note that this may not be available if metadata lookup failed.
            return null;
        }
        int timestamp = (int) model.getStartedAt(); // cast down because it was originally a long
        ScrobbleData theScrobble = new ScrobbleData(song.getArtist(), song.getTitle(), timestamp);
        theScrobble.setAlbum(song.getAlbum());
        theScrobble.setDuration(song.getDuration());
        return theScrobble;
    }

    public void updateNowPlaying(NowPlayingEvent npe) {
        SongModel model = npe.model;
        ScrobbleData theScrobble = getScrobbleDataFor(model);
        if(theScrobble != null) {
            ScrobbleResult result = Track.updateNowPlaying(theScrobble, theSession);
            if(result == null) {
                // todo: retry
                logger.error("🚫 failed to update 'now playing' {}", model.getSong());
            } else {
                logger.info("🎸 now playing {}", model.getSong());
            }
        }
    }

    public void scrobble(ScrobbleEvent e) {
        SongModel model = e.model;
        ScrobbleData theScrobble = getScrobbleDataFor(model);
        if(theScrobble != null) {
            ScrobbleResult result = Track.scrobble(theScrobble, theSession);
            if(result == null) {
                // todo: retry
                logger.error("🚫 failed to scrobble {}", model.getSong());
            } else {
                logger.info("✨ scrobbled {}", model.getSong());
            }
        }
    }
}
