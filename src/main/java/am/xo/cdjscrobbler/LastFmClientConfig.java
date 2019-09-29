package am.xo.cdjscrobbler;

import java.io.IOException;
import java.util.Properties;

public class LastFmClientConfig {

    private String apiKey    = "";
    private String apiSecret = "";
    private String apiSk     = "";
    private String userAgent = "";

    public LastFmClientConfig(Properties config) {
        apiKey    = config.getProperty("lastfm.api.key", "");
        apiSecret = config.getProperty("lastfm.api.secret", "");
        apiSk     = config.getProperty("lastfm.api.sk", "");
        userAgent = config.getProperty("cdjscrobbler.useragent", "CDJ Scrobbler");
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getApiSk() {
        return apiSk;
    }

    public void setApiSk(String apiSk) {
        this.apiSk = apiSk;
    }

    public void assertConfigured() throws IOException {
        if (apiKey.isEmpty() || apiSecret.isEmpty()) {
            String msg = "You need to put a Last.fm API key and API secret into your config. https://www.last.fm/api";
            throw new IOException(msg);
        }
    }
}
