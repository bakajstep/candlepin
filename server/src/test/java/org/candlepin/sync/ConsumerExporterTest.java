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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.candlepin.common.config.Configuration;
import org.candlepin.common.config.MapConfiguration;
import org.candlepin.config.ConfigProperties;
import org.candlepin.dto.ModelTranslator;
import org.candlepin.dto.SimpleModelTranslator;
import org.candlepin.dto.StandardTranslator;
import org.candlepin.dto.manifest.v1.CdnDTO;
import org.candlepin.dto.manifest.v1.CdnTranslator;
import org.candlepin.dto.manifest.v1.ConsumerDTO;
import org.candlepin.dto.manifest.v1.ConsumerTypeDTO;
import org.candlepin.model.Cdn;
import org.candlepin.model.Consumer;
import org.candlepin.model.ConsumerType;
import org.candlepin.model.ConsumerTypeCurator;
import org.candlepin.model.EnvironmentCurator;
import org.candlepin.model.OwnerCurator;
import org.candlepin.test.TestUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;



/**
 * ConsumerExporterTest
 */
@ExtendWith(MockitoExtension.class)
public class ConsumerExporterTest {

    private ModelTranslator translator;

    @BeforeEach
    void setUp() {
        translator = new SimpleModelTranslator();
        translator.registerTranslator(new CdnTranslator(), Cdn.class, CdnDTO.class);
    }

    @Test
    public void testConsumerExport() throws IOException {
        SpyingExporter fileExporter = new SpyingExporter();
        ConsumerExporter exporter = new ConsumerExporter(
            mock(Configuration.class),
            fileExporter,
            translator
        );
        ConsumerType ctype = createCType();
        Consumer consumer = createConsumer(ctype);

        Path exportDir = Paths.get("");
        exporter.exportTo(exportDir, consumer, "/subscriptions", "/candlepin");

        assertThat(fileExporter.calledTimes).isEqualTo(1);
        Object lastExport = fileExporter.lastExports[0];
        assertThat(lastExport).isInstanceOf(Consumer.class);
        ConsumerDTO exportedConsumer = (ConsumerDTO) lastExport;

        assertThat(exportedConsumer.getUuid()).isEqualTo(consumer.getUuid());
        assertThat(exportedConsumer.getName()).isEqualTo(consumer.getName());
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
