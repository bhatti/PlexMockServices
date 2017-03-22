package com.plexobject.mock.util;

import java.io.File;
import java.io.StringWriter;
import java.util.Map;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.AbstractContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import com.plexobject.mock.domain.RequestInfo;

public class ThymeleafUtils {
    static class ParamsContext extends AbstractContext {
    }

    private final TemplateEngine templateEngine = new TemplateEngine();

    public ThymeleafUtils() {
        FileTemplateResolver templateResolver = new FileTemplateResolver();
        templateResolver.setOrder(Integer.valueOf(1));
        templateResolver.setCheckExistence(true);
        templateResolver.setCacheable(false);
        templateResolver.setTemplateMode(TemplateMode.TEXT);

        templateEngine.setTemplateResolver(templateResolver);
    }

    public String transform(File file, RequestInfo requestInfo) {
        ParamsContext ctx = new ParamsContext();
        ctx.setVariable("params", requestInfo.getParams());
        ctx.setVariable("headers", requestInfo.getHeaders());
        ctx.setVariable("url", requestInfo.getUrl());
        for (Map.Entry<java.lang.String, java.lang.String> e : requestInfo.getHeaders().entrySet()) {
            if (e.getValue().length() > 0) {
                ctx.setVariable(e.getKey(), e.getValue());
            }
        }

        for (Map.Entry<java.lang.String, java.lang.String> e : requestInfo.getParams().entrySet()) {
            if (e.getValue() != null) {
                ctx.setVariable(e.getKey(), e.getValue());
            }
        }

        StringWriter sw = new StringWriter();
        templateEngine.process(file.getAbsolutePath(), ctx, sw);
        System.out.println("\n\n " + sw + "\n\n");
        return sw.toString();
    }

}
