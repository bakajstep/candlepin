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

import org.candlepin.model.RulesCurator;

import com.google.inject.Inject;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * RulesExporter
 */
public class RulesExporter {

    private static final String LEGACY_RULES_FILE = "/rules/default-rules.js";

    private Path legacyRulesFile;
    private RulesCurator rulesCurator;
    private FileExporter<String> fileExporter;

    @Inject
    public RulesExporter(Path asd, RulesCurator rulesCurator) {
        this.rulesCurator = rulesCurator;
    }

    public void exportTo(Path exportDir) throws ExportCreationException {
        // Because old candlepin servers assume to import a file in rules dir, we had to
        // move to a new directory for versioned rules file:
        Path newRulesDir = exportDir.resolve("rules2");
        Path newRulesFile = newRulesDir.resolve("rules.js");
        export(newRulesFile);
        exportLegacyRules(exportDir);
    }

    /*
     * We still need to export a copy of the deprecated default-rules.js so new manifests
     * can still be imported by old candlepin servers.
     */
    private void exportLegacyRules(Path baseDir) throws ExportCreationException {
        Path oldRulesDir = baseDir.resolve("rules");
        Path oldRulesFile = oldRulesDir.resolve("default-rules.js");

        // TODO: does this need a "exporter" object as well?
        try {
            Files.copy(this.legacyRulesFile, oldRulesFile);
        }
        catch (IOException e) {
            throw new ExportCreationException("", e);
        }
//        FileUtils.copyFile(new File(
//                this.getClass().getResource(LEGACY_RULES_FILE).getPath()),
//            oldRulesFile);
    }

    private void export(Path export) throws ExportCreationException {
        fileExporter.exportTo(export, rulesCurator.getRules().getRules());
    }

}
