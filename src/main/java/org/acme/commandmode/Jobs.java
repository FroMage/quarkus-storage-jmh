package org.acme.commandmode;

import javax.enterprise.event.Observes;
import javax.inject.Singleton;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.StartupEvent;

@Singleton
public class Jobs {

    public void startUp(@Observes StartupEvent startupEvent){
        System.err.println("STARTUP");
        System.err.println(Arc.class.getClassLoader());
        //Signal application has stared
        CPBenchmark.BenchmarkScope.startLatch.countDown();
    }
}