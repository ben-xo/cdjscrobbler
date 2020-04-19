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

package am.xo.cdjscrobbler.Plugins;

import am.xo.cdjscrobbler.ConfigException;
import am.xo.cdjscrobbler.SongDetails;
import am.xo.cdjscrobbler.SongEventListeners.NowPlayingListener;
import am.xo.cdjscrobbler.SongEvents.NowPlayingEvent;
import com.github.scribejava.apis.TwitterApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

/**
 * Component that is responsible for authenticating to Twitter, and then tweeting the now playing tracks.
 *
 * Call ensureUserIsConnected() to make sure we have a valid session, then sendNowPlaying to take action!
 *
 */
public class TwitterClient implements NowPlayingListener {

    private static final String PROTECTED_RESOURCE_URL = "https://api.twitter.com/1.1/account/verify_credentials.json";

    static final Logger logger = LoggerFactory.getLogger(TwitterClient.class);

    private TwitterClientConfig config;
    private OAuth1AccessToken accessToken;
    private boolean debug = false;

    public TwitterClient(TwitterClientConfig config) {
        this.config = config;
    }

    /**
     * Gets us a valid session, or dies trying.
     *
     * If No Consume Key or Consume Secret (from https://developer.twitter.com) are configured, throws an exception.
     * You will need to put these in your cdjscrobbler.properties file.
     *
     * If an Access Token and Access Token Secret are loaded, they is tested for validity making one call to
     * Twitter's account/verify_credentials method. If they are not valid or none were found, then the user is prompted
     * to authorize against Twitter. The user must paste the code back into the terminal.
     *
     * TODO: other ways to authorize that don't involve pasting into the terminal.
     *
     * @throws IOException
     */
    public void ensureUserIsConnected() throws IOException, ConfigException {

        String OAuthAccessToken, OAuthAccessTokenSecret;

        config.assertConfigured();

        do {
            OAuthAccessToken = config.getOAuthAccessToken();
            OAuthAccessTokenSecret = config.getOAuthAccessTokenSecret();

            if (OAuthAccessToken.isEmpty() || OAuthAccessTokenSecret.isEmpty()) {
                // trigger auth flow
                try {
                    accessToken = authorize();
                } catch (Exception e) {
                    String msg = "❌ Failed to authorize with Twitter.";
                    logger.error(msg, e);
                    throw new IOException(msg, e);
                }
                OAuthAccessToken = accessToken.getToken();
                OAuthAccessTokenSecret = accessToken.getTokenSecret();
                saveCredentials(OAuthAccessToken, OAuthAccessTokenSecret);
            } else {
                logger.info("Restored Twitter session from saved config");
                accessToken = new OAuth1AccessToken(OAuthAccessToken, OAuthAccessTokenSecret);
            }
        } while(!isSessionValid(accessToken));
    }

    public void saveCredentials(String OAuthAccessToken, String OAuthAccessTokenSecret) {
        try {
            config.setOauthAccessToken(OAuthAccessToken);
            config.setOauthAccessTokenSecret(OAuthAccessTokenSecret);
            config.save();
        } catch (IOException ex) {
            logger.error("🚫 Saving credentials failed!", ex);
            // carry on anyway
        }
    }

    protected boolean isSessionValid(OAuth1AccessToken accessToken) throws IOException {
        // https://developer.twitter.com/en/docs/accounts-and-users/manage-account-settings/api-reference/get-account-verify_credentials
        final OAuth10aService service = getTwitterOAuthService();
        final OAuthRequest request = new OAuthRequest(Verb.GET, PROTECTED_RESOURCE_URL);
        service.signRequest(accessToken, request);
        boolean isValid = false;
        try (Response response = service.execute(request)) {
            isValid = response.isSuccessful();
        } catch(InterruptedException | ExecutionException e) {
            logger.warn("Invalid Twitter session. Try again.");
        }
        if(!isValid) {
            // invalid token? bin-it
            config.setOauthAccessToken("");
            config.setOauthAccessTokenSecret("");
        }
        return isValid;
    }

    protected OAuth10aService getTwitterOAuthService() {
        return new ServiceBuilder(config.getOAuthConsumerKey())
                .apiSecret(config.getOAuthConsumerSecret())
                .build(TwitterApi.instance());
    }

    protected OAuth1AccessToken authorize() throws IOException, InterruptedException, ExecutionException {
        // see https://github.com/scribejava/scribejava/blob/master/scribejava-apis/src/test/java/com/github/scribejava/apis/examples/TwitterExample.java

        final OAuth10aService service = getTwitterOAuthService();
        final Scanner in = new Scanner(System.in);

        // Obtain the Request Token
        System.out.println("Fetching the Request Token...");
        final OAuth1RequestToken requestToken = service.getRequestToken();
        System.out.println("Got the Request Token!");
        System.out.println();

        System.out.println("Now go and authorize CDJ Scrobbler here:");
        System.out.println(service.getAuthorizationUrl(requestToken));
        System.out.println("And paste the verifier here");
        System.out.print(">>");
        final String oauthVerifier = in.nextLine();
        System.out.println();

        // Trade the Request Token and Verfier for the Access Token
        System.out.println("Trading the Request Token for an Access Token...");
        final OAuth1AccessToken accessToken = service.getAccessToken(requestToken, oauthVerifier);
        System.out.println("Got the Access Token!");

        return accessToken;
    }

    @Override
    public void nowPlaying(NowPlayingEvent npe) {
        Twitter twitter = getTwitterFromConfig();
        SongDetails song = npe.model.getSong();
        String[] params = { song.getFullTitle() };
        try {
            twitter.tweets().updateStatus(MessageFormatter.arrayFormat(config.getTweetTemplate(), params).getMessage());
            logger.info("🎸 now playing {}", song);
        } catch(TwitterException e) {
            logger.error("🚫 failed to tweet 'now playing': {}", e.getMessage());
        }
    }

    /**
     * Get a Twitter instance based on the supplied config.
     * Example based on http://twitter4j.org/en/configuration.html
     *
     * @return Twitter the Twitter4J client
     */
    protected Twitter getTwitterFromConfig() {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(debug)
                .setOAuthConsumerKey(config.getOAuthConsumerKey())
                .setOAuthConsumerSecret(config.getOAuthConsumerSecret())
                .setOAuthAccessToken(config.getOAuthAccessToken())
                .setOAuthAccessTokenSecret(config.getOAuthAccessTokenSecret())
        ;
        TwitterFactory tf = new TwitterFactory(cb.build());
        return tf.getInstance();
    }
}
