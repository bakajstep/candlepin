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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class LegacyRulesFileProviderTest {

    @Test
    void couldNotReadLegacyRules() {
        final LegacyRulesFileProvider provider = new LegacyRulesFileProvider();

        assertThatThrownBy(provider::get)
            .isInstanceOf(ExportCreationException.class);
    }

    @Test
    void returnsPathToLegacyRules() throws ExportCreationException {
        final LegacyRulesFileProvider provider = new LegacyRulesFileProvider();

        assertThat(provider.get())
            .isNotEmptyFile();
    }

}