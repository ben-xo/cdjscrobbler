package am.xo.cdjscrobbler;

import java.io.IOException;
import java.util.Properties;

public class TwitterClientConfig {

    private String oauthConsumerKey;
    private String oauthConsumerSecret;
    private String oauthAccessToken;
    private String oauthAccessTokenSecret;
    private String userAgent;

    public TwitterClientConfig(Properties config) {
        oauthConsumerKey       = config.getProperty("twitter4j.oauth.consumerKey","");
        oauthConsumerSecret    = config.getProperty("twitter4j.oauth.consumerSecret", "");
        oauthAccessToken       = config.getProperty("twitter4j.oauth.accessToken","");
        oauthAccessTokenSecret = config.getProperty("twitter4j.oauth.accessTokenSecret", "");
        userAgent              = config.getProperty("cdjscrobbler.useragent", "CDJ Scrobbler");
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getOAuthConsumerKey() {
        return oauthConsumerKey;
    }

    public String getOAuthConsumerSecret() {
        return oauthConsumerSecret;
    }

    public String getOAuthAccessToken() {
        return oauthAccessToken;
    }

    public String getOAuthAccessTokenSecret() {
        return oauthAccessTokenSecret;
    }

    public void assertConfigured() throws IOException {
        if (oauthConsumerKey.isEmpty() || oauthConsumerSecret.isEmpty()) {
            String msg = "You need to put a Last.fm API key and API secret into your config. https://www.last.fm/api";
            throw new IOException(msg);
        }
    }
}
