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
import org.candlepin.dto.manifest.v1.DistributorVersionDTO;
import org.candlepin.dto.manifest.v1.DistributorVersionTranslator;
import org.candlepin.model.DistributorVersion;
import org.candlepin.model.DistributorVersionCurator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class DistributorVersionExporterTest {

    private static final Path EXPORT_PATH = Paths.get("/export");

    private DistributorVersionCurator dvCurator;
    private ModelTranslator translator;

    @BeforeEach
    void setUp() {
        this.dvCurator = mock(DistributorVersionCurator.class);
        this.translator = new SimpleModelTranslator();
        this.translator.registerTranslator(new DistributorVersionTranslator(), DistributorVersion.class, DistributorVersionDTO.class);
    }

    @Test
    public void successfulExport() throws ExportCreationException {
        SpyingExporter<Object> fileExporter = new SpyingExporter<>();
        when(dvCurator.findAll()).thenReturn(createDVs());
        DistributorVersionExporter exporter = new DistributorVersionExporter(dvCurator, fileExporter, translator);

        exporter.exportTo(EXPORT_PATH);

        assertEquals(3, fileExporter.calledTimes);
        DistributorVersionDTO result = (DistributorVersionDTO) fileExporter.lastExports[0];
        assertEquals(result.getName(), "distributor_version_3");
    }

    @Test
    public void nothingToExport() throws ExportCreationException {
        SpyingExporter<Object> fileExporter = new SpyingExporter<>();
        when(dvCurator.findAll()).thenReturn(Collections.emptyList());
        DistributorVersionExporter exporter = new DistributorVersionExporter(dvCurator, fileExporter, translator);

        exporter.exportTo(EXPORT_PATH);

        assertEquals(0, fileExporter.calledTimes);
        assertEquals(0, fileExporter.exports.size());
    }

    private List<DistributorVersion> createDVs() {
        return Arrays.asList(
            new DistributorVersion("distributor_version_1"),
            new DistributorVersion("distributor_version_2"),
            new DistributorVersion("distributor_version_3")
        );
    }

}