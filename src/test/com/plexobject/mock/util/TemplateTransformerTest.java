package com.plexobject.mock.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.plexobject.mock.domain.Configuration;
import com.plexobject.mock.domain.MethodType;
import com.plexobject.mock.domain.MockRequest;
import com.plexobject.mock.domain.MockResponse;

public class TemplateTransformerTest {
    private Configuration config;
    private TemplateTransformer velocityUtils;
    private TemplateTransformer thymeleafUtils;
    private Map<String, String> headers = new HashMap<>();
    private Map<String, String> params = new HashMap<>();

    @Before
    public void setUp() throws Exception {
        config = new Configuration();
        velocityUtils = new VelocityUtils(config);
        thymeleafUtils = new ThymeleafUtils();
    }

    @Test
    public void testThymeleaf() throws IOException {
        params.put("mockResponseCode", "201");
        params.put("name", "jake");
        MockRequest req = new MockRequest(config, "http://localhost",
                MethodType.GET, headers, params, "content");
        File in = new File(new File("src/test/resources"), "template.th");
        File targetDir = new File(config.getDataDir() + "/test");
        targetDir.mkdirs();
        File out = new File(targetDir, "template.th");
        copy(in, out);
        String json = thymeleafUtils.transform(out.getAbsolutePath(), config,
                req);
        MockResponse resp = JSONUtils.unmarshal(json, MockResponse.class);
        Assert.assertEquals(201, resp.getResponseCode());
        Assert.assertEquals(1, resp.getHeaders().size());
        Assert.assertEquals("application/json; charset=utf-8",
                resp.getContentType());
        Map<?, ?> map = JSONUtils.unmarshal((String) resp.getContents(),
                Map.class);
        List<?> list = (List<?>) map.get("Devices");
        Assert.assertEquals(11, list.size());
    }

    @Test
    public void testVelocity() throws IOException {
        params.put("mockResponseCode", "200");
        params.put("name", "john");
        MockRequest req = new MockRequest(config, "http://localhost",
                MethodType.GET, headers, params, "content");
        File in = new File(new File("src/test/resources"), "template.vm");
        File targetDir = new File(config.getDataDir() + "/test");
        targetDir.mkdirs();
        File out = new File(targetDir, "template.vm");
        copy(in, out);
        String json = velocityUtils.transform("test/template.vm", config, req);
        MockResponse resp = JSONUtils.unmarshal(json, MockResponse.class);
        Assert.assertEquals(200, resp.getResponseCode());
        Assert.assertEquals(1, resp.getHeaders().size());
        Assert.assertEquals("application/json; charset=utf-8",
                resp.getContentType());
        Map<?, ?> map = JSONUtils.unmarshal((String) resp.getContents(),
                Map.class);
        List<?> list = (List<?>) map.get("Devices");
        Assert.assertEquals(11, list.size());
    }

    // helper method to copy file
    private static void copy(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }
}
