package am.xo.cdjscrobbler;

import am.xo.cdjscrobbler.SongEvents.NowPlayingEvent;
import com.github.scribejava.apis.TwitterApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.*;
import com.github.scribejava.core.oauth.OAuth10aService;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class TwitterClient {

    private static final String PROTECTED_RESOURCE_URL = "https://api.twitter.com/1.1/account/verify_credentials.json";

    static final Logger logger = LoggerFactory.getLogger(TwitterClient.class);

    private TwitterClientConfig config;
    private OAuth1AccessToken accessToken;
    private boolean debug = false;

    public TwitterClient(TwitterClientConfig config) {
        this.config = config;
    }

    public void ensureUserIsConnected() throws IOException {

        String OAuthAccessToken, OAuthAccessTokenSecret;

        try {
            config.assertConfigured();
        } catch(IOException ioe) {
            logger.error("Connection to Twitter failed: {}", ioe.getMessage());
            throw ioe;
        }

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
            Application.config.setProperty("twitter4j.oauth.accessToken", OAuthAccessToken);
            Application.config.setProperty("twitter4j.oauth.accessTokenSecret", OAuthAccessTokenSecret);
            ConfigFileUtil.save(Application.config, Application.localConfigFile);
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

    public void sendNowPlaying(NowPlayingEvent npe) {
        // TODO: finish
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
