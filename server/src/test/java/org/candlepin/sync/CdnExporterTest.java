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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.candlepin.dto.ModelTranslator;
import org.candlepin.dto.SimpleModelTranslator;
import org.candlepin.dto.manifest.v1.CdnDTO;
import org.candlepin.dto.manifest.v1.CdnTranslator;
import org.candlepin.model.CandlepinQuery;
import org.candlepin.model.Cdn;
import org.candlepin.model.CdnCurator;
import org.candlepin.test.MockResultIterator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class CdnExporterTest {

    private static final Path EXPORT_PATH = Paths.get("/export");

    private CdnCurator cdnCurator;
    private ModelTranslator translator;

    @BeforeEach
    void setUp() {
        this.cdnCurator = mock(CdnCurator.class);
        translator = new SimpleModelTranslator();
        translator.registerTranslator(new CdnTranslator(), Cdn.class, CdnDTO.class);
    }

    @Test
    public void successfulExport() throws ExportCreationException {
        SpyingExporter<Object> fileExporter = new SpyingExporter<>();
        mockCurator(createCdns());
        CdnExporter exporter = new CdnExporter(cdnCurator, fileExporter, translator);

        exporter.exportTo(EXPORT_PATH);

        assertEquals(3, fileExporter.calledTimes);
        CdnDTO result = (CdnDTO) fileExporter.nth(0);
        assertEquals(result.getLabel(), "cdn_label_1");
        assertEquals(result.getName(), "cdn_name_1");
        assertEquals(result.getUrl(), "cdn_url_1");
    }

    @Test
    public void nothingToExport() throws ExportCreationException {
        SpyingExporter<Object> fileExporter = new SpyingExporter<>();
        mockCurator(Collections.emptyList());
        CdnExporter exporter = new CdnExporter(cdnCurator, fileExporter, translator);

        exporter.exportTo(EXPORT_PATH);

        assertEquals(0, fileExporter.calledTimes);
    }

    private void mockCurator(List<Cdn> cdns) {
        CandlepinQuery<Cdn> q = mock(CandlepinQuery.class);
        when(q.iterate()).thenReturn(new MockResultIterator<>(cdns.iterator()));
        when(cdnCurator.listAll()).thenReturn(q);
    }

    private List<Cdn> createCdns() {
        return Arrays.asList(
            new Cdn("cdn_label_1", "cdn_name_1", "cdn_url_1"),
            new Cdn("cdn_label_2", "cdn_name_2", "cdn_url_2"),
            new Cdn("cdn_label_3", "cdn_name_3", "cdn_url_3")
        );
    }

}