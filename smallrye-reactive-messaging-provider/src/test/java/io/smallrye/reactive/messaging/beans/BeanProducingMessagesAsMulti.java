package io.smallrye.reactive.messaging.beans;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.reactivex.Flowable;
import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class BeanProducingMessagesAsMulti {

    @Outgoing("sink")
    Multi<Message<String>> publisher() {
        return Multi.createFrom().range(1, 11).flatMap(i -> Flowable.just(i, i)).map(i -> Integer.toString(i))
                .map(Message::of);
    }

}
