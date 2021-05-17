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

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

@Tag("integration")
class TextFileExporterTest {

    public static final String VERSION_1 = "0.1.0";
    public static final String VERSION_2 = "0.2.0";

    private FileSystem fileSystem;

    @BeforeEach
    void setUp() {
        this.fileSystem = Jimfs.newFileSystem(Configuration.unix());
    }

    @Test
    public void canExportFile() throws IOException, ExportCreationException {
        FileExporter<String> exporter = new TextFileExporter();
        Path exportPath = this.fileSystem.getPath("/export.json");

        exporter.exportTo(exportPath, VERSION_1);

        assertEquals(VERSION_1, Files.readString(exportPath));
    }

    @Test
    public void canExportMultipleObjects() throws IOException, ExportCreationException {
        FileExporter<String> exporter = new TextFileExporter();
        Path exportPath = this.fileSystem.getPath("/export.json");

        exporter.exportTo(exportPath, VERSION_1, VERSION_2);

        String expected = VERSION_1 + VERSION_2;
        assertEquals(expected, Files.readString(exportPath));
    }

    @Test
    public void canExportToDirectory() throws IOException, ExportCreationException {
        FileExporter<String> exporter = new TextFileExporter();
        Path exportPath = this.fileSystem.getPath("/a/b/export.json");

        exporter.exportTo(exportPath, VERSION_1);

        assertEquals(VERSION_1, Files.readString(exportPath));
    }

}
