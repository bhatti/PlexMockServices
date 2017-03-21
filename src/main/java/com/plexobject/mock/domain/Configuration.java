package com.plexobject.mock.domain;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.servlet.ServletConfig;

public class Configuration {
    private final int maxSamples;
    private final int connectionTimeoutMillis;
    private final boolean recordMode;
    private final String urlPrefix;
    private final File dataDir;
    private final boolean randomResponseOrder;
    private final int injectFailuresAndWaitTimesPerc;
    private final int minWaitTimeMillis;
    private final int maxWaitTimeMillis;
    private final ExportFormat defaultExportFormat;
    private Map<String, Integer> readCounters = new HashMap<>();
    private Map<String, Integer> writeCounters = new HashMap<>();

    public Configuration(ServletConfig servletConfig) {
        connectionTimeoutMillis = getInteger(servletConfig, "connectionTimeoutMillis");
        maxSamples = getInteger(servletConfig, "maxSamples");
        injectFailuresAndWaitTimesPerc = getInteger(servletConfig, "injectFailuresAndWaitTimesPerc");
        minWaitTimeMillis = getInteger(servletConfig, "minWaitTimeMillis");
        maxWaitTimeMillis = getInteger(servletConfig, "maxWaitTimeMillis");
        recordMode = "true".equals(getString(servletConfig, "recordMode", "true"));
        randomResponseOrder = "true".equals(getString(servletConfig, "randomResponseOrder", "false"));
        dataDir = new File(getString(servletConfig, "dataDir", "data"));
        defaultExportFormat = ExportFormat
                .valueOf(getString(servletConfig, "defaultExportFormat", "YAML").toUpperCase());
        dataDir.mkdirs();

        urlPrefix = getUrlPrefix(servletConfig);

        if (recordMode && (urlPrefix == null || urlPrefix.length() == 0)) {
            throw new RuntimeException("Running in record mode but mock.target.url property is not set");
        }
    }

    public File toFile(String url, MethodType methodType, boolean readOnly) {
        Random random = new Random();
        final String name = normalizeName(url, methodType.name());
        if (readOnly) {
            if (isRandomResponseOrder()) {
                File[] files = getDataDir().listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.startsWith(name);
                    }
                });
                return files != null && files.length > 0 ? files[random.nextInt(files.length)] : null;
            } else {
                int counter = getNextCounter(readCounters, name);
                return getFileIfExists(name, counter);
            }
        } else {
            int counter = getNextCounter(writeCounters, name);
            return new File(getDataDir(), name + "_" + counter + getDefaultExportFormat().getExtension());
        }
    }

    private File getFileIfExists(String name, int counter) {
        for (String ext : ExportFormat.EXTS) {
            File file = new File(getDataDir(), name + "_" + counter + ext);
            if (file.exists()) {
                return file;
            }
        }
        if (counter > 1) {
            counter = 1;
            readCounters.remove(name);
            return getFileIfExists(name, counter);
        }
        return null;
    }

    public int getMaxSamples() {
        return maxSamples;
    }

    public int getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    public boolean isRecordMode() {
        return recordMode;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public File getDataDir() {
        return dataDir;
    }

    public boolean isRandomResponseOrder() {
        return randomResponseOrder;
    }

    public int getInjectFailuresAndWaitTimesPerc() {
        return injectFailuresAndWaitTimesPerc;
    }

    public int getRandomFailuresAndWaitTimesPerc() {
        if (injectFailuresAndWaitTimesPerc > 0) {
            Random random = new Random();
            int value = random.nextInt(100);
            if (value <= injectFailuresAndWaitTimesPerc) {
                int delay = random.nextInt(maxWaitTimeMillis - minWaitTimeMillis);
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

    @Override
    public String toString() {
        return "Configuration [maxSamples=" + maxSamples + ", connectionTimeoutMillis=" + connectionTimeoutMillis
                + ", recordMode=" + recordMode + ", urlPrefix=" + urlPrefix + ", dataDir=" + dataDir
                + ", randomResponseOrder=" + randomResponseOrder + "]";
    }

    private int getNextCounter(Map<String, Integer> counters, String name) {
        Integer counter = counters.get(name);
        if (counter == null) {
            counter = 1;
        } else {
            counter = counter + 1;
        }
        if (counter > getMaxSamples()) {
            counter = 1;
        }
        counters.put(name, counter);
        return counter;
    }

    private static String normalizeName(String url, String operation) {
        int q = url.indexOf("?");
        if (q != -1) {
            url = url.substring(0, q);
        }
        if (url.startsWith("/")) {
            url = url.substring(1);
        }
        url = url.replaceAll("[\\&\\/\\?:;,\\s]", "_");

        return url.length() > 0 ? operation + "_" + url : operation;
    }

    private static String getUrlPrefix(ServletConfig servletConfig) {
        String urlPrefix = servletConfig.getInitParameter("urlPrefix");
        if (!urlPrefix.startsWith("http://") && !urlPrefix.startsWith("https://")) {
            throw new RuntimeException("Invalid mock.target.url property '" + urlPrefix + "'");
        }
        if (urlPrefix.endsWith("/")) {
            urlPrefix = urlPrefix.substring(0, urlPrefix.length() - 1);
        }
        return urlPrefix;
    }

    private static String getString(ServletConfig servletConfig, String name, String defValue) {
        String value = servletConfig.getInitParameter(name);
        if (value != null && value.length() > 0) {
            return value;
        }
        return defValue;
    }

    private static int getInteger(ServletConfig servletConfig, String name) {
        String value = servletConfig.getInitParameter(name);
        if (value != null && value.length() > 0) {
            return Integer.parseInt(value.trim());
        }
        return 0;
    }
}
