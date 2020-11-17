package org.acme.commandmode;

import io.quarkus.runtime.storage.QuarkusStorageManager;
import io.quarkus.runtime.storage.QuarkusThread;
import io.quarkus.runtime.storage.QuarkusThreadContext;

public class StorageThread extends Thread implements QuarkusThread {

    private final QuarkusThreadContext contexts = QuarkusStorageManager.instance().newContext();

    @Override
    public QuarkusThreadContext getQuarkusThreadContext() {
        return contexts;
    }


}
