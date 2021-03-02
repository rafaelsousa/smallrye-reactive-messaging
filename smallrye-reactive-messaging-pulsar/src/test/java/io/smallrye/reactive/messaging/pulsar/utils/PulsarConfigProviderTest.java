package io.smallrye.reactive.messaging.pulsar.utils;

import org.eclipse.microprofile.reactive.messaging.spi.ConnectorLiteral;
import org.junit.Assert;
import org.junit.Test;

import io.smallrye.reactive.messaging.pulsar.PulsarConnector;
import io.smallrye.reactive.messaging.pulsar.PulsarTestBase;

public class PulsarConfigProviderTest extends PulsarTestBase {

    @Test
    public void testConfigProvider() {
        PulsarConfigProvider provider = weldContainer.getBeanManager().createInstance()
                .select(PulsarConfigProvider.class,
                        ConnectorLiteral.of(PulsarConnector.CONNECTOR_NAME))
                .get();
        Assert.assertNotNull(provider.getConfig());
    }

}
