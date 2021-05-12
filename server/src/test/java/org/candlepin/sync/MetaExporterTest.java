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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.candlepin.auth.NoAuthPrincipal;
import org.candlepin.guice.PrincipalProvider;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

public class MetaExporterTest {

    private static final Path EXPORT_PATH = Paths.get("/export");
    private static final String CDN_KEY = "test-cdn";

    @Test
    public void exportMeta() throws ExportCreationException {
        Date now = new Date();
        SpyingExporter<Object> exporter = new SpyingExporter<>();
        PrincipalProvider principalProvider = mock(PrincipalProvider.class);
        when(principalProvider.get()).thenReturn(new NoAuthPrincipal());
        MetaExporter metaEx = new MetaExporter(principalProvider, exporter);

        metaEx.exportTo(EXPORT_PATH, CDN_KEY);

        Meta result = (Meta) exporter.nth(0);
        assertEquals(result.getCdnLabel(), CDN_KEY);
        assertEquals(result.getPrincipalName(), "Anonymous");
        assertNotNull(result.getVersion());
        Assertions.assertThat(result.getCreated()).isAfterOrEqualTo(now);
    }

}
