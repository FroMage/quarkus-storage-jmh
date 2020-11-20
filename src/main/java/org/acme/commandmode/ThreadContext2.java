package org.acme.commandmode;

import io.smallrye.context.storage.ThreadLocalStorage;

public class ThreadContext2 {
    private final static ThreadLocalStorage<Integer> tl = new ThreadLocalStorage<>() {};
    
    public static void reset() {
        tl.remove();
    }
    
    public static void set(Integer val) {
        tl.set(val);
    }

    public static Integer get() {
        return tl.get();
    }
}
