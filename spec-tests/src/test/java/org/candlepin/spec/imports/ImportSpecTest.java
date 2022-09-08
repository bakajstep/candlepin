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

import org.candlepin.ApiException;
import org.candlepin.dto.api.v1.AsyncJobStatusDTO;
import org.candlepin.dto.api.v1.BrandingDTO;
import org.candlepin.dto.api.v1.ConsumerDTO;
import org.candlepin.dto.api.v1.ContentDTO;
import org.candlepin.dto.api.v1.OwnerDTO;
import org.candlepin.dto.api.v1.PoolDTO;
import org.candlepin.dto.api.v1.ProductDTO;
import org.candlepin.dto.api.v1.ProvidedProductDTO;
import org.candlepin.dto.api.v1.ReleaseVerDTO;
import org.candlepin.dto.api.v1.RoleDTO;
import org.candlepin.dto.api.v1.UserDTO;
import org.candlepin.spec.bootstrap.client.ApiClient;
import org.candlepin.spec.bootstrap.client.ApiClients;
import org.candlepin.spec.bootstrap.data.builder.Branding;
import org.candlepin.spec.bootstrap.data.builder.ConsumerTypes;
import org.candlepin.spec.bootstrap.data.builder.Consumers;
import org.candlepin.spec.bootstrap.data.builder.Content;
import org.candlepin.spec.bootstrap.data.builder.Owners;
import org.candlepin.spec.bootstrap.data.builder.ProductAttributes;
import org.candlepin.spec.bootstrap.data.builder.Products;
import org.candlepin.spec.bootstrap.data.builder.Roles;
import org.candlepin.spec.bootstrap.data.util.StringUtil;
import org.candlepin.spec.bootstrap.data.util.UserUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class ImportSpecTest {

    private static final String CORRELATION_ID = "a7b79f6d-63ca-40d8-8bfb-f255041f4e3a";

    private static ApiClient admin;

    private OwnerDTO owner;
    private UserDTO user;
    private ApiClient userClient;

    @BeforeAll
    static void beforeAll() {
        admin = ApiClients.admin();
    }

    @BeforeEach
    void setUp() throws ApiException {
        this.owner = admin.owners().createOwner(Owners.random());
        this.user = UserUtil.createUser(admin, owner);
        this.userClient = ApiClients.trustedUser(this.user.getUsername());
        URL manifest = ImportSpecTest.class.getClassLoader().getResource("manifests/manifest");
        try {
            File file = new File(manifest.toURI());
            importNow(this.owner.getKey(), file);
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void tearDown() throws ApiException {
        admin.owners().deleteOwner(owner.getKey(), true, true);
    }

    //    private void initializeData() throws Exception {
//        OwnerDTO owner = admin.owners().createOwner(Owners.random());
//        String ownerKey = owner.getKey();
//
//        RoleDTO role = admin.roles().createRole(Roles.all(owner));
//        UserDTO user = admin.users().createUser(randomUser());
//        role = admin.roles().addUserToRole(role.getName(), user.getUsername());
//        ConsumerDTO consumer = admin.consumers().createConsumer(Consumers.random(owner, ConsumerTypes.Candlepin),
//            user.getUsername(), owner.getKey(), null, true);
//
//        ProductDTO engProduct = admin.ownerProducts().createProductByOwner(ownerKey, Products.randomEng());
//        productIdToProduct.put(engProduct.getId(), engProduct);
//
//        Set<BrandingDTO> brandings = Set.of(Branding.build("Branded Eng Product", "OS")
//            .productId(engProduct.getId()));
//
//        ProductDTO derivedProvidedProduct = ownerProductApi
//            .createProductByOwner(ownerKey, Products.random());
//        productIdToProduct.put(derivedProvidedProduct.getId(), derivedProvidedProduct);
//
//        ProductDTO derivedProduct = Products.random();
//        derivedProduct.setProvidedProducts(Set.of(derivedProvidedProduct));
//        derivedProduct = ownerProductApi.createProductByOwner(ownerKey, derivedProduct);
//        productIdToProduct.put(derivedProduct.getId(), derivedProduct);
//
//        ProductDTO product1 = Products.random();
//        product1.setMultiplier(2L);
//        product1.setBranding(brandings);
//        product1.setProvidedProducts(Set.of(engProduct));
//        product1 = ownerProductApi.createProductByOwner(ownerKey, product1);
//        productIdToProduct.put(product1.getId(), product1);
//
//        ProductDTO product2 = Products.random();
//        product2 = ownerProductApi.createProductByOwner(ownerKey, product2);
//        productIdToProduct.put(product2.getId(), product2);
//
//        ProductDTO virtProduct = Products.withAttributes(ProductAttributes.VirtualOnly.withValue("true"));
//        virtProduct = ownerProductApi.createProductByOwner(ownerKey, virtProduct);
//        productIdToProduct.put(virtProduct.getId(), virtProduct);
//
//        ProductDTO product3 = Products.withAttributes(ProductAttributes.Arch.withValue("x86_64"),
//            ProductAttributes.VirtualLimit.withValue("unlimited"));
//        product3.setDerivedProduct(derivedProduct);
//        product3 = ownerProductApi.createProductByOwner(ownerKey, product3);
//        productIdToProduct.put(product3.getId(), product3);
//
//        ProductDTO productVdc = createVDCProduct(client, ownerKey);
//        productIdToProduct.put(productVdc.getId(), productVdc);
//        ProductDTO productDc = productVdc.getDerivedProduct();
//        productIdToProduct.put(productDc.getId(), productDc);
//
//        // this is for the update process
//        ProductDTO productUp = ownerProductApi.createProductByOwner(ownerKey, Products.random());
//        productIdToProduct.put(productUp.getId(), productUp);
//
//        ContentDTO content1 = Content.random()
//            .metadataExpire(6000L)
//            .requiredTags("TAG1,TAG2");
//        content1 = ownerContentApi.createContent(ownerKey, content1);
//
//        ContentDTO archContent = Content.random()
//            .metadataExpire(6000L)
//            .contentUrl("/path/to/arch/specific/content")
//            .requiredTags("TAG1,TAG2")
//            .arches("i386,x86_64");
//        archContent = ownerContentApi.createContent(ownerKey, archContent);
//
//        ownerProductApi.addContent(ownerKey, product1.getId(), content1.getId(), true);
//        ownerProductApi.addContent(ownerKey, product2.getId(), content1.getId(), true);
//        ownerProductApi.addContent(ownerKey, product2.getId(), archContent.getId(), true);
//        ownerProductApi.addContent(ownerKey, derivedProduct.getId(), content1.getId(), true);
//
//        List<ProductDTO> poolProducts =
//            List.of(product1, product2, virtProduct, product3, productUp, productVdc);
//        Map<String, PoolDTO> poolIdToPool =
//            createPoolsForProducts(ownerApi, ownerKey, poolProducts, brandings);
//
//        consumer.setFacts(Map.of("distributor_version", "sam-1.3"));
//        ReleaseVerDTO releaseVer = new ReleaseVerDTO()
//            .releaseVer("");
//        consumer.setReleaseVer(releaseVer);
//        admin.consumers().updateConsumer(consumer.getUuid(), consumer);
//        consumer = admin.consumers().getConsumer(consumer.getUuid());
//
//        bindPoolsToConsumer(consumerApi, consumer.getUuid(), poolIdToPool.keySet());
//
//        cdn = cdnApi.createCdn(Cdns.random());
//    }

//    it 'creates pools' do
//    pools = @import_owner_client.list_pools({:owner => @import_owner['id']})
//    pools.length.should == 8
//
//        # Some of these pools must carry provided/derived provided products,
//        # don't care which pool just need to be sure that they're getting
//      # imported at all:
//    provided_found = false
//    derived_found = false
//    pools.each do |pool|
//        if pool['providedProducts'].size > 0
//    provided_found = true
//    end
//        if pool['derivedProvidedProducts'].size > 0
//    derived_found = true
//    end
//        end
//    provided_found.should be true
//    derived_found.should be true
//    end
    @Test
    @DisplayName("should create pools")
    void createsPools() throws ApiException {
        List<PoolDTO> pools = userClient.pools().listPoolsByOwner(owner.getKey());
        assertThat(pools).hasSize(8);

        assertAnyNonEmpty(pools, PoolDTO::getProvidedProducts);
        assertAnyNonEmpty(pools, PoolDTO::getDerivedProvidedProducts);
    }

    //    def import_now
//    lambda { |owner_key, export_file, param_map={}|
//        @cp.import(owner_key, export_file, param_map)
//    }
//    end

    private void importNow(String ownerKey, File export) throws ApiException {
        admin.owners().importManifest(ownerKey, null, export);
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
