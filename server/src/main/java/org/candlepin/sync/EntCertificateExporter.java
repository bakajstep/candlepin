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
package org.candlepin.sync;

import org.candlepin.model.Certificate;
import org.candlepin.model.Consumer;
import org.candlepin.model.EntitlementCertificate;
import org.candlepin.policy.js.export.ExportRules;
import org.candlepin.service.EntitlementCertServiceAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Set;

public class EntCertificateExporter {

    private static final Logger log = LoggerFactory.getLogger(EntCertificateExporter.class);

    private final EntitlementCertServiceAdapter entCertAdapter;
    private final ExportRules exportRules;
    private final FileExporter<String> exporter;

    public EntCertificateExporter(EntitlementCertServiceAdapter entCertAdapter, ExportRules exportRules, FileExporter<String> exporter) {
        this.entCertAdapter = entCertAdapter;
        this.exportRules = exportRules;
        this.exporter = exporter;
    }

    public void exportTo(Path exportDir, Consumer consumer, Set<Long> serials, boolean manifest)
        throws ExportCreationException {

        Path entCertDir = exportDir.resolve("entitlement_certificates");
        for (EntitlementCertificate cert : entCertAdapter.listForConsumer(consumer)) {
            if (manifest && !this.exportRules.canExport(cert.getEntitlement())) {
                log.debug("Skipping export of entitlement cert with product: {}",
                    cert.getEntitlement().getPool().getProductId());

                continue;
            }

            if (contains(serials, cert)) {
                log.debug("Exporting entitlement certificate: {}", cert.getSerial());
                Path exportFile = entCertDir.resolve(cert.getSerial().getId() + ".pem");
                exportCertificate(cert, exportFile);
            }
        }
    }

    private boolean contains(Set<Long> serials, EntitlementCertificate cert) {
        return (serials == null) || (serials.contains(cert.getSerial().getId()));
    }

    void exportCertificate(Certificate<?> cert, Path file) throws ExportCreationException {
        this.exporter.exportTo(file,
            cert.getCert(),
            cert.getKey()
        );
    }

}
