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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JsonFileExporter implements FileExporter<Object> {

    private final ObjectMapper mapper;
    private final FileExporter<String> exporter;

    public JsonFileExporter(ObjectMapper mapper, FileExporter<String> exporter) {
        this.mapper = mapper;
        this.exporter = exporter;
    }

    @Override
    public void exportTo(Path path, Object... exports) throws ExportCreationException {
        String[] strings = toJson(exports);
        this.exporter.exportTo(path, strings);
    }

    private String[] toJson(Object[] exports) throws ExportCreationException {
        String[] strings = new String[exports.length];
        for (int i = 0; i < exports.length; i++) {
            Object export = exports[i];
            strings[i] = toJson(export);
        }
        return strings;
    }

    private String toJson(Object object) throws ExportCreationException {
        try {
            return this.mapper.writeValueAsString(object);
        }
        catch (JsonProcessingException e) {
            throw new ExportCreationException("Could not write to the export file: ", e);
        }
    }

}
