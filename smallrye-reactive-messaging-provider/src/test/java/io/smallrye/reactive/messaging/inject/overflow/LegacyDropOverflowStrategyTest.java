package io.smallrye.reactive.messaging.inject.overflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import io.smallrye.reactive.messaging.WeldTestBaseWithoutTails;
import io.smallrye.reactive.messaging.annotations.Channel;
import io.smallrye.reactive.messaging.annotations.Emitter;
import io.smallrye.reactive.messaging.annotations.OnOverflow;

public class LegacyDropOverflowStrategyTest extends WeldTestBaseWithoutTails {

    private static ExecutorService executor;

    @BeforeClass
    public static void init() {
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterClass
    public static void cleanup() {
        executor.shutdown();
    }

    @Test
    public void testNormal() {
        BeanUsingDropOverflowStrategy bean = installInitializeAndGet(BeanUsingDropOverflowStrategy.class);
        bean.emitThree();

        await().until(() -> bean.output().size() == 3);
        assertThat(bean.output()).containsExactly("1", "2", "3");
        assertThat(bean.exception()).isNull();
    }

    @Test
    public void testOverflow() {
        BeanUsingDropOverflowStrategy bean = installInitializeAndGet(BeanUsingDropOverflowStrategy.class);
        bean.emitALotOfItems();

        await().until(bean::isDone);
        assertThat(bean.output()).contains("1", "2", "3", "4", "5").hasSizeLessThan(999);
        assertThat(bean.failure()).isNull();
        assertThat(bean.exception()).isNull();
    }

    @SuppressWarnings("deprecation")
    @ApplicationScoped
    public static class BeanUsingDropOverflowStrategy {

        @Inject
        @Channel("hello")
        @OnOverflow(value = OnOverflow.Strategy.DROP)
        Emitter<String> emitter;

        private final List<String> output = new CopyOnWriteArrayList<>();

        private volatile Throwable downstreamFailure;
        private volatile boolean done;
        private Exception callerException;

        public boolean isDone() {
            return done;
        }

        public List<String> output() {
            return output;
        }

        public Throwable failure() {
            return downstreamFailure;
        }

        public Exception exception() {
            return callerException;
        }

        public void emitThree() {
            try {
                emitter.send("1");
                emitter.send("2");
                emitter.send("3");
                emitter.complete();
            } catch (Exception e) {
                callerException = e;
            }
        }

        public void emitALotOfItems() {
            new Thread(() -> {
                try {
                    for (int i = 1; i < 1000; i++) {
                        emitter.send("" + i);
                    }
                } catch (Exception e) {
                    callerException = e;
                } finally {
                    done = true;
                }
            }).start();
        }

        @Incoming("hello")
        @Outgoing("out")
        public Flowable<String> consume(Flowable<String> values) {
            Scheduler scheduler = Schedulers.from(executor);
            return values
                    .observeOn(scheduler)
                    .delay(1, TimeUnit.MILLISECONDS, scheduler)
                    .doOnError(err -> {
                        downstreamFailure = err;
                    });
        }

        @Incoming("out")
        public void out(String s) {
            output.add(s);
        }

    }
}
