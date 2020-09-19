package com.plexobject.mock.util;

import java.io.StringWriter;
import java.util.Map;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.AbstractContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import com.plexobject.mock.domain.Configuration;
import com.plexobject.mock.domain.MockData;
import com.plexobject.mock.domain.MockRequest;

/**
 * This class uses Thymeleaf library for template output response
 * 
 * @author shahzad bhatti
 *
 */

public class ThymeleafUtils implements TemplateTransformer {
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

    @Override
    public String transform(String file, Configuration config,
            MockRequest requestInfo) {
        ParamsContext ctx = new ParamsContext();
        ctx.setVariable("params", requestInfo.getParams());
        ctx.setVariable("headers", requestInfo.getHeaders());
        ctx.setVariable("url", requestInfo.getURL());
        ctx.setVariable("helper", new MockData(config));
        for (Map.Entry<java.lang.String, java.lang.String> e : requestInfo
                .getHeaders().entrySet()) {
            if (e.getValue().length() > 0) {
                ctx.setVariable(e.getKey(), e.getValue());
            }
        }

        for (Map.Entry<java.lang.String, java.lang.String> e : requestInfo
                .getParams().entrySet()) {
            if (e.getValue() != null) {
                ctx.setVariable(e.getKey(), e.getValue());
            }
        }

        StringWriter sw = new StringWriter();
        templateEngine.process(file, ctx, sw);
        return sw.toString();
    }

}
