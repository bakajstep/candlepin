/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
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
package org.candlepin.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.candlepin.dto.ModelTranslator;
import org.candlepin.dto.SimpleModelTranslator;
import org.candlepin.dto.StandardTranslator;
import org.candlepin.dto.manifest.v1.ContentDTO;
import org.candlepin.dto.manifest.v1.ContentTranslator;
import org.candlepin.dto.manifest.v1.EntitlementDTO;
import org.candlepin.dto.manifest.v1.EntitlementTranslator;
import org.candlepin.dto.manifest.v1.ProductDTO;
import org.candlepin.dto.manifest.v1.ProductTranslator;
import org.candlepin.model.Consumer;
import org.candlepin.model.ConsumerTypeCurator;
import org.candlepin.model.Content;
import org.candlepin.model.Entitlement;
import org.candlepin.model.EnvironmentCurator;
import org.candlepin.model.Owner;
import org.candlepin.model.OwnerCurator;
import org.candlepin.model.Pool;
import org.candlepin.model.Product;
import org.candlepin.model.ProductCertificate;
import org.candlepin.service.ProductServiceAdapter;
import org.candlepin.test.TestUtil;

import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;


public class ProductExporterTest {

    private static final Path EXPORT_PATH = Paths.get("/export");
    private static final String EXPECTED_CERT = "hpj-08ha-w4gpoknpon*)&^%#";
    private static final String PRODUCT_ID_1 = "12345";
    private static final String PRODUCT_ID_2 = "MKT-prod";
    private static final String PRODUCT_ID_3 = "MKT-sub-prod";
    private static final String PRODUCT_ID_4 = "332211";

    private OwnerCurator ownerCurator;
    private ProductServiceAdapter productAdapter;

    @Before
    public void setUp() throws Exception {
        ownerCurator = mock(OwnerCurator.class);
        productAdapter = mock(ProductServiceAdapter.class);
    }

    @Test
    public void exportProducts() throws Exception {
        Consumer consumer = mock(Consumer.class);
        Entitlement ent = mock(Entitlement.class);

        Set<Entitlement> entitlements = new HashSet<>();
        entitlements.add(ent);

        Owner owner = TestUtil.createOwner("Example-Corporation");

        Product prod = createProduct(PRODUCT_ID_1, "RHEL Product");
        Product prod1 = createProduct(PRODUCT_ID_2, "RHEL Product");
        Product subProduct = createProduct(PRODUCT_ID_3, "Sub Product");
        Product subProvidedProduct = createProduct(PRODUCT_ID_4, "Sub Product");

        prod1.addProvidedProduct(prod);
        prod1.setDerivedProduct(subProduct);
        subProduct.addProvidedProduct(subProvidedProduct);

        ProductCertificate pcert = new ProductCertificate();
        pcert.setKey("euh0876puhapodifbvj094");
        pcert.setCert(EXPECTED_CERT);
        pcert.setCreated(new Date());
        pcert.setUpdated(new Date());

        Pool pool = TestUtil.createPool(owner)
            .setId("MockedPoolId")
            .setProduct(prod1);

        when(ent.getPool()).thenReturn(pool);
        when(consumer.getEntitlements()).thenReturn(entitlements);
        when(productAdapter.getProductCertificate(any(String.class), any(String.class))).thenReturn(pcert);

        when(consumer.getOwnerId()).thenReturn(owner.getId());
        when(ownerCurator.findOwnerById(eq(owner.getId()))).thenReturn(owner);

        SpyingExporter<Object> jsonExporter = new SpyingExporter<>();
        SpyingExporter<String> certExporter = new SpyingExporter<>();
        ModelTranslator translator = new SimpleModelTranslator();
        translator.registerTranslator(new ProductTranslator(), Product.class, ProductDTO.class);
        translator.registerTranslator(new ContentTranslator(), Content.class, ContentDTO.class);
        ProductExporter exporter = new ProductExporter(ownerCurator, productAdapter,
            jsonExporter, certExporter, translator);

        exporter.exportTo(EXPORT_PATH, consumer);

        assertThat(jsonExporter.calledTimes).isEqualTo(4);
        assertThat(certExporter.calledTimes).isEqualTo(2);
        assertThat(jsonExporter.exports)
            .map(objects -> objects.get(0))
            .map(objects -> (ProductDTO) objects)
            .map(ProductDTO::getId)
            .containsAll(Arrays.asList(
                PRODUCT_ID_1,
                PRODUCT_ID_2,
                PRODUCT_ID_3,
                PRODUCT_ID_4
            ));
        assertThat(certExporter.exports.get(0).get(0)).isEqualTo(EXPECTED_CERT);
        assertThat(certExporter.exports.get(1).get(0)).isEqualTo(EXPECTED_CERT);
    }

    private Product createProduct(String id, String name) {
        Product product = TestUtil.createProduct(id, name);
        product.setMultiplier(1L);
        product.setCreated(new Date());
        product.setUpdated(new Date());
        product.setAttributes(Collections.emptyMap());
        return product;
    }

}
