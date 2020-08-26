package com.plexobject.mock.domain;

import java.io.File;
import java.io.IOException;

import com.plexobject.mock.util.JSONUtils;
import com.plexobject.mock.util.ThymeleafUtils;
import com.plexobject.mock.util.VelocityUtils;
import com.plexobject.mock.util.YAMLUtils;

public enum ExportFormat {
    JSON, YAML, Thymeleaf, Velocity;
    public static String[] EXTS = { ExportFormat.Thymeleaf.getExtension(),
            ExportFormat.Velocity.getExtension(),
            ExportFormat.YAML.getExtension(),
            ExportFormat.JSON.getExtension() };
    private ThymeleafUtils thymeleafUtils = new ThymeleafUtils();
    private VelocityUtils _velocityUtils;

    public String getExtension() {
        switch (this) {
        case JSON:
            return ".json";
        case YAML:
            return ".yml";
        case Velocity:
            return ".vm";
        case Thymeleaf:
            return ".th";
        }
        return ".exp";
    }

    public void write(File path, Object value) throws IOException {
        if (value instanceof SerializationLifecycle) {
            ((SerializationLifecycle) value).beforeSerialize();
        }
        switch (this) {
        case JSON:
            JSONUtils.write(path, value);
            break;
        case YAML:
        case Velocity:
        case Thymeleaf:
            YAMLUtils.write(path, value);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T read(File path, Class<?> klass, MockRequest requestInfo,
            Configuration config) throws IOException {
        T value = null;
        if (path.getName().endsWith(Thymeleaf.getExtension())) {
            String contents = thymeleafUtils.transform(path, requestInfo);
            value = (T) YAMLUtils.read(contents, klass);
        } else if (path.getName().endsWith(Velocity.getExtension())) {
            String contents = getVelocityUtils(config).transform(path,
                    requestInfo);
            value = (T) YAMLUtils.read(contents, klass);
        } else if (path.getName().endsWith(YAML.getExtension())) {
            value = (T) YAMLUtils.read(path, klass);
        } else if (path.getName().endsWith(JSON.getExtension())) {
            value = (T) JSONUtils.read(path, klass);
        }
        if (value instanceof SerializationLifecycle) {
            ((SerializationLifecycle) value).afterDeserialize();
        }
        return value;
    }

    private synchronized VelocityUtils getVelocityUtils(Configuration config) {
        if (_velocityUtils == null) {
            _velocityUtils = new VelocityUtils(config);
        }
        return _velocityUtils;
    }
}
