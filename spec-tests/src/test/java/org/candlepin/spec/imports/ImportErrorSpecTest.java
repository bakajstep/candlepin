/**
 * Copyright (c) 2009 - 2022 Red Hat, Inc.
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

package org.candlepin.spec.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.candlepin.spec.bootstrap.assertions.StatusCodeAssertions.assertBadRequest;
import static org.candlepin.spec.bootstrap.assertions.StatusCodeAssertions.assertConflict;

import org.candlepin.dto.api.client.v1.ImportRecordDTO;
import org.candlepin.dto.api.client.v1.OwnerDTO;
import org.candlepin.dto.api.client.v1.PoolDTO;
import org.candlepin.dto.api.client.v1.UpstreamConsumerDTO;
import org.candlepin.dto.api.client.v1.UserDTO;
import org.candlepin.spec.bootstrap.assertions.OnlyInStandalone;
import org.candlepin.spec.bootstrap.client.ApiClient;
import org.candlepin.spec.bootstrap.client.ApiClients;
import org.candlepin.spec.bootstrap.client.SpecTest;
import org.candlepin.spec.bootstrap.data.builder.Consumers;
import org.candlepin.spec.bootstrap.data.builder.Export;
import org.candlepin.spec.bootstrap.data.builder.Owners;
import org.candlepin.spec.bootstrap.data.util.Importer;
import org.candlepin.spec.bootstrap.data.util.UserUtil;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SpecTest
@OnlyInStandalone
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ImportErrorSpecTest {

    private ApiClient admin;
    private OwnerDTO owner;
    private ApiClient userClient;
    private Export export;
    private Export exportOld;
    private Importer importer;

    @BeforeAll
    public void beforeAll() {
        admin = ApiClients.admin();
        importer = getImporter(admin);
        owner = admin.owners().createOwner(Owners.random());
        UserDTO user = UserUtil.createUser(admin, owner);
        userClient = ApiClients.trustedUser(user.getUsername());
        userClient.consumers().createConsumer(Consumers.random(owner));

        exportOld = this.importer.generateSimpleExport();
        export = this.importer.generateSimpleExport();

        this.importer.doImport(owner.getKey(), export.file());
    }

    /**
     * Create an instance of importer in overridable method so that async test can replace it.
     */
    protected Importer getImporter(ApiClient client) {
        return new Importer(client, false);
    }

    @Test
    void shouldReturnCorrectErrorStatusMessageOnADuplicateImport() {
        assertConflict(() -> this.importer.doImport(owner.getKey(), export.file()))
            .hasMessageContaining("MANIFEST_SAME");

        List<ImportRecordDTO> pools = userClient.owners().getImports(owner.getKey());

        assertThat(pools)
            .filteredOn(input -> "FAILURE".equalsIgnoreCase(input.getStatus()))
            .map(ImportRecordDTO::getStatusMessage)
            .contains("Import is the same as existing data");
    }

    @Test
    void shouldAllowForcingTheSameManifest() {
        assertThatNoException().isThrownBy(() -> this.importer.doImport(
            owner.getKey(), export.file(), "MANIFEST_SAME", "DISTRIBUTOR_CONFLICT"));
    }

    @Test
    void shouldAllowImportingOlderManifestsIntoAnotherOwner() {
        // Old Candlepin was blocking imports in multi-tenant deployments if one org imports
        // a manifest, and then another tries with a manifest that is even slightly
        // older. This tests that this restriction no longer applies.
        OwnerDTO otherOwner = admin.owners().createOwner(Owners.random());

        assertThatNoException().isThrownBy(() -> this.importer
            .doImport(otherOwner.getKey(), exportOld.file(), "MANIFEST_SAME", "DISTRIBUTOR_CONFLICT"));
    }

    @Test
    void shouldReturnConflictWhenImportingManifestFromDifferentSubscriptionManagementApplication() {
        OwnerDTO updatedOwner = admin.owners().getOwner(owner.getKey());
        String expected = "Owner has already imported from another subscription management application.";
        Export otherExport = this.importer.generateSimpleExport();

        assertConflict(() -> this.importer.doImport(updatedOwner.getKey(), otherExport.file()))
            .hasMessageContaining("DISTRIBUTOR_CONFLICT")
            .hasMessageContaining(expected);

        assertThat(updatedOwner.getUpstreamConsumer())
            .extracting(UpstreamConsumerDTO::getUuid)
            .isEqualTo(export.consumer().getUuid());

        // Try again and make sure we don't see MANIFEST_SAME appear: (this was a bug)
        assertConflict(() -> this.importer.doImport(updatedOwner.getKey(), otherExport.file()))
            .hasMessageNotContaining("MANIFEST_SAME")
            .hasMessageContaining("DISTRIBUTOR_CONFLICT");
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    void shouldAllowForcingManifestFromDifferentSubscriptionManagementApplication() {
        OwnerDTO otherOwner = admin.owners().createOwner(Owners.random());
        OwnerDTO ownerBefore = admin.owners().getOwner(owner.getKey());
        Set<String> poolsBefore = listPoolIds(owner);
        Export anotherExport = this.importer.generateSimpleExport();

        this.importer.doImport(otherOwner.getKey(), anotherExport.file(), "DISTRIBUTOR_CONFLICT");

        OwnerDTO ownerAfter = admin.owners().getOwner(owner.getKey());
        Set<String> poolsAfter = listPoolIds(owner);

        assertSameUuid(ownerBefore.getUpstreamConsumer(), ownerAfter.getUpstreamConsumer());
        assertThat(poolsBefore)
            .containsAll(poolsAfter);
    }

    @Test
    void shouldReturnBadRequestWhenImportingManifestInUseByAnotherOwner() {
        String msg = "This subscription management application has already been imported by another owner.";
        OwnerDTO otherOwner = admin.owners().createOwner(Owners.random());

        assertBadRequest(() -> this.importer.doImport(otherOwner.getKey(), export.file()))
            .hasMessageContaining(msg);
    }

    private Set<String> listPoolIds(OwnerDTO owner) {
        return userClient.pools().listPoolsByOwner(owner.getId()).stream()
            .map(PoolDTO::getId)
            .collect(Collectors.toSet());
    }

    private void assertSameUuid(UpstreamConsumerDTO consumer, UpstreamConsumerDTO other) {
        assertThat(consumer).isNotNull();
        assertThat(other).isNotNull();
        assertThat(consumer.getUuid())
            .isEqualTo(other.getUuid());
    }

}
