package com.plexobject.mock.util;

import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.plexobject.mock.domain.Configuration;
import com.plexobject.mock.domain.MockData;
import com.plexobject.mock.domain.MockRequest;

/**
 * This class abstracts Velocity templating engine
 * 
 * @author shahzad bhatti
 *
 */

public class VelocityUtils implements TemplateTransformer {
    private final VelocityEngine ve;

    public VelocityUtils(Configuration config) {
        Properties props = new Properties();
        props.setProperty("resource.loader", "file, class");
        props.setProperty("file.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
        props.setProperty("file.resource.loader.path",
                config.getDataDir().getAbsolutePath());
        props.setProperty("file.resource.loader.cache", "false");
        props.setProperty("file.resource.loader.modificationCheckInterval",
                "0");

        props.setProperty("class.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        props.setProperty("class.resource.loader.path",
                config.getDataDir().getAbsolutePath());
        //
        ve = new VelocityEngine(props);
        ve.init();
    }

    @Override
    public String transform(String file, Configuration config,
            MockRequest requestInfo) {
        Template t = ve.getTemplate(file);
        return doTransform(requestInfo, config, t);
    }

    private String doTransform(MockRequest requestInfo, Configuration config,
            Template t) {
        VelocityContext vc = new VelocityContext();
        vc.put("params", requestInfo.getParams());
        vc.put("headers", requestInfo.getHeaders());
        vc.put("url", requestInfo.getURL());
        vc.put("helper", new MockData(config));
        for (Map.Entry<java.lang.String, java.lang.String> e : requestInfo
                .getHeaders().entrySet()) {
            if (e.getValue().length() > 0) {
                vc.put(e.getKey(), e.getValue());
            }
        }
        for (Map.Entry<java.lang.String, java.lang.String> e : requestInfo
                .getParams().entrySet()) {
            if (e.getValue() != null) {
                vc.put(e.getKey(), e.getValue());
            }
        }
        StringWriter sw = new StringWriter();
        t.merge(vc, sw);
        return sw.toString();
    }
}
