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

import org.candlepin.dto.api.client.v1.AsyncJobStatusDTO;
import org.candlepin.dto.api.client.v1.AttributeDTO;
import org.candlepin.dto.api.client.v1.ContentDTO;
import org.candlepin.dto.api.client.v1.ImportRecordDTO;
import org.candlepin.dto.api.client.v1.ImportUpstreamConsumerDTO;
import org.candlepin.dto.api.client.v1.OwnerDTO;
import org.candlepin.dto.api.client.v1.PoolDTO;
import org.candlepin.dto.api.client.v1.ProvidedProductDTO;
import org.candlepin.dto.api.client.v1.SubscriptionDTO;
import org.candlepin.dto.api.client.v1.UserDTO;
import org.candlepin.invoker.client.ApiException;
import org.candlepin.spec.bootstrap.client.ApiClient;
import org.candlepin.spec.bootstrap.client.ApiClients;
import org.candlepin.spec.bootstrap.data.builder.Owners;
import org.candlepin.spec.bootstrap.data.util.UserUtil;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ImportSpecTest {

    private static final String CORRELATION_ID = "a7b79f6d-63ca-40d8-8bfb-f255041f4e3a";
    public static final String UNMAPPED_ATTRIBUTE_NAME = "unmapped_guests_only";
    public static final String IMPORT_CONSUMER_UUID = "7e46cc0f-e129-45f2-9d18-68bee849f88a";
    public static final String EXPECTED_CONTENT_URL = "/path/to/arch/specific/content";

    private static ApiClient admin;

    private static OwnerDTO owner;
    private static UserDTO user;
    private static ApiClient userClient;

    @BeforeAll
    static void beforeAll() throws ApiException {
        admin = ApiClients.admin();
//    }
//
//    @BeforeEach
//    void setUp() throws ApiException {
        owner = admin.owners().createOwner(Owners.random());
        user = UserUtil.createUser(admin, owner);
        userClient = ApiClients.trustedUser(user.getUsername());
        URL manifest = ImportSpecTest.class.getClassLoader().getResource("manifests/manifest");
        try {
            File file = new File(manifest.toURI());
            importNow(owner.getKey(), file);
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    static void tearDown() throws ApiException {
        admin.owners().deleteOwner(owner.getKey(), true, true);
    }

    @Test
    void shouldCreatePools() throws ApiException {
        List<PoolDTO> pools = userClient.pools().listPoolsByOwner(owner.getId());
        assertThat(pools).hasSize(8);

        assertAnyNonEmpty(pools, PoolDTO::getProvidedProducts);
        assertAnyNonEmpty(pools, PoolDTO::getDerivedProvidedProducts);
    }

    @Test
    void shouldIgnoreMultiplierForPoolQuantity() throws ApiException {
        List<PoolDTO> pools = userClient.pools().listPoolsByOwner(owner.getId());
        assertThat(pools).hasSize(8);

        Set<PoolDTO> mappedPools = filterMappedPools(pools);

        assertThat(mappedPools)
            .map(PoolDTO::getQuantity)
            .containsOnly(1L);
    }

    @Test
    void shouldModifyTheOwnerToReferenceUpstreamConsumer() throws ApiException {
        OwnerDTO updatedOwner = userClient.owners().getOwner(owner.getKey());
        List<PoolDTO> pools = userClient.pools().listPoolsByOwner(owner.getId());
        assertThat(pools).hasSize(8);

        assertThat(updatedOwner.getUpstreamConsumer().getUuid()).isEqualTo(IMPORT_CONSUMER_UUID);
    }

    @Test
    void shouldPopulateOriginInfoOfTheImportRecord() throws ApiException {
        List<ImportRecordDTO> imports = userClient.owners().getImports(owner.getKey());

        for (ImportRecordDTO anImport : imports) {
            assertThat(anImport.getGeneratedBy()).isEqualTo("admin");
            assertThat(anImport.getGeneratedDate()).isAtSameInstantAs(toDate(1662640168));
            assertThat(anImport.getFileName()).isEqualTo("manifest");

            ImportUpstreamConsumerDTO consumer = anImport.getUpstreamConsumer();
            assertThat(consumer.getUuid()).isEqualTo(IMPORT_CONSUMER_UUID);
            assertThat(consumer.getName()).isEqualTo("test_consumer-4D0D09D3");
            assertThat(consumer.getOwnerId()).isEqualTo(owner.getId());
        }
    }

    @Test
    void shouldCreateSuccessRecordOfTheImport() throws ApiException {
        List<ImportRecordDTO> imports = userClient.owners().getImports(owner.getKey());

        assertThat(imports)
            .map(ImportRecordDTO::getStatus)
            .containsOnly("SUCCESS");
    }

    @Test
    void shouldImportArchContentCorrectly() throws ApiException {
        List<ContentDTO> ownerContent = userClient.ownerContent().listOwnerContent(owner.getKey());

        assertThat(ownerContent)
            .filteredOn(content -> EXPECTED_CONTENT_URL.equalsIgnoreCase(content.getContentUrl()))
            .map(ContentDTO::getArches)
            .containsExactly("i386,x86_64");
    }

//    @Test
//    void shouldStoreTheSubscriptionUpstreamEntitlementCert() throws ApiException {
//        List<SubscriptionDTO> subscriptions = admin.owners().getOwnerSubscriptions(owner.getKey());
//
//        // we only want the product that maps to a normal pool
//        // i.e. no virt, no multipliers, etc.
//        // this is to fix a intermittent test failures when trying
//        // to bind to a virt_only or other weird pool
////        sub = sublist.find_all {
////        |s| s.product.id.start_with?("prod2")
////        }
//
//        for (SubscriptionDTO subscription : subscriptions) {
//            System.out.println(subscription);
//        }
//
//        List<PoolDTO> pools = userClient.pools().listPoolsByOwner(owner.getId());
//
//        Set<String> collect = pools.stream()
//            .map(PoolDTO::getSubscriptionSubKey)
//            .collect(Collectors.toSet());
//        System.out.println(collect);
//
//    }

    private static OffsetDateTime toDate(int epochSecond) {
        return OffsetDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), ZoneOffset.UTC);
    }

    private Set<PoolDTO> filterMappedPools(List<PoolDTO> pools) {
        return pools.stream()
            .filter(this::isMapped)
            .collect(Collectors.toSet());
    }

    private boolean isMapped(PoolDTO pool) {
        if (pool == null || pool.getAttributes() == null || pool.getAttributes().isEmpty()) {
            return false;
        }

        boolean isUnmapped = pool.getAttributes().stream()
            .filter(attribute -> UNMAPPED_ATTRIBUTE_NAME.equalsIgnoreCase(attribute.getName()))
            .map(AttributeDTO::getValue)
            .anyMatch(Boolean::parseBoolean);

        return !isUnmapped;
    }

    private static void importNow(String ownerKey, File export) throws ApiException {
        admin.owners().importManifest(ownerKey, List.of("DISTRIBUTOR_CONFLICT"), export);
    }
//    def import_and_wait
//    lambda { |owner_key, export_file, param_map={}|
//        headers = { :correlation_id => @cp_correlation_id }
//        job = @cp.import_async(owner_key, export_file, param_map, headers)
//        # Wait a little longer here as import can take a bit of time
//        wait_for_job(job["id"], 10)
//        status = @cp.get_job(job["id"], true)
//        if status["state"] == "FAILED"
//        raise AsyncImportFailure.new(status)
//            end
//        status["resultData"]
//    }
//    end

    private void importAsync(String ownerKey, File export) throws ApiException {
//        new Request(admin.getApiClient())
//            .addHeader("X-Correlation-ID", CORRELATION_ID)
//            .;
        AsyncJobStatusDTO importJob = admin.owners().importManifestAsync(ownerKey, null, export);
        admin.jobs().waitForJob(importJob);
    }

    private void assertAnyNonEmpty(List<PoolDTO> pools,
        Function<PoolDTO, Set<ProvidedProductDTO>> getDerivedProvidedProducts) {
        assertThat(pools)
            .map(getDerivedProvidedProducts)
            .filteredOn(products -> !products.isEmpty())
            .isNotEmpty();
    }
}
