package io.smallrye.reactive.messaging.amqp.fault;

import static io.smallrye.reactive.messaging.amqp.i18n.AMQPLogging.log;

import java.util.concurrent.CompletionStage;

import io.smallrye.reactive.messaging.amqp.AmqpMessage;
import io.smallrye.reactive.messaging.amqp.ConnectionHolder;
import io.vertx.mutiny.core.Context;

public class AmqpRelease implements AmqpFailureHandler {

    private final String channel;

    public AmqpRelease(String channel) {
        this.channel = channel;
    }

    @Override
    public <V> CompletionStage<Void> handle(AmqpMessage<V> msg, Context context, Throwable reason) {
        log.nackedReleaseMessage(channel);
        log.fullIgnoredFailure(reason);
        return ConnectionHolder.runOnContext(context, () -> msg.getAmqpMessage().released());
    }
}
