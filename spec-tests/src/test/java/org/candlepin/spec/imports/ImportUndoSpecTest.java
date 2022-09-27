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

import org.candlepin.dto.api.client.v1.ImportRecordDTO;
import org.candlepin.dto.api.client.v1.OwnerDTO;
import org.candlepin.dto.api.client.v1.PoolDTO;
import org.candlepin.dto.api.client.v1.ProductDTO;
import org.candlepin.dto.api.client.v1.UserDTO;
import org.candlepin.spec.bootstrap.assertions.OnlyInStandalone;
import org.candlepin.spec.bootstrap.client.ApiClient;
import org.candlepin.spec.bootstrap.client.ApiClients;
import org.candlepin.spec.bootstrap.client.SpecTest;
import org.candlepin.spec.bootstrap.data.builder.Export;
import org.candlepin.spec.bootstrap.data.builder.Owners;
import org.candlepin.spec.bootstrap.data.builder.Pools;
import org.candlepin.spec.bootstrap.data.builder.Products;
import org.candlepin.spec.bootstrap.data.util.Importer;
import org.candlepin.spec.bootstrap.data.util.UserUtil;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.List;

/**
 * These tests are run sequentially as they imports/reimports which are heavily
 * modifying the state and as such would not work in concurrent manner.
 */
@SpecTest
@OnlyInStandalone
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
public class ImportUndoSpecTest {

    private ApiClient admin;
    private OwnerDTO owner;
    private ApiClient userClient;
    private PoolDTO customPool;
    private Export export;
    private Importer importer;

    @BeforeAll
    public void beforeAll() {
        admin = ApiClients.admin();
        importer = getImporter(admin);
        owner = admin.owners().createOwner(Owners.random());
        UserDTO user = UserUtil.createUser(admin, owner);
        userClient = ApiClients.trustedUser(user.getUsername());
        ProductDTO product = admin.ownerProducts().createProductByOwner(owner.getKey(), Products.random());
        customPool = admin.owners().createPool(owner.getKey(), Pools.random(product));

        export = importer.generateSimpleExport();
    }

    /**
     * Create an instance of importer in overridable method so that async test can replace it.
     */
    protected Importer getImporter(ApiClient client) {
        return new Importer(client, false);
    }

    @Test
    void shouldUnlinkUpstreamConsumer() {
        this.importer.doImport(owner.getKey(), export.file());
        this.importer.undoImport(owner);

        OwnerDTO updatedOwner = admin.owners().getOwner(owner.getKey());

        assertThat(updatedOwner.getUpstreamConsumer()).isNull();
        assertOnlyCustomPoolPresent(customPool);
    }

    @Test
    void shouldCreateADeleteRecordOnADeletedImport() {
        this.importer.doImport(owner.getKey(), export.file());
        this.importer.undoImport(owner);

        List<ImportRecordDTO> pools = userClient.owners().getImports(owner.getKey());

        assertThat(pools)
            .map(ImportRecordDTO::getStatus)
            .filteredOn("DELETE"::equals)
            .isNotEmpty();
        assertOnlyCustomPoolPresent(customPool);
    }

    @Test
    void shouldBeAbleToReimportWithoutError() {
        this.importer.doImport(owner.getKey(), export.file());
        this.importer.undoImport(owner);

        this.importer.doImport(owner.getKey(), export.file());
        OwnerDTO updatedOwner = admin.owners().getOwner(owner.getKey());

        assertThat(updatedOwner.getUpstreamConsumer())
            .hasFieldOrPropertyWithValue("uuid", export.consumer().getUuid());
        this.importer.undoImport(owner);
        assertOnlyCustomPoolPresent(customPool);
    }

    @Test
    void shouldAllowAnotherOrgToImportTheSameManifest() {
        this.importer.doImport(owner.getKey(), export.file());
        this.importer.undoImport(owner);
        OwnerDTO otherOrg = admin.owners().createOwner(Owners.random());

        assertThatNoException().isThrownBy(() -> this.importer.doImport(otherOrg.getKey(), export.file()));

        admin.owners().deleteOwner(otherOrg.getKey(), true, true);
        assertOnlyCustomPoolPresent(customPool);
    }

    private void assertOnlyCustomPoolPresent(PoolDTO customPool) {
        List<PoolDTO> pools = userClient.pools().listPoolsByOwner(owner.getId());
        assertThat(pools)
            .hasSize(1)
            .map(PoolDTO::getId)
            .containsExactly(customPool.getId());
    }

}
