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
package org.candlepin.audit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.candlepin.audit.Event.Target;
import org.candlepin.audit.Event.Type;
import org.candlepin.config.CommonConfigKey;
import org.candlepin.config.DevConfig;
import org.candlepin.config.TestConfig;

import org.junit.jupiter.api.Test;

import java.util.Map;


public class EventFilterTest {

    @Test
    public void disabledShouldNotFilter() {
        Event event1 = createEvent(Type.CREATED, Target.ENTITLEMENT);
        Event event2 = createEvent(Type.MODIFIED, Target.CONSUMER);
        DevConfig config = TestConfig.custom(Map.of(
            CommonConfigKey.AUDIT_FILTER_ENABLED.key(), "false",
            CommonConfigKey.AUDIT_FILTER_DEFAULT_POLICY.key(), "DO_FILTER"
        ));

        EventFilter filter = new EventFilter(config);

        assertFalse(filter.shouldFilter(event1));
        assertFalse(filter.shouldFilter(event2));
    }

    @Test
    public void filterUnknownEventPolicyDoFilter() {
        Event event = createEvent(Type.MODIFIED, Target.CONSUMER);
        DevConfig config = TestConfig.custom(Map.of(
            CommonConfigKey.AUDIT_FILTER_ENABLED.key(), "true",
            CommonConfigKey.AUDIT_FILTER_DO_NOT_FILTER.key(),
            "CREATED-ENTITLEMENT",
            "DELETED-ENTITLEMENT",
            "CREATED-POOL",
            "DELETED-POOL",
            "CREATED-COMPLIANCE",
            CommonConfigKey.AUDIT_FILTER_DEFAULT_POLICY.key(), "DO_FILTER"
        ));

        EventFilter filter = new EventFilter(config);

        assertTrue(filter.shouldFilter(event));
    }

    @Test
    public void notFilterIncludes() {
        Event event = createEvent(Type.CREATED, Target.ENTITLEMENT);
        DevConfig config = TestConfig.custom(Map.of(
            CommonConfigKey.AUDIT_FILTER_ENABLED.key(), "true",
            CommonConfigKey.AUDIT_FILTER_DO_NOT_FILTER.key(),
            "CREATED-ENTITLEMENT",
            "DELETED-ENTITLEMENT",
            "CREATED-POOL",
            "DELETED-POOL",
            "CREATED-COMPLIANCE",
            CommonConfigKey.AUDIT_FILTER_DEFAULT_POLICY.key(), "DO_FILTER"
        ));

        EventFilter filter = new EventFilter(config);

        assertFalse(filter.shouldFilter(event));
    }

    @Test
    public void policyDoNotFilterShouldNotFilter() {
        Event event1 = createEvent(Type.CREATED, Target.ENTITLEMENT);
        Event event2 = createEvent(Type.MODIFIED, Target.CONSUMER);
        DevConfig config = TestConfig.custom(Map.of(
            CommonConfigKey.AUDIT_FILTER_ENABLED.key(), "true",
            CommonConfigKey.AUDIT_FILTER_DO_NOT_FILTER.key(),
            "CREATED-ENTITLEMENT",
            "DELETED-ENTITLEMENT",
            "CREATED-POOL",
            "DELETED-POOL",
            "CREATED-COMPLIANCE",
            CommonConfigKey.AUDIT_FILTER_DEFAULT_POLICY.key(), "DO_NOT_FILTER"
        ));

        EventFilter filter = new EventFilter(config);

        assertFalse(filter.shouldFilter(event1));
        assertFalse(filter.shouldFilter(event2));
    }

    @Test
    public void policyDoNotFilterShouldFilterExcludes() {
        Event event = createEvent(Type.MODIFIED, Target.EXPORT);
        DevConfig config = TestConfig.custom(Map.of(
            CommonConfigKey.AUDIT_FILTER_ENABLED.key(), "true",
            CommonConfigKey.AUDIT_FILTER_DO_FILTER.key(), "MODIFIED-EXPORT",
            CommonConfigKey.AUDIT_FILTER_DEFAULT_POLICY.key(), "DO_NOT_FILTER"
        ));

        EventFilter filter = new EventFilter(config);

        assertTrue(filter.shouldFilter(event));
    }

    private Event createEvent(Type type, Target target) {
        Event event = new Event();
        event.setType(type);
        event.setTarget(target);
        return event;
    }

}
