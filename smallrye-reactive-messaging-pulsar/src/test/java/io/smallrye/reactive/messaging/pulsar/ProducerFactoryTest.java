package io.smallrye.reactive.messaging.pulsar;

import org.jboss.weld.junit4.WeldInitiator;
import org.junit.*;

class ProducerFactoryTest {

    @ClassRule
    public WeldInitiator weld = WeldInitiator.of(ProducerFactory.class);

    @Before
    public void setUp() {
    }

    @Test
    public void createFactory(ProducerFactory producerFactory) {
        System.out.println("hello" + producerFactory.toString());
        Assert.assertTrue(true);
    }

    @After
    public void tearDown() throws InterruptedException {

    }
}
