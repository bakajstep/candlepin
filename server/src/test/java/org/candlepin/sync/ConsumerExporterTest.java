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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.candlepin.common.config.Configuration;
import org.candlepin.dto.ModelTranslator;
import org.candlepin.dto.SimpleModelTranslator;
import org.candlepin.dto.manifest.v1.ConsumerDTO;
import org.candlepin.dto.manifest.v1.ConsumerTranslator;
import org.candlepin.dto.manifest.v1.ConsumerTypeDTO;
import org.candlepin.dto.manifest.v1.ConsumerTypeTranslator;
import org.candlepin.model.Consumer;
import org.candlepin.model.ConsumerType;
import org.candlepin.model.ConsumerTypeCurator;
import org.candlepin.model.OwnerCurator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;



/**
 * ConsumerExporterTest
 */
@ExtendWith(MockitoExtension.class)
public class ConsumerExporterTest {

    private static final Path EXPORT_DIR = Paths.get("/export");
    private static final String WEB_URL = "/subscriptions";
    private static final String API_URL = "/candlepin";
    private static final String CONFIG_OVERRIDE = "/config_override";

    private ModelTranslator translator;
    private Configuration configuration;
    private ConsumerTypeCurator typeCurator;

    @BeforeEach
    void setUp() {
        configuration = mock(Configuration.class);
        translator = new SimpleModelTranslator();
        typeCurator = mock(ConsumerTypeCurator.class);
        translator.registerTranslator(new ConsumerTranslator(typeCurator, mock(OwnerCurator.class)), Consumer.class, ConsumerDTO.class);
        translator.registerTranslator(new ConsumerTypeTranslator(), ConsumerType.class, ConsumerTypeDTO.class);
    }

    @Test
    public void exportWithOverrides() throws ExportCreationException {
        SpyingExporter<Object> fileExporter = new SpyingExporter<>();
        ConsumerExporter exporter = new ConsumerExporter(configuration, fileExporter, translator);
        ConsumerType ctype = createCType();
        Consumer consumer = createConsumer(ctype);
        when(typeCurator.getConsumerType(any(Consumer.class))).thenReturn(ctype);

        exporter.exportTo(EXPORT_DIR, consumer, WEB_URL, API_URL);

        assertThat(fileExporter.calledTimes).isEqualTo(1);
        ConsumerDTO exportedConsumer = (ConsumerDTO) fileExporter.nth(0);
        assertThat(exportedConsumer.getUuid()).isEqualTo(consumer.getUuid());
        assertThat(exportedConsumer.getName()).isEqualTo(consumer.getName());
        assertThat(exportedConsumer.getUrlWeb()).isEqualTo(WEB_URL);
        assertThat(exportedConsumer.getUrlApi()).isEqualTo(API_URL);
        ConsumerTypeDTO exportedCType = exportedConsumer.getType();
        assertThat(exportedCType.getId()).isEqualTo(ctype.getId());
        assertThat(exportedCType.getLabel()).isEqualTo(ctype.getLabel());
        assertThat(exportedCType.isManifest()).isEqualTo(ctype.isManifest());
        verifyZeroInteractions(this.configuration);
    }

    @Test
    public void exportWithoutOverrides() throws ExportCreationException {
        SpyingExporter<Object> fileExporter = new SpyingExporter<>();
        ConsumerExporter exporter = new ConsumerExporter(configuration, fileExporter, translator);
        ConsumerType ctype = createCType();
        Consumer consumer = createConsumer(ctype);
        when(configuration.getString(anyString())).thenReturn(CONFIG_OVERRIDE);
        when(typeCurator.getConsumerType(any(Consumer.class))).thenReturn(ctype);

        exporter.exportTo(EXPORT_DIR, consumer, null, null);

        assertThat(fileExporter.calledTimes).isEqualTo(1);
        ConsumerDTO exportedConsumer = (ConsumerDTO) fileExporter.nth(0);
        assertThat(exportedConsumer.getUuid()).isEqualTo(consumer.getUuid());
        assertThat(exportedConsumer.getName()).isEqualTo(consumer.getName());
        assertThat(exportedConsumer.getUrlWeb()).isEqualTo(CONFIG_OVERRIDE);
        assertThat(exportedConsumer.getUrlApi()).isEqualTo(CONFIG_OVERRIDE);
        ConsumerTypeDTO exportedCType = exportedConsumer.getType();
        assertThat(exportedCType.getId()).isEqualTo(ctype.getId());
        assertThat(exportedCType.getLabel()).isEqualTo(ctype.getLabel());
        assertThat(exportedCType.isManifest()).isEqualTo(ctype.isManifest());
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
