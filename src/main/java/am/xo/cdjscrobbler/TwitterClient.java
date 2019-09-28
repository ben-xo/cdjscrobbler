package am.xo.cdjscrobbler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.io.IOException;

public class TwitterClient {

    static final Logger logger = LoggerFactory.getLogger(TwitterClient.class);

    private TwitterClientConfig config;
    private boolean debug = false;

    public TwitterClient(TwitterClientConfig config) {
        this.config = config;
    }

    public void ensureUserIsConnected() throws IOException {

        String consumerKey = config.getOAuthConsumerKey();
        String consumerSecret = config.getOAuthConsumerSecret();

//        do {
            try {
                config.assertConfigured();
            } catch(IOException ioe) {
                logger.error("Connection to Twitter failed: {}", ioe.getMessage());
                throw ioe;
            }

//            if (apiSk.isEmpty()) {
//                // trigger auth flow
//                theSession = authorize(apiKey, apiSecret);
//                apiSk = theSession.getKey();
//                saveCredentials(apiSk);
//            } else {
//                logger.info("Restored Last.fm session from saved config");
//                theSession = Session.createSession(apiKey, apiSecret, apiSk);
//            }
//        } while(!isSessionValid(theSession));
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
