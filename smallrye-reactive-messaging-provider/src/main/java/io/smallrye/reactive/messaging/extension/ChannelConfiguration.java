package io.smallrye.reactive.messaging.extension;

public class ChannelConfiguration {

    public String channelName;

    public ChannelConfiguration() {
        // Used for proxies.
    }

    public ChannelConfiguration(String channelName) {
        this.channelName = channelName;
    }
}
