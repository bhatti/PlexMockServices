package com.plexobject.mock.domain;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.servlet.ServletConfig;

public class Configuration {
    public static final String YAML = ".yml";
    public static final String VELOCITY = ".vm";

    private final int maxSamples;
    private final int connectionTimeoutMillis;
    private final boolean recordMode;
    private final String urlPrefix;
    private final File dataDir;
    private final boolean randomResponseOrder;
    private final int injectFailuresAndWaitTimesPerc;
    private final int minWaitTimeMillis;
    private final int maxWaitTimeMillis;
    private Map<String, Integer> readCounters = new HashMap<>();
    private Map<String, Integer> writeCounters = new HashMap<>();

    public Configuration(ServletConfig servletConfig) {
        connectionTimeoutMillis = Integer.parseInt(servletConfig.getInitParameter("connectionTimeoutMillis"));
        maxSamples = Integer.parseInt(servletConfig.getInitParameter("maxSamples"));
        injectFailuresAndWaitTimesPerc = Integer
                .parseInt(servletConfig.getInitParameter("injectFailuresAndWaitTimesPerc"));
        minWaitTimeMillis = Integer.parseInt(servletConfig.getInitParameter("minWaitTimeMillis"));
        maxWaitTimeMillis = Integer.parseInt(servletConfig.getInitParameter("maxWaitTimeMillis"));
        recordMode = "true".equals(servletConfig.getInitParameter("recordMode"));
        randomResponseOrder = "true".equals(servletConfig.getInitParameter("randomResponseOrder"));
        dataDir = new File(servletConfig.getInitParameter("dataDir"));
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
                File yamlFile = new File(getDataDir(), name + "_" + counter + YAML);
                File velocityFile = new File(getDataDir(), name + "_" + counter + VELOCITY);
                if (yamlFile.exists()) {
                    return yamlFile;
                } else if (velocityFile.exists()) {
                    return velocityFile;
                } else if (counter > 1) {
                    readCounters.remove(name);
                    return toFile(url, methodType, readOnly);
                }
                return null;
            }
        } else {
            int counter = getNextCounter(writeCounters, name);
            return new File(getDataDir(), name + "_" + counter + YAML);
        }
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
}
