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
package org.candlepin.model;

import org.candlepin.model.dto.ProductContentData;
import org.candlepin.service.model.ProductContentInfo;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlTransient;



/**
 * ProductContent
 */
@Entity
@Immutable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Table(name = ProductContent.DB_TABLE)
public class ProductContent extends AbstractHibernateObject implements ProductContentInfo {

    /** Name of the table backing this object in the database */
    public static final String DB_TABLE = "cp2_product_content";

    /** The default state of the enabled flag */
    public static final Boolean DEFAULT_ENABLED_STATE = Boolean.FALSE;

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @NotNull
    private String id;

    @ManyToOne
    @JoinColumn(name = "product_uuid", nullable = false)
    @NotNull
    private Product product;

    @ManyToOne
    @JoinColumn(name = "content_uuid", nullable = false)
    @NotNull
    private Content content;

    private boolean enabled;

    public ProductContent() {
        // Intentionally left empty
    }

    public ProductContent(Product product, Content content, boolean enabled) {
        this.setContent(content);
        this.setProduct(product);
        this.setEnabled(enabled);
    }

    @Override
    @XmlTransient
    public Serializable getId() {
        return this.id;
    }

    /**
     * @param content the content to set
     */
    public void setContent(Content content) {
        this.content = content;
    }

    /**
     * @return the content
     */
    @Override
    public Content getContent() {
        return content;
    }

    /**
     * @param product the product to set
     */
    public void setProduct(Product product) {
        this.product = product;
    }

    /**
     * @return the product
     */
    @XmlTransient
    public Product getProduct() {
        return product;
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return the enabled
     */
    @Override
    public Boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof ProductContent) {
            ProductContent that = (ProductContent) obj;

            return new EqualsBuilder()
                .append(this.getContent(), that.getContent())
                .append(this.isEnabled(), that.isEnabled())
                .isEquals();
        }

        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(3, 23)
            .append(this.getContent() != null ? this.getContent().getId() : null)
            .toHashCode();
    }

    /**
     * Calculates and returns a version hash for this entity. This method operates much like the
     * hashCode method, except that it is more accurate and should have fewer collisions.
     *
     * @return
     *  a version hash for this entity
     */
    public int getEntityVersion() {
        return new HashCodeBuilder(7, 19)
            .append(this.getContent() != null ? this.getContent().getEntityVersion() : 0)
            .append(this.isEnabled())
            .toHashCode();
    }

    /**
     * Returns a DTO representing this entity.
     *
     * @return
     *  a DTO representing this entity
     */
    public ProductContentData toDTO() {
        return new ProductContentData(this);
    }

    public String toString() {
        return String.format("ProductContent [id: %s, product = %s, content = %s, enabled = %s]",
            this.getId(), this.getProduct(), this.getContent(), this.isEnabled());
    }
}