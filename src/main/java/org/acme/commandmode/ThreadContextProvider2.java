package org.acme.commandmode;

import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

public class ThreadContextProvider2 implements ThreadContextProvider {

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        Integer val = ThreadContext2.get();
        return () -> {
            Integer prev = ThreadContext2.get();
            ThreadContext2.set(val);
            return () -> {
                ThreadContext2.set(prev);
            };
        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        Integer val = null;
        return () -> {
            Integer prev = ThreadContext2.get();
            ThreadContext2.set(val);
            return () -> {
                ThreadContext2.set(prev);
            };
        };
    }

    @Override
    public String getThreadContextType() {
        return "Context2";
    }

}
