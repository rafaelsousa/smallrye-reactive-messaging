package io.smallrye.reactive.messaging.skip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.junit.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.WeldTestBaseWithoutTails;
import io.smallrye.reactive.messaging.annotations.Broadcast;

public class StreamSkipTest extends WeldTestBaseWithoutTails {

    @Test
    public void testSkip() {
        addBeanClass(SkippingProcessors.class, Sender.class, Receiver.class);
        initialize();
        Receiver receiver = get(Receiver.class);
        await().until(() -> receiver.out1.size() == 10);
        await().until(() -> receiver.out2.size() == 10);
        await().until(() -> receiver.out3.size() == 10);
        await().until(() -> receiver.out4.size() == 10);

        assertThat(receiver.out1).containsExactly("A", "B", "C", "D", "E", "F", "G", "H", "I", "J");
        assertThat(receiver.out2).containsExactly("A", "B", "C", "D", "E", "F", "G", "H", "I", "J");
        assertThat(receiver.out3).containsExactly("A", "B", "C", "D", "E", "F", "G", "H", "I", "J");
        assertThat(receiver.out4).containsExactly("A", "B", "C", "D", "E", "F", "G", "H", "I", "J");

    }

    @ApplicationScoped
    public static class Sender {

        @Outgoing("in")
        @Broadcast(4)
        public Multi<String> generate() {
            return Multi.createFrom().items(
                    "a", "b", "c", "skip", "d", "skip", "e", "f", "skip", "g", "h", "i", "skip", "j");
        }
    }

    @Singleton
    public static class Receiver {

        private final List<String> out1 = new CopyOnWriteArrayList<>();
        private final List<String> out2 = new CopyOnWriteArrayList<>();
        private final List<String> out3 = new CopyOnWriteArrayList<>();
        private final List<String> out4 = new CopyOnWriteArrayList<>();

        @Incoming("out-1")
        public void consumeOut1(String s) {
            out1.add(s);
        }

        @Incoming("out-2")
        public void consumeOut2(String s) {
            out2.add(s);
        }

        @Incoming("out-3")
        public void consumeOut3(String s) {
            out3.add(s);
        }

        @Incoming("out-4")
        public void consumeOut4(String s) {
            out4.add(s);
        }

    }

    @ApplicationScoped
    public static class SkippingProcessors {
        @Incoming("in")
        @Outgoing("out-1")
        public Multi<String> processPayload(String s) {
            if (s.equalsIgnoreCase("skip")) {
                return Multi.createFrom().empty();
            }
            return Multi.createFrom().item(s.toUpperCase());
        }

        @Incoming("in")
        @Outgoing("out-2")
        public Multi<Message<String>> processMessage(Message<String> m) {
            String s = m.getPayload();
            if (s.equalsIgnoreCase("skip")) {
                return Multi.createFrom().empty();
            }
            return Multi.createFrom().item(m.withPayload(s.toUpperCase()));
        }

        @Incoming("in")
        @Outgoing("out-3")
        public Multi<String> processPayloadStream(Multi<String> stream) {
            return stream
                    .select().where(s -> !s.equalsIgnoreCase("skip"))
                    .onItem().transform(String::toUpperCase);
        }

        @Incoming("in")
        @Outgoing("out-4")
        public Multi<Message<String>> processMessageStream(Multi<Message<String>> stream) {
            return stream
                    .select().where(m -> !m.getPayload().equalsIgnoreCase("skip"))
                    .onItem().transform(m -> m.withPayload(m.getPayload().toUpperCase()));
        }
    }
}
