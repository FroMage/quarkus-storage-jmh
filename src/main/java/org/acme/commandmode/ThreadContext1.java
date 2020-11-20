package org.acme.commandmode;

import io.smallrye.context.storage.ThreadLocalStorage;

public class ThreadContext1 {
    private final static ThreadLocalStorage<String> tl = new ThreadLocalStorage<>() {};
    
    public static void reset() {
        tl.remove();
    }
    
    public static void set(String val) {
        tl.set(val);
    }

    public static String get() {
        return tl.get();
    }
}
