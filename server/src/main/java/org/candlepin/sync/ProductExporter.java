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

import org.candlepin.dto.ModelTranslator;
import org.candlepin.dto.manifest.v1.ProductDTO;
import org.candlepin.model.Consumer;
import org.candlepin.model.Entitlement;
import org.candlepin.model.Owner;
import org.candlepin.model.OwnerCurator;
import org.candlepin.model.Pool;
import org.candlepin.model.Product;
import org.candlepin.service.ProductServiceAdapter;
import org.candlepin.service.model.CertificateInfo;

import com.google.inject.Inject;

import org.apache.commons.lang.StringUtils;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * ProductExporter
 */
public class ProductExporter {

    private final OwnerCurator ownerCurator;
    private final ProductServiceAdapter productAdapter;
    private final FileExporter<Object> jsonFileExporter;
    private final FileExporter<String> fileExporter;
    private final ModelTranslator translator;

    @Inject
    public ProductExporter(
        OwnerCurator ownerCurator,
        ProductServiceAdapter productAdapter,
        FileExporter<Object> jsonFileExporter,
        FileExporter<String> fileExporter,
        ModelTranslator translator) {
        this.ownerCurator = ownerCurator;
        this.productAdapter = productAdapter;
        this.jsonFileExporter = jsonFileExporter;
        this.fileExporter = fileExporter;
        this.translator = translator;
    }

    public void exportTo(Path exportDir, Consumer consumer) throws ExportCreationException {
        Path productDir = exportDir.resolve("products");

        Map<String, Product> products = productsOf(consumer);

        for (Product product : products.values()) {
            exportProduct(productDir, product);
            if (isRealProduct(product)) {
                exportProductCert(consumer, productDir, product);
            }
        }
    }

    private void exportProductCert(Consumer consumer, Path productDir, Product product) throws ExportCreationException {
        Owner owner = ownerCurator.findOwnerById(consumer.getOwnerId());

        CertificateInfo cert = productAdapter.getProductCertificate(owner.getKey(), product.getId());

        // XXX: not all product adapters implement getProductCertificate,
        // so just skip over this if we get null back
        // XXX: need to decide if the cert should always be in the export, or never.
        if (cert != null) {
            Path certExportFile = productDir.resolve(product.getId() + ".pem");
            export(certExportFile, cert);
        }
    }

    private void exportProduct(Path productDir, Product product) throws ExportCreationException {
        // Clear the owner and UUID so they can be re-generated/assigned on import
        // product.setUuid(null);
        // product.setOwner(null);

        String productId = product.getId();

        Path exportFile = productDir.resolve(productId + ".json");
        export(exportFile, product);
    }

    private Map<String, Product> productsOf(Consumer consumer) {
        Map<String, Product> products = new HashMap<>();
        for (Entitlement entitlement : consumer.getEntitlements()) {
            Pool pool = entitlement.getPool();

            // Don't forget the 'main' product!
            Product product = pool.getProduct();
            products.put(product.getId(), product);

            addProvidedProducts(product.getProvidedProducts(), products);

            // Also need to check for sub products
            Product derivedProduct = product.getDerivedProduct();
            if (derivedProduct != null) {
                products.put(derivedProduct.getId(), derivedProduct);
                addProvidedProducts(derivedProduct.getProvidedProducts(), products);
            }
        }
        return products;
    }

    // Real products have a numeric id.
    private boolean isRealProduct(Product product) {
        return StringUtils.isNumeric(product.getId());
    }

    private void addProvidedProducts(Collection<Product> providedProducts, Map<String, Product> products) {
        if (providedProducts == null || providedProducts.isEmpty()) {
            return;
        }

        for (Product product : providedProducts) {
            if (product != null) {
                products.put(product.getId(), product);
                addProvidedProducts(product.getProvidedProducts(), products);
            }
        }
    }

    private void export(Path exportFile, Product product) throws ExportCreationException {
        this.jsonFileExporter.exportTo(exportFile, this.translator.translate(product, ProductDTO.class));
    }

    private void export(Path exportFile, CertificateInfo cert) throws ExportCreationException {
        this.fileExporter.exportTo(exportFile, cert.getCertificate());
    }

}
