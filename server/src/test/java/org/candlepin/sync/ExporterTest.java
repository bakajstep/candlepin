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

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.candlepin.model.Consumer;
import org.candlepin.policy.js.export.ExportRules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;


/**
 * Orchestrates exporters
 */
public class ExporterTest {

    public static final String CONSUMER_UUID = "consumer_uuid";

    private MetaExporter me;
    private EntCertificateExporter ece;
    private ScaCertificateExporter sce;
    private IdentCertificateExporter ice;
    private ConsumerExporter ce;
    private ConsumerTypeExporter cte;
    private RulesExporter re;
    private ProductExporter pe;
    private DistributorVersionExporter dve;
    private CdnExporter cdne;
    private EntitlementExporter ee;
    private SyncUtils su;
    private Zipper zipper;
    private Exporter exporter;

    @BeforeEach
    public void setUp() {
        this.me = mock(MetaExporter.class);
        this.ece = mock(EntCertificateExporter.class);
        this.sce = mock(ScaCertificateExporter.class);
        this.ice = mock(IdentCertificateExporter.class);
        this.su = mock(SyncUtils.class);
        this.ce = mock(ConsumerExporter.class);
        this.cte = mock(ConsumerTypeExporter.class);
        this.re = mock(RulesExporter.class);
        this.pe = mock(ProductExporter.class);
        this.ee = mock(EntitlementExporter.class);
        this.dve = mock(DistributorVersionExporter.class);
        this.cdne = mock(CdnExporter.class);
        this.zipper = mock(Zipper.class);
        this.exporter = new Exporter(
            this.me,
            this.ece,
            this.sce,
            this.ice,
            this.ce,
            this.cte,
            this.re,
            this.pe,
            this.ee,
            this.dve,
            this.cdne,
            this.su,
            this.zipper
        );
    }

    @Test
    void entitlementExport() throws ExportCreationException, IOException {
        when(this.su.makeTempDirPath(anyString())).thenReturn(Paths.get("/tmp"));

        this.exporter.getEntitlementExport(createConsumer(), new HashSet<>());

        verify(this.me).exportTo(any(Path.class), any());
        verify(this.ece).exportTo(any(Path.class), any(Consumer.class), anySet(), anyBoolean());
        verify(this.sce).exportTo(any(Path.class), any(Consumer.class));
        verify(this.zipper).makeArchive(eq(CONSUMER_UUID), any(Path.class), any(Path.class));
    }

    @Test
    void fullExport() throws ExportCreationException, IOException {
        when(this.su.makeTempDirPath(anyString())).thenReturn(Paths.get("/tmp"));

        this.exporter.getFullExport(createConsumer(), "cdn", "webUrl", "apiUrl");

        verify(this.me).exportTo(any(Path.class), any());
        verify(this.ce).exportTo(any(Path.class), any(Consumer.class), anyString(), anyString());
        verify(this.ice).exportTo(any(Path.class), any(Consumer.class));
        verify(this.ee).exportTo(any(Path.class), any(Consumer.class));
        verify(this.ece).exportTo(any(Path.class), any(Consumer.class), isNull(), anyBoolean());
        verify(this.pe).exportTo(any(Path.class), any(Consumer.class));
        verify(this.cte).exportTo(any(Path.class));
        verify(this.re).exportTo(any(Path.class));
        verify(this.dve).exportTo(any(Path.class));
        verify(this.cdne).exportTo(any(Path.class));
        verify(this.zipper).makeArchive(eq(CONSUMER_UUID), any(Path.class), any(Path.class));
    }

    private Consumer createConsumer() {
        Consumer consumer = new Consumer();
        consumer.setUuid(CONSUMER_UUID);
        return consumer;
    }

    //    private KeyPair createKeyPair() {
//        KeyPair cpKeyPair = null;
//
//        try {
//            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
//            generator.initialize(2048);
//            java.security.KeyPair newPair = generator.generateKeyPair();
//            cpKeyPair = new KeyPair(newPair.getPrivate(), newPair.getPublic());
//        }
//        catch (NoSuchAlgorithmException e) {
//            throw new RuntimeException(e);
//        }
//
//        return cpKeyPair;
//    }
//
//    @SuppressWarnings("unchecked")
//    @Test
//    public void exportProducts() throws Exception {
//        config.setProperty(ConfigProperties.SYNC_WORK_DIR, "/tmp/");
//        Consumer consumer = mock(Consumer.class);
//        Entitlement ent = mock(Entitlement.class);
//        Rules mrules = mock(Rules.class);
//        Principal principal = mock(Principal.class);
//        IdentityCertificate idcert = new IdentityCertificate();
//
//        Set<Entitlement> entitlements = new HashSet<>();
//        entitlements.add(ent);
//
//        Owner owner = TestUtil.createOwner("Example-Corporation");
//
//        Product prod = TestUtil.createProduct("12345", "RHEL Product");
//        prod.setMultiplier(1L);
//        prod.setCreated(new Date());
//        prod.setUpdated(new Date());
//        prod.setAttributes(Collections.<String, String>emptyMap());
//
//        Product prod1 = TestUtil.createProduct("MKT-prod", "RHEL Product");
//        prod1.setMultiplier(1L);
//        prod1.setCreated(new Date());
//        prod1.setUpdated(new Date());
//        prod1.setAttributes(Collections.<String, String>emptyMap());
//
//        Product subProduct = TestUtil.createProduct("MKT-sub-prod", "Sub Product");
//        subProduct.setMultiplier(1L);
//        subProduct.setCreated(new Date());
//        subProduct.setUpdated(new Date());
//        subProduct.setAttributes(Collections.<String, String>emptyMap());
//
//        Product subProvidedProduct = TestUtil.createProduct("332211", "Sub Product");
//        subProvidedProduct.setMultiplier(1L);
//        subProvidedProduct.setCreated(new Date());
//        subProvidedProduct.setUpdated(new Date());
//        subProvidedProduct.setAttributes(Collections.<String, String>emptyMap());
//
//        prod1.addProvidedProduct(prod);
//        prod1.setDerivedProduct(subProduct);
//        subProduct.addProvidedProduct(subProvidedProduct);
//
//        ProductCertificate pcert = new ProductCertificate();
//        pcert.setKey("euh0876puhapodifbvj094");
//        pcert.setCert("hpj-08ha-w4gpoknpon*)&^%#");
//        pcert.setCreated(new Date());
//        pcert.setUpdated(new Date());
//
//        Pool pool = TestUtil.createPool(owner)
//            .setId("MockedPoolId")
//            .setProduct(prod1);
//
//        when(ent.getPool()).thenReturn(pool);
//        when(mrules.getRules()).thenReturn("foobar");
//        when(pki.getSHA256WithRSAHash(any(InputStream.class))).thenReturn("signature".getBytes());
//        when(rc.getRules()).thenReturn(mrules);
//        when(consumer.getEntitlements()).thenReturn(entitlements);
//        when(psa.getProductCertificate(any(String.class), any(String.class))).thenReturn(pcert);
//        when(pprov.get()).thenReturn(principal);
//        when(principal.getUsername()).thenReturn("testUser");
//        idcert.setSerial(new CertificateSerial(10L, new Date()));
//        idcert.setKey("euh0876puhapodifbvj094");
//        idcert.setCert("hpj-08ha-w4gpoknpon*)&^%#");
//        idcert.setCreated(new Date());
//        idcert.setUpdated(new Date());
//        when(consumer.getIdCert()).thenReturn(idcert);
//
//        KeyPair keyPair = createKeyPair();
//        when(consumer.getKeyPair()).thenReturn(keyPair);
//        when(pki.getPemEncoded(keyPair.getPrivateKey())).thenReturn("privateKey".getBytes());
//
//        CandlepinQuery cqmock = mock(CandlepinQuery.class);
//        when(cqmock.iterator()).thenReturn(Arrays.asList(new ConsumerType("system")).iterator());
//        when(ctc.listAll()).thenReturn(cqmock);
//
//        CandlepinQuery emptyIteratorMock = mock(CandlepinQuery.class);
//        when(emptyIteratorMock.iterate()).thenReturn(new MockResultIterator(Arrays.asList().iterator()));
//        when(emptyIteratorMock.iterator()).thenReturn(Arrays.asList().iterator());
//        when(cdnc.listAll()).thenReturn(emptyIteratorMock);
//        when(ctc.listAll()).thenReturn(emptyIteratorMock);
//
//        when(consumer.getOwnerId()).thenReturn(owner.getId());
//        when(oc.findOwnerById(eq(owner.getId()))).thenReturn(owner);
//
//        // FINALLY test this badboy
//        Exporter e = new Exporter(
//            ctc, oc, me, ce, cte, re, ecsa, pe, psa,
//            pce, ec, ee, pki, config, exportRules, pprov, dvc, dve, cdnc, cdne, su,
//            translator, contentAccessManager);
//
//        File export = e.getFullExport(consumer, null, null, null);
//
//        // VERIFY
//        assertNotNull(export);
//        verifyContent(export, "export/products/12345.pem", new VerifyProductCert("12345.pem"));
//        assertFalse(verifyHasEntry(export, "export/products/MKT-prod.pem"));
//
//        verifyContent(export, "export/products/332211.pem", new VerifyProductCert("332211.pem"));
//        assertFalse(verifyHasEntry(export, "export/products/MKT-sub-prod.pem"));
//
//        FileUtils.deleteDirectory(export.getParentFile());
//        assertTrue(new File("/tmp/consumer_export.zip").delete());
//        assertTrue(new File("/tmp/12345.pem").delete());
//        assertTrue(new File("/tmp/332211.pem").delete());
//    }
//
//    @Test
//    public void doNotExportDirtyEntitlements() throws Exception {
//        config.setProperty(ConfigProperties.SYNC_WORK_DIR, "/tmp/");
//        Consumer consumer = mock(Consumer.class);
//        Entitlement ent = mock(Entitlement.class);
//        Principal principal = mock(Principal.class);
//        IdentityCertificate idcert = new IdentityCertificate();
//
//        List<Entitlement> entitlements = new ArrayList<>();
//        entitlements.add(ent);
//
//        when(pki.getSHA256WithRSAHash(any(InputStream.class))).thenReturn(
//            "signature".getBytes());
//        when(pprov.get()).thenReturn(principal);
//        when(principal.getUsername()).thenReturn("testUser");
//
//        when(ec.listByConsumer(consumer)).thenReturn(entitlements);
//        when(ent.isDirty()).thenReturn(true);
//        idcert.setSerial(new CertificateSerial(10L, new Date()));
//        idcert.setKey("euh0876puhapodifbvj094");
//        idcert.setCert("hpj-08ha-w4gpoknpon*)&^%#");
//        idcert.setCreated(new Date());
//        idcert.setUpdated(new Date());
//        when(consumer.getIdCert()).thenReturn(idcert);
//
//        KeyPair keyPair = createKeyPair();
//        when(consumer.getKeyPair()).thenReturn(keyPair);
//        when(pki.getPemEncoded(keyPair.getPrivateKey())).thenReturn("privateKey".getBytes());
//
//        CandlepinQuery cqmock = mock(CandlepinQuery.class);
//        when(cqmock.iterator()).thenReturn(Arrays.asList(new ConsumerType("system")).iterator());
//        when(ctc.listAll()).thenReturn(cqmock);
//
//        Exporter e = new Exporter(ctc, oc, me, ce, cte, re, ecsa, pe, psa,
//            pce, ec, ee, pki, config, exportRules, pprov, dvc, dve, cdnc, cdne, su,
//            translator, contentAccessManager);
//
//        assertThrows(ExportCreationException.class, () ->
//            e.getFullExport(consumer, null, null, null));
//    }
//
//    @Test
//    public void exportMetadata() throws ExportCreationException, IOException {
//        config.setProperty(ConfigProperties.SYNC_WORK_DIR, "/tmp/");
//        Date start = new Date();
//        Rules mrules = mock(Rules.class);
//        Consumer consumer = mock(Consumer.class);
//        Principal principal = mock(Principal.class);
//        IdentityCertificate idcert = new IdentityCertificate();
//
//        when(mrules.getRules()).thenReturn("foobar");
//        when(pki.getSHA256WithRSAHash(any(InputStream.class))).thenReturn("signature".getBytes());
//        when(rc.getRules()).thenReturn(mrules);
//        when(pprov.get()).thenReturn(principal);
//        when(principal.getUsername()).thenReturn("testUser");
//
//        idcert.setSerial(new CertificateSerial(10L, new Date()));
//        idcert.setKey("euh0876puhapodifbvj094");
//        idcert.setCert("hpj-08ha-w4gpoknpon*)&^%#");
//        idcert.setCreated(new Date());
//        idcert.setUpdated(new Date());
//        when(consumer.getIdCert()).thenReturn(idcert);
//
//        KeyPair keyPair = createKeyPair();
//        when(consumer.getKeyPair()).thenReturn(keyPair);
//        when(pki.getPemEncoded(keyPair.getPrivateKey())).thenReturn("privateKey".getBytes());
//
//        CandlepinQuery cqmock = mock(CandlepinQuery.class);
//        when(cqmock.iterator()).thenReturn(Arrays.asList(new ConsumerType("system")).iterator());
//        when(ctc.listAll()).thenReturn(cqmock);
//
//        CandlepinQuery emptyIteratorMock = mock(CandlepinQuery.class);
//        when(emptyIteratorMock.iterate()).thenReturn(new MockResultIterator(Arrays.asList().iterator()));
//        when(cdnc.listAll()).thenReturn(emptyIteratorMock);
//
//        // FINALLY test this badboy
//        Exporter e = new Exporter(ctc, oc, me, ce, cte, re, ecsa, pe, psa,
//            pce, ec, ee, pki, config, exportRules, pprov, dvc, dve, cdnc, cdne, su,
//            translator, contentAccessManager);
//        File export = e.getFullExport(consumer, null, null, null);
//
//        // VERIFY
//        assertNotNull(export);
//        assertTrue(export.exists());
//        verifyContent(export, "export/meta.json", new VerifyMetadata(start));
//
//        // cleanup the mess
//        FileUtils.deleteDirectory(export.getParentFile());
//        assertTrue(new File("/tmp/consumer_export.zip").delete());
//        assertTrue(new File("/tmp/meta.json").delete());
//    }
//
//    @Test
//    public void exportIdentityCertificate() throws Exception {
//        config.setProperty(ConfigProperties.SYNC_WORK_DIR, "/tmp/");
//        Rules mrules = mock(Rules.class);
//        Consumer consumer = mock(Consumer.class);
//        Principal principal = mock(Principal.class);
//
//        when(mrules.getRules()).thenReturn("foobar");
//        when(pki.getSHA256WithRSAHash(any(InputStream.class))).thenReturn("signature".getBytes());
//        when(rc.getRules()).thenReturn(mrules);
//        when(pprov.get()).thenReturn(principal);
//        when(principal.getUsername()).thenReturn("testUser");
//
//        // specific to this test
//        IdentityCertificate idcert = new IdentityCertificate();
//        idcert.setSerial(new CertificateSerial(10L, new Date()));
//        idcert.setKey("euh0876puhapodifbvj094");
//        idcert.setCert("hpj-08ha-w4gpoknpon*)&^%#");
//        idcert.setCreated(new Date());
//        idcert.setUpdated(new Date());
//        when(consumer.getIdCert()).thenReturn(idcert);
//
//        KeyPair keyPair = createKeyPair();
//        when(consumer.getKeyPair()).thenReturn(keyPair);
//        when(pki.getPemEncoded(keyPair.getPrivateKey())).thenReturn("privateKey".getBytes());
//
//        CandlepinQuery cqmock = mock(CandlepinQuery.class);
//        when(cqmock.iterator()).thenReturn(Arrays.asList(new ConsumerType("system")).iterator());
//        when(ctc.listAll()).thenReturn(cqmock);
//
//        CandlepinQuery emptyIteratorMock = mock(CandlepinQuery.class);
//        when(emptyIteratorMock.iterate()).thenReturn(new MockResultIterator(Arrays.asList().iterator()));
//        when(emptyIteratorMock.iterator()).thenReturn(Arrays.asList().iterator());
//        when(cdnc.listAll()).thenReturn(emptyIteratorMock);
//
//        // FINALLY test this badboy
//        Exporter e = new Exporter(ctc, oc, me, ce, cte, re, ecsa, pe, psa,
//            pce, ec, ee, pki, config, exportRules, pprov, dvc, dve, cdnc, cdne, su,
//            translator, contentAccessManager);
//        File export = e.getFullExport(consumer, null, null, null);
//
//        // VERIFY
//        assertNotNull(export);
//        assertTrue(export.exists());
//        verifyContent(export, "export/upstream_consumer/10.pem", new VerifyIdentityCert("10.pem"));
//    }
//
//    @Test
//    public void exportConsumer() throws ExportCreationException, IOException {
//        config.setProperty(ConfigProperties.SYNC_WORK_DIR, "/tmp/");
//        config.setProperty(ConfigProperties.PREFIX_WEBURL, "localhost:8443/weburl");
//        config.setProperty(ConfigProperties.PREFIX_APIURL, "localhost:8443/apiurl");
//        Rules mrules = mock(Rules.class);
//        Consumer consumer = mock(Consumer.class);
//        Principal principal = mock(Principal.class);
//
//        when(mrules.getRules()).thenReturn("foobar");
//        when(pki.getSHA256WithRSAHash(any(InputStream.class))).thenReturn("signature".getBytes());
//        when(rc.getRules()).thenReturn(mrules);
//        when(pprov.get()).thenReturn(principal);
//        when(principal.getUsername()).thenReturn("testUser");
//
//        // specific to this test
//        IdentityCertificate idcert = new IdentityCertificate();
//        idcert.setSerial(new CertificateSerial(10L, new Date()));
//        idcert.setKey("euh0876puhapodifbvj094");
//        idcert.setCert("hpj-08ha-w4gpoknpon*)&^%#");
//        idcert.setCreated(new Date());
//        idcert.setUpdated(new Date());
//        when(consumer.getIdCert()).thenReturn(idcert);
//
//        ConsumerType ctype = new ConsumerType(ConsumerTypeEnum.CANDLEPIN);
//        ctype.setId("test-ctype");
//
//        KeyPair keyPair = createKeyPair();
//        when(consumer.getKeyPair()).thenReturn(keyPair);
//        when(pki.getPemEncoded(keyPair.getPrivateKey())).thenReturn("privateKey".getBytes());
//        when(consumer.getUuid()).thenReturn("8auuid");
//        when(consumer.getName()).thenReturn("consumer_name");
//        when(consumer.getContentAccessMode()).thenReturn("access_mode");
//        when(consumer.getTypeId()).thenReturn(ctype.getId());
//
//        when(ctc.getConsumerType(eq(consumer))).thenReturn(ctype);
//        when(ctc.get(eq(ctype.getId()))).thenReturn(ctype);
//
//        CandlepinQuery cqmock = mock(CandlepinQuery.class);
//        when(cqmock.iterator()).thenReturn(Arrays.asList(new ConsumerType("system")).iterator());
//        when(ctc.listAll()).thenReturn(cqmock);
//
//        CandlepinQuery emptyIteratorMock = mock(CandlepinQuery.class);
//        when(emptyIteratorMock.iterate()).thenReturn(new MockResultIterator(Arrays.asList().iterator()));
//        when(cdnc.listAll()).thenReturn(emptyIteratorMock);
//
//        // FINALLY test this badboy
//        Exporter e = new Exporter(ctc, oc, me, ce, cte, re, ecsa, pe, psa,
//            pce, ec, ee, pki, config, exportRules, pprov, dvc, dve, cdnc, cdne, su,
//            translator, contentAccessManager);
//        File export = e.getFullExport(consumer, null, null, null);
//
//        verifyContent(export, "export/consumer.json", new VerifyConsumer("consumer.json"));
//    }
//
//    @Test
//    public void exportDistributorVersions() throws ExportCreationException, IOException {
//        config.setProperty(ConfigProperties.SYNC_WORK_DIR, "/tmp/");
//        config.setProperty(ConfigProperties.PREFIX_WEBURL, "localhost:8443/weburl");
//        config.setProperty(ConfigProperties.PREFIX_APIURL, "localhost:8443/apiurl");
//        Rules mrules = mock(Rules.class);
//        Consumer consumer = mock(Consumer.class);
//        Principal principal = mock(Principal.class);
//
//        when(mrules.getRules()).thenReturn("foobar");
//        when(pki.getSHA256WithRSAHash(any(InputStream.class))).thenReturn("signature".getBytes());
//        when(rc.getRules()).thenReturn(mrules);
//        when(pprov.get()).thenReturn(principal);
//        when(principal.getUsername()).thenReturn("testUser");
//
//        IdentityCertificate idcert = new IdentityCertificate();
//        idcert.setSerial(new CertificateSerial(10L, new Date()));
//        idcert.setKey("euh0876puhapodifbvj094");
//        idcert.setCert("hpj-08ha-w4gpoknpon*)&^%#");
//        idcert.setCreated(new Date());
//        idcert.setUpdated(new Date());
//        when(consumer.getIdCert()).thenReturn(idcert);
//
//        ConsumerType ctype = new ConsumerType(ConsumerTypeEnum.CANDLEPIN);
//        ctype.setId("test-ctype");
//
//        KeyPair keyPair = createKeyPair();
//        when(consumer.getKeyPair()).thenReturn(keyPair);
//        when(pki.getPemEncoded(keyPair.getPrivateKey())).thenReturn("privateKey".getBytes());
//        when(consumer.getUuid()).thenReturn("8auuid");
//        when(consumer.getName()).thenReturn("consumer_name");
//        when(consumer.getTypeId()).thenReturn(ctype.getId());
//        when(ctc.getConsumerType(eq(consumer))).thenReturn(ctype);
//        when(ctc.get(eq(ctype.getId()))).thenReturn(ctype);
//
//        DistributorVersion dv = new DistributorVersion("test-dist-ver");
//        Set<DistributorVersionCapability> dvcSet = new HashSet<>();
//        dvcSet.add(new DistributorVersionCapability(dv, "capability-1"));
//        dvcSet.add(new DistributorVersionCapability(dv, "capability-2"));
//        dvcSet.add(new DistributorVersionCapability(dv, "capability-3"));
//        dv.setCapabilities(dvcSet);
//        List<DistributorVersion> dvList = new ArrayList<>();
//        dvList.add(dv);
//        when(dvc.findAll()).thenReturn(dvList);
//
//        CandlepinQuery cqmock = mock(CandlepinQuery.class);
//        when(cqmock.iterator()).thenReturn(Arrays.asList(new ConsumerType("system")).iterator());
//        when(ctc.listAll()).thenReturn(cqmock);
//
//        CandlepinQuery emptyIteratorMock = mock(CandlepinQuery.class);
//        when(emptyIteratorMock.iterate()).thenReturn(new MockResultIterator(Arrays.asList().iterator()));
//        when(emptyIteratorMock.iterator()).thenReturn(Arrays.asList().iterator());
//        when(cdnc.listAll()).thenReturn(emptyIteratorMock);
//        when(ctc.listAll()).thenReturn(emptyIteratorMock);
//
//        // FINALLY test this badboy
//        Exporter e = new Exporter(ctc, oc, me, ce, cte, re, ecsa, pe, psa,
//            pce, ec, ee, pki, config, exportRules, pprov, dvc, dve, cdnc, cdne, su,
//            translator, contentAccessManager);
//        File export = e.getFullExport(consumer, null, null, null);
//
//        verifyContent(export, "export/distributor_version/test-dist-ver.json",
//            new VerifyDistributorVersion("test-dist-ver.json"));
//    }
//
//    @Test
//    public void testGetEntitlementExport() throws ExportCreationException,
//        IOException, GeneralSecurityException {
//        config.setProperty(ConfigProperties.SYNC_WORK_DIR, "/tmp/");
//
//        // Setup consumer
//        Consumer consumer = mock(Consumer.class);
//        ConsumerType ctype = new ConsumerType(ConsumerTypeEnum.CANDLEPIN);
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
//
//    /**
//     * return true if export has a given entry named name.
//     * @param export zip file to inspect
//     * @param name entry
//     * @return
//     */
//    private boolean verifyHasEntry(File export, String name) {
//        ZipInputStream zis = null;
//        boolean found = false;
//
//        try {
//            zis = new ZipInputStream(new FileInputStream(export));
//            ZipEntry entry = null;
//
//            while ((entry = zis.getNextEntry()) != null) {
//                byte[] buf = new byte[1024];
//
//                if (entry.getName().equals("consumer_export.zip")) {
//                    OutputStream os = new FileOutputStream("/tmp/consumer_export.zip");
//
//                    int n;
//                    while ((n = zis.read(buf, 0, 1024)) > -1) {
//                        os.write(buf, 0, n);
//                    }
//                    os.flush();
//                    os.close();
//                    File exportdata = new File("/tmp/consumer_export.zip");
//                    // open up the zip and look for the metadata
//                    found = verifyHasEntry(exportdata, name);
//                }
//                else if (entry.getName().equals(name)) {
//                    found = true;
//                }
//
//                zis.closeEntry();
//            }
//
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//        finally {
//            if (zis != null) {
//                try {
//                    zis.close();
//                }
//                catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//        return found;
//    }
//
//    private void verifyContent(File export, String name, Verify v) {
//        ZipInputStream zis = null;
//
//        try {
//            zis = new ZipInputStream(new FileInputStream(export));
//            ZipEntry entry = null;
//            while ((entry = zis.getNextEntry()) != null) {
//                byte[] buf = new byte[1024];
//
//                if (entry.getName().equals("consumer_export.zip")) {
//                    OutputStream os = new FileOutputStream("/tmp/consumer_export.zip");
//
//                    int n;
//                    while ((n = zis.read(buf, 0, 1024)) > -1) {
//                        os.write(buf, 0, n);
//                    }
//                    os.flush();
//                    os.close();
//                    File exportdata = new File("/tmp/consumer_export.zip");
//                    // open up the zip and look for the metadata
//                    verifyContent(exportdata, name, v);
//                }
//                else if (entry.getName().equals(name)) {
//                    v.verify(zis, buf);
//                }
//                zis.closeEntry();
//            }
//        }
//        catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
//        finally {
//            if (zis != null) {
//                try {
//                    zis.close();
//                }
//                catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//
//    public interface Verify {
//        void verify(ZipInputStream zis, byte[] buf) throws IOException;
//    }
//
//    public static class VerifyMetadata implements Verify {
//        private Date start;
//
//        public VerifyMetadata(Date start) {
//            this.start = start;
//        }
//
//        public void verify(ZipInputStream zis, byte[] buf) throws IOException {
//            OutputStream os = new FileOutputStream("/tmp/meta.json");
//            int n;
//            while ((n = zis.read(buf, 0, 1024)) > -1) {
//                os.write(buf, 0, n);
//            }
//            os.flush();
//            os.close();
//
//            Map<String, String> configProps = new HashMap<>();
//            configProps.put(ConfigProperties.FAIL_ON_UNKNOWN_IMPORT_PROPERTIES, "false");
//
//            ObjectMapper mapper = new SyncUtils(new MapConfiguration(configProps)).getObjectMapper();
//
//            Meta m = mapper.readValue(new FileInputStream("/tmp/meta.json"), Meta.class);
//
//            Map<String, String> vmap = VersionUtil.getVersionMap();
//
//            assertNotNull(m);
//            assertEquals(vmap.get("version") + '-' + vmap.get("release"), m.getVersion());
//            assertTrue(start.before(m.getCreated()));
//        }
//    }
//
//    public static class VerifyProductCert implements Verify {
//        private String filename;
//        public VerifyProductCert(String filename) {
//            this.filename = filename;
//        }
//
//        public void verify(ZipInputStream zis, byte[] buf) throws IOException {
//            OutputStream os = new FileOutputStream("/tmp/" + filename);
//            int n;
//            while ((n = zis.read(buf, 0, 1024)) > -1) {
//                os.write(buf, 0, n);
//            }
//            os.flush();
//            os.close();
//
//            BufferedReader br = new BufferedReader(new FileReader("/tmp/" + filename));
//            assertEquals("hpj-08ha-w4gpoknpon*)&^%#", br.readLine());
//            br.close();
//        }
//    }
//
//    public static class VerifyIdentityCert implements Verify {
//        private String filename;
//        public VerifyIdentityCert(String filename) {
//            this.filename = filename;
//        }
//
//        public void verify(ZipInputStream zis, byte[] buf) throws IOException {
//            OutputStream os = new FileOutputStream("/tmp/" + filename);
//            int n;
//            while ((n = zis.read(buf, 0, 1024)) > -1) {
//                os.write(buf, 0, n);
//            }
//            os.flush();
//            os.close();
//
//            BufferedReader br = new BufferedReader(new FileReader("/tmp/" + filename));
//            assertEquals("hpj-08ha-w4gpoknpon*)&^%#euh0876puhapodifbvj094", br.readLine());
//            br.close();
//        }
//    }
//
//    public static class VerifyKeyPair implements Verify {
//        private String filename;
//        public VerifyKeyPair(String filename) {
//            this.filename = filename;
//        }
//
//        public void verify(ZipInputStream zis, byte[] buf) throws IOException {
//            OutputStream os = new FileOutputStream("/tmp/" + filename);
//            int n;
//            while ((n = zis.read(buf, 0, 1024)) > -1) {
//                os.write(buf, 0, n);
//            }
//            os.flush();
//            os.close();
//
//            BufferedReader br = new BufferedReader(new FileReader("/tmp/" + filename));
//            assertEquals("privateKeypublicKey", br.readLine());
//            br.close();
//        }
//    }
//
//    public static class VerifyConsumer implements Verify {
//        private String filename;
//        public VerifyConsumer(String filename) {
//            this.filename = filename;
//        }
//
//        public void verify(ZipInputStream zis, byte[] buf) throws IOException {
//            OutputStream os = new FileOutputStream("/tmp/" + filename);
//            int n;
//            while ((n = zis.read(buf, 0, 1024)) > -1) {
//                os.write(buf, 0, n);
//            }
//            os.flush();
//            os.close();
//
//
//            Map<String, String> configProps = new HashMap<>();
//            configProps.put(ConfigProperties.FAIL_ON_UNKNOWN_IMPORT_PROPERTIES, "false");
//
//            ObjectMapper mapper = new SyncUtils(new MapConfiguration(configProps)).getObjectMapper();
//
//            ConsumerDTO c = mapper.readValue(new FileInputStream("/tmp/" + filename), ConsumerDTO.class);
//
//            assertEquals("localhost:8443/apiurl", c.getUrlApi());
//            assertEquals("localhost:8443/weburl", c.getUrlWeb());
//            assertEquals("8auuid", c.getUuid());
//            assertEquals("consumer_name", c.getName());
//            assertEquals("access_mode", c.getContentAccessMode());
//
//            ConsumerType type = new ConsumerType(ConsumerTypeEnum.CANDLEPIN);
//            assertEquals(type.getLabel(), c.getType().getLabel());
//            assertEquals(type.isManifest(), c.getType().isManifest());
//        }
//    }
//
//    public static class VerifyDistributorVersion implements Verify {
//        private String filename;
//
//        public VerifyDistributorVersion(String filename) {
//            this.filename = filename;
//        }
//
//        public void verify(ZipInputStream zis, byte[] buf) throws IOException {
//            OutputStream os = new FileOutputStream("/tmp/" + filename);
//            int n;
//            while ((n = zis.read(buf, 0, 1024)) > -1) {
//                os.write(buf, 0, n);
//            }
//            os.flush();
//            os.close();
//
//            Map<String, String> configProps = new HashMap<>();
//            configProps.put(ConfigProperties.FAIL_ON_UNKNOWN_IMPORT_PROPERTIES, "false");
//
//            ObjectMapper mapper = new SyncUtils(new MapConfiguration(configProps)).getObjectMapper();
//
//            DistributorVersion dv = mapper.readValue(
//                new FileInputStream("/tmp/" + filename),
//                DistributorVersion.class);
//            assertNotNull(dv);
//            assertEquals("test-dist-ver", dv.getName());
//            assertEquals(3, dv.getCapabilities().size());
//        }
//    }
}
