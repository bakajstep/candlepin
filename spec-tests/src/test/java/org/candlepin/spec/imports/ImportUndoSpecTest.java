/*
 *  Copyright (c) 2009 - ${YEAR} Red Hat, Inc.
 *
 *  This software is licensed to you under the GNU General Public License,
 *  version 2 (GPLv2). There is NO WARRANTY for this software, express or
 *  implied, including the implied warranties of MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 *  along with this software; if not, see
 *  http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 *  Red Hat trademarks are not licensed under GPLv2. No permission is
 *  granted to use or replicate Red Hat trademarks that are incorporated
 *  in this software or its documentation.
 */

package org.candlepin.spec.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.candlepin.dto.api.client.v1.AsyncJobStatusDTO;
import org.candlepin.dto.api.client.v1.ImportRecordDTO;
import org.candlepin.dto.api.client.v1.OwnerDTO;
import org.candlepin.dto.api.client.v1.PoolDTO;
import org.candlepin.dto.api.client.v1.ProductDTO;
import org.candlepin.dto.api.client.v1.UserDTO;
import org.candlepin.invoker.client.ApiException;
import org.candlepin.spec.bootstrap.client.ApiClient;
import org.candlepin.spec.bootstrap.client.ApiClients;
import org.candlepin.spec.bootstrap.data.builder.Export;
import org.candlepin.spec.bootstrap.data.builder.ExportGenerator;
import org.candlepin.spec.bootstrap.data.builder.Owners;
import org.candlepin.spec.bootstrap.data.builder.Pools;
import org.candlepin.spec.bootstrap.data.builder.Products;
import org.candlepin.spec.bootstrap.data.util.UserUtil;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

/**
 * These tests are run sequentially as they imports/reimports which are heavily
 * modifying the state and as such would not work in concurrent manner.
 */
@Execution(ExecutionMode.SAME_THREAD)
public class ImportUndoSpecTest {

    private static ApiClient admin;

    private static OwnerDTO owner;
    private static ApiClient userClient;
    private static PoolDTO customPool;
    private static Export export;

    @BeforeAll
    static void beforeAll() throws ApiException {
        admin = ApiClients.admin();
        owner = admin.owners().createOwner(Owners.random());
        UserDTO user = UserUtil.createUser(admin, owner);
        userClient = ApiClients.trustedUser(user.getUsername());
        ProductDTO product = admin.ownerProducts().createProductByOwner(owner.getKey(), Products.random());
        customPool = admin.owners().createPool(owner.getKey(), Pools.random(product));

        try (ExportGenerator exportGenerator = new ExportGenerator(admin)) {
            export = exportGenerator.simple().export();
        }
    }

    @Test
    void shouldUnlinkUpstreamConsumer() throws ApiException {
        importNow(owner.getKey(), export.file());
        undoImport(owner);

        OwnerDTO updatedOwner = admin.owners().getOwner(owner.getKey());

        assertThat(updatedOwner.getUpstreamConsumer()).isNull();
        assertOnlyCustomPoolPresent(customPool);
    }

    @Test
    void shouldCreateADeleteRecordOnADeletedImport() throws ApiException {
        importNow(owner.getKey(), export.file());
        undoImport(owner);

        List<ImportRecordDTO> pools = userClient.owners().getImports(owner.getKey());

        assertThat(pools)
            .map(ImportRecordDTO::getStatus)
            .filteredOn("DELETE"::equals)
            .isNotEmpty();
        assertOnlyCustomPoolPresent(customPool);
    }

    @Test
    void shouldBeAbleToReimportWithoutError() throws ApiException {
        importNow(owner.getKey(), export.file());
        undoImport(owner);

        importNow(owner.getKey(), export.file());
        OwnerDTO asd2 = admin.owners().getOwner(owner.getKey());

        assertThat(asd2.getUpstreamConsumer())
            .hasFieldOrPropertyWithValue("uuid", export.consumer().getUuid());
        undoImport(owner);
        assertOnlyCustomPoolPresent(customPool);
    }

    @Test
    void shouldAllowAnotherOrgToImportTheSameManifest() throws ApiException {
        importNow(owner.getKey(), export.file());
        undoImport(owner);
        OwnerDTO otherOrg = admin.owners().createOwner(Owners.random());

        assertThatNoException().isThrownBy(() -> importNow(otherOrg.getKey(), export.file()));

        admin.owners().deleteOwner(otherOrg.getKey(), true, true);
        assertOnlyCustomPoolPresent(customPool);
    }

    private static void undoImport(OwnerDTO owner) throws ApiException {
        List<ImportRecordDTO> imports = admin.owners().getImports(owner.getKey());
        if (imports.stream().map(ImportRecordDTO::getStatus).noneMatch("DELETED"::equals)) {
            AsyncJobStatusDTO job = admin.owners().undoImports(owner.getKey());
            admin.jobs().waitForJob(job);
        }
    }

    private void assertOnlyCustomPoolPresent(PoolDTO customPool) throws ApiException {
        List<PoolDTO> pools = userClient.pools().listPoolsByOwner(owner.getId());
        assertThat(pools)
            .hasSize(1)
            .map(PoolDTO::getId)
            .containsExactly(customPool.getId());
    }

    private static File getImportFile(URL manifest) {
        try {
            return new File(manifest.toURI());
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static void importNow(String ownerKey, File export) throws ApiException {
        admin.owners().importManifest(ownerKey, List.of("DISTRIBUTOR_CONFLICT"), export);
    }

    private void importAsync(String ownerKey, File export) throws ApiException {
//        new Request(admin.getApiClient())
//            .addHeader("X-Correlation-ID", CORRELATION_ID)
//            .;
        AsyncJobStatusDTO importJob = admin.owners().importManifestAsync(ownerKey, null, export);
        admin.jobs().waitForJob(importJob);
    }

}
