package am.xo.cdjscrobbler;

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
}
