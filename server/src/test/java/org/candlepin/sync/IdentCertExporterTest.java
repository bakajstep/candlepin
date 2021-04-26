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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.candlepin.dto.ModelTranslator;
import org.candlepin.dto.SimpleModelTranslator;
import org.candlepin.dto.manifest.v1.CertificateDTO;
import org.candlepin.dto.manifest.v1.CertificateSerialDTO;
import org.candlepin.dto.manifest.v1.CertificateSerialTranslator;
import org.candlepin.dto.manifest.v1.CertificateTranslator;
import org.candlepin.dto.manifest.v1.DistributorVersionDTO;
import org.candlepin.dto.manifest.v1.DistributorVersionTranslator;
import org.candlepin.model.Certificate;
import org.candlepin.model.CertificateSerial;
import org.candlepin.model.Consumer;
import org.candlepin.model.ConsumerType;
import org.candlepin.model.DistributorVersion;
import org.candlepin.model.DistributorVersionCurator;
import org.candlepin.model.IdentityCertificate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

class IdentCertExporterTest {

    private ModelTranslator translator;

    @BeforeEach
    void setUp() {
        this.translator = new SimpleModelTranslator();
        this.translator.registerTranslator(new CertificateTranslator(), Certificate.class, CertificateDTO.class);
        this.translator.registerTranslator(new CertificateSerialTranslator(), CertificateSerial.class, CertificateSerialDTO.class);
    }

    @Test
    public void testExporter() throws ExportCreationException {
        SpyingExporter fileExporter = new SpyingExporter();
        IdentCertificateExporter exporter = new IdentCertificateExporter(fileExporter, translator);
        Consumer consumer = getConsumer();
        Path path = Paths.get("/certs");

        exporter.exportTo(path, consumer);

        assertEquals(1, fileExporter.calledTimes);
        CertificateDTO result = (CertificateDTO) fileExporter.lastExports[0];
        assertEquals(result.getId(), "id_cert_1");
        assertEquals(result.getCertificate(), "id_cert_payload_1");
    }

    private Consumer getConsumer() {
        Consumer consumer = new Consumer();
        ConsumerType ctype = new ConsumerType(ConsumerType.ConsumerTypeEnum.CANDLEPIN);
        ctype.setId("test-ctype");
        consumer.setType(ctype);
        consumer.setUuid("consumer");
        consumer.setName("consumer_name");
        consumer.setIdCert(getIdCert());
        return consumer;
    }

    private IdentityCertificate getIdCert() {
        IdentityCertificate idCert = new IdentityCertificate();
        idCert.setId("id_cert_1");
        idCert.setKey("id_cert_key_1");
        idCert.setCert("id_cert_payload_1");
        idCert.setSerial(new CertificateSerial(123L));
        return idCert;
    }

}
