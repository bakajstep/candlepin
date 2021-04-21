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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.candlepin.dto.ModelTranslator;
import org.candlepin.dto.SimpleModelTranslator;
import org.candlepin.dto.manifest.v1.EntitlementDTO;
import org.candlepin.dto.manifest.v1.EntitlementTranslator;
import org.candlepin.dto.manifest.v1.PoolDTO;
import org.candlepin.dto.manifest.v1.PoolTranslator;
import org.candlepin.model.Consumer;
import org.candlepin.model.ConsumerType;
import org.candlepin.model.Entitlement;
import org.candlepin.model.EntitlementCurator;
import org.candlepin.model.Pool;
import org.candlepin.policy.js.export.ExportRules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

class EntitlementExporterTest {

    public static final Path EXPORT_DIR = Paths.get("entitlements");

    private EntitlementCurator entitlementCurator;
    private ModelTranslator translator;
    private ExportRules exportRules;

    @BeforeEach
    void setUp() {
        exportRules = mock(ExportRules.class);
        entitlementCurator = mock(EntitlementCurator.class);
        translator = new SimpleModelTranslator();
        translator.registerTranslator(new EntitlementTranslator(), Entitlement.class, EntitlementDTO.class);
        translator.registerTranslator(new PoolTranslator(), Pool.class, PoolDTO.class);
    }

    @Test
    public void shouldExport() throws ExportCreationException, IOException {
        when(exportRules.canExport(any(Entitlement.class))).thenReturn(true);
        Consumer consumer = mock(Consumer.class);
        ConsumerType ctype = new ConsumerType(ConsumerType.ConsumerTypeEnum.CANDLEPIN);
        ctype.setId("test-ctype");
        when(consumer.getUuid()).thenReturn("consumer");
        when(consumer.getName()).thenReturn("consumer_name");
        when(consumer.getTypeId()).thenReturn(ctype.getId());
//        when(ctc.getConsumerType(eq(consumer))).thenReturn(ctype);
//        when(ctc.get(eq(ctype.getId()))).thenReturn(ctype);

//        when(ecsa.listForConsumer(consumer)).thenReturn(Arrays.asList(entCert));
//        when(contentAccessManager.getCertificate(consumer)).thenReturn(cac);

        List<Entitlement> entitlements = Arrays.asList(
            createEntitlement(),
            createEntitlement()
        );

        SpyingExporter fileExporter = new SpyingExporter();
        when(entitlementCurator.listByConsumer(eq(consumer))).thenReturn(entitlements);
        EntitlementExporter exporter = new EntitlementExporter(
            entitlementCurator,
            exportRules,
            fileExporter,
            translator
        );

        exporter.exportTo(EXPORT_DIR, consumer);

        assertThat(fileExporter.calledTimes).isEqualTo(2);
    }

    @Test
    public void shouldNotExportDirtyEntitlements() {
        Consumer consumer = mock(Consumer.class);
        List<Entitlement> entitlements = Arrays.asList(
            createDirtyEntitlement(),
            createDirtyEntitlement(),
            createDirtyEntitlement()
        );
        when(entitlementCurator.listByConsumer(consumer)).thenReturn(entitlements);
        SpyingExporter fileExporter = new SpyingExporter();
        EntitlementExporter exporter = new EntitlementExporter(
            entitlementCurator,
            exportRules,
            fileExporter,
            translator
        );

        assertThatThrownBy(() -> exporter.exportTo(EXPORT_DIR, consumer))
            .isInstanceOf(ExportCreationException.class);
        assertThat(fileExporter.calledTimes).isEqualTo(0);
    }

    @Test
    public void shouldFilterExportedEntitlementsByRules() throws Exception {
        when(exportRules.canExport(any(Entitlement.class))).thenReturn(false);
        Consumer consumer = mock(Consumer.class);
        List<Entitlement> entitlements = Arrays.asList(
            createEntitlement(),
            createEntitlement(),
            createEntitlement()
        );
        when(entitlementCurator.listByConsumer(eq(consumer))).thenReturn(entitlements);
        SpyingExporter fileExporter = new SpyingExporter();
        EntitlementExporter exporter = new EntitlementExporter(
            entitlementCurator,
            mock(ExportRules.class),
            fileExporter,
            translator
        );

        exporter.exportTo(EXPORT_DIR, consumer);

        assertThat(fileExporter.calledTimes).isEqualTo(0);
    }

    private Entitlement createDirtyEntitlement() {
        return createEntitlement(true);
    }

    private Entitlement createEntitlement() {
        return createEntitlement(false);
    }

    private Entitlement createEntitlement(boolean dirty) {
        Entitlement entitlement = new Entitlement();
        entitlement.setPool(new Pool());
        entitlement.setDirty(dirty);
        return entitlement;
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