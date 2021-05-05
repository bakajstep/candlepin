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

    public static final Path EXPORT_DIR = Paths.get("export");
    public static final Path EXPECTED_PATH_1 = Paths.get("export/entitlements/ent_1.json");
    public static final Path EXPECTED_PATH_2 = Paths.get("export/entitlements/ent_2.json");

    private EntitlementCurator entitlementCurator;
    private ExportRules exportRules;
    private SpyingExporter<Object> fileExporter;
    private EntitlementExporter exporter;

    @BeforeEach
    void setUp() {
        this.exportRules = mock(ExportRules.class);
        this.entitlementCurator = mock(EntitlementCurator.class);
        this.fileExporter = new SpyingExporter<>();
        ModelTranslator translator = new SimpleModelTranslator();
        translator.registerTranslator(new EntitlementTranslator(), Entitlement.class, EntitlementDTO.class);
        translator.registerTranslator(new PoolTranslator(), Pool.class, PoolDTO.class);
        this.exporter = new EntitlementExporter(
            entitlementCurator,
            exportRules,
            fileExporter,
            translator
        );
    }

    @Test
    public void shouldExport() throws ExportCreationException {
        when(exportRules.canExport(any(Entitlement.class))).thenReturn(true);
        Consumer consumer = getConsumer();
        List<Entitlement> entitlements = Arrays.asList(
            createEntitlement("ent_1"),
            createEntitlement("ent_2")
        );
        when(entitlementCurator.listByConsumer(eq(consumer))).thenReturn(entitlements);

        exporter.exportTo(EXPORT_DIR, consumer);

        assertThat(fileExporter.calledTimes).isEqualTo(2);
        assertThat(fileExporter.exports).hasSize(2);
        assertThat(fileExporter.exports.get(0)).hasSize(1);
        assertThat(fileExporter.exports.get(1)).hasSize(1);
    }

    @Test
    public void shouldNotExportDirtyEntitlements() {
        Consumer consumer = getConsumer();
        List<Entitlement> entitlements = createDirtyEntitlements();
        when(entitlementCurator.listByConsumer(consumer)).thenReturn(entitlements);

        assertThatThrownBy(() -> exporter.exportTo(EXPORT_DIR, consumer))
            .isInstanceOf(ExportCreationException.class);
        assertThat(fileExporter.calledTimes).isEqualTo(0);
    }

    private List<Entitlement> createDirtyEntitlements() {
        return Arrays.asList(
            createDirtyEntitlement(),
            createDirtyEntitlement(),
            createDirtyEntitlement()
        );
    }

    @Test
    public void shouldFilterExportedEntitlementsByRules() throws Exception {
        when(exportRules.canExport(any(Entitlement.class))).thenReturn(false);
        Consumer consumer = getConsumer();
        List<Entitlement> entitlements = Arrays.asList(
            createEntitlement("ent_1"),
            createEntitlement("ent_2"),
            createEntitlement("ent_3")
        );
        when(entitlementCurator.listByConsumer(eq(consumer))).thenReturn(entitlements);

        exporter.exportTo(EXPORT_DIR, consumer);

        assertThat(fileExporter.calledTimes).isEqualTo(0);
    }

    private Consumer getConsumer() {
        Consumer consumer = mock(Consumer.class);
        ConsumerType ctype = new ConsumerType(ConsumerType.ConsumerTypeEnum.CANDLEPIN);
        ctype.setId("test-ctype");
        when(consumer.getUuid()).thenReturn("consumer");
        when(consumer.getName()).thenReturn("consumer_name");
        when(consumer.getTypeId()).thenReturn(ctype.getId());
        return consumer;
    }

    private Entitlement createDirtyEntitlement() {
        return createEntitlement(true, "ent");
    }

    private Entitlement createEntitlement(String id) {
        return createEntitlement(false, id);
    }

    private Entitlement createEntitlement(boolean dirty, String id) {
        Entitlement entitlement = new Entitlement();
        entitlement.setId(id);
        entitlement.setPool(new Pool());
        entitlement.setDirty(dirty);
        return entitlement;
    }

}
