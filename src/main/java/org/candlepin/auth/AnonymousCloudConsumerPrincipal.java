package org.candlepin.auth;

import java.util.Objects;

import org.candlepin.auth.permissions.AnonymousCloudConsumerPermission;
import org.candlepin.model.AnonymousCloudConsumer;

// TODO: Java Doc throughout
public class AnonymousCloudConsumerPrincipal extends Principal {

    private AnonymousCloudConsumer consumer;
    
    public AnonymousCloudConsumerPrincipal(AnonymousCloudConsumer consumer) {
        this.consumer = Objects.requireNonNull(consumer);

        addPermission(new AnonymousCloudConsumerPermission(consumer));

    }

    public AnonymousCloudConsumer getAnonymousCloudConsumer() {
        return this.consumer;
    }

    @Override
    public String getType() {
        return "anonymouscloudconsumer";
    }

    @Override
    public boolean hasFullAccess() {
        return false;
    }

    @Override
    public String getName() {
        return this.consumer.getUuid();
    }
    
}
