/*
 * Copyright (c) 2009 - 2025 Red Hat, Inc.
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

package org.candlepin.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.candlepin.test.DatabaseTestFixture;
import org.candlepin.test.TestUtil;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RhsmApiCompatCuratorTest extends DatabaseTestFixture {

    @Test
    public void testGetConsumerFeedFilterByOwner() {
        Owner ownerA = createOwner("A");
        Owner ownerB = createOwner("B");
        Consumer consumerA1 = createConsumer(ownerA);
        Consumer consumerA2 = createConsumer(ownerA);
        Consumer consumerB1 = createConsumer(ownerB);

        List<ConsumerFeed> result = rhsmApiCompatCurator.getConsumerFeed(ownerA, null, null, null, 1,
            10);

        assertThat(result).extracting(ConsumerFeed::getId)
            .containsExactly(consumerA1.getId(), consumerA2.getId());
    }

    @Test
    public void testGetConsumerFeedFilterByAfterId() {
        Owner owner = createOwner("C");
        Consumer c1 = createConsumer(owner);
        Consumer c2 = createConsumer(owner);
        Consumer c3 = createConsumer(owner);

        // Should only return consumers with id > id20
        List<ConsumerFeed> result = rhsmApiCompatCurator.getConsumerFeed(owner, c2.getId(), null,
            null, 1, 10);

        assertThat(result).extracting(ConsumerFeed::getId).containsExactly(c3.getId());
    }

    @Test
    public void testGetConsumerFeedFilterByAfterUuid() {
        Owner owner = createOwner("D");
        Consumer c1 = createConsumer(owner, "uuid1", null, null, null, null);
        Consumer c2 = createConsumer(owner, "uuid2", null, null, null, null);
        Consumer c3 = createConsumer(owner, "uuid3", null, null, null, null);

        // Should only return consumers with uuid1 > uuid2 (lexicographically)
        List<ConsumerFeed> result = rhsmApiCompatCurator.getConsumerFeed(owner, null, "uuid2", null,
            1, 10);

        assertThat(result).extracting(ConsumerFeed::getUuid).containsExactly("uuid3");
    }

    @Test
    public void testGetConsumerFeedFilterByAfterCheckin() {
        Owner owner = createOwner("E");
        OffsetDateTime ts1 = OffsetDateTime.now().minusDays(3);
        OffsetDateTime ts2 = OffsetDateTime.now().minusDays(2);
        OffsetDateTime ts3 = OffsetDateTime.now().minusDays(1);
        Consumer c1 = createConsumer(owner, "uuidX", Date.from(ts1.toInstant()), null, null, null);
        Consumer c2 = createConsumer(owner, "uuidY", Date.from(ts2.toInstant()), null, null, null);
        Consumer c3 = createConsumer(owner, "uuidZ", Date.from(ts3.toInstant()), null, null, null);

        // Only return those after ts2 (should get c3 only)
        List<ConsumerFeed> result = rhsmApiCompatCurator.getConsumerFeed(owner, null, null, ts2, 1,
            10);

        assertThat(result).extracting(ConsumerFeed::getId).containsExactly(c3.getId());
    }

    @Test
    public void testGetConsumerFeedFilterByAfterIdAndCheckin() {
        Owner owner = createOwner("G");
        OffsetDateTime ts1 = OffsetDateTime.now().minusDays(3);
        OffsetDateTime ts2 = OffsetDateTime.now().minusDays(2);
        OffsetDateTime ts3 = OffsetDateTime.now().minusDays(1);

        Consumer c1 = createConsumer(owner, "uuidX", Date.from(ts1.toInstant()), null, null, null);
        Consumer c2 = createConsumer(owner, "uuidY", Date.from(ts2.toInstant()), null, null, null);
        Consumer c3 = createConsumer(owner, "uuidZ", Date.from(ts3.toInstant()), null, null, null);

        // id > c1.getId() AND checkin > ts1 → c2 a c3
        List<ConsumerFeed> result = rhsmApiCompatCurator.getConsumerFeed(owner, c1.getId(), null,
            ts1, 1, 10);

        assertThat(result).extracting(ConsumerFeed::getId).containsExactlyInAnyOrder(c2.getId(), c3.getId());
    }

    @Test
    public void testGetConsumerFeedFilterByAfterIdAndUuid() {
        Owner owner = createOwner("H");
        Consumer c1 = createConsumer(owner, "uuid1", null, null, null, null);
        Consumer c2 = createConsumer(owner, "uuid2", null, null, null, null);
        Consumer c3 = createConsumer(owner, "uuid3", null, null, null, null);

        // id > c1.getId() AND uuid > "uuid1" → c2, c3
        List<ConsumerFeed> result = rhsmApiCompatCurator.getConsumerFeed(owner, c1.getId(), "uuid1",
            null, 1, 10);

        assertThat(result).extracting(ConsumerFeed::getUuid).containsExactlyInAnyOrder("uuid2", "uuid3");
    }

    @Test
    public void testGetConsumerFeedFilterByAfterUuidAndCheckin() {
        Owner owner = createOwner("I");
        OffsetDateTime ts1 = OffsetDateTime.now().minusDays(3);
        OffsetDateTime ts2 = OffsetDateTime.now().minusDays(2);
        OffsetDateTime ts3 = OffsetDateTime.now().minusDays(1);

        Consumer c1 = createConsumer(owner, "uuidA", Date.from(ts1.toInstant()), null, null, null);
        Consumer c2 = createConsumer(owner, "uuidB", Date.from(ts2.toInstant()), null, null, null);
        Consumer c3 = createConsumer(owner, "uuidC", Date.from(ts3.toInstant()), null, null, null);

        // uuid > "uuidA" AND checkin > ts1 → c2, c3
        List<ConsumerFeed> result = rhsmApiCompatCurator.getConsumerFeed(owner, null, "uuidA", ts1,
            1, 10);

        assertThat(result).extracting(ConsumerFeed::getUuid).containsExactlyInAnyOrder("uuidB", "uuidC");
    }

    @Test
    public void testGetConsumerFeedFilterByAll() {
        Owner owner = createOwner("F");
        OffsetDateTime ts1 = OffsetDateTime.now().minusDays(5);
        OffsetDateTime ts2 = OffsetDateTime.now().minusDays(3);
        OffsetDateTime ts3 = OffsetDateTime.now().minusDays(1);
        Consumer c1 = createConsumer(owner, "uuidA", Date.from(ts1.toInstant()), null, null, null);
        Consumer c2 = createConsumer(owner, "uuidB", Date.from(ts2.toInstant()), null, null, null);
        Consumer c3 = createConsumer(owner, "uuidC", Date.from(ts3.toInstant()), null, null, null);

        List<ConsumerFeed> result = rhsmApiCompatCurator.getConsumerFeed(owner, c1.getId(), "uuidA",
            ts1, 1, 10);

        assertThat(result).extracting(ConsumerFeed::getId).containsExactlyInAnyOrder(c2.getId(), c3.getId());
    }

    @Test
    public void testGetConsumerFeedPagingFirstAndSecondPage() {
        Owner owner = createOwner("K");
        Consumer c1 = createConsumer(owner, "uuid1", null, null, null, null);
        Consumer c2 = createConsumer(owner, "uuid2", null, null, null, null);
        Consumer c3 = createConsumer(owner, "uuid3", null, null, null, null);

        List<ConsumerFeed> page1 = rhsmApiCompatCurator
            .getConsumerFeed(owner, null, null, null, 1, 2);

        assertThat(page1)
            .hasSize(2);
        assertThat(page1)
            .extracting(ConsumerFeed::getUuid)
            .containsExactly("uuid1", "uuid2");

        List<ConsumerFeed> page2 = rhsmApiCompatCurator
            .getConsumerFeed(owner, null, null, null, 2, 2);

        assertThat(page2)
            .hasSize(1);
        assertThat(page2.get(0))
            .extracting(ConsumerFeed::getUuid)
            .isEqualTo("uuid3");
    }

    @Test
    public void testGetFactsByConsumerReturnsOnlyRelevantFacts() {
        Owner owner = createOwner("FactTest");
        Map<String, String> facts = new HashMap<>();
        facts.put("cpu.cpu_socket(s)", "2");             // allowed
        facts.put("network.fqdn", "host.redhat.com");    // allowed
        facts.put("random.fact", "xxx");                 // not allowed
        Consumer consumer = createConsumer(owner, null, null, facts, null, null);

        List<ConsumerFeed> consumerFeeds =
            rhsmApiCompatCurator.getConsumerFeed(owner, null, null, null, 1, 100);

        assertThat(consumerFeeds.get(0))
            .extracting(ConsumerFeed::getFacts)
            .satisfies(x -> {
                assertThat(x).containsEntry("cpu.cpu_socket(s)", "2");
                assertThat(x).containsEntry("network.fqdn", "host.redhat.com");
                assertThat(x).doesNotContainKey("random.fact");
            });
    }

    @Test
    public void testGetAddOnsByConsumerReturnsAllAddOns() {
        Owner owner = createOwner("AddOnTest");
        HashSet<String> addOns = new HashSet<>();
        addOns.add("addon1");
        addOns.add("addon2");
        Consumer consumer = createConsumer(owner, null, null, null, addOns, null);

        List<ConsumerFeed> consumerFeed = rhsmApiCompatCurator.getConsumerFeed(owner, null, null, null, 1,
            100);

        assertThat(consumerFeed.get(0))
            .extracting(ConsumerFeed::getSyspurposeAddons)
            .satisfies(x -> {
                assertThat(x).containsExactlyInAnyOrder("addon1", "addon2");
            });
    }

    @Test
    public void testGetConsumerFeedReturnsInstalledProducts() {
        Owner owner = createOwner("ProductTest");
        ConsumerInstalledProduct installedProd1 = createConsumerInstalledProduct();
        ConsumerInstalledProduct installedProd2 = createConsumerInstalledProduct();
        Consumer consumer = createConsumer(owner, null, null, null, null, List.of(installedProd1,
            installedProd2));

        List<ConsumerFeed> consumerFeed = rhsmApiCompatCurator.getConsumerFeed(owner, null, null, null, 1,
            100);

        List<ConsumerInstalledProduct> list = consumerFeed.get(0).getInstalledProducts();
        assertThat(list)
            .extracting(ConsumerInstalledProduct::getProductId)
            .containsExactlyInAnyOrder(installedProd1.getProductId(), installedProd2.getProductId());
        assertThat(list)
            .extracting(ConsumerInstalledProduct::getProductName)
            .contains(installedProd1.getProductName(), installedProd2.getProductName());
    }

    private Consumer createConsumer(Owner owner, String uuid, Date lastCheckin, Map<String, String> facts,
        Set<String> addons, List<ConsumerInstalledProduct> installedProducts) {
        Consumer consumer = new Consumer().setOwner(owner).setName("test-consumer")
            .setType(this.createConsumerType()).setUuid(uuid).setLastCheckin(lastCheckin).setFacts(facts)
            .setAddOns(addons).setInstalledProducts(installedProducts);

        return this.consumerCurator.create(consumer);
    }

    private ConsumerInstalledProduct createConsumerInstalledProduct() {
        ConsumerInstalledProduct source = new ConsumerInstalledProduct();

        source.setProductId(TestUtil.randomString("test_product_id"));
        source.setProductName(TestUtil.randomString("test_product_name"));
        source.setVersion(TestUtil.randomString("test_version"));
        source.setArch(TestUtil.randomString("test_arch"));
        source.setStatus(TestUtil.randomString("test_status"));
        source.setStartDate(new Date());
        source.setEndDate(new Date());
        source.setCreated(new Date());
        source.setUpdated(new Date());

        return source;
    }

}
