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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.candlepin.config.ConfigProperties;
import org.candlepin.config.MapConfiguration;
import org.candlepin.model.ConsumerType;
import org.candlepin.model.ConsumerTypeCurator;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class ConsumerTypeImporterTest {
    private ObjectMapper mapper;

    @BeforeEach
    public void init() {
        Map<String, String> configProps = new HashMap<>();
        configProps.put(ConfigProperties.FAIL_ON_UNKNOWN_IMPORT_PROPERTIES, "false");

        this.mapper = new SyncUtils(new MapConfiguration(configProps)).getObjectMapper();
    }

    @Test
    public void testDeserialize() throws IOException {
        String consumerTypeString = "{\"id\":15, \"label\":\"prosumer\"}";

        Reader reader = new StringReader(consumerTypeString);
        ConsumerType consumerType = new ConsumerTypeImporter(null).createObject(this.mapper, reader);

        assertEquals("prosumer", consumerType.getLabel());
    }

    @Test
    public void testDeserializeIdIsNull() throws IOException {
        String consumerTypeString = "{\"id\":15, \"label\":\"prosumer\"}";

        Reader reader = new StringReader(consumerTypeString);
        ConsumerType consumerType = new ConsumerTypeImporter(null).createObject(this.mapper, reader);

        assertNull(consumerType.getId());
    }

    @Test
    public void testSingleConsumerTypeInDbAndListCausesNoChange() {
        final ConsumerType testType = new ConsumerType();
        testType.setLabel("prosumer");

        ConsumerTypeCurator curator = mock(ConsumerTypeCurator.class);

        when(curator.getByLabel("prosumer")).thenReturn(testType);

        ConsumerTypeImporter importer = new ConsumerTypeImporter(curator);
        importer.store(Set.of(testType));

        verify(curator, never()).create(testType);
        verify(curator, never()).merge(testType);
    }

    @Test
    public void testSingleConsumerTypeInListEmptyDbCausesInsert() {
        final ConsumerType testType = new ConsumerType();
        testType.setLabel("prosumer");

        ConsumerTypeCurator curator = mock(ConsumerTypeCurator.class);

        when(curator.getByLabel("prosumer")).thenReturn(null);

        ConsumerTypeImporter importer = new ConsumerTypeImporter(curator);
        importer.store(Set.of(testType));

        verify(curator).create(testType);
        verify(curator, never()).merge(testType);
    }
}
