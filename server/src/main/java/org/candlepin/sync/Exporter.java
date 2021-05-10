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

import org.candlepin.common.config.Configuration;
import org.candlepin.controller.ContentAccessManager;
import org.candlepin.dto.ModelTranslator;
import org.candlepin.guice.PrincipalProvider;
import org.candlepin.model.Cdn;
import org.candlepin.model.CdnCurator;
import org.candlepin.model.Consumer;
import org.candlepin.model.ConsumerType;
import org.candlepin.model.ConsumerTypeCurator;
import org.candlepin.model.DistributorVersion;
import org.candlepin.model.DistributorVersionCurator;
import org.candlepin.model.EntitlementCurator;
import org.candlepin.model.OwnerCurator;
import org.candlepin.model.ResultIterator;
import org.candlepin.pki.PKIUtility;
import org.candlepin.policy.js.export.ExportRules;
import org.candlepin.service.EntitlementCertServiceAdapter;
import org.candlepin.service.ProductServiceAdapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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
     * @param webUrl the URL pointing to the manifest's originating web application.
     * @param apiUrl the API URL pointing to the manifest's originating candlepin API.
     * @return a newly created manifest file for the target consumer.
     * @throws ExportCreationException when an error occurs while creating the manifest file.
     */
    public Path getFullExport(Consumer consumer, String cdnLabel, String webUrl,
        String apiUrl) throws ExportCreationException {
        try {
            Path tmpDir = syncUtils.makeTempDirPath("export");
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
        catch (IOException e) {
            log.error("Error generating entitlement export", e);
            throw new ExportCreationException("Unable to create export archive", e);
        }
    }

    public Path getEntitlementExport(Consumer consumer, Set<Long> serials) throws ExportCreationException {
        // TODO: need to delete tmpDir (which contains the archive,
        // which we need to return...)
        try {
            Path tmpDir = syncUtils.makeTempDirPath("export");
            Path baseDir = tmpDir.resolve("export");

            exportMeta(baseDir, null);
            exportEntitlementsCerts(baseDir, consumer, serials, false);
            exportContentAccessCerts(baseDir, consumer);
            return makeArchive(consumer, tmpDir, baseDir);
        }
        catch (IOException e) {
            log.error("Error generating entitlement export", e);
            throw new ExportCreationException("Unable to create export archive", e);
        }
    }

    /**
     * Create a tar.gz archive of the exported directory.
     *
     * @param exportDir Directory where Candlepin data was exported.
     * @return File reference to the new archive zip.
     */
    private Path makeArchive(Consumer consumer, Path tempDir, Path exportDir)
        throws ExportCreationException {
        return this.zipper.makeArchive(consumer.getUuid(),tempDir,exportDir);
    }

    private void exportMeta(Path baseDir, String cdnKey) throws IOException, ExportCreationException {
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

    /**
     * Exports content access certificates for a consumer.
     * Consumer must belong to owner with SCA enabled.
     *
     * @param consumer
     *  Consumer for which content access certificates needs to be exported.
     *
     * @param baseDir
     *  Base directory path.
     *
     * @throws IOException
     *  Throws IO exception if unable to export content access certs for the consumer.
     */
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

    private void exportConsumerTypes(Path baseDir) throws IOException, ExportCreationException {
        this.consumerTypes.exportTo(baseDir);
    }

    private void exportRules(Path baseDir) throws IOException, ExportCreationException {
        this.rules.exportTo(baseDir);
    }

    private void exportDistributorVersions(Path baseDir) throws ExportCreationException {
        this.distVerExporter.exportTo(baseDir);
    }

    private void exportContentDeliveryNetworks(Path baseDir) throws ExportCreationException {
        this.cdnExporter.exportTo(baseDir);
    }
}
