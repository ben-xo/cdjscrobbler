package am.xo.cdjscrobbler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class ConfigFileUtil {

    public static Properties maybeLoad(Properties config, String filename) {
        try (InputStream is = Files.newInputStream(Paths.get(filename))) {
            config.load(is);
        } catch (IOException ioe) {
            // don't care. (If you care, use load())
        }
        return config;
    }

    public static Properties load(Properties config, String filename) throws IOException {
        try (InputStream is = Files.newInputStream(Paths.get(filename))) {
            config.load(is);
        }
        return config;
    }

    public static void save(Properties config, String filename) throws IOException {
        try (FileOutputStream writer = new FileOutputStream(filename)) {
            config.store(writer, null);
        }
    }
}
