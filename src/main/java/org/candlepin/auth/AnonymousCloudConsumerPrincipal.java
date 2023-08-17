/*
 * Copyright (c) 2009 - 2023 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.auth;

import org.candlepin.auth.permissions.AnonymousCloudConsumerPermission;
import org.candlepin.model.AnonymousCloudConsumer;

import java.util.Objects;



/**
 * The {@link AnonymousCloudConsumerPrincipal} is used for access to retrieve temporary content
 * access certificates for cloud consumers that belong to no owner.
 */
public class AnonymousCloudConsumerPrincipal extends Principal {

    private AnonymousCloudConsumer consumer;

    public AnonymousCloudConsumerPrincipal(AnonymousCloudConsumer consumer) {
        this.consumer = Objects.requireNonNull(consumer);

        addPermission(new AnonymousCloudConsumerPermission(consumer));
    }

    /**
     * @return the {@link AnonymousCloudConsumer} for this principal
     */
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
