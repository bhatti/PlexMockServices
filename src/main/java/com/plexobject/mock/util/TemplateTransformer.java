package com.plexobject.mock.util;

import com.plexobject.mock.domain.Configuration;
import com.plexobject.mock.domain.MockRequest;

public interface TemplateTransformer {

    String transform(String file, Configuration config,
            MockRequest requestInfo);

}