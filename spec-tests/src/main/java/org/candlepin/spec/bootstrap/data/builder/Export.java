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

package org.candlepin.spec.bootstrap.data.builder;

import org.candlepin.dto.api.client.v1.BrandingDTO;
import org.candlepin.dto.api.client.v1.ConsumerDTO;
import org.candlepin.dto.api.client.v1.ProductDTO;

import java.io.File;
import java.util.Map;

public class Export {

    public enum ProductId {
        eng_product,
        derived_provided_prod,
        derived_product,
        product1,
        product2,
        product3,
        virt_product,
        product_dc,
        product_vdc,
        product_up
    }

    private final ConsumerDTO consumer;
    private final ExportCdn cdn;
    private final File file;
    private final Map<ProductId, ProductDTO> products;

    public Export(File file, ConsumerDTO consumer, ExportCdn cdn, Map<ProductId, ProductDTO> products) {
        this.file = file;
        this.consumer = consumer;
        this.cdn = cdn;
        this.products = products;
    }

    public ConsumerDTO consumer() {
        return consumer;
    }

    public ExportCdn cdn() {
        return cdn;
    }

    public ProductDTO product(ProductId productId) {
        return products.get(productId);
    }

    public File file() {
        return file;
    }
}
