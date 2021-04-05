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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MetaExporterTest {

    @Test
    public void testMetaExporter() throws IOException {
        SpyingExporter exporter = new SpyingExporter();
        NoAuthPrincipal principal = new NoAuthPrincipal();
        PrincipalProvider principalProvider = mock(PrincipalProvider.class);
        when(principalProvider.get()).thenReturn(principal);

        MetaExporter metaEx = new MetaExporter(principalProvider, exporter);
        String cdnKey = "test-cdn";
        Path path = Paths.get("/meta.json");

        metaEx.exportTo(path, cdnKey);

        Meta result = (Meta) exporter.lastExports.get(0);
        assertEquals(result.getCdnLabel(), cdnKey);
        assertEquals(result.getPrincipalName(), "Anonymous");
        assertNotNull(result.getVersion());
        assertNotNull(result.getCreated());
    }

    //TODO
//    private String expectedJson(String nowString) {
//        return "{\"version\":\"0.1.0\",\"created\":\"" + nowString +
//            "\",\"principalName\":\"myUsername\"," +
//            "\"webAppPrefix\":\"webapp_prefix\"," +
//            "\"cdnLabel\":\"test-cdn\"}";
//    }

}