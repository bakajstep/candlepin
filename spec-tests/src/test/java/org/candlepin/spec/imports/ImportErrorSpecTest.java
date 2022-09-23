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
import org.candlepin.dto.api.client.v1.UpstreamConsumerDTO;
import org.candlepin.dto.api.client.v1.UserDTO;
import org.candlepin.invoker.client.ApiException;
import org.candlepin.spec.bootstrap.client.ApiClient;
import org.candlepin.spec.bootstrap.client.ApiClients;
import org.candlepin.spec.bootstrap.data.builder.Consumers;
import org.candlepin.spec.bootstrap.data.builder.Export;
import org.candlepin.spec.bootstrap.data.builder.ExportGenerator;
import org.candlepin.spec.bootstrap.data.builder.Owners;
import org.candlepin.spec.bootstrap.data.util.UserUtil;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ImportErrorSpecTest {

    private static ApiClient admin;
    private static OwnerDTO owner;
    private static ApiClient userClient;
    private static Export export;
    private static Export exportOld;

    @BeforeAll
    static void beforeAll() throws ApiException {
        admin = ApiClients.admin();
        owner = admin.owners().createOwner(Owners.random());
        UserDTO user = UserUtil.createUser(admin, owner);
        userClient = ApiClients.trustedUser(user.getUsername());
        userClient.consumers().createConsumer(Consumers.random(owner));

        exportOld = generateExport();
        export = generateExport();

        importNow(owner.getKey(), export.file());
    }

    private static Export generateExport() {
        try (ExportGenerator exportGenerator = new ExportGenerator(admin)) {
            return exportGenerator.simple().export();
        }
    }

    @Test
    void shouldReturnCorrectErrorStatusMessageOnADuplicateImport() {
        assertConflict(() -> importNow(owner.getKey(), export.file()))
            .hasMessageContaining("MANIFEST_SAME");

        List<ImportRecordDTO> pools = userClient.owners().getImports(owner.getKey());

        assertThat(pools)
            .filteredOn(input -> "FAILURE".equalsIgnoreCase(input.getStatus()))
            .map(ImportRecordDTO::getStatusMessage)
            .containsOnly("Import is the same as existing data");
    }

    @Test
    void shouldAllowForcingTheSameManifest() {
        assertThatNoException()
            .isThrownBy(() -> importNow(owner.getKey(), export.file(), "MANIFEST_SAME", "DISTRIBUTOR_CONFLICT"));
    }

    @Test
    void shouldAllowImportingOlderManifestsIntoAnotherOwner() {
        // Old Candlepin was blocking imports in multi-tenant deployments if one org imports
        // a manifest, and then another tries with a manifest that is even slightly
        // older. This tests that this restriction no longer applies.
        OwnerDTO otherOwner = admin.owners().createOwner(Owners.random());

        assertThatNoException()
            .isThrownBy(() -> importNow(otherOwner.getKey(), exportOld.file(), "MANIFEST_SAME", "DISTRIBUTOR_CONFLICT"));
    }

    @Test
    void shouldReturnConflictWhenImportingManifestFromDifferentSubscriptionManagementApplication() throws ApiException {
        OwnerDTO updatedOwner = admin.owners().getOwner(owner.getKey());
        String expected = "Owner has already imported from another subscription management application.";
        Export otherExport = generateExport();

        assertConflict(() -> importNow(updatedOwner.getKey(), otherExport.file()))
            .hasMessageContaining("DISTRIBUTOR_CONFLICT")
            .hasMessageContaining(expected);

        assertThat(updatedOwner.getUpstreamConsumer())
            .extracting(UpstreamConsumerDTO::getUuid)
            .isEqualTo(export.consumer().getUuid());

        // Try again and make sure we don't see MANIFEST_SAME appear: (this was a bug)
        assertConflict(() -> importNow(updatedOwner.getKey(), otherExport.file()))
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
    @Execution(ExecutionMode.SAME_THREAD)
    void shouldAllowForcingManifestFromDifferentSubscriptionManagementApplication() throws ApiException {
        OwnerDTO otherOwner = admin.owners().createOwner(Owners.random());
        OwnerDTO ownerBefore = admin.owners().getOwner(owner.getKey());
        Set<String> poolsBefore = listPoolIds(owner);
        Export anotherExport = generateExport();

        importNow(otherOwner.getKey(), anotherExport.file(), "DISTRIBUTOR_CONFLICT");

        OwnerDTO ownerAfter = admin.owners().getOwner(owner.getKey());
        Set<String> poolsAfter = listPoolIds(owner);

        assertSameUuid(ownerBefore.getUpstreamConsumer(), ownerAfter.getUpstreamConsumer());
        assertThat(poolsBefore)
            .containsAll(poolsAfter);
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
        String msg = "This subscription management application has already been imported by another owner.";
        OwnerDTO otherOwner = admin.owners().createOwner(Owners.random());

        assertBadRequest(() -> importNow(otherOwner.getKey(), export.file()))
            .hasMessageContaining(msg);
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

    private static void importNow(String ownerKey, File export) throws ApiException {
        admin.owners().importManifest(ownerKey, null, export);
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
