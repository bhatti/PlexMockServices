package com.plexobject.mock.util;

import com.plexobject.mock.domain.Configuration;
import com.plexobject.mock.domain.MockRequest;

/**
 * This interface defines method for creating output response using templating
 * 
 * @author shahzad bhatti
 *
 */

public interface TemplateTransformer {

    String transform(String file, Configuration config,
            MockRequest requestInfo);

}