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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.candlepin.auth.NoAuthPrincipal;
import org.candlepin.guice.PrincipalProvider;
import org.candlepin.model.CertificateSerial;
import org.candlepin.model.Consumer;
import org.candlepin.model.Entitlement;
import org.candlepin.model.EntitlementCertificate;
import org.candlepin.model.Pool;
import org.candlepin.policy.js.export.ExportRules;
import org.candlepin.service.EntitlementCertServiceAdapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class EntitlementCertExporterTest {

    private static final Path EXPORT_PATH = Paths.get("/export");
    private static final String CERT = "content-access-cert";
    private static final String KEY = "content-access-key";

    private EntitlementCertServiceAdapter ecsa;
    private ExportRules exportRules;
    private SpyingExporter<String> fileExporter;
    private Consumer consumer;
    private Set<Long> certSerials;

    @BeforeEach
    void setUp() {
        ecsa = mock(EntitlementCertServiceAdapter.class);
        exportRules = mock(ExportRules.class);
        fileExporter = new SpyingExporter<>();

        when(exportRules.canExport(any(Entitlement.class))).thenReturn(true);
        consumer = mock(Consumer.class);

        EntitlementCertificate entCert = new EntitlementCertificate();
        CertificateSerial entSerial = new CertificateSerial();
        entSerial.setId(123456L);
        entCert.setSerial(entSerial);
        entCert.setCert(CERT);
        entCert.setKey(KEY);
        Entitlement entitlement = new Entitlement();
        entitlement.setPool(new Pool());
        entCert.setEntitlement(entitlement);

        when(ecsa.listForConsumer(consumer)).thenReturn(Arrays.asList(entCert));
        this.certSerials = new HashSet<>();
        this.certSerials.add(entSerial.getId());
    }

    @Test
    public void nothingToExport() throws ExportCreationException {
        NoAuthPrincipal principal = new NoAuthPrincipal();
        PrincipalProvider principalProvider = mock(PrincipalProvider.class);
        when(principalProvider.get()).thenReturn(principal);
        EntitlementCertExporter exporter = new EntitlementCertExporter(ecsa, exportRules, fileExporter);
        when(ecsa.listForConsumer(consumer)).thenReturn(Collections.emptyList());

        exporter.exportTo(EXPORT_PATH, consumer, certSerials, false);

        assertEquals(0, fileExporter.calledTimes);
    }

    @Test
    public void skipWhenManifestAndCannotExport() throws ExportCreationException {
        NoAuthPrincipal principal = new NoAuthPrincipal();
        PrincipalProvider principalProvider = mock(PrincipalProvider.class);
        when(principalProvider.get()).thenReturn(principal);
        EntitlementCertExporter exporter = new EntitlementCertExporter(ecsa, exportRules, fileExporter);
        when(exportRules.canExport(any(Entitlement.class))).thenReturn(false);

        exporter.exportTo(EXPORT_PATH, consumer, certSerials, true);

        assertEquals(0, fileExporter.calledTimes);
    }

    @Test
    public void skipWhenNotInSerials() throws ExportCreationException {
        NoAuthPrincipal principal = new NoAuthPrincipal();
        PrincipalProvider principalProvider = mock(PrincipalProvider.class);
        when(principalProvider.get()).thenReturn(principal);
        EntitlementCertExporter exporter = new EntitlementCertExporter(ecsa, exportRules, fileExporter);

        exporter.exportTo(EXPORT_PATH, consumer, Collections.emptySet(), false);

        assertEquals(0, fileExporter.calledTimes);
    }

    @Test
    public void exportCerts() throws ExportCreationException {
        NoAuthPrincipal principal = new NoAuthPrincipal();
        PrincipalProvider principalProvider = mock(PrincipalProvider.class);
        when(principalProvider.get()).thenReturn(principal);
        EntitlementCertExporter exporter = new EntitlementCertExporter(ecsa, exportRules, fileExporter);

        exporter.exportTo(EXPORT_PATH, consumer, certSerials, false);

        String cert = (String) fileExporter.nth(0);
        String key = (String) fileExporter.nth(0, 1);
        assertEquals(CERT, cert);
        assertEquals(KEY, key);
    }

}