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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.candlepin.common.config.MapConfiguration;
import org.candlepin.config.ConfigProperties;
import org.candlepin.test.TestUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

class JsonFileExporterTest {

    public static final String VERSION_1 = "0.1.0";
    public static final String VERSION_2 = "0.2.0";
    private FileSystem fileSystem;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        this.fileSystem = Jimfs.newFileSystem(Configuration.unix());
        this.mapper = createMapper();
    }

    @Test
    public void canExportFile() throws IOException, ExportCreationException {
        FileExporter exporter = new JsonFileExporter(mapper);
        Path exportPath = this.fileSystem.getPath("/export.json");
        Date now = new Date();
        String nowString = mapper.convertValue(now, String.class);
        Meta meta = createMeta("0.1.0", now);

        exporter.exportTo(exportPath, meta);

        String expectedJson = expectedJson(VERSION_1, nowString);
        assertTrue(TestUtil.isJsonEqual(expectedJson, Files.readString(exportPath)));
    }

    @Test
    public void canExportMultipleObjects() throws IOException, ExportCreationException {
        FileExporter exporter = new JsonFileExporter(mapper);
        Path exportPath = this.fileSystem.getPath("/export.json");

        exporter.exportTo(exportPath, VERSION_1, VERSION_2);

        String expectedJson = "\"" + VERSION_1 + "\"\n\"" + VERSION_2 + "\"";
        assertEquals(expectedJson, Files.readString(exportPath));
    }

    @Test
    public void canExportToDirectory() throws IOException, ExportCreationException {
        FileExporter exporter = new JsonFileExporter(mapper);
        Path exportPath = this.fileSystem.getPath("/a/b/export.json");
        Date now = new Date();
        String nowString = mapper.convertValue(now, String.class);
        Meta meta = createMeta("0.1.0", now);

        exporter.exportTo(exportPath, meta);

        String expectedJson = expectedJson(VERSION_1, nowString);
        assertTrue(TestUtil.isJsonEqual(expectedJson, Files.readString(exportPath)));
    }

    private Meta createMeta(String version, Date now) {
        Meta meta = new Meta();
        meta.setVersion(version);
        meta.setCreated(now);
        meta.setPrincipalName("myUsername");
        meta.setWebAppPrefix("webapp_prefix");
        meta.setCdnLabel("test-cdn");
        return meta;
    }

    private ObjectMapper createMapper() {
        Map<String, String> configProps = new HashMap<>();
        configProps.put(ConfigProperties.FAIL_ON_UNKNOWN_IMPORT_PROPERTIES, "false");

        return new SyncUtils(new MapConfiguration(configProps)).getObjectMapper();
    }

    private String expectedMultiJson(String nowString) {
        return expectedJson(VERSION_1, nowString) + "\n" + expectedJson(VERSION_2, nowString);
    }

    private String expectedJson(String version, String nowString) {
        return "{\"version\":\"" + version + "\",\"created\":\"" + nowString +
            "\",\"principalName\":\"myUsername\"," +
            "\"webAppPrefix\":\"webapp_prefix\"," +
            "\"cdnLabel\":\"test-cdn\"}";
    }

}