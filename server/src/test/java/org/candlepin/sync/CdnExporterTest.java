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

import org.candlepin.auth.NoAuthPrincipal;
import org.candlepin.dto.ModelTranslator;
import org.candlepin.dto.SimpleModelTranslator;
import org.candlepin.dto.manifest.v1.CdnDTO;
import org.candlepin.dto.manifest.v1.CdnTranslator;
import org.candlepin.guice.PrincipalProvider;
import org.candlepin.model.CandlepinQuery;
import org.candlepin.model.Cdn;
import org.candlepin.model.CdnCurator;
import org.candlepin.test.MockResultIterator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

class CdnExporterTest {

    private CdnCurator cdnCurator;
    private ModelTranslator translator;

    @BeforeEach
    void setUp() {
        this.cdnCurator = mock(CdnCurator.class);
        translator = new SimpleModelTranslator();
        translator.registerTranslator(new CdnTranslator(),Cdn.class,CdnDTO.class);
    }

    @Test
    public void testCdnExporter() throws ExportCreationException {
        SpyingExporter fileExporter = new SpyingExporter();
        NoAuthPrincipal principal = new NoAuthPrincipal();
        PrincipalProvider principalProvider = mock(PrincipalProvider.class);
        when(principalProvider.get()).thenReturn(principal);
        CandlepinQuery<Cdn> q = mock(CandlepinQuery.class);
        when(q.iterate()).thenReturn(new MockResultIterator<>(createCdns().iterator()));
        when(cdnCurator.listAll()).thenReturn(q);

        CdnExporter exporter = new CdnExporter(cdnCurator, fileExporter, translator);
        Path path = Paths.get("/cdn.json");

        exporter.exportTo(path);

        assertEquals(3, fileExporter.calledTimes);
        CdnDTO result = (CdnDTO) fileExporter.lastExports[0];
        assertEquals(result.getLabel(), "cdn_label_3");
        assertEquals(result.getName(), "cdn_name_3");
        assertEquals(result.getUrl(), "cdn_url_3");
    }

    private List<Cdn> createCdns() {
        return Arrays.asList(
            new Cdn("cdn_label_1", "cdn_name_1", "cdn_url_1"),
            new Cdn("cdn_label_2", "cdn_name_2", "cdn_url_2"),
            new Cdn("cdn_label_3", "cdn_name_3", "cdn_url_3")
        );
    }

}