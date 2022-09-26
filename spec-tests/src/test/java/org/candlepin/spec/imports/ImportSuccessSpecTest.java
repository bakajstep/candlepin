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
import org.candlepin.spec.bootstrap.client.SpecTest;
import org.candlepin.spec.bootstrap.data.builder.Consumers;
import org.candlepin.spec.bootstrap.data.builder.Export;
import org.candlepin.spec.bootstrap.data.builder.Owners;
import org.candlepin.spec.bootstrap.data.builder.ProductAttributes;
import org.candlepin.spec.bootstrap.data.util.Importer;
import org.candlepin.spec.bootstrap.data.util.UserUtil;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpecTest
@OnlyInStandalone
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ImportSuccessSpecTest {

    private static final String UNMAPPED_ATTRIBUTE_NAME = "unmapped_guests_only";
    private static final String EXPECTED_CONTENT_URL = "/path/to/arch/specific/content";

    private ApiClient admin;
    private OwnerDTO owner;
    private UserDTO user;
    private ApiClient userClient;
    private ConsumerDTO consumer;
    private Export export;
    private Importer importer;

    @BeforeAll
    public void beforeAll() throws ApiException {
        admin = ApiClients.admin();
        importer = getImporter(admin);
        export = importer.generateFullExport();
        owner = admin.owners().createOwner(Owners.random());
        user = UserUtil.createUser(admin, owner);
        userClient = ApiClients.trustedUser(user.getUsername());
        consumer = userClient.consumers().createConsumer(Consumers.random(owner));

        importer.doImport(owner.getKey(), export.file());
    }

    /**
     * Create an instance of importer in overridable method so that async test can replace it.
     */
    protected Importer getImporter(ApiClient client) {
        return new Importer(client, false);
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

        assertThat(updatedOwner.getUpstreamConsumer().getUuid()).isEqualTo(export.consumer().getUuid());
    }

    @Test
    void shouldPopulateOriginInfoOfTheImportRecord() throws ApiException {
        List<ImportRecordDTO> imports = userClient.owners().getImports(owner.getKey());

        for (ImportRecordDTO anImport : imports) {
            assertThat(anImport.getGeneratedBy()).isEqualTo("admin");
            assertThat(anImport.getGeneratedDate()).isNotNull();
            assertThat(anImport.getFileName()).isEqualTo(export.file().getName());

            ImportUpstreamConsumerDTO upstreamConsumer = anImport.getUpstreamConsumer();
            assertThat(upstreamConsumer.getUuid()).isEqualTo(export.consumer().getUuid());
            assertThat(upstreamConsumer.getName()).isEqualTo(export.consumer().getName());
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

    @Test
    void shouldStoreTheSubscriptionUpstreamEntitlementCert() {
        // we only want the product that maps to a normal pool
        // i.e. no virt, no multipliers, etc.
        // this is to fix a intermittent test failures when trying
        // to bind to a virt_only or other weird pool
        List<PoolDTO> pools = userClient.pools().listPoolsByOwner(owner.getId());
        PoolDTO pool = pools.stream()
            .filter(dto -> "master".equalsIgnoreCase(dto.getSubscriptionSubKey()))
            .filter(dto -> !isVirtOnly(dto))
            .findFirst()
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

    @Test
    void shouldContainsUpstreamConsumer() throws ApiException {
        OwnerDTO importOwner = admin.owners().getOwner(owner.getKey());
        UpstreamConsumerDTO upstreamConsumer = importOwner.getUpstreamConsumer();

        assertThat(upstreamConsumer)
            .isNotNull()
            .hasFieldOrPropertyWithValue("uuid", export.consumer().getUuid())
            .hasFieldOrPropertyWithValue("name", export.consumer().getName())
            .hasFieldOrPropertyWithValue("apiUrl", export.cdn().apiUrl())
            .hasFieldOrPropertyWithValue("webUrl", export.cdn().webUrl());
        assertThat(upstreamConsumer.getId()).isNotNull();
        assertThat(upstreamConsumer.getIdCert()).isNotNull();
        assertThat(upstreamConsumer.getType()).isEqualTo(export.consumer().getType());
    }

    @Test
    void shouldContainAllDerivedProductData() throws ApiException {
        String prod3 = export.product(Export.ProductId.product3).getId();
        String derivedProductId = export.product(Export.ProductId.derived_product).getId();
        String derivedProvidedProductId = export.product(Export.ProductId.derived_provided_prod).getId();
        List<PoolDTO> pools = userClient.pools().listPoolsByProduct(owner.getId(), prod3);
        PoolDTO pool = pools.stream().findFirst().orElseThrow();

        assertThat(pool.getDerivedProductId()).isEqualTo(derivedProductId);
        assertThat(pool.getDerivedProvidedProducts())
            .hasSize(1)
            .first()
            .hasFieldOrPropertyWithValue("productId", derivedProvidedProductId);
    }

    @Test
    void shouldContainBrandingInfo() throws ApiException {
        String productId = export.product(Export.ProductId.product1).getId();
        String engProductId = export.product(Export.ProductId.eng_product).getId();
        List<PoolDTO> pools = userClient.pools().listPoolsByProduct(owner.getId(), productId);
        PoolDTO pool = pools.stream().findFirst().orElseThrow();

        assertThat(pool.getBranding())
            .hasSize(1)
            .first()
            .satisfies(branding -> {
                assertThat(branding.getProductId()).isEqualTo(engProductId);
                assertThat(branding.getName()).isEqualTo("Branded Eng Product");
            });
    }

    @Test
    void shouldNotContainBrandingInfo() throws ApiException {
        String productId = export.product(Export.ProductId.product2).getId();
        List<PoolDTO> pools = userClient.pools().listPoolsByProduct(owner.getId(), productId);

        PoolDTO pool = pools.stream().findFirst().orElseThrow();

        assertThat(pool.getBranding()).isEmpty();
    }


    @Test
    void shouldPutTheCdnFromTheManifestIntoTheCreatedSubscriptions() throws ApiException {
        List<SubscriptionDTO> subscriptions = admin.owners().getOwnerSubscriptions(owner.getKey());

        assertThat(subscriptions)
            .map(SubscriptionDTO::getCdn)
            .map(CdnDTO::getLabel)
            .containsOnly(export.cdn().label());
    }

    private boolean isVirtOnly(PoolDTO dto) {
        return dto.getAttributes().stream()
            .anyMatch(ProductAttributes.VirtOnly::isKeyOf);
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

    private void assertAnyNonEmpty(List<PoolDTO> pools,
        Function<PoolDTO, Set<ProvidedProductDTO>> getDerivedProvidedProducts) {
        assertThat(pools)
            .map(getDerivedProvidedProducts)
            .filteredOn(products -> !products.isEmpty())
            .isNotEmpty();
    }
}
