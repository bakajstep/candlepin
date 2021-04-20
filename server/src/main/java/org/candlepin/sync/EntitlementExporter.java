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
import org.candlepin.dto.manifest.v1.EntitlementDTO;
import org.candlepin.model.Consumer;
import org.candlepin.model.Entitlement;
import org.candlepin.model.EntitlementCurator;
import org.candlepin.policy.js.export.ExportRules;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * EntitlementExporter
 */
public class EntitlementExporter {

    private static final Logger log = LoggerFactory.getLogger(EntitlementExporter.class);

    private final EntitlementCurator entitlementCurator;
    private final ExportRules exportRules;
    private final FileExporter fileExporter;
    private final ModelTranslator translator;

    @Inject
    public EntitlementExporter(EntitlementCurator entitlementCurator, ExportRules exportRules, FileExporter fileExporter, ModelTranslator translator) {
        this.entitlementCurator = entitlementCurator;
        this.exportRules = exportRules;
        this.fileExporter = fileExporter;
        this.translator = translator;
    }

    public void exportTo(Path exportDir, Consumer consumer)
        throws IOException, ExportCreationException {
        Path entCertDir = exportDir.resolve("entitlements");
        Files.createDirectory(entCertDir);

        for (Entitlement ent : entitlementCurator.listByConsumer(consumer)) {
            if (ent.isDirty()) {
                log.error("Entitlement {} is marked as dirty.", ent.getId());
                throw new ExportCreationException("Attempted to export dirty entitlements");
            }

            if (!this.exportRules.canExport(ent)) {
                log.debug("Skipping export of entitlement with product: {}", ent.getPool().getProductId());
                continue;
            }

            log.debug("Exporting entitlement for product {}", ent.getPool().getProductId());

            Path file = entCertDir.resolve(ent.getId() + ".json");
            export(file, ent);
        }
    }

    private void export(Path path, Entitlement entitlement) throws IOException {
        fileExporter.exportTo(path, this.translator.translate(entitlement, EntitlementDTO.class));
    }
}
