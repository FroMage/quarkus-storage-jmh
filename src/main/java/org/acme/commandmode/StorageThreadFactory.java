package org.acme.commandmode;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class StorageThreadFactory implements ThreadFactory {

    private final AtomicInteger counter;
    private final String prefix;

    public StorageThreadFactory(String prefix) {
        this.counter = new AtomicInteger();
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        System.err.println("USING STORAGE THREAD");
        Thread thread = new StorageThread();
        thread.setName(prefix + "-jmh-worker-" + counter.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    }
}
