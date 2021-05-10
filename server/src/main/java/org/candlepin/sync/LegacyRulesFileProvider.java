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

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LegacyRulesFileProvider {

    private static final String LEGACY_RULES_FILE = "/rules/default-rules.js";

    public Path get() throws ExportCreationException {
        try {
            return Paths.get(this.getClass().getResource(LEGACY_RULES_FILE).toURI());
        }
        catch (URISyntaxException e) {
            throw new ExportCreationException("Could not retrieve legacy rules", e);
        }
    }

}
