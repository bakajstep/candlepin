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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.candlepin.dto.ModelTranslator;
import org.candlepin.dto.SimpleModelTranslator;
import org.candlepin.dto.manifest.v1.ConsumerTypeDTO;
import org.candlepin.dto.manifest.v1.ConsumerTypeTranslator;
import org.candlepin.model.CandlepinQuery;
import org.candlepin.model.ConsumerType;
import org.candlepin.model.ConsumerTypeCurator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;


public class ConsumerTypeExporterTest {

    private static final Path EXPORT_PATH = Paths.get("/export");
    public static final String TEST_LABEL = "TESTTYPE";

    private ConsumerTypeCurator consumerTypeCurator;
    private ModelTranslator translator;

    @BeforeEach
    void setUp() {
        consumerTypeCurator = mock(ConsumerTypeCurator.class);
        translator = new SimpleModelTranslator();
        translator.registerTranslator(new ConsumerTypeTranslator(), ConsumerType.class, ConsumerTypeDTO.class);
    }

    @Test
    public void successfulExport() throws ExportCreationException {
        SpyingExporter<Object> fileExporter = new SpyingExporter<>();
        ConsumerTypeExporter consumerType = new ConsumerTypeExporter(consumerTypeCurator, fileExporter, translator);
        CandlepinQuery query = mock(CandlepinQuery.class);
        when(query.iterator()).thenReturn(createTypes());
        when(consumerTypeCurator.listAll()).thenReturn(query);

        consumerType.exportTo(EXPORT_PATH);

        assertEquals(1, fileExporter.calledTimes);
        ConsumerTypeDTO export = (ConsumerTypeDTO) fileExporter.lastExports[0];
        assertNull(export.getId());
        assertEquals(TEST_LABEL, export.getLabel());
        assertFalse(export.isManifest());
    }

    @Test
    public void nothingToExport() throws ExportCreationException {
        SpyingExporter<Object> fileExporter = new SpyingExporter<>();
        ConsumerTypeExporter consumerType = new ConsumerTypeExporter(consumerTypeCurator, fileExporter, translator);
        CandlepinQuery query = mock(CandlepinQuery.class);
        when(query.iterator()).thenReturn(Collections.emptyList().iterator());
        when(consumerTypeCurator.listAll()).thenReturn(query);

        consumerType.exportTo(EXPORT_PATH);

        assertEquals(0, fileExporter.calledTimes);
    }

    private Iterator<ConsumerType> createTypes() {
        return Arrays.asList(
            new ConsumerType(TEST_LABEL)).iterator();
    }

}
