package org.acme.commandmode;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

public class ThreadContextProvider1 implements ThreadContextProvider {

    AtomicInteger counter = new AtomicInteger();
    
    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        int ct = counter.incrementAndGet();
        String val = ThreadContext1.get();
        return () -> {
            String prev = ThreadContext1.get();
            ThreadContext1.set(val);
            return () -> {
                ThreadContext1.set(prev);
            };
        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        String val = null;
        return () -> {
            String prev = ThreadContext1.get();
            ThreadContext1.set(val);
            return () -> {
                ThreadContext1.set(prev);
            };
        };
    }

    @Override
    public String getThreadContextType() {
        return "Context1";
    }

}
