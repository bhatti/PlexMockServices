package com.plexobject.mock.domain;

import java.io.File;
import java.io.IOException;

import com.plexobject.mock.util.FileUtils;
import com.plexobject.mock.util.JSONUtils;
import com.plexobject.mock.util.TemplateTransformer;
import com.plexobject.mock.util.ThymeleafUtils;
import com.plexobject.mock.util.VelocityUtils;
import com.plexobject.mock.util.YAMLUtils;

/**
 * This enum defines common serialization format
 * 
 * @author shahzad bhatti
 *
 */

public enum ExportFormat {
    JSON, YAML, THYMELEAF, VELOCITY, TEXT;
    public static String[] EXTS = { ExportFormat.THYMELEAF.getExtension(),
            ExportFormat.VELOCITY.getExtension(),
            ExportFormat.YAML.getExtension(),
            ExportFormat.JSON.getExtension() };
    private ThymeleafUtils thymeleafUtils = new ThymeleafUtils();
    private TemplateTransformer _velocityUtils;

    public String getExtension() {
        switch (this) {
        case JSON:
            return ".json";
        case YAML:
            return ".yml";
        case VELOCITY:
            return ".vm";
        case THYMELEAF:
            return ".th";
        case TEXT:
            return ".txt";
        }
        return ".exp";
    }

    public void write(File path, Object value, Configuration config)
            throws IOException {
        if (value instanceof SerializationLifecycle) {
            ((SerializationLifecycle) value).beforeSerialize(config);
        }
        if (value instanceof byte[]) {
            FileUtils.write((byte[]) value, path);
        } else if (value instanceof String) {
            FileUtils.write(((String) value).getBytes(), path);
        } else {
            switch (this) {
            case JSON:
                JSONUtils.write(path, value);
                break;
            case YAML:
                YAMLUtils.write(path, value);
                break;
            case VELOCITY:
            case THYMELEAF:
            case TEXT:
                throw new IOException("Unsupported type " + value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T read(File path, Class<?> klass, MockRequest requestInfo,
            Configuration config) throws IOException {
        T value = null;
        if (path.getName().endsWith(THYMELEAF.getExtension())) {
            String contents = thymeleafUtils.transform(path.getAbsolutePath(),
                    config, requestInfo);
            if (contents.startsWith("{") || contents.startsWith("[")) {
                value = (T) JSONUtils.read(contents, klass);
            } else {
                value = (T) YAMLUtils.read(contents, klass);
            }
        } else if (path.getName().endsWith(VELOCITY.getExtension())) {
            String file = path.getAbsolutePath()
                    .replaceAll(config.getDataDir().getAbsolutePath(), "");
            String contents = getVelocityUtils(config).transform(file, config,
                    requestInfo);
            if (contents.startsWith("{") || contents.startsWith("[")) {
                value = (T) JSONUtils.read(contents, klass);
            } else {
                value = (T) YAMLUtils.read(contents, klass);
            }
        } else if (path.getName().endsWith(YAML.getExtension())) {
            value = (T) YAMLUtils.read(path, klass);
        } else if (path.getName().endsWith(JSON.getExtension())) {
            value = (T) JSONUtils.read(path, klass);
        } else if (path.getName().endsWith(TEXT.getExtension())) {
            value = (T) JSONUtils.read(path, klass);
        }
        if (value instanceof SerializationLifecycle) {
            ((SerializationLifecycle) value).afterDeserialize(config);
        }
        return value;
    }

    private synchronized TemplateTransformer getVelocityUtils(
            Configuration config) {
        if (_velocityUtils == null) {
            _velocityUtils = new VelocityUtils(config);
        }
        return _velocityUtils;
    }
}
