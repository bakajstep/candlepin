/**
 * Copyright (c) 2009 - 2022 Red Hat, Inc.
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

package org.candlepin.spec.bootstrap.data.util;

import org.candlepin.dto.api.client.v1.AsyncJobStatusDTO;
import org.candlepin.dto.api.client.v1.OwnerDTO;
import org.candlepin.invoker.client.ApiException;
import org.candlepin.spec.bootstrap.client.ApiClient;
import org.candlepin.spec.bootstrap.data.builder.Export;
import org.candlepin.spec.bootstrap.data.builder.ExportGenerator;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public final class Importer {

    private final ApiClient client;
    private final boolean async;

    public Importer(ApiClient client, boolean async) {
        this.client = client;
        this.async = async;
    }

    public void doImport(String ownerKey, File export) {
        doImport(ownerKey, export, List.of());
    }

    public void doImport(String ownerKey, File export, String... force) {
        doImport(ownerKey, export, Arrays.asList(force));
    }
    public void doImport(String ownerKey, File export, List<String> force) {
        if (this.async) {
            importAsync(ownerKey, export, force);
        }
        else {
            importSync(ownerKey, export, force);
        }
    }

    public void undoImport(OwnerDTO owner) {
        AsyncJobStatusDTO job = client.owners().undoImports(owner.getKey());
        client.jobs().waitForJob(job);
    }

    public Export generateFullExport() {
        try (ExportGenerator exportGenerator = new ExportGenerator(client)) {
            return exportGenerator.full().export();
        }
    }

    public Export generateSimpleExport() {
        try (ExportGenerator exportGenerator = new ExportGenerator(client)) {
            return exportGenerator.full().export();
        }
    }

    private void importSync(String ownerKey, File export, List<String> force) {
        client.owners().importManifest(ownerKey, force, export);
    }

    private void importAsync(String ownerKey, File export, List<String> force) throws ApiException {
        AsyncJobStatusDTO importJob = client.owners().importManifestAsync(ownerKey, force, export);
        AsyncJobStatusDTO result = client.jobs().waitForJob(importJob);
        String resultData = result.getResultData().toString();
        if (resultData.contains("already been imported")) {
            throw new ApiException(400, resultData);
        }
        else if (resultData.contains("conflict")) {
            throw new ApiException(409, resultData);
        }
    }


}
