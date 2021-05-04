/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
 * <p>
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 * <p>
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.sync;

import org.candlepin.dto.ModelTranslator;
import org.candlepin.dto.manifest.v1.DistributorVersionDTO;
import org.candlepin.model.DistributorVersion;
import org.candlepin.model.DistributorVersionCurator;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

public class DistributorVersionExporter {

    private static final Logger log = LoggerFactory.getLogger(DistributorVersionExporter.class);

    private final DistributorVersionCurator distVerCurator;
    private final FileExporter<Object> fileExporter;
    private final ModelTranslator translator;

    @Inject
    public DistributorVersionExporter(DistributorVersionCurator distVerCurator, FileExporter<Object> fileExporter, ModelTranslator translator) {
        this.distVerCurator = distVerCurator;
        this.fileExporter = fileExporter;
        this.translator = translator;
    }

    public void exportTo(Path exportDir) throws ExportCreationException {
        List<DistributorVersion> versions = distVerCurator.findAll();
        if (versions == null || versions.isEmpty()) {
            return;
        }

        Path distVerDir = exportDir.resolve("distributor_version");

        for (DistributorVersion dv : versions) {
            log.debug("Exporting Distributor Version: {}", dv.getName());
            Path export = distVerDir.resolve(dv.getName() + ".json");
            this.fileExporter.exportTo(export, this.translator.translate(dv, DistributorVersionDTO.class));
        }
    }

}
