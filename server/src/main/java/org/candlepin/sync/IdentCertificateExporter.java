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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.candlepin.dto.ModelTranslator;
import org.candlepin.dto.manifest.v1.CertificateDTO;
import org.candlepin.model.Consumer;
import org.candlepin.model.IdentityCertificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class IdentCertificateExporter {

    private static final Logger log = LoggerFactory.getLogger(IdentCertificateExporter.class);

    private final FileExporter fileExporter;
    private final ModelTranslator translator;

    public IdentCertificateExporter(FileExporter fileExporter, ModelTranslator translator) {
        this.fileExporter = fileExporter;
        this.translator = translator;
    }

    public void exportTo(Path exportDir, Consumer consumer) throws IOException {
//        File idcertdir = new File(baseDir.getCanonicalPath(), "upstream_consumer");
//        idcertdir.mkdir();

        IdentityCertificate cert = consumer.getIdCert();

        // paradigm dictates this should go in an exporter.export method
        Path export = exportDir.resolve(cert.getSerial().getId() + ".json");
        this.fileExporter.exportTo(export, this.translator.translate(cert, CertificateDTO.class));
//        try (FileWriter writer = new FileWriter(file)) {
//            mapper.writeValue(writer, this.translator.translate(cert, CertificateDTO.class));
//        }
    }

}