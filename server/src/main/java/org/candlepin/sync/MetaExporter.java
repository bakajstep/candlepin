/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
 * <p>
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 * <p>
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.sync;

import org.candlepin.common.util.VersionUtil;
import org.candlepin.guice.PrincipalProvider;

import com.google.inject.Inject;

import java.nio.file.Path;
import java.util.Date;
import java.util.Map;

/**
 * Meta maps to meta.json in the export
 */
public class MetaExporter {

    private final PrincipalProvider principalProvider;
    private final FileExporter<Object> exporter;

    @Inject
    MetaExporter(PrincipalProvider principalProvider, FileExporter<Object> exporter) {
        this.principalProvider = principalProvider;
        this.exporter = exporter;
    }

    public void exportTo(Path exportDir, String cdnKey) throws ExportCreationException {
        Path exportFile = exportDir.resolve("meta.json");
        Meta meta = createMeta(cdnKey);
        this.exporter.exportTo(exportFile, meta);
    }

    private Meta createMeta(String cdnKey) {
        return new Meta(createVersion(), new Date(),
            this.principalProvider.get().getName(), null, cdnKey);
    }

    private String createVersion() {
        // TODO This should be injectable dependency as it accesses the FS
        Map<String, String> map = VersionUtil.getVersionMap();
        System.out.println(map);
        return map.get("version") + "-" + map.get("release");
    }

}
