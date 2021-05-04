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
import org.candlepin.dto.manifest.v1.CdnDTO;
import org.candlepin.model.Cdn;
import org.candlepin.model.CdnCurator;
import org.candlepin.model.ResultIterator;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;

public class CdnExporter {

    private static final Logger log = LoggerFactory.getLogger(CdnExporter.class);

    private final CdnCurator cdnCurator;
    private final FileExporter<Object> fileExporter;
    private final ModelTranslator translator;

    @Inject
    public CdnExporter(CdnCurator cdnCurator, FileExporter<Object> fileExporter, ModelTranslator translator) {
        this.cdnCurator = cdnCurator;
        this.fileExporter = fileExporter;
        this.translator = translator;
    }

    public void exportTo(Path exportDir) throws ExportCreationException {
        Path cdnDir = exportDir.resolve("content_delivery_network");
        try (ResultIterator<Cdn> iterator = this.cdnCurator.listAll().iterate()) {
            while (iterator.hasNext()) {
                Cdn cdn = iterator.next();
                log.debug("Exporting CDN: {}", cdn.getName());

                Path file = cdnDir.resolve(cdn.getLabel() + ".json");
                this.fileExporter.exportTo(file, this.translator.translate(cdn, CdnDTO.class));
            }
        }
    }

}
