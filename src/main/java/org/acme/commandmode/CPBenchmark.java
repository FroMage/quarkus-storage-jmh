package org.acme.commandmode;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.Blackhole;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.runtime.Quarkus;
import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

public class CPBenchmark {

//    @Benchmark
//    @Threads(20)
//    public void testWorkerThreadSingleCallback(Blackhole blackhole,
//                                               ThreadScope threadScope,
//                                               BenchmarkScope benchmarkScope) throws InterruptedException, ExecutionException  {
//        ManagedContext requestContext = Arc.container().requestContext();
//        // one contextual checker
//        Runnable checker = benchmarkScope.threadContext.contextualRunnable(() -> {
//            if(Arc.container().requestContext() != requestContext)
//                throw new IllegalStateException("Context not properly propagated");
//        });
//        // 5 contextual runners
//        Runnable[] runners = new Runnable[5];
//        for(int i=0;i<runners.length;i++) {
//            runners[i] = benchmarkScope.threadContext.contextualRunnable(() -> {
//                blackhole.consume(Arc.container().requestContext().isActive());
//            });
//        }
//        
//        CompletableFuture<Object> cf = new CompletableFuture<>();
//        benchmarkScope.vertx.executeBlocking(prom -> {
//            checker.run();
//            for (Runnable runner : runners) {
//                runner.run();
//            }
//            prom.complete();
//        }, res -> {
//            if(res.succeeded())
//                cf.complete(res.result());
//            else
//                cf.completeExceptionally(res.cause());
//        });
//        cf.get();
//    }
//
//    @Benchmark
//    @Threads(20)
//    public void testWorkerThreadMutiny(Blackhole blackhole,
//                                               ThreadScope threadScope,
//                                               BenchmarkScope benchmarkScope) throws InterruptedException, ExecutionException  {
//        CompletableFuture<Object> emitter = new CompletableFuture<>();
//        ManagedContext requestContext = Arc.container().requestContext();
//        Uni<Boolean> uni = Uni.createFrom().completionStage(emitter)
//                // one contextual checker
//            .map(val -> { 
//                if(Arc.container().requestContext() != requestContext)
//                    throw new IllegalStateException("Context not properly propagated");
//                return val;
//            })
//            // five contextual runners
//            .map(val -> Arc.container().requestContext().isActive())
//            .map(val -> Arc.container().requestContext().isActive())
//            .map(val -> Arc.container().requestContext().isActive())
//            .map(val -> Arc.container().requestContext().isActive())
//            .map(val -> Arc.container().requestContext().isActive())
//            ;
//        
//        CompletableFuture<Object> cf = new CompletableFuture<>();
//        benchmarkScope.vertx.executeBlocking(prom -> {
//            // complete from a quarkus thread
//            emitter.complete("foo");
//            // now subscribe
//            uni.subscribe().with(val -> prom.complete(), x -> prom.fail(x));
//        }, res -> {
//            if(res.succeeded())
//                cf.complete(res.result());
//            else
//                cf.completeExceptionally(res.cause());
//        });
//        cf.get();
//    }

    @Benchmark
    @Threads(20)
    public void testWorkerThreadCompletionStage(Blackhole blackhole,
                                               ThreadScope threadScope,
                                               BenchmarkScope benchmarkScope) throws InterruptedException, ExecutionException  {
        CompletableFuture<Object> emitter = new CompletableFuture<>();
        ManagedContext requestContext = Arc.container().requestContext();
        CompletableFuture<Object> cs = emitter
                // one contextual checker
            .thenApply(val -> { 
                if(Arc.container().requestContext() != requestContext)
                    throw new IllegalStateException("Context not properly propagated");
                return val;
            })
            // five contextual runners
            .thenApply(val -> Arc.container().requestContext().isActive())
            .thenApply(val -> Arc.container().requestContext().isActive())
            .thenApply(val -> Arc.container().requestContext().isActive())
            .thenApply(val -> Arc.container().requestContext().isActive())
            .thenApply(val -> Arc.container().requestContext().isActive())
            ;
        
        CompletableFuture<Object> cf = new CompletableFuture<>();
        benchmarkScope.vertx.executeBlocking(prom -> {
            // complete from a quarkus thread
            emitter.complete("foo");
            // now subscribe
            try {
                cs.get();
                prom.complete();
            } catch (InterruptedException | ExecutionException e) {
                prom.complete(e);
            }
        }, res -> {
            if(res.succeeded())
                cf.complete(res.result());
            else
                cf.completeExceptionally(res.cause());
        });
        cf.get();
    }

    @State(Scope.Thread)
    public static class ThreadScope {

        @Setup(Level.Trial)
        public void setUp() throws InterruptedException {
            Arc.container().requestContext().activate();
        }

        @TearDown
        public void tearDown(){
            Arc.container().requestContext().destroy();
        }
    }
    
    @State(Scope.Benchmark)
    public static class BenchmarkScope {

        public static volatile CountDownLatch startLatch = new CountDownLatch(1);

        private ExecutorService executor;

        SmallRyeThreadContext threadContext;
        Vertx vertx;

        @Setup(Level.Trial)
        public void setUp() throws InterruptedException {

            System.err.println("Starting Quarkus app");
            executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> Quarkus.run(BenchApplication.class));

            System.err.println("Waiting for app to be up");
            startLatch.await();
            System.err.println("App is up");

            //There is a delay between StartEvent fires and the HTTP layer is available to service requests
            //there must be a better way to synchronize bootstrapping
            Thread.currentThread().sleep(100);
            
            threadContext = Arc.container().instance(SmallRyeThreadContext.class).get();
            vertx = Arc.container().instance(Vertx.class).get();
        }

        @TearDown
        public void tearDown(){
            System.err.println("Tearing down Quarkus app");
            Quarkus.asyncExit();
            executor.shutdown();
        }
    }

}
