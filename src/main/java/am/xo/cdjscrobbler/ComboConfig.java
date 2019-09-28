package am.xo.cdjscrobbler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Class that encapsulates several config files, with "latest wins" semantics
 */
public class ComboConfig extends Properties {

    private List<Properties> configs = new ArrayList<>();

    public ComboConfig() {}

    public void add(Properties p) {
        configs.add(p);
    }

    /**
     * Loads a config file and adds it to the FRONT of the list - on the assumption that the last loaded file
     * is the one most likely to contain overrides
     *
     * @param inStream
     * @throws IOException
     */
    @Override
    public synchronized void load(InputStream inStream) throws IOException {
        Properties p = new Properties();
        p.load(inStream);
        configs.add(0, p);
    }

    /**
     * Saves the most recently loaded config file.
     *
     * @param out
     * @param comments
     * @throws IOException
     */
    @Override
    public void store(OutputStream out, String comments)
            throws IOException {
        configs.get(0).store(out, comments);
    }


    /**
     * Returns the first value found in the collection of loaded configs.
     * Ignores null or empty values.
     *
     * @param key
     * @param defaultValue
     * @return
     */
    @Override
    public String getProperty(String key, String defaultValue) {
        for(Properties p: configs) {
            String val = p.getProperty(key);
            if(val != null && !val.isEmpty()) {
                return val;
            }
        }
        return defaultValue;
    }

    @Override
    public String getProperty(String key) {
        return getProperty(key, null);
    }

    /**
     * Saves the value over the first-found instance of that key.
     *
     * @param key
     * @param value
     * @return
     */
    @Override
    public synchronized Object setProperty(String key, String value) {
        for(Properties p: configs) {
            String prevVal = p.getProperty(key);
            if(prevVal != null && !prevVal.isEmpty()) {
                return p.setProperty(key, value);
            }
        }

        // if we got here, no existing config has the element - so we add it to the first one.
        return configs.get(0).setProperty(key, value);
    }
}
