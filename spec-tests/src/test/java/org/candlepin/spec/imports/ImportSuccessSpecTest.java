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
import org.candlepin.dto.api.client.v1.CdnDTO;
import org.candlepin.dto.api.client.v1.ConsumerDTO;
import org.candlepin.dto.api.client.v1.ContentDTO;
import org.candlepin.dto.api.client.v1.ImportRecordDTO;
import org.candlepin.dto.api.client.v1.ImportUpstreamConsumerDTO;
import org.candlepin.dto.api.client.v1.OwnerDTO;
import org.candlepin.dto.api.client.v1.PoolDTO;
import org.candlepin.dto.api.client.v1.ProvidedProductDTO;
import org.candlepin.dto.api.client.v1.SubscriptionDTO;
import org.candlepin.dto.api.client.v1.UpstreamConsumerDTO;
import org.candlepin.dto.api.client.v1.UserDTO;
import org.candlepin.invoker.client.ApiException;
import org.candlepin.spec.bootstrap.assertions.OnlyInStandalone;
import org.candlepin.spec.bootstrap.client.ApiClient;
import org.candlepin.spec.bootstrap.client.ApiClients;
import org.candlepin.spec.bootstrap.data.builder.Consumers;
import org.candlepin.spec.bootstrap.data.builder.Export;
import org.candlepin.spec.bootstrap.data.builder.ExportGenerator;
import org.candlepin.spec.bootstrap.data.builder.Owners;
import org.candlepin.spec.bootstrap.data.util.UserUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

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
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@OnlyInStandalone
public class ImportSuccessSpecTest {

    private static final String CORRELATION_ID = "a7b79f6d-63ca-40d8-8bfb-f255041f4e3a";
    public static final String UNMAPPED_ATTRIBUTE_NAME = "unmapped_guests_only";
    public static final String IMPORT_CONSUMER_UUID = "7e46cc0f-e129-45f2-9d18-68bee849f88a";
    public static final String EXPECTED_CONTENT_URL = "/path/to/arch/specific/content";

    private static ApiClient admin;

    private static OwnerDTO owner;
    private static UserDTO user;
    private static ApiClient userClient;
    private static ConsumerDTO consumer;
    private static Export export;

    @BeforeAll
    static void beforeAll() throws ApiException {
        admin = ApiClients.admin();
        export = new ExportGenerator(admin).full().export();
        owner = admin.owners().createOwner(Owners.random());
        user = UserUtil.createUser(admin, owner);
        userClient = ApiClients.trustedUser(user.getUsername());
        consumer = userClient.consumers().createConsumer(Consumers.random(owner));
//        URL manifest = ImportSuccessSpecTest.class.getClassLoader().getResource("manifests/manifest");
//        try {
//            File file = new File(manifest.toURI());
            importNow(owner.getKey(), export.file());
//        }
//        catch (URISyntaxException e) {
//            throw new RuntimeException(e);
//        }
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

        assertThat(updatedOwner.getUpstreamConsumer().getUuid()).isEqualTo(export.consumerUuid());
    }

    @Test
    void shouldPopulateOriginInfoOfTheImportRecord() throws ApiException {
        List<ImportRecordDTO> imports = userClient.owners().getImports(owner.getKey());

        for (ImportRecordDTO anImport : imports) {
            assertThat(anImport.getGeneratedBy()).isEqualTo("admin");
            assertThat(anImport.getGeneratedDate()).isNotNull();
            assertThat(anImport.getFileName()).isEqualTo(export.file().getName());

            ImportUpstreamConsumerDTO upstreamConsumer = anImport.getUpstreamConsumer();
            assertThat(upstreamConsumer.getUuid()).isEqualTo(export.consumerUuid());
            assertThat(upstreamConsumer.getName()).isEqualTo(export.consumerName());
            assertThat(upstreamConsumer.getOwnerId()).isEqualTo(owner.getId());
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

    // TODO refactor
    @Test
    void shouldStoreTheSubscriptionUpstreamEntitlementCert() {
        // we only want the product that maps to a normal pool
        // i.e. no virt, no multipliers, etc.
        // this is to fix a intermittent test failures when trying
        // to bind to a virt_only or other weird pool
        List<PoolDTO> pools = userClient.pools().listPoolsByOwner(owner.getId());

        PoolDTO pool = pools.stream()
            .filter(poolDTO -> "master".equalsIgnoreCase(poolDTO.getSubscriptionSubKey()))
            .findAny()
            .orElseThrow();

        Map<String, String> subCert = admin.pools().getCert(pool.getId());

        assertThat(subCert.get("key"))
            .startsWith("-----BEGIN PRIVATE KEY-----");
        assertThat(subCert.get("cert"))
            .startsWith("-----BEGIN CERTIFICATE-----");

        JsonNode jsonNode = userClient.consumers().bindPool(consumer.getUuid(), pool.getId(), 1);
        String ent = admin.entitlements().getUpstreamCert(jsonNode.get(0).get("id").asText());

        assertThat(ent)
            .contains(subCert.get("key"))
            .contains(subCert.get("cert"));
    }

    // TODO fixme
    @Test
    void shouldContainsUpstreamConsumer() throws ApiException {
        OwnerDTO importOwner = admin.owners().getOwner(owner.getKey());
        UpstreamConsumerDTO upstreamConsumer = importOwner.getUpstreamConsumer();

        assertThat(upstreamConsumer)
            .hasFieldOrPropertyWithValue("uuid", export.consumerUuid())
            .hasFieldOrPropertyWithValue("name", export.consumerName())
            .hasFieldOrPropertyWithValue("apiUrl", "https://cdn.test.com")
            .hasFieldOrPropertyWithValue("webUrl", "webapp1");
        assertThat(upstreamConsumer.getId()).isNotNull();
        assertThat(upstreamConsumer.getIdCert()).isNotNull();
        assertThat(upstreamConsumer.getType()).isEqualTo(consumer.getType());
    }


    @Test
    void shouldContainAllDerivedProductData() throws ApiException {
//        List<PoolDTO> pools = userClient.pools().listPoolsByProduct(owner.getId(), "");
    }


    @Test
    void shouldContainBrandingInfo() throws ApiException {
//        List<PoolDTO> pools = userClient.pools().listPoolsByOwner(owner.getId());
    }


    @Test
    void shouldPutTheCdnFromTheManifestIntoTheCreatedSubscriptions() throws ApiException {
        List<SubscriptionDTO> subscriptions = admin.owners().getOwnerSubscriptions(owner.getKey());

        assertThat(subscriptions)
            .map(SubscriptionDTO::getCdn)
            .map(CdnDTO::getLabel)
            .containsOnly(export.cdnLabel());
    }

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
