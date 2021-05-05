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
import org.candlepin.dto.manifest.v1.ConsumerTypeDTO;
import org.candlepin.model.ConsumerType;
import org.candlepin.model.ConsumerTypeCurator;

import com.google.inject.Inject;

import java.nio.file.Path;

public class ConsumerTypeExporter {

    private final ConsumerTypeCurator consumerTypeCurator;
    private final FileExporter<Object> fileExporter;
    private final ModelTranslator translator;

    @Inject
    public ConsumerTypeExporter(ConsumerTypeCurator consumerTypeCurator,
        FileExporter<Object> fileExporter, ModelTranslator translator) {
        this.consumerTypeCurator = consumerTypeCurator;
        this.fileExporter = fileExporter;
        this.translator = translator;
    }

    public void exportTo(Path baseDir) throws ExportCreationException {
        Path typeDir = baseDir.resolve("consumer_types");

        for (ConsumerType type : consumerTypeCurator.listAll()) {
            Path export = typeDir.resolve(type.getLabel() + ".json");
            this.fileExporter.exportTo(export, this.translator.translate(type, ConsumerTypeDTO.class));
        }
    }

}
