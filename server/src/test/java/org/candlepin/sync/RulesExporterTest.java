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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.candlepin.model.Rules;
import org.candlepin.model.RulesCurator;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

public class RulesExporterTest {

    private static final String FAKE_RULES = "//Version: 2.0\nHELLO WORLD";
    private static final String LEGACY_RULES = "//Version: 2.0\nLEGACY WORLD";

    private FileSystem fileSystem;

    @BeforeEach
    void setUp() {
        this.fileSystem = Jimfs.newFileSystem(Configuration.unix());
    }

    @Test
    public void exportRules() throws ExportCreationException, IOException {
        Path exportPath = this.fileSystem.getPath("/export");
        Path legacyRulesPath = createLegacyRules(exportPath);
        SpyingExporter<String> fileExporter = new SpyingExporter<>();
        LegacyRulesFileProvider legacyRules = mock(LegacyRulesFileProvider.class);
        when(legacyRules.get()).thenReturn(legacyRulesPath);
        RulesCurator rulesCurator = mock(RulesCurator.class);
        when(rulesCurator.getRules()).thenReturn(new Rules(FAKE_RULES));
        RulesExporter exporter = new RulesExporter(rulesCurator, legacyRules, fileExporter);

        exporter.exportTo(exportPath);

        assertEquals(2, fileExporter.calledTimes);
        assertEquals(FAKE_RULES, fileExporter.nth(0));
        assertEquals(LEGACY_RULES, fileExporter.nth(1));
    }

    private Path createLegacyRules(Path exportPath) throws IOException {
        Path legacyRules = exportPath.resolve("legacy");
        Files.createDirectories(exportPath);
        Files.write(legacyRules, LEGACY_RULES.getBytes(StandardCharsets.UTF_8));
        return legacyRules;
    }

}
