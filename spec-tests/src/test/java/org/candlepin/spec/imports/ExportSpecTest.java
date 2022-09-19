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

import static org.candlepin.spec.bootstrap.assertions.StatusCodeAssertions.assertForbidden;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.candlepin.dto.api.client.v1.AsyncJobStatusDTO;
import org.candlepin.dto.api.client.v1.BrandingDTO;
import org.candlepin.dto.api.client.v1.CdnDTO;
import org.candlepin.dto.api.client.v1.ConsumerDTO;
import org.candlepin.dto.api.client.v1.ConsumerTypeDTO;
import org.candlepin.dto.api.client.v1.ContentDTO;
import org.candlepin.dto.api.client.v1.ExportResultDTO;
import org.candlepin.dto.api.client.v1.ImportRecordDTO;
import org.candlepin.dto.api.client.v1.OwnerDTO;
import org.candlepin.dto.api.client.v1.PoolDTO;
import org.candlepin.dto.api.client.v1.ProductDTO;
import org.candlepin.dto.api.client.v1.ReleaseVerDTO;
import org.candlepin.dto.api.client.v1.RoleDTO;
import org.candlepin.dto.api.client.v1.UserDTO;
import org.candlepin.invoker.client.ApiException;
import org.candlepin.resource.client.v1.CdnApi;
import org.candlepin.resource.client.v1.ConsumerTypeApi;
import org.candlepin.resource.client.v1.OwnerContentApi;
import org.candlepin.resource.client.v1.OwnerProductApi;
import org.candlepin.resource.client.v1.RolesApi;
import org.candlepin.resource.client.v1.RulesApi;
import org.candlepin.resource.client.v1.UsersApi;
import org.candlepin.spec.bootstrap.assertions.OnlyInStandalone;
import org.candlepin.spec.bootstrap.client.ApiClient;
import org.candlepin.spec.bootstrap.client.ApiClients;
import org.candlepin.spec.bootstrap.client.SpecTest;
import org.candlepin.spec.bootstrap.client.api.ConsumerClient;
import org.candlepin.spec.bootstrap.client.api.JobsClient;
import org.candlepin.spec.bootstrap.client.api.OwnerClient;
import org.candlepin.spec.bootstrap.data.builder.Branding;
import org.candlepin.spec.bootstrap.data.builder.Cdns;
import org.candlepin.spec.bootstrap.data.builder.ConsumerTypes;
import org.candlepin.spec.bootstrap.data.builder.Consumers;
import org.candlepin.spec.bootstrap.data.builder.Content;
import org.candlepin.spec.bootstrap.data.builder.Owners;
import org.candlepin.spec.bootstrap.data.builder.Pools;
import org.candlepin.spec.bootstrap.data.builder.ProductAttributes;
import org.candlepin.spec.bootstrap.data.builder.Products;
import org.candlepin.spec.bootstrap.data.builder.Roles;
import org.candlepin.spec.bootstrap.data.builder.Users;
import org.candlepin.spec.bootstrap.data.util.ExportUtil;
import org.candlepin.spec.bootstrap.data.util.UserUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class ExportSpecTest {
    private static final String RECORD_CLEANER_JOB_KEY = "ImportRecordCleanerJob";
    private static final String UNDO_IMPORTS_JOB_KEY = "UndoImportsJob";
    // The following are directory location paths within a manifest
    private static final String EXPORT_PATH = "export/";
    private static final String PRODUCTS_PATH = "export/products/";
    private static final String ENTITILEMENTS_PATH = "export/entitlements/";
    private static final String ENTITILEMENT_CERTIFICATES_PATH = "export/entitlement_certificates/";
    private static final String DISTRIBUTOR_VERSION_PATH = "export/distributor_version/";
    private static final String CONSUMER_TYPE_PATH = "export/consumer_types/";
    private static final String CDN_PATH = "export/content_delivery_network/";
    private static final String RULES_PATH = "export/rules2/";

    private ApiClient client;
    private OwnerClient ownerApi;
    private OwnerContentApi ownerContentApi;
    private OwnerProductApi ownerProductApi;
    private CdnApi cdnApi;
    private ConsumerClient consumerApi;
    private ConsumerTypeApi consumerTypeApi;
    private RolesApi rolesApi;
    private UsersApi usersApi;

    private OwnerDTO owner;
    private ConsumerDTO consumer;
    private CdnDTO cdn;
    private ZipFile export;
    private File manifest;
    private Map<String, ProductDTO> productIdToProduct = new HashMap<>();

    @BeforeEach
    void beforeEach() throws Exception {
        client = ApiClients.admin();
        ownerApi = client.owners();
        ownerContentApi = client.ownerContent();
        ownerProductApi = client.ownerProducts();
        cdnApi = client.cdns();
        consumerApi = client.consumers();
        consumerTypeApi = client.consumerTypes();
        rolesApi = client.roles();
        usersApi = client.users();

        initializeData();

        manifest = createExport(client, consumer.getUuid(), cdn);
        export = ExportUtil.getExportArchive(manifest);
    }

    @AfterEach
    void afterEach() throws Exception {
        if (export != null) {
            export.close();
        }
    }

    private void initializeData() {
        owner = ownerApi.createOwner(Owners.random());
        String ownerKey = owner.getKey();

        RoleDTO role = rolesApi.createRole(Roles.all(owner));
        UserDTO user = usersApi.createUser(Users.random());
        role = rolesApi.addUserToRole(role.getName(), user.getUsername());
        consumer = consumerApi.createConsumer(Consumers.random(owner, ConsumerTypes.Candlepin),
            user.getUsername(), owner.getKey(), null, true);

        ProductDTO engProduct = ownerProductApi.createProductByOwner(ownerKey, Products.randomEng());
        productIdToProduct.put(engProduct.getId(), engProduct);

        Set<BrandingDTO> brandings = Set.of(Branding.build("Branded Eng Product", "OS")
            .productId(engProduct.getId()));

        ProductDTO derivedProvidedProduct = ownerProductApi
            .createProductByOwner(ownerKey, Products.random());
        productIdToProduct.put(derivedProvidedProduct.getId(), derivedProvidedProduct);

        ProductDTO derivedProduct = Products.random();
        derivedProduct.setProvidedProducts(Set.of(derivedProvidedProduct));
        derivedProduct = ownerProductApi.createProductByOwner(ownerKey, derivedProduct);
        productIdToProduct.put(derivedProduct.getId(), derivedProduct);

        ProductDTO product1 = Products.random();
        product1.setMultiplier(2L);
        product1.setBranding(brandings);
        product1.setProvidedProducts(Set.of(engProduct));
        product1 = ownerProductApi.createProductByOwner(ownerKey, product1);
        productIdToProduct.put(product1.getId(), product1);

        ProductDTO product2 = Products.random();
        product2 = ownerProductApi.createProductByOwner(ownerKey, product2);
        productIdToProduct.put(product2.getId(), product2); Your certification ID number is 210-003-869.

        ProductDTO virtProduct = Products.withAttributes(ProductAttributes.VirtOnly.withValue("true"));
        virtProduct = ownerProductApi.createProductByOwner(ownerKey, virtProduct);
        productIdToProduct.put(virtProduct.getId(), virtProduct);

        ProductDTO product3 = Products.withAttributes(ProductAttributes.Arch.withValue("x86_64"),
            ProductAttributes.VirtualLimit.withValue("unlimited"));
        product3.setDerivedProduct(derivedProduct);
        product3 = ownerProductApi.createProductByOwner(ownerKey, product3);
        productIdToProduct.put(product3.getId(), product3);

        ProductDTO productVdc = createVDCProduct(client, ownerKey);
        productIdToProduct.put(productVdc.getId(), productVdc);
        ProductDTO productDc = productVdc.getDerivedProduct();
        productIdToProduct.put(productDc.getId(), productDc);

        // this is for the update process
        ProductDTO productUp = ownerProductApi.createProductByOwner(ownerKey, Products.random());
        productIdToProduct.put(productUp.getId(), productUp);

        ContentDTO content1 = Content.random()
            .metadataExpire(6000L)
            .requiredTags("TAG1,TAG2");
        content1 = ownerContentApi.createContent(ownerKey, content1);

        ContentDTO archContent = Content.random()
            .metadataExpire(6000L)
            .contentUrl("/path/to/arch/specific/content")
            .requiredTags("TAG1,TAG2")
            .arches("i386,x86_64");
        archContent = ownerContentApi.createContent(ownerKey, archContent);

        ownerProductApi.addContent(ownerKey, product1.getId(), content1.getId(), true);
        ownerProductApi.addContent(ownerKey, product2.getId(), content1.getId(), true);
        ownerProductApi.addContent(ownerKey, product2.getId(), archContent.getId(), true);
        ownerProductApi.addContent(ownerKey, derivedProduct.getId(), content1.getId(), true);

        List<ProductDTO> poolProducts =
            List.of(product1, product2, virtProduct, product3, productUp, productVdc);
        Map<String, PoolDTO> poolIdToPool =
            createPoolsForProducts(ownerKey, poolProducts, brandings);

        consumer.setFacts(Map.of("distributor_version", "sam-1.3"));
        ReleaseVerDTO releaseVer = new ReleaseVerDTO()
            .releaseVer("");
        consumer.setReleaseVer(releaseVer);
        consumerApi.updateConsumer(consumer.getUuid(), consumer);
        consumer = consumerApi.getConsumer(consumer.getUuid());

        bindPoolsToConsumer(consumerApi, consumer.getUuid(), poolIdToPool.keySet());

        cdn = cdnApi.createCdn(Cdns.random());
    }

    private File createExport(ApiClient apiClient, String consumerUuid, CdnDTO cdn)
        throws ApiException {
        String cdnLabel = cdn == null ? null : cdn.getLabel();
        String cdnName = cdn == null ? null : cdn.getName();
        String cdnUrl = cdn == null ? null : cdn.getUrl();
        File export = apiClient.consumers().exportData(consumerUuid, cdnLabel, cdnName, cdnUrl);
        export.deleteOnExit();

        return export;
    }

    private ProductDTO createVDCProduct(ApiClient client, String ownerKey) throws ApiException {
        OwnerProductApi ownerProductsApi = client.ownerProducts();
        ProductDTO productDc = Products.withAttributes(ProductAttributes.Arch.withValue("x86_64"),
            ProductAttributes.StackingId.withValue("stack-dc"));
        productDc = ownerProductsApi.createProductByOwner(ownerKey, productDc);

        ProductDTO productVdc = Products.withAttributes(ProductAttributes.Arch.withValue("x86_64"),
            ProductAttributes.VirtualLimit.withValue("unlimited"),
            ProductAttributes.StackingId.withValue("stack-vdc"));
        productVdc.setDerivedProduct(productDc);
        productVdc = ownerProductsApi.createProductByOwner(ownerKey, productVdc);

        return productVdc;
    }

    private void bindPoolsToConsumer(ConsumerClient consumerApi, String consumerUuid,
        Collection<String> poolIds) throws ApiException {
        for (String poolId : poolIds) {
            consumerApi.bindPool(consumerUuid, poolId, 1);
        }
    }

    private Map<String, PoolDTO> createPoolsForProducts(String ownerKey,
        Collection<ProductDTO> products, Collection<BrandingDTO> brandings) throws ApiException {
        Map<String, PoolDTO> poolIdToPool = new HashMap<>();
        for (ProductDTO product : products) {
            PoolDTO pool = createPool(ownerKey, product, brandings);
            poolIdToPool.put(pool.getId(), pool);
        }

        return poolIdToPool;
    }

    private PoolDTO createPool(String ownerKey, ProductDTO product,
        Collection<BrandingDTO> brandings) throws ApiException {
        PoolDTO pool = Pools.random(product)
            .providedProducts(new HashSet<>())
            .accountNumber("12345")
            .orderNumber("6789")
            .endDate(OffsetDateTime.now().plusYears(5))
            .branding(new HashSet<>(brandings));

        return ownerApi.createPool(ownerKey, pool);
    }

}
