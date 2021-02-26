package io.smallrye.reactive.messaging.jms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Named;
import javax.jms.ConnectionFactory;

import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.Test;

import io.smallrye.reactive.messaging.jms.support.JmsTestBase;
import io.smallrye.reactive.messaging.test.common.config.MapBasedConfig;

public class NamedFactoryTest extends JmsTestBase {

    @Override
    protected boolean withConnectionFactory() {
        return false;
    }

    @Test
    public void testNamedConnectionFactory() {
        initWithoutConnectionFactory().addBeanClasses(BarConnectionFactoryBean.class, FooConnectionFactoryBean.class);
        Map<String, Object> map = new HashMap<>();
        map.put("mp.messaging.connector." + JmsConnector.CONNECTOR_NAME + ".connection-factory-name", "foo");
        map.put("mp.messaging.outgoing.queue-one.connector", JmsConnector.CONNECTOR_NAME);
        map.put("mp.messaging.incoming.jms.connector", JmsConnector.CONNECTOR_NAME);
        map.put("mp.messaging.incoming.jms.destination", "queue-one");
        MapBasedConfig config = new MapBasedConfig(map);
        addConfig(config);
        WeldContainer container = deploy(PayloadConsumerBean.class, ProducerBean.class);

        PayloadConsumerBean bean = container.select(PayloadConsumerBean.class).get();
        await().until(() -> bean.list().size() > 3);
    }

    @Test
    public void testWithoutConnectionFactoryAndNoNameSet() {
        initWithoutConnectionFactory();
        Map<String, Object> map = new HashMap<>();
        map.put("mp.messaging.outgoing.queue-one.connector", JmsConnector.CONNECTOR_NAME);
        map.put("mp.messaging.incoming.jms.connector", JmsConnector.CONNECTOR_NAME);
        map.put("mp.messaging.incoming.jms.destination", "queue-one");
        MapBasedConfig config = new MapBasedConfig(map);
        addConfig(config);
        try {
            deploy(PayloadConsumerBean.class, ProducerBean.class);
        } catch (DeploymentException de) {
            assertThat(de).hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("javax.jms.ConnectionFactory bean");
        }
    }

    @Test
    public void testWithoutConnectionFactoryAndNameSet() {
        initWithoutConnectionFactory();
        Map<String, Object> map = new HashMap<>();
        map.put("mp.messaging.connector." + JmsConnector.CONNECTOR_NAME + ".connection-factory-name", "foo");
        map.put("mp.messaging.outgoing.queue-one.connector", JmsConnector.CONNECTOR_NAME);
        map.put("mp.messaging.incoming.jms.connector", JmsConnector.CONNECTOR_NAME);
        map.put("mp.messaging.incoming.jms.destination", "queue-one");
        MapBasedConfig config = new MapBasedConfig(map);
        addConfig(config);
        try {
            deploy(PayloadConsumerBean.class, ProducerBean.class);
        } catch (DeploymentException de) {
            assertThat(de).hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("javax.jms.ConnectionFactory bean named");
        }
    }

    @Test
    public void testWithNonMatchingConnectionFactory() {
        initWithoutConnectionFactory().addBeanClasses(FooConnectionFactoryBean.class);
        Map<String, Object> map = new HashMap<>();
        map.put("mp.messaging.connector." + JmsConnector.CONNECTOR_NAME + ".connection-factory-name", "bar");
        map.put("mp.messaging.outgoing.queue-one.connector", JmsConnector.CONNECTOR_NAME);
        map.put("mp.messaging.incoming.jms.connector", JmsConnector.CONNECTOR_NAME);
        map.put("mp.messaging.incoming.jms.destination", "queue-one");
        MapBasedConfig config = new MapBasedConfig(map);
        addConfig(config);
        try {
            deploy(PayloadConsumerBean.class, ProducerBean.class);
        } catch (DeploymentException de) {
            assertThat(de).hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("javax.jms.ConnectionFactory bean named");
        }
    }

    @ApplicationScoped
    public static class BarConnectionFactoryBean {

        @Produces
        @Named("bar")
        ConnectionFactory factory() {
            return new ActiveMQJMSConnectionFactory(
                    "tcp://localhost:61618", // Wrong on purpose
                    null, null);
        }

    }

    @ApplicationScoped
    public static class FooConnectionFactoryBean {

        @Produces
        @Named("foo")
        ConnectionFactory factory() {
            return new ActiveMQJMSConnectionFactory(
                    "tcp://localhost:61616",
                    null, null);
        }

    }

}
