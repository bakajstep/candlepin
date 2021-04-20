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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.candlepin.auth.Principal;
import org.candlepin.common.config.Configuration;
import org.candlepin.config.ConfigProperties;
import org.candlepin.dto.ModelTranslator;
import org.candlepin.dto.SimpleModelTranslator;
import org.candlepin.dto.manifest.v1.ConsumerDTO;
import org.candlepin.dto.manifest.v1.ConsumerTranslator;
import org.candlepin.dto.manifest.v1.ConsumerTypeDTO;
import org.candlepin.dto.manifest.v1.EntitlementDTO;
import org.candlepin.dto.manifest.v1.EntitlementTranslator;
import org.candlepin.model.CandlepinQuery;
import org.candlepin.model.CertificateSerial;
import org.candlepin.model.Consumer;
import org.candlepin.model.ConsumerType;
import org.candlepin.model.ConsumerTypeCurator;
import org.candlepin.model.ContentAccessCertificate;
import org.candlepin.model.Entitlement;
import org.candlepin.model.EntitlementCertificate;
import org.candlepin.model.EntitlementCurator;
import org.candlepin.model.IdentityCertificate;
import org.candlepin.model.KeyPair;
import org.candlepin.model.OwnerCurator;
import org.candlepin.policy.js.export.ExportRules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class EntitlementExporterTest {

    private EntitlementCurator entitlementCurator;
    private ModelTranslator translator;

    @BeforeEach
    void setUp() {
        entitlementCurator = mock(EntitlementCurator.class);
        translator = new SimpleModelTranslator();
        translator.registerTranslator(new EntitlementTranslator(), Entitlement.class, EntitlementDTO.class);
    }

//    @Test
//    public void testConsumerExport() throws IOException, ExportCreationException {
//        Entitlement entitlement = mock(Entitlement.class);
//        Set<Entitlement> entitlements = new HashSet<>();
//        entitlements.add(entitlement);
//        SpyingExporter fileExporter = new SpyingExporter();
//        EntitlementExporter exporter = new EntitlementExporter(
//            mock(EntitlementCurator.class),
//            mock(ExportRules.class),
//            fileExporter,
//            translator
//        );
//        ConsumerType ctype = createCType();
//        Consumer consumer = createConsumer(ctype);
//
//        Path exportDir = Paths.get("");
//        exporter.exportTo(exportDir, consumer);
//
//        assertThat(fileExporter.calledTimes).isEqualTo(1);
//        Object lastExport = fileExporter.lastExports[0];
//        assertThat(lastExport).isInstanceOf(Consumer.class);
//        ConsumerDTO exportedConsumer = (ConsumerDTO) lastExport;
//
//        assertThat(exportedConsumer.getUuid()).isEqualTo(consumer.getUuid());
//        assertThat(exportedConsumer.getName()).isEqualTo(consumer.getName());
//        ConsumerTypeDTO exportedCType = exportedConsumer.getType();
//        assertThat(exportedCType.getId()).isEqualTo(ctype.getId());
//        assertThat(exportedCType.getLabel()).isEqualTo(ctype.getLabel());
//        assertThat(exportedCType.isManifest()).isEqualTo(ctype.isManifest());
//    }
//
//    @Test
//    public void testGetEntitlementExport() throws ExportCreationException,
//        IOException, GeneralSecurityException {
//        config.setProperty(ConfigProperties.SYNC_WORK_DIR, "/tmp/");
//
//        // Setup consumer
//        Consumer consumer = mock(Consumer.class);
//        ConsumerType ctype = new ConsumerType(ConsumerType.ConsumerTypeEnum.CANDLEPIN);
//        ctype.setId("test-ctype");
//        KeyPair keyPair = createKeyPair();
//        when(consumer.getKeyPair()).thenReturn(keyPair);
//        when(pki.getPemEncoded(keyPair.getPrivateKey())).thenReturn("privateKey".getBytes());
//        when(consumer.getUuid()).thenReturn("consumer");
//        when(consumer.getName()).thenReturn("consumer_name");
//        when(consumer.getTypeId()).thenReturn(ctype.getId());
//        when(ctc.getConsumerType(eq(consumer))).thenReturn(ctype);
//        when(ctc.get(eq(ctype.getId()))).thenReturn(ctype);
//
//        when(pki.getSHA256WithRSAHash(any(InputStream.class))).thenReturn("signature".getBytes());
//
//        // Setup principal
//        Principal principal = mock(Principal.class);
//        when(pprov.get()).thenReturn(principal);
//        when(principal.getUsername()).thenReturn("testUser");
//
//        // Create dummy ent cert
//        EntitlementCertificate entCert = new EntitlementCertificate();
//        CertificateSerial entSerial = new CertificateSerial();
//        entSerial.setId(123456L);
//        entCert.setSerial(entSerial);
//        entCert.setCert("ent-cert");
//        entCert.setKey("ent-cert-key");
//
//        // Create dummy content access cert
//        ContentAccessCertificate cac = new ContentAccessCertificate();
//        CertificateSerial cacSerial = new CertificateSerial();
//        cacSerial.setId(654321L);
//        cac.setSerial(cacSerial);
//        cac.setCert("content-access-cert");
//        cac.setKey("content-access-key");
//
//        when(ecsa.listForConsumer(consumer)).thenReturn(Arrays.asList(entCert));
//        when(contentAccessManager.getCertificate(consumer)).thenReturn(cac);
//
//        Exporter e = new Exporter(ctc, oc, me, ce, cte, re, ecsa, pe, psa,
//            pce, ec, ee, pki, config, exportRules, pprov, dvc, dve, cdnc, cdne, su,
//            translator, contentAccessManager);
//        File export = e.getEntitlementExport(consumer, null);
//
//        // Verify
//        assertNotNull(export);
//        assertTrue(export.exists());
//
//        // Check consumer export has entitlement cert.
//        assertTrue(verifyHasEntry(export, "export/entitlement_certificates/123456.pem"));
//
//        // Check consumer export has content access cert.
//        assertTrue(verifyHasEntry(export, "export/content_access_certificates/654321.pem"));
//    }

    @Test
    public void doNotExportDirtyEntitlements() throws Exception {
        Consumer consumer = mock(Consumer.class);
        List<Entitlement> entitlements = Arrays.asList(
            createDirtyEntitlement(),
            createDirtyEntitlement(),
            createDirtyEntitlement()
        );
        when(entitlementCurator.listByConsumer(consumer)).thenReturn(entitlements);
        SpyingExporter fileExporter = new SpyingExporter();
        EntitlementExporter exporter = new EntitlementExporter(
            mock(EntitlementCurator.class),
            mock(ExportRules.class),
            fileExporter,
            translator
        );

        exporter.exportTo(Paths.get(""), consumer);

        assertThat(fileExporter.calledTimes).isEqualTo(0);
    }

    private Entitlement createDirtyEntitlement() {
        Entitlement ent = mock(Entitlement.class);
        when(ent.isDirty()).thenReturn(true);
        return ent;
    }

    private Consumer createConsumer(ConsumerType ctype) {
        Consumer consumer = new Consumer().setUuid("test-uuid");
        consumer.setName("testy consumer");
        consumer.setType(ctype);
        consumer.setContentAccessMode("access_mode");
        return consumer;
    }

    private ConsumerType createCType() {
        ConsumerType ctype = new ConsumerType("candlepin");
        ctype.setId("8888");
        ctype.setManifest(true);
        return ctype;
    }
}