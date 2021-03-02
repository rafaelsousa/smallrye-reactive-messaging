package io.smallrye.reactive.messaging.pulsar.utils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;

@ApplicationScoped
public class PulsarConfigProvider {

    @Inject
    private Config config;

    public Object getConfig() {
        return config.getPropertyNames();
    }

}
