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

package org.candlepin.spec.bootstrap.data.builder;

import org.candlepin.dto.api.client.v1.BrandingDTO;
import org.candlepin.dto.api.client.v1.CdnDTO;
import org.candlepin.dto.api.client.v1.ConsumerDTO;
import org.candlepin.dto.api.client.v1.ContentDTO;
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
import org.candlepin.resource.client.v1.UsersApi;
import org.candlepin.spec.bootstrap.client.ApiClient;
import org.candlepin.spec.bootstrap.client.api.ConsumerClient;
import org.candlepin.spec.bootstrap.client.api.OwnerClient;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

public class ExportGenerator implements AutoCloseable {

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
    private ExportCdn cdn;
    private ZipFile export;
    private File manifest;
    private Map<String, ProductDTO> productIdToProduct = new HashMap<>();
    private Map<Export.ProductId, ProductDTO> products;

    public ExportGenerator(ApiClient adminClient) {
        client = adminClient;
        ownerApi = client.owners();
        ownerContentApi = client.ownerContent();
        ownerProductApi = client.ownerProducts();
        cdnApi = client.cdns();
        consumerApi = client.consumers();
        consumerTypeApi = client.consumerTypes();
        rolesApi = client.roles();
        usersApi = client.users();
    }

    @Override
    public void close() {
        if (owner != null) {
            this.client.owners().deleteOwner(owner.getKey(), true, true);
        }
    }

    public ExportGenerator full() {
        initializeFullExport();

        return this;
    }

    public ExportGenerator simple() {
        initializeSimpleExport();

        return this;
    }

    public Export export() {
        manifest = createExport(consumer.getUuid(), cdn);

        return new Export(manifest, consumer, cdn, products);
    }

    private void initializeSimpleExport() {
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

        ProductDTO product1 = ownerProductApi.createProductByOwner(ownerKey, Products.random()
            .multiplier(2L)
            .branding(brandings)
            .providedProducts(Set.of(engProduct)));
        productIdToProduct.put(product1.getId(), product1);

        products = Map.ofEntries(
            Map.entry(Export.ProductId.product1, product1)
        );

        ContentDTO content1 = ownerContentApi.createContent(ownerKey, Content.random()
            .metadataExpire(6000L)
            .requiredTags("TAG1,TAG2"));

        ownerProductApi.addContent(ownerKey, product1.getId(), content1.getId(), true);

        List<ProductDTO> poolProducts = List.of(product1);
        Map<String, PoolDTO> poolIdToPool = createPoolsForProducts(ownerKey, poolProducts);

        consumer.putFactsItem("distributor_version", "sam-1.3")
            .releaseVer(new ReleaseVerDTO().releaseVer(""));
        consumerApi.updateConsumer(consumer.getUuid(), consumer);
        consumer = consumerApi.getConsumer(consumer.getUuid());

        bindPoolsToConsumer(consumerApi, consumer.getUuid(),
            poolIdToPool.values().stream().map(PoolDTO::getId).collect(Collectors.toSet()));

        CdnDTO cdn2 = cdnApi.createCdn(Cdns.random());
        cdn = Cdns.toExport(cdn2);
    }

    private void initializeFullExport() {
        initialize();
    }

    @SuppressWarnings("indentation")
    private void initialize() {
        owner = ownerApi.createOwner(Owners.random());
        String ownerKey = owner.getKey();

        RoleDTO role = rolesApi.createRole(Roles.all(owner));
        UserDTO user = usersApi.createUser(Users.random());
        role = rolesApi.addUserToRole(role.getName(), user.getUsername());
        consumer = consumerApi.createConsumer(Consumers.random(owner, ConsumerTypes.Candlepin),
            user.getUsername(), owner.getKey(), null, true);

        ProductDTO engProduct = ownerProductApi
            .createProductByOwner(ownerKey, Products.randomEng().name("eng_prod"));
        productIdToProduct.put(engProduct.getId(), engProduct);

        Set<BrandingDTO> brandings = Set.of(Branding.build("Branded Eng Product", "OS")
            .productId(engProduct.getId()));

        ProductDTO derivedProvidedProduct = ownerProductApi
            .createProductByOwner(ownerKey, Products.random().name("der_prov_prod"));
        productIdToProduct.put(derivedProvidedProduct.getId(), derivedProvidedProduct);

        ProductDTO derivedProduct = ownerProductApi.createProductByOwner(ownerKey, Products
            .withAttributes(ProductAttributes.Cores.withValue("2"))
            .name("der_prod")
            .providedProducts(Set.of(derivedProvidedProduct)));
        productIdToProduct.put(derivedProduct.getId(), derivedProduct);

        ProductDTO product1 = ownerProductApi.createProductByOwner(ownerKey, Products.random()
            .name("prod1")
            .multiplier(2L)
            .branding(brandings)
            .providedProducts(Set.of(engProduct)));
        productIdToProduct.put(product1.getId(), product1);

        ProductDTO product2 = ownerProductApi
            .createProductByOwner(ownerKey, Products.random().name("prod2"));
        productIdToProduct.put(product2.getId(), product2);

        ProductDTO virtProduct = ownerProductApi.createProductByOwner(ownerKey, Products
            .withAttributes(ProductAttributes.VirtOnly.withValue("true"))
            .name("virt_prod"));
        productIdToProduct.put(virtProduct.getId(), virtProduct);

        ProductDTO product3 = ownerProductApi.createProductByOwner(ownerKey, Products
            .withAttributes(
                ProductAttributes.Arch.withValue("x86_64"),
                ProductAttributes.VirtualLimit.withValue("unlimited")
            )
            .name("prod3")
            .derivedProduct(derivedProduct));
        productIdToProduct.put(product3.getId(), product3);

        ProductDTO productDc = client.ownerProducts().createProductByOwner(ownerKey, Products.withAttributes(
            ProductAttributes.Arch.withValue("x86_64"),
            ProductAttributes.StackingId.withValue("stack-dc")
        ).name("dc_prod"));

        ProductDTO productVdc = client.ownerProducts().createProductByOwner(ownerKey, Products
            .withAttributes(
                ProductAttributes.Arch.withValue("x86_64"),
                ProductAttributes.VirtualLimit.withValue("unlimited"),
                ProductAttributes.StackingId.withValue("stack-vdc")
            )
            .name("vdc_prod")
            .derivedProduct(productDc));

        productIdToProduct.put(productVdc.getId(), productVdc);
        productIdToProduct.put(productDc.getId(), productDc);

        // this is for the update process
        ProductDTO productUp = ownerProductApi
            .createProductByOwner(ownerKey, Products.random().name("up_prod"));
        productIdToProduct.put(productUp.getId(), productUp);

        products = Map.ofEntries(
            Map.entry(Export.ProductId.eng_product, engProduct),
            Map.entry(Export.ProductId.derived_provided_prod, derivedProvidedProduct),
            Map.entry(Export.ProductId.derived_product, derivedProduct),
            Map.entry(Export.ProductId.product1, product1),
            Map.entry(Export.ProductId.product2, product2),
            Map.entry(Export.ProductId.product3, product3),
            Map.entry(Export.ProductId.virt_product, virtProduct),
            Map.entry(Export.ProductId.product_dc, productDc),
            Map.entry(Export.ProductId.product_vdc, productVdc),
            Map.entry(Export.ProductId.product_up, productUp)
        );

        ContentDTO content1 = ownerContentApi.createContent(ownerKey, Content.random()
            .metadataExpire(6000L)
            .requiredTags("TAG1,TAG2"));

        ContentDTO archContent = ownerContentApi.createContent(ownerKey, Content.random()
            .metadataExpire(6000L)
            .contentUrl("/path/to/arch/specific/content")
            .requiredTags("TAG1,TAG2")
            .arches("i386,x86_64"));

        ownerProductApi.addContent(ownerKey, product1.getId(), content1.getId(), true);
        ownerProductApi.addContent(ownerKey, product2.getId(), content1.getId(), true);
        ownerProductApi.addContent(ownerKey, product2.getId(), archContent.getId(), true);
        ownerProductApi.addContent(ownerKey, derivedProduct.getId(), content1.getId(), true);

        List<ProductDTO> poolProducts = List.of(
            product1, product2, virtProduct, product3, productUp, productVdc);
        Map<String, PoolDTO> poolIdToPool = createPoolsForProducts(ownerKey, poolProducts);

        consumer.putFactsItem("distributor_version", "sam-1.3")
            .releaseVer(new ReleaseVerDTO().releaseVer(""));
        consumerApi.updateConsumer(consumer.getUuid(), consumer);
        consumer = consumerApi.getConsumer(consumer.getUuid());

        Set<String> poolIds = poolIdToPool.values().stream()
            .map(PoolDTO::getId)
            .collect(Collectors.toSet());
        bindPoolsToConsumer(consumerApi, consumer.getUuid(), poolIds);
//        consumerApi.bindPool(consumer.getUuid(), poolIdToPool.get(product3.getId()).getId(), 1);

        CdnDTO cdn2 = cdnApi.createCdn(Cdns.random());
        cdn = Cdns.toExport(cdn2);
    }

    private File createExport(String consumerUuid, ExportCdn cdn) {
//        String cdnLabel = cdn == null ? null : cdn.getLabel();
//        String cdnName = cdn == null ? null : cdn.getName();
//        String cdnUrl = cdn == null ? null : cdn.getUrl();
        File export = client.consumers().exportData(consumerUuid, cdn.label(), cdn.webUrl(), cdn.apiUrl());
        export.deleteOnExit();

        return export;
    }

    private void bindPoolsToConsumer(ConsumerClient consumerApi, String consumerUuid,
        Collection<String> poolIds) throws ApiException {
        for (String poolId : poolIds) {
            consumerApi.bindPool(consumerUuid, poolId, 1);
        }
    }

    private Map<String, PoolDTO> createPoolsForProducts(String ownerKey, Collection<ProductDTO> products) {
        Set<PoolDTO> collect = products.stream()
            .map(product -> createPool(ownerKey, product))
            .collect(Collectors.toSet());

        return collect.stream()
            .collect(Collectors.toMap(PoolDTO::getProductId, Function.identity()));
    }

    private PoolDTO createPool(String ownerKey, ProductDTO product) throws ApiException {
        PoolDTO pool = Pools.random(product)
            .providedProducts(new HashSet<>())
            .contractNumber("")
            .accountNumber("12345")
            .orderNumber("6789")
            .endDate(OffsetDateTime.now().plusYears(5));

        if (product.getBranding() == null || product.getBranding().isEmpty()) {
            pool.setBranding(null);
        }
        else {
            pool.setBranding(new HashSet<>(product.getBranding()));
        }


        return ownerApi.createPool(ownerKey, pool);
    }
}
