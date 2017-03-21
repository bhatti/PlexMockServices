package com.plexobject.mock.domain;

import java.io.IOException;

public interface SerializationLifecycle {
    void beforeSerialize() throws IOException;

    void afterDeserialize()throws IOException;
}
