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

import org.candlepin.model.Consumer;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public class Exporter {
    private static final Logger log = LoggerFactory.getLogger(Exporter.class);

    private final MetaExporter meta;
    private final EntCertificateExporter entCerts;
    private final ScaCertificateExporter scaCerts;
    private final IdentCertificateExporter identCerts;
    private final ConsumerExporter consumerExporter;
    private final ConsumerTypeExporter consumerTypes;
    private final EntitlementExporter entExporter;
    private final ProductExporter productExporter;
    private final DistributorVersionExporter distVerExporter;
    private final CdnExporter cdnExporter;
    private final RulesExporter rules;
    private final Zipper zipper;
    private final SyncUtils syncUtils;
    //    private ProductCertExporter productCertExporter;

    @Inject
    public Exporter(MetaExporter meta,
        EntCertificateExporter entCertificateExporter,
        ScaCertificateExporter scaCertificateExporter,
        IdentCertificateExporter identCertificateExporter,
        ConsumerExporter consumerExporter,
        ConsumerTypeExporter consumerType,
        RulesExporter rules,
        ProductExporter productExporter,
        EntitlementExporter entExporter,
        DistributorVersionExporter distVerExporter,
        CdnExporter cdnExporter,
        SyncUtils syncUtils,
        Zipper zipper
    ) {
        this.meta = meta;
        this.entCerts = entCertificateExporter;
        this.scaCerts = scaCertificateExporter;
        this.identCerts = identCertificateExporter;
        this.consumerExporter = consumerExporter;
        this.consumerTypes = consumerType;
        this.rules = rules;
        this.productExporter = productExporter;
        this.entExporter = entExporter;
        this.distVerExporter = distVerExporter;
        this.cdnExporter = cdnExporter;
        this.zipper = zipper;
        this.syncUtils = syncUtils;
    }

    /**
     * Creates a manifest archive for the target {@link Consumer}.
     *
     * @param consumer the target consumer to export.
     * @param cdnLabel the CDN label to store in the meta file.
     * @param webUrl   the URL pointing to the manifest's originating web application.
     * @param apiUrl   the API URL pointing to the manifest's originating candlepin API.
     * @return a newly created manifest file for the target consumer.
     * @throws ExportCreationException when an error occurs while creating the manifest file.
     */
    public Path getFullExport(Consumer consumer, String cdnLabel, String webUrl, String apiUrl)
        throws ExportCreationException {

        Path tmpDir = createTmpDir();
        Path baseDir = tmpDir.resolve("export");

        exportMeta(baseDir, cdnLabel);
        exportConsumer(baseDir, consumer, webUrl, apiUrl);
        exportIdentityCertificate(baseDir, consumer);
        exportEntitlements(baseDir, consumer);
        exportEntitlementsCerts(baseDir, consumer, null, true);
        exportProducts(baseDir, consumer);
        exportConsumerTypes(baseDir);
        exportRules(baseDir);
        exportDistributorVersions(baseDir);
        exportContentDeliveryNetworks(baseDir);

        return makeArchive(consumer, tmpDir, baseDir);
    }

    public Path getEntitlementExport(Consumer consumer, Set<Long> serials) throws ExportCreationException {
        // TODO: need to delete tmpDir (which contains the archive,
        // which we need to return...)
        Path tmpDir = createTmpDir();
        Path baseDir = tmpDir.resolve("export");

        exportMeta(baseDir, null);
        exportEntitlementsCerts(baseDir, consumer, serials, false);
        exportContentAccessCerts(baseDir, consumer);
        return makeArchive(consumer, tmpDir, baseDir);
    }

    private Path createTmpDir() throws ExportCreationException {
        try {
            return syncUtils.makeTempDirPath("export");
        }
        catch (IOException e) {
            log.error("Error generating entitlement export", e);
            throw new ExportCreationException("Unable to create export archive", e);
        }
    }

    private Path makeArchive(Consumer consumer, Path tempDir, Path exportDir)
        throws ExportCreationException {
        return this.zipper.makeArchive(consumer.getUuid(), tempDir, exportDir);
    }

    private void exportMeta(Path baseDir, String cdnKey) throws ExportCreationException {
        meta.exportTo(baseDir, cdnKey);
    }

    private void exportConsumer(Path baseDir, Consumer consumer, String webAppPrefix, String apiUrl)
        throws ExportCreationException {
        this.consumerExporter.exportTo(baseDir, consumer, webAppPrefix, apiUrl);
    }

    private void exportEntitlementsCerts(Path baseDir, Consumer consumer,
        Set<Long> serials, boolean manifest) throws ExportCreationException {
        this.entCerts.exportTo(baseDir, consumer, serials, manifest);
    }

    private void exportContentAccessCerts(Path baseDir, Consumer consumer) throws ExportCreationException {
        this.scaCerts.exportTo(baseDir, consumer);
    }

    private void exportIdentityCertificate(Path baseDir, Consumer consumer)
        throws ExportCreationException {
        this.identCerts.exportTo(baseDir, consumer);
    }

    private void exportEntitlements(Path baseDir, Consumer consumer)
        throws ExportCreationException {
        this.entExporter.exportTo(baseDir, consumer);
    }

    private void exportProducts(Path baseDir, Consumer consumer) throws ExportCreationException {
        this.productExporter.exportTo(baseDir, consumer);
    }

    private void exportConsumerTypes(Path baseDir) throws ExportCreationException {
        this.consumerTypes.exportTo(baseDir);
    }

    private void exportRules(Path baseDir) throws ExportCreationException {
        this.rules.exportTo(baseDir);
    }

    private void exportDistributorVersions(Path baseDir) throws ExportCreationException {
        this.distVerExporter.exportTo(baseDir);
    }

    private void exportContentDeliveryNetworks(Path baseDir) throws ExportCreationException {
        this.cdnExporter.exportTo(baseDir);
    }
}
