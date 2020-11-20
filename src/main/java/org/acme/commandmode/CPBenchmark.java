package org.acme.commandmode;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

public class CPBenchmark {

    @Benchmark
    @Threads(20)
    public void testWorkerThreadSingleCallback(Blackhole blackhole,
                                               ThreadScope threadScope,
                                               BenchmarkScope benchmarkScope) throws InterruptedException, ExecutionException  {
        // one contextual checker
        Runnable checker = benchmarkScope.threadContext.contextualRunnable(() -> {
            checkPropagation(threadScope);
        });
        // 5 contextual runners
        Runnable[] runners = new Runnable[5];
        for(int i=0;i<runners.length;i++) {
            runners[i] = benchmarkScope.threadContext.contextualRunnable(() -> {
                usePropagation(blackhole);
            });
        }
        
        runOnQuarkusThread(benchmarkScope, promise -> {
            checker.run();
            for (Runnable runner : runners) {
                runner.run();
            }
            promise.complete();
        });
    }

    @Benchmark
    @Threads(20)
    public void testWorkerThreadMutiny(Blackhole blackhole,
                                               ThreadScope threadScope,
                                               BenchmarkScope benchmarkScope) throws InterruptedException, ExecutionException  {
        CompletableFuture<Object> emitter = new CompletableFuture<>();
        Uni<Object> uni = Uni.createFrom().completionStage(emitter)
                // one contextual checker
                .map(val -> checkPropagation(threadScope))
            // five contextual runners
                .map(val -> usePropagation(blackhole))
                .map(val -> usePropagation(blackhole))
                .map(val -> usePropagation(blackhole))
                .map(val -> usePropagation(blackhole))
                .map(val -> usePropagation(blackhole))
            ;
        
        runOnQuarkusThread(benchmarkScope, promise -> {
            // complete from a quarkus thread
            emitter.complete("foo");
            // now subscribe
            uni.subscribe().with(val -> promise.complete(), x -> promise.fail(x));
        });
    }

    @Benchmark
    @Threads(20)
    public void testWorkerThreadCompletionStage(Blackhole blackhole,
                                               ThreadScope threadScope,
                                               BenchmarkScope benchmarkScope) throws InterruptedException, ExecutionException  {
        CompletableFuture<Object> emitter = benchmarkScope.threadContext.newIncompleteFuture();
        CompletableFuture<Object> cs = emitter
                // one contextual checker
            .thenApply(val -> checkPropagation(threadScope))
            // five contextual runners
            .thenApply(val -> usePropagation(blackhole))
            .thenApply(val -> usePropagation(blackhole))
            .thenApply(val -> usePropagation(blackhole))
            .thenApply(val -> usePropagation(blackhole))
            .thenApply(val -> usePropagation(blackhole))
            ;
        
        runOnQuarkusThread(benchmarkScope, promise -> {
            // complete from a quarkus thread
            emitter.complete("foo");
            // now subscribe
            try {
                cs.get();
                promise.complete();
            } catch (InterruptedException | ExecutionException e) {
                promise.fail(e);
            }
        });
    }

    private <T> void runOnQuarkusThread(BenchmarkScope benchmarkScope, Handler<Promise<T>> consumer) throws InterruptedException, ExecutionException {
        CompletableFuture<T> cf = new CompletableFuture<>();
        benchmarkScope.vertx.executeBlocking(consumer, res -> {
            if(res.succeeded())
                cf.complete(res.result());
            else
                cf.completeExceptionally(res.cause());
        });
        cf.get();
    }

    private Object checkPropagation(ThreadScope threadScope) {
        if(!Arc.container().requestContext().isActive())
            throw new IllegalStateException("Context not properly propagated");
        if(ThreadContext1.get() != threadScope.context1)
            throw new IllegalStateException("Context not properly propagated");
        if(ThreadContext2.get() != threadScope.context2)
            throw new IllegalStateException("Context not properly propagated");
        return null;
    }

    private Object usePropagation(Blackhole hole) {
        hole.consume(Arc.container().requestContext().isActive());
        hole.consume(ThreadContext1.get());
        hole.consume(ThreadContext2.get());
        return null;
    }

    @State(Scope.Thread)
    public static class ThreadScope {

        public static final AtomicInteger counter = new AtomicInteger();
        
        public String context1;
        public int context2;
        public ManagedContext requestContext;
        
        @Setup(Level.Trial)
        public void setUp() throws InterruptedException {
            Arc.container().requestContext().activate();
            context2 = counter.incrementAndGet();
            context1 = "in req "+context2;
            ThreadContext1.set(context1);
            ThreadContext2.set(context2);
        }

        @TearDown
        public void tearDown(){
            Arc.container().requestContext().destroy();
            ThreadContext1.reset();
            ThreadContext2.reset();
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

    // for debugging
    public static void main(String[] args) throws Exception {
        BenchmarkScope benchmarkScope = new BenchmarkScope();
        benchmarkScope.setUp();
        
        ThreadScope threadScope = new ThreadScope();
        threadScope.setUp();
        
        CPBenchmark benchmark = new CPBenchmark();
        Blackhole blackhole = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
        benchmark.testWorkerThreadMutiny(blackhole, threadScope, benchmarkScope);
        
        threadScope.tearDown();
        
        benchmarkScope.tearDown();
    }
}
