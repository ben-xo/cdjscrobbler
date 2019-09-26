package am.xo.cdjscrobbler;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Session;
import de.umass.lastfm.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class LastFmClient {

    static final Logger logger = LoggerFactory.getLogger(LastFmClient.class);

    Session theSession;

    private String apiKey    = "";
    private String apiSecret = "";
    private String apiSk     = "";

    public LastFmClient(Properties config) {

        apiKey    = config.getProperty("lastfm.api.key", "");
        apiSecret = config.getProperty("lastfm.api.secret", "");
        apiSk     = config.getProperty("lastfm.api.sk", "");

        Caller.getInstance().setUserAgent(config.getProperty("cdjscrobbler.useragent", "CDJ Scrobbler"));
    }

    public void ensureUserIsConnected() throws IOException {

        if(apiKey.isEmpty() || apiSecret.isEmpty()) {
            String msg = "You need to put a Last.fm API key and API secret into your config. https://www.last.fm/api";
            logger.error("Connection to Last.fm failed: {}", msg);
            throw new IOException(msg);
        }

        if(apiSk.isEmpty()) {
            // trigger auth flow
            theSession = authorize(apiKey, apiSecret);
            apiSk = theSession.getKey();
            saveCredentials(apiSk);
        } else {
            logger.info("Restored Last.fm session from saved config");
            theSession = Session.createSession(apiKey, apiSecret, apiSk);
        }

        checkValidSession();
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
            Properties p = new Properties();
            p.setProperty("lastfm.api.sk", apiSk);
            FileOutputStream writer = new FileOutputStream(Application.localSessionFile);
            p.store(writer, null);
            writer.close();
        } catch (IOException ex) {
            logger.error("🚫 Saving credentials failed!", ex);
            // carry on anyway
        }
    }

    public void checkValidSession() throws IOException {
        User u = User.getInfo(theSession);
        if(u == null) {
            throw new IOException("Invalid last.fm session.");
        }
        logger.info("💃 Logged in to Last.fm as {}", u.getName());
    }

}
