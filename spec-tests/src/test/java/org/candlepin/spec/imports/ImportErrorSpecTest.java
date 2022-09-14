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
import static org.candlepin.spec.bootstrap.assertions.StatusCodeAssertions.assertBadRequest;
import static org.candlepin.spec.bootstrap.assertions.StatusCodeAssertions.assertConflict;

import org.candlepin.dto.api.client.v1.AsyncJobStatusDTO;
import org.candlepin.dto.api.client.v1.ImportRecordDTO;
import org.candlepin.dto.api.client.v1.OwnerDTO;
import org.candlepin.dto.api.client.v1.PoolDTO;
import org.candlepin.dto.api.client.v1.ProductDTO;
import org.candlepin.dto.api.client.v1.UserDTO;
import org.candlepin.invoker.client.ApiException;
import org.candlepin.spec.bootstrap.assertions.StatusCodeAssertions;
import org.candlepin.spec.bootstrap.client.ApiClient;
import org.candlepin.spec.bootstrap.client.ApiClients;
import org.candlepin.spec.bootstrap.data.builder.Owners;
import org.candlepin.spec.bootstrap.data.builder.Pools;
import org.candlepin.spec.bootstrap.data.builder.Products;
import org.candlepin.spec.bootstrap.data.util.UserUtil;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class ImportErrorSpecTest {

    public static final String IMPORT_CONSUMER_UUID = "7e46cc0f-e129-45f2-9d18-68bee849f88a";

    private static ApiClient admin;

    private static OwnerDTO owner;
    private static ApiClient userClient;
    private static File importFile;
    private static PoolDTO customPool;

    @BeforeAll
    static void beforeAll() throws ApiException {
        admin = ApiClients.admin();
        owner = admin.owners().createOwner(Owners.random());
        UserDTO user = UserUtil.createUser(admin, owner);
        userClient = ApiClients.trustedUser(user.getUsername());
        ProductDTO product = admin.ownerProducts().createProductByOwner(owner.getKey(), Products.random());
        customPool = admin.owners().createPool(owner.getKey(), Pools.random(product));

        URL manifest = ImportErrorSpecTest.class.getClassLoader().getResource("manifests/manifest");
        importFile = getImportFile(manifest);
        importNow(owner.getKey(), importFile);
    }

    @AfterAll
    static void tearDown() throws ApiException {
        admin.owners().deleteOwner(owner.getKey(), true, true);
    }

    @Test
    void shouldReturnAnErrorOnADuplicateImport() {
        assertConflict(() -> importNow(owner.getKey(), importFile))
            .hasMessageContaining("MANIFEST_SAME");
    }

    @Test
    void shouldCreateADeleteRecordOnADeletedImport() throws ApiException {
        List<ImportRecordDTO> pools = userClient.owners().getImports(owner.getKey());

        assertThat(pools)
            .map(ImportRecordDTO::getStatus)
            .filteredOn("FAILURE"::equals)
            .isNotEmpty();
    }

    @Test
    void shouldSetTheCorrectErrorStatusMessage() throws ApiException {
        List<ImportRecordDTO> pools = userClient.owners().getImports(owner.getKey());

        assertThat(pools)
            .filteredOn(input -> "FAILURE".equalsIgnoreCase(input.getStatus()))
            .map(ImportRecordDTO::getStatusMessage)
            .containsOnly("Import is the same as existing data");
    }

    @Test
    void shouldAllowForcingTheSameManifest() {
        assertThatNoException()
            .isThrownBy(() -> importNow(owner.getKey(), importFile, "MANIFEST_SAME", "DISTRIBUTOR_CONFLICT"));
    }

    @Test
    void shouldAllowImportingOlderManifestsIntoAnotherOwner() {
        // TODO use old import
        assertThatNoException()
            .isThrownBy(() -> importNow(owner.getKey(), importFile, "MANIFEST_SAME", "DISTRIBUTOR_CONFLICT"));
    }

    @Test
    void shouldReturnConflictWhenImportingManifestFromDifferentSubscriptionManagementApplication() throws ApiException {
        OwnerDTO otherOrg = admin.owners().createOwner(Owners.random());

        assertConflict(() -> importNow(otherOrg.getKey(), importFile))
            .hasMessageContaining("DISTRIBUTOR_CONFLICT");

        assertThat(owner.getUpstreamConsumer().getUuid()).isEqualTo(IMPORT_CONSUMER_UUID);

        assertConflict(() -> importNow(otherOrg.getKey(), importFile))
            .hasMessageNotContaining("MANIFEST_SAME")
            .hasMessageContaining("DISTRIBUTOR_CONFLICT");
    }

//       # TODO
//    it 'should allow forcing a manifest from a different subscription management application' do
//    exporter = StandardExporter.new
//    @exporters << exporter
//        another = exporter.create_candlepin_export().export_filename
//
//    old_upstream_uuid = @cp.get_owner(@import_owner['key'])['upstreamConsumer']['uuid']
//    pools = @cp.list_owner_pools(@import_owner['key'])
//    pool_ids = pools.collect { |p| p['id'] }
//    @import_method.call(@import_owner['key'], another,
//        {:force => ['DISTRIBUTOR_CONFLICT']})
//    @cp.get_owner(@import_owner['key'])['upstreamConsumer']['uuid'].should_not == old_upstream_uuid
//        pools = @cp.list_owner_pools(@import_owner['key'])
//        new_pool_ids = pools.collect { |p| p['id'] }
//      # compare without considering order, pools should have changed completely:
//    new_pool_ids.should_not =~ pool_ids
//        end
    @Test
    void shouldAllowForcingManifestFromDifferentSubscriptionManagementApplication() throws ApiException {
        OwnerDTO otherOrg = admin.owners().createOwner(Owners.random());

        assertConflict(() -> importNow(otherOrg.getKey(), importFile))
            .hasMessageContaining("DISTRIBUTOR_CONFLICT");

        assertThat(owner.getUpstreamConsumer().getUuid()).isEqualTo(IMPORT_CONSUMER_UUID);

        assertConflict(() -> importNow(otherOrg.getKey(), importFile))
            .hasMessageNotContaining("MANIFEST_SAME")
            .hasMessageContaining("DISTRIBUTOR_CONFLICT");
    }

//    # TODO
//    it 'should return 400 when importing manifest in use by another owner' do
//        # Because the previous tests put the original import into a different state
//      # than if you just run this single one, we need to clear first and then
//      # re-import the original.
//        # Also added the confirmation that the exception occurs when importing to
//      # another owner.
//    job = @import_owner_client.undo_import(@import_owner['key'])
//    wait_for_job(job['id'], 30)
//
//    @import_method.call(@import_owner['key'], @cp_export_file)
//    owner2 = @cp.create_owner(random_string("owner2"))
//    exception = false
//    expected = "This subscription management application has already been imported by another owner."
//    begin
//    @import_method.call(owner2['key'], @cp_export_file)
//    rescue RestClient::Exception => e
//    async.should be false
//    e.http_code.should == 400
//    json = JSON.parse(e.http_body)
//    exception = true
//    rescue AsyncImportFailure => aif
//    async.should be true
//    json = aif.data["resultData"]
//    json.should_not be_nil
//    exception = true
//    end
//
//    @cp.delete_owner(owner2['key'])
//    exception.should be true
//        if async
//    json.include?(expected).should be true
//        else
//    message = json["displayMessage"]
//    message.should_not be_nil
//    message.should == expected
//        end
//    end
    @Test
    void shouldReturnBadRequestWhenImportingManifestInUseByAnotherOwner() throws ApiException {
        OwnerDTO otherOrg = admin.owners().createOwner(Owners.random());

        assertBadRequest(() -> importNow(otherOrg.getKey(), importFile))
            .hasMessageContaining("DISTRIBUTOR_CONFLICT");

        assertThat(owner.getUpstreamConsumer().getUuid()).isEqualTo(IMPORT_CONSUMER_UUID);

        assertConflict(() -> importNow(otherOrg.getKey(), importFile))
            .hasMessageNotContaining("MANIFEST_SAME")
            .hasMessageContaining("DISTRIBUTOR_CONFLICT");
    }

    private static void undoImport(OwnerDTO owner) throws ApiException {
        AsyncJobStatusDTO job = admin.owners().undoImports(owner.getKey());
        admin.jobs().waitForJob(job);
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

    private static void importNow(String ownerKey, File export, String... force) throws ApiException {
        admin.owners().importManifest(ownerKey, Arrays.asList(force), export);
    }

    private void importAsync(String ownerKey, File export) throws ApiException {
//        new Request(admin.getApiClient())
//            .addHeader("X-Correlation-ID", CORRELATION_ID)
//            .;
        AsyncJobStatusDTO importJob = admin.owners().importManifestAsync(ownerKey, null, export);
        admin.jobs().waitForJob(importJob);
    }

}
