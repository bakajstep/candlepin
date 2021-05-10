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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RulesExporter {

    private final RulesCurator rulesCurator;
    private final LegacyRulesFileProvider legacyRules;
    private final FileExporter<String> fileExporter;

    @Inject
    public RulesExporter(RulesCurator rulesCurator, LegacyRulesFileProvider legacyRules, FileExporter<String> fileExporter) {
        this.rulesCurator = rulesCurator;
        this.legacyRules = legacyRules;
        this.fileExporter = fileExporter;
    }

    public void exportTo(Path exportDir) throws ExportCreationException {
        // Because old candlepin servers assume to import a file in rules dir, we had to
        // move to a new directory for versioned rules file:
        Path newRulesDir = exportDir.resolve("rules2");
        Path newRulesFile = newRulesDir.resolve("rules.js");
        exportRules(newRulesFile);
        exportLegacyRules(exportDir);
    }

    private void exportRules(Path export) throws ExportCreationException {
        fileExporter.exportTo(export, rulesCurator.getRules().getRules());
    }

    /**
     * We still need to export a copy of the deprecated default-rules.js so new manifests
     * can still be imported by old candlepin servers.
     */
    private void exportLegacyRules(Path baseDir) throws ExportCreationException {
        Path oldRulesDir = baseDir.resolve("rules");
        Path oldRulesFile = oldRulesDir.resolve("default-rules.js");

        String legacyRules = readLegacyRules();
        fileExporter.exportTo(oldRulesFile, legacyRules);
    }

    private String readLegacyRules() throws ExportCreationException {
        try {
            return Files.readString(this.legacyRules.get());
        }
        catch (IOException e) {
            throw new ExportCreationException("Failed to copy legacy rules", e);
        }
    }

}
