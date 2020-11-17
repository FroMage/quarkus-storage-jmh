package org.acme.commandmode;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// jmh.executor must be set to CUSTOM
// jmh.executor.class must point to this class
public class StorageExecutor extends ThreadPoolExecutor {
    
    public StorageExecutor(int maxThreads, String prefix) {
        super(maxThreads, maxThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new StorageThreadFactory(prefix));
        System.err.println("USING STORAGE EXECUTOR");
    }
}
