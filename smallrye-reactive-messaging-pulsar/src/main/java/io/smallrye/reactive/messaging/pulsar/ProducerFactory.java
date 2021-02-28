package io.smallrye.reactive.messaging.pulsar;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ProducerFactory {

    @Inject
    @ConfigProperty(name = "mp.messaging.outgoing.producer")
    private Map<String, String> producerProperties;

    private ProducerFactory() {
        producerProperties.entrySet().stream()
                .forEach(key -> {
                    System.out.println(producerProperties.get(key).toString());
                });
    }

}
