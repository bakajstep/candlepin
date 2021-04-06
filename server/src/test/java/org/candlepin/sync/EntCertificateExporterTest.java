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
//
//package org.candlepin.sync;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//import org.candlepin.auth.NoAuthPrincipal;
//import org.candlepin.auth.Principal;
//import org.candlepin.guice.PrincipalProvider;
//import org.candlepin.model.CertificateSerial;
//import org.candlepin.model.Consumer;
//import org.candlepin.model.ConsumerType;
//import org.candlepin.model.ContentAccessCertificate;
//import org.candlepin.model.Entitlement;
//import org.candlepin.model.EntitlementCertificate;
//import org.candlepin.model.KeyPair;
//import org.candlepin.policy.js.export.ExportRules;
//import org.candlepin.service.EntitlementCertServiceAdapter;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import java.io.IOException;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.Arrays;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//class EntCertificateExporterTest {
//
//    private EntitlementCertServiceAdapter ecsa;
//    private ExportRules exportRules;
//    private FileExporter fileExporter;
//
//
//    private Consumer consumer;
//    private Set<Long> certSerials;
//
//    @BeforeEach
//    void setUp() {
//        ecsa = mock(EntitlementCertServiceAdapter .class);
//        exportRules = mock(ExportRules.class);
//        fileExporter = new SpyingExporter();
//
//        when(exportRules.canExport(any(Entitlement.class)))
//            .thenReturn(true);
//        consumer = mock(Consumer.class);
//        ConsumerType ctype = new ConsumerType(ConsumerType.ConsumerTypeEnum.CANDLEPIN);
//        ctype.setId("test-ctype");
//        when(consumer.getUuid()).thenReturn("consumer");
//        when(consumer.getName()).thenReturn("consumer_name");
//        when(consumer.getTypeId()).thenReturn(ctype.getId());
//
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
//        this.certSerials = new HashSet<>();
//        this.certSerials.add(entSerial.getId());
//        this.certSerials.add(cacSerial.getId());
//    }
//
//    @Test
//    public void testMetaExporter() throws IOException {
//        SpyingExporter exporter = new SpyingExporter();
//        NoAuthPrincipal principal = new NoAuthPrincipal();
//        PrincipalProvider principalProvider = mock(PrincipalProvider.class);
//        when(principalProvider.get()).thenReturn(principal);
//
//        EntCertificateExporter metaEx = new EntCertificateExporter(ecsa, exportRules, fileExporter);
//        String cdnKey = "test-cdn";
//        Path path = Paths.get("/meta.json");
//
//        metaEx.exportTo(path, consumer, certSerials, false);
//
//        String cert = (String) exporter.lastExports.get(0);
//        String key = (String) exporter.lastExports.get(1);
//        assertEquals(result.getCdnLabel(), cdnKey);
//        assertEquals(result.getPrincipalName(), "Anonymous");
//        assertNotNull(result.getVersion());
//        assertNotNull(result.getCreated());
//    }
//
//}