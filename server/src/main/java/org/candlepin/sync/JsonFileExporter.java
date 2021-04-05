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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class JsonFileExporter implements FileExporter {

    private final ObjectMapper mapper;

    public JsonFileExporter(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void export(Path file, List<Object> exports) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(file)) {
            for (Object export : exports) {
                mapper.writeValue(outputStream, export);
            }
        }
    }

}
