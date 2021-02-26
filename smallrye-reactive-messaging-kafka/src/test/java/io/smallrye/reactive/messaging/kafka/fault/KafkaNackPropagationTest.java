package io.smallrye.reactive.messaging.kafka.fault;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.context.ApplicationScoped;

import org.apache.kafka.common.serialization.IntegerSerializer;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import io.smallrye.reactive.messaging.kafka.base.KafkaMapBasedConfig;
import io.smallrye.reactive.messaging.kafka.base.KafkaTestBase;

public class KafkaNackPropagationTest extends KafkaTestBase {

    @Test
    public void testDoubleNackPropagation() {
        List<Integer> messages = new CopyOnWriteArrayList<>();
        usage.consumeIntegers("double-topic", 9, 1, TimeUnit.MINUTES, null,
                (key, value) -> messages.add(value));

        runApplication(getDoubleNackConfig(), DoubleNackBean.class);

        DoubleNackBean bean = get(DoubleNackBean.class);
        await().atMost(2, TimeUnit.MINUTES).until(() -> bean.list().size() >= 9);
        assertThat(bean.list()).containsExactly(0, 1, 2, 3, 4, 5, 7, 8, 9);

        assertThat(bean.m2Nack()).isEqualTo(1);
        assertThat(bean.m1Nack()).isEqualTo(1);
        assertThat(bean.nackedException()).isNotNull().isInstanceOf(IllegalArgumentException.class);

        await().until(() -> messages.size() == 9);
        assertThat(messages).containsExactly(1, 2, 3, 4, 5, 6, 8, 9, 10);
    }

    @Test
    public void testNackPropagation() {
        List<Integer> messages = new CopyOnWriteArrayList<>();
        usage.consumeIntegers(topic, 9, 1, TimeUnit.MINUTES, null,
                (key, value) -> messages.add(value));

        runApplication(getPassedNackConfig(topic), PassedNackBean.class);

        PassedNackBean bean = get(PassedNackBean.class);
        await().atMost(2, TimeUnit.MINUTES).until(() -> bean.list().size() >= 9);
        assertThat(bean.list()).containsExactly(0, 1, 2, 4, 5, 6, 7, 8, 9);

        assertThat(bean.m1Nack()).isEqualTo(1);
        assertThat(bean.nackedException()).isNotNull().isInstanceOf(IllegalArgumentException.class);

        await().until(() -> messages.size() == 9);
        assertThat(messages).containsExactly(1, 2, 3, 5, 6, 7, 8, 9, 10);
    }

    private KafkaMapBasedConfig getDoubleNackConfig() {
        KafkaMapBasedConfig.Builder builder = KafkaMapBasedConfig.builder("mp.messaging.outgoing.kafka");
        builder.put("value.serializer", IntegerSerializer.class.getName());
        builder.put("topic", "double-topic");
        return builder.build();
    }

    private KafkaMapBasedConfig getPassedNackConfig(String topic) {
        KafkaMapBasedConfig.Builder builder = KafkaMapBasedConfig.builder("mp.messaging.outgoing.kafka");
        builder.put("value.serializer", IntegerSerializer.class.getName());
        builder.put("topic", topic);
        return builder.build();
    }

    @ApplicationScoped
    public static class DoubleNackBean {
        private final List<Integer> received = new ArrayList<>();
        private final AtomicInteger m1Nack = new AtomicInteger();
        private final AtomicInteger m2Nack = new AtomicInteger();
        private final AtomicReference<Throwable> nackedException = new AtomicReference<>();

        @Outgoing("source")
        public Multi<Message<Integer>> producer() {
            return Multi.createFrom().range(0, 10)
                    .map(val -> KafkaRecord.of("1", val)
                            .withNack(cause -> {
                                assertThat(cause).isNotNull();
                                m1Nack.incrementAndGet();
                                nackedException.set(cause);
                                return CompletableFuture.completedFuture(null);
                            }));
        }

        @Incoming("source")
        @Outgoing("output")
        public Message<Integer> processMessage(Message<Integer> input) {
            return KafkaRecord.from(input)
                    .withNack(cause -> {
                        assertThat(cause).isNotNull();
                        m2Nack.incrementAndGet();
                        input.nack(cause);
                        return CompletableFuture.completedFuture(null);
                    });
        }

        @Incoming("output")
        @Outgoing("kafka")
        public Message<Integer> handle(Message<Integer> record) {
            if (record.getPayload().equals(6)) {
                record.nack(new IllegalArgumentException()).toCompletableFuture().join();
                return null;
            } else {
                received.add(record.getPayload());
                record.ack().toCompletableFuture().join();
            }
            return record.withPayload(record.getPayload() + 1);
        }

        public List<Integer> list() {
            return received;
        }

        public int m1Nack() {
            return m1Nack.get();
        }

        public int m2Nack() {
            return m2Nack.get();
        }

        public Throwable nackedException() {
            return nackedException.get();
        }
    }

    @ApplicationScoped
    public static class PassedNackBean {
        private final List<Integer> received = new ArrayList<>();
        private final AtomicInteger nack = new AtomicInteger();
        private final AtomicReference<Throwable> nackedException = new AtomicReference<>();

        @Outgoing("source")
        public Multi<Message<Integer>> producer() {
            return Multi.createFrom().range(0, 10)
                    .map(val -> KafkaRecord.of("1", val)
                            .withNack(cause -> {
                                assertThat(cause).isNotNull();
                                nack.incrementAndGet();
                                nackedException.set(cause);
                                return CompletableFuture.completedFuture(null);
                            }));
        }

        @Incoming("source")
        @Outgoing("output")
        public Message<Integer> processMessage(Message<Integer> input) {
            return KafkaRecord.from(input);
        }

        @Incoming("output")
        @Outgoing("kafka")
        public Message<Integer> handle(Message<Integer> record) {
            if (record.getPayload().equals(3)) {
                record.nack(new IllegalArgumentException()).toCompletableFuture().join();
                return null;
            } else {
                received.add(record.getPayload());
                record.ack().toCompletableFuture().join();
            }
            return record.withPayload(record.getPayload() + 1);
        }

        public List<Integer> list() {
            return received;
        }

        public int m1Nack() {
            return nack.get();
        }

        public Throwable nackedException() {
            return nackedException.get();
        }
    }

}
