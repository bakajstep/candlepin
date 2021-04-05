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
import java.io.StringWriter;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

class JsonFileExporterTest {

    private FileSystem fileSystem;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        this.fileSystem = Jimfs.newFileSystem(Configuration.unix());
        this.mapper = createMapper();
    }

    @Test
    public void testMetaExporter() throws IOException {
        FileExporter exporter = new JsonFileExporter(mapper);
        Path exportPath = this.fileSystem.getPath("/export");
        Date now = new Date();
        String nowString = mapper.convertValue(now, String.class);
        Meta meta = createMeta(now);

        exporter.export(exportPath, Arrays.asList(meta));

        StringBuffer json = expectedJson(nowString);
        assertTrue(TestUtil.isJsonEqual(json.toString(), Files.readString(exportPath)));
    }

    private Meta createMeta(Date now) {
        Meta meta = new Meta();
        meta.setVersion("0.1.0");
        meta.setCreated(now);
        meta.setPrincipalName("myUsername");
        meta.setWebAppPrefix("webapp_prefix");
        meta.setCdnLabel("test-cdn");
        return meta;
    }

    private ObjectMapper createMapper() {
        Map<String, String> configProps = new HashMap<>();
        configProps.put(ConfigProperties.FAIL_ON_UNKNOWN_IMPORT_PROPERTIES, "false");

        ObjectMapper mapper = new SyncUtils(new MapConfiguration(configProps)).getObjectMapper();
        return mapper;
    }

    private StringBuffer expectedJson(String nowString) {
        StringBuffer json = new StringBuffer();
        json.append("{\"version\":\"0.1.0\",\"created\":\"").append(nowString);
        json.append("\",\"principalName\":\"myUsername\",");
        json.append("\"webAppPrefix\":\"webapp_prefix\",");
        json.append("\"cdnLabel\":\"test-cdn\"}");
        return json;
    }

}