package com.plexobject.mock.domain;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;

import org.apache.log4j.Logger;
import org.thymeleaf.util.StringUtils;

/**
 * This class defines common configuraiton
 * 
 * @author shahzad bhatti
 *
 */
public class Configuration {
    private static final Logger logger = Logger.getLogger(Configuration.class);

    private final int connectionTimeoutMillis;
    private final MockMode mockMode;
    private final String targetURL;
    private final File dataDir;
    private final boolean saveRawRequestResponses;
    private final boolean unserializeJsonContentBeforeSave;
    private final boolean saveAPIResponsesOnly;
    private final int injectFailuresAndWaitTimesPerc;
    private final int minWaitTimeMillis;
    private final int maxWaitTimeMillis;
    private final ExportFormat defaultExportFormat;

    public Configuration() throws IOException {
        Properties props = new Properties();
        props.load(getClass().getClassLoader()
                .getResourceAsStream("application.properties"));
        connectionTimeoutMillis = getInteger(props, "connectionTimeoutMillis");
        injectFailuresAndWaitTimesPerc = getInteger(props,
                "injectFailuresAndWaitTimesPerc");
        minWaitTimeMillis = getInteger(props, "minWaitTimeMillis");
        maxWaitTimeMillis = getInteger(props, "maxWaitTimeMillis");
        mockMode = MockMode
                .valueOf(getString(props, "mockMode", MockMode.PLAY.name())
                        .toUpperCase());
        saveRawRequestResponses = "true"
                .equals(getString(props, "saveRawRequestResponses", "false"));
        saveAPIResponsesOnly = "true"
                .equals(getString(props, "saveAPIResponsesOnly", "false"));
        unserializeJsonContentBeforeSave = "true".equals(
                getString(props, "unserializeJsonContentBeforeSave", "false"));
        dataDir = new File(getString(props, "dataDir", "data"));
        defaultExportFormat = ExportFormat.valueOf(
                getString(props, "defaultExportFormat", "YAML").toUpperCase());
        dataDir.mkdirs();

        targetURL = getTargetURL(props);

        if (mockMode == MockMode.RECORD
                && (targetURL == null || targetURL.length() == 0)) {
            throw new RuntimeException(
                    "Running in record mode but mock.target.url property is not set");
        }
    }

    public File find(String name) throws IOException {
        Optional<Path> path = Files
                .walk(Paths.get(getDataDir().getAbsolutePath()))
                .filter(Files::isRegularFile)
                .filter(f -> f.toFile().getName().equals(name)).findAny();
        return path.isPresent() ? path.get().toFile() : null;
    }

    public File toReadFile(MockRequest req) throws URISyntaxException {
        Random random = new Random();
        File dir = normalizeDir(new URI(req.getURL()), getDataDir());

        File[] files = findFiles(req, dir, false);
        if (files.length == 0) {
            files = findFiles(req, dir, true);
        }
        File file = files.length > 0 ? files[random.nextInt(files.length)]
                : null;
        logger.debug("Returning random read-only file " + file + " for " + req);
        return file;
    }

    private File[] findFiles(MockRequest req, File dir, boolean skipHash) {
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(req.getMethod().name())
                        && (skipHash || name.contains(req.getHash()))
                        && (StringUtils.isEmpty(req.getRequestId())
                                || name.contains(req.getRequestId()));
            }
        });
        return files;
    }

    public File toWriteFile(MockRequest req) throws URISyntaxException {
        File file = req.getExportFormat() == ExportFormat.TEXT
                ? new File(getDataDir(),
                        new URI(req.getURL()).getPath()
                                + req.getExportFormat().getExtension())
                : new File(normalizeDir(new URI(req.getURL()), getDataDir()),
                        req.getMethod().name() + "_" + req.getRequestId() + "_"
                                + req.getHash()
                                + req.getExportFormat().getExtension());
        logger.debug("Returning write-only file " + file + " for " + req);

        return file;
    }

    public File getNextIOCounterFile(MockRequest req)
            throws URISyntaxException {
        File dir = normalizeDir(new URI(req.getURL()), getDataDir());
        File file = new File(dir, req.getMethod().name() + "_"
                + req.getRequestId() + "_" + req.getHash() + ".dat");
        logger.debug("Returning io file " + file + " for " + req);

        return file;
    }

    public int getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    public MockMode getMockMode() {
        return mockMode;
    }

    public String getTargetURL() {
        return targetURL;
    }

    public File getDataDir() {
        return dataDir;
    }

    public int getInjectFailuresAndWaitTimesPerc() {
        return injectFailuresAndWaitTimesPerc;
    }

    public int getRandomFailuresAndWaitTimesPerc() {
        if (injectFailuresAndWaitTimesPerc > 0) {
            Random random = new Random();
            int value = random.nextInt(100);
            if (value <= injectFailuresAndWaitTimesPerc) {
                int delay = random
                        .nextInt(maxWaitTimeMillis - minWaitTimeMillis);
                if (delay == 0) {
                    delay = random.nextInt(1000);
                }
                return delay + minWaitTimeMillis;
            }
        }
        return 0;
    }

    public int getMinWaitTimeMillis() {
        return minWaitTimeMillis;
    }

    public int getMaxWaitTimeMillis() {
        return maxWaitTimeMillis;
    }

    public ExportFormat getDefaultExportFormat() {
        return defaultExportFormat;
    }

    public boolean isSaveRawRequestResponses() {
        return saveRawRequestResponses;
    }

    public boolean isUnserializeJsonContentBeforeSave() {
        return unserializeJsonContentBeforeSave;
    }

    public boolean isSaveAPIResponsesOnly() {
        return saveAPIResponsesOnly;
    }

    @Override
    public String toString() {
        return "Configuration [connectionTimeoutMillis="
                + connectionTimeoutMillis + ", mockMode=" + mockMode
                + ", targetURL=" + targetURL + ", dataDir=" + dataDir + "]";
    }

    //////////////////// PRIVATE METHODS //////////////////

    private static File normalizeDir(URI url, File dir) {
        if (!StringUtils.isEmpty(url.getPath())) {
            dir = new File(dir, url.getPath().replace("/",
                    System.getProperty("file.separator")));
        }
        dir.mkdirs();
        return dir;
    }

    private static String getString(Properties props, String name,
            String defValue) {
        return System.getProperty(name, props.getProperty(name, defValue));
    }

    private static String getTargetURL(Properties props) {
        String url = getString(props, Constants.TARGET_URL, "");
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new RuntimeException(
                    "Invalid mock.target.url property '" + url + "'");
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static int getInteger(Properties props, String name) {
        return Integer.parseInt(getString(props, name, "0"));
    }

}
