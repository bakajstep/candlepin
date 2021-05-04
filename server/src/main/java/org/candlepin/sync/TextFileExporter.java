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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TextFileExporter implements FileExporter<String> {

    private static final Logger log = LoggerFactory.getLogger(TextFileExporter.class);

    @Override
    public void exportTo(Path path, String... exports) throws ExportCreationException {
        createExportDirectories(path);
        writeExport(path, exports);
    }

    private void createExportDirectories(Path path) throws ExportCreationException {
        Path exportDir = path.getParent();
        log.trace("Creating an export dir: {}", exportDir);
        try {
            Files.createDirectories(exportDir);
        }
        catch (IOException e) {
            throw new ExportCreationException("Could not create the export directory: " + exportDir, e);
        }
    }

    private void writeExport(Path path, String[] exports) throws ExportCreationException {
        log.debug("Creating an export: {}", path);
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (String export : exports) {
                writer.write(export);
            }
        }
        catch (IOException e) {
            throw new ExportCreationException("Could not write to the export file: " + path, e);
        }
    }

}
