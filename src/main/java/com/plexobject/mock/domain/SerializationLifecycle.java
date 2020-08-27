package com.plexobject.mock.domain;

import java.io.IOException;

/**
 * This interface defines serialization callback methods
 * 
 * @author shahzad bhatti
 *
 */
public interface SerializationLifecycle {

    void beforeSerialize(Configuration config) throws IOException;

    void afterDeserialize(Configuration config) throws IOException;
}
