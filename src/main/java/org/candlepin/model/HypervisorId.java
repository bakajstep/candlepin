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

import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;



/**
 * HypervisorId represents a hypervisor host, unique per organization
 *
 * This ID is generated by the hypervisor.  In most cases it is a uuid,
 * so we can be fairly sure it is unique, however in some cases such
 * as openstack it may be a hostname.
 *
 * This structure allows us to tag a consumer (hypervisor type or otherwise)
 * with a hypervisorId so that hypervisorCheckIn can update its guest list
 * without additional info or lookups.
 */
@Entity
@Table(name = HypervisorId.DB_TABLE, uniqueConstraints =
    @UniqueConstraint(name = "cp_consumer_hypervisor_ukey", columnNames = {"owner_id", "hypervisor_id"}))
public class HypervisorId extends AbstractHibernateObject {

    /** Name of the table backing this object in the database */
    public static final String DB_TABLE = "cp_consumer_hypervisor";

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(length = 32)
    @NotNull
    private String id;

    @Column(name = "hypervisor_id", nullable = false)
    @Size(max = 255)
    @NotNull
    private String hypervisorId;

    @Column(name = "reporter_id")
    @Size(max = 255)
    private String reporterId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, unique = true)
    @NotNull
    private Consumer consumer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @NotNull
    private Owner owner;

    public HypervisorId() {
        // Intentionally left empty
    }

    @Override
    public Serializable getId() {
        return this.id;
    }

    /**
     * @param id the id to set
     *
     * @return
     *  a reference to this HypervisorId
     */
    public HypervisorId setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * @return the hypervisorId
     */
    public String getHypervisorId() {
        return hypervisorId;
    }

    /**
     * @param hypervisorId the hypervisorId to set
     *
     * @return
     *  a reference to this HypervisorId
     */
    public HypervisorId setHypervisorId(String hypervisorId) {
        // Hypervisor uuid is case insensitive, we need to force it to lower
        // case in order to enforce the unique hypervisorId per org constraint
        //
        // Queries in Candlepin are dependent on the fact this attribute is
        // being stored in lower case.
        this.hypervisorId = hypervisorId != null ? hypervisorId.toLowerCase() : null;
        return this;
    }

    /**
     * @return the reporterId
     */
    public String getReporterId() {
        return reporterId;
    }

    /**
     * @param reporterId the reporterId to set
     *
     * @return
     *  a reference to this HypervisorId
     */
    public HypervisorId setReporterId(String reporterId) {
        this.reporterId = reporterId;
        return this;
    }

    /**
     * @return the consumer
     */
    public Consumer getConsumer() {
        return consumer;
    }

    /**
     * @param consumer the consumer to set
     *
     * @return
     *  a reference to this HypervisorId
     */
    public HypervisorId setConsumer(Consumer consumer) {
        this.consumer = consumer;
        return this;
    }

    /**
     * @return the owner
     */
    public Owner getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     *
     * @return
     *  a reference to this HypervisorId
     */
    public HypervisorId setOwner(Owner owner) {
        this.owner = owner;
        return this;
    }
}