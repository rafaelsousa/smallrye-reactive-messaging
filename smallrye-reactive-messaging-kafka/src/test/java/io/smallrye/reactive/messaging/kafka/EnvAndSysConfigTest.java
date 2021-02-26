package io.smallrye.reactive.messaging.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import javax.enterprise.context.ApplicationScoped;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.junitpioneer.jupiter.SetSystemProperty;

import io.smallrye.reactive.messaging.kafka.base.KafkaMapBasedConfig;
import io.smallrye.reactive.messaging.kafka.base.KafkaTestBase;

public class EnvAndSysConfigTest extends KafkaTestBase {

    public static final String TOPIC_1 = "EnvConfigTest-IN-1";
    public static final String TOPIC_2 = "EnvConfigTest-IN-2";

    @Test
    @SetEnvironmentVariable(key = "MP_MESSAGING_INCOMING_KAFKA_TOPIC", value = TOPIC_1)
    @SetEnvironmentVariable(key = "MP_MESSAGING_INCOMING_KAFKA_VALUE_DESERIALIZER", value = "org.apache.kafka.common.serialization.StringDeserializer")
    public void testConsumerConfigurationComingFromEnv() throws InterruptedException {
        KafkaConsumer bean = runApplication(KafkaMapBasedConfig.builder("mp.messaging.incoming.kafka")
                .put(
                        "auto.offset.reset", "earliest")
                .build(), KafkaConsumer.class);

        verify(bean, TOPIC_1);
    }

    @Test
    @SetSystemProperty(key = "mp.messaging.incoming.kafka.topic", value = TOPIC_2)
    @SetSystemProperty(key = "mp.messaging.incoming.kafka.value.deserializer", value = "org.apache.kafka.common.serialization.StringDeserializer")
    public void testConsumerConfigurationComingFromSys() throws InterruptedException {
        KafkaConsumer bean = runApplication(KafkaMapBasedConfig.builder("mp.messaging.incoming.kafka")
                .put(
                        "auto.offset.reset", "earliest")
                .build(), KafkaConsumer.class);

        verify(bean, TOPIC_2);
    }

    private void verify(KafkaConsumer bean, String topic) throws InterruptedException {
        await().until(this::isReady);
        await().until(this::isAlive);

        CountDownLatch latch = new CountDownLatch(1);
        usage.produceStrings(1, latch::countDown, () -> new ProducerRecord<>(topic, "key", "hello"));

        latch.await();

        await().untilAsserted(() -> {
            assertThat(bean.getMessages()).hasSize(1);
            assertThat(bean.getMessages().get(0).getPayload()).isEqualTo("hello");
        });
    }

    @ApplicationScoped
    public static class KafkaConsumer {

        private final List<Message<String>> messages = new CopyOnWriteArrayList<>();

        @Incoming("kafka")
        public CompletionStage<Void> consume(Message<String> incoming) {
            messages.add(incoming);
            return incoming.ack();
        }

        public List<Message<String>> getMessages() {
            return messages;
        }
    }
}
