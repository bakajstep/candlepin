///*
// *  Copyright (c) 2009 - ${YEAR} Red Hat, Inc.
// *
// *  This software is licensed to you under the GNU General Public License,
// *  version 2 (GPLv2). There is NO WARRANTY for this software, express or
// *  implied, including the implied warranties of MERCHANTABILITY or FITNESS
// *  FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
// *  along with this software; if not, see
// *  http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
// *
// *  Red Hat trademarks are not licensed under GPLv2. No permission is
// *  granted to use or replicate Red Hat trademarks that are incorporated
// *  in this software or its documentation.
// */
//package org.candlepin.sync;
//
//import org.candlepin.controller.ContentAccessManager;
//import org.candlepin.model.Certificate;
//import org.candlepin.model.Consumer;
//import org.candlepin.model.ContentAccessCertificate;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.FileWriter;
//import java.io.IOException;
//import java.nio.file.Path;
//import java.security.GeneralSecurityException;
//
//public class ScaCertificateExporter {
//
//    private static final Logger log = LoggerFactory.getLogger(ScaCertificateExporter.class);
//
//    private final ContentAccessManager contentAccessManager;
//    private final FileExporter fileExporter;
//
//    public ScaCertificateExporter(ContentAccessManager contentAccessManager, FileExporter fileExporter) {
//        this.contentAccessManager = contentAccessManager;
//        this.fileExporter = fileExporter;
//    }
//
//    public void exportTo(Path exportDir, Consumer consumer) throws IOException {
//        ContentAccessCertificate contentAccessCert = null;
//
//        try {
//            contentAccessCert = this.contentAccessManager.getCertificate(consumer);
//        }
//        catch (GeneralSecurityException gse) {
//            throw new IOException("Cannot retrieve content access certificate", gse);
//        }
//
//        if (contentAccessCert != null) {
////            File contentAccessCertDir = new File(baseDir.getCanonicalPath(),
////                "content_access_certificates");
////            contentAccessCertDir.mkdir();
//            log.debug("Exporting content access certificate: " + contentAccessCert.getSerial());
//            Path export = exportDir.resolve(contentAccessCert.getSerial().getId() + ".pem");
//            exportCertificate(contentAccessCert, export);
//        }
//    }
//
//    void exportCertificate(Certificate<?> cert, Path file) throws IOException {
//        this.fileExporter.exportTo(file,
//                cert.getCert(),
//                cert.getKey()
//            );
////        try (FileWriter writer = new FileWriter(file)) {
////        } catch (IOException ioExp) {
////            throw new IOException("Error occurred while exporting certificates", ioExp);
////        }
//    }
//
//}