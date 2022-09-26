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

package org.candlepin.spec.imports;

import org.candlepin.spec.bootstrap.assertions.OnlyInStandalone;
import org.candlepin.spec.bootstrap.client.ApiClient;
import org.candlepin.spec.bootstrap.client.SpecTest;
import org.candlepin.spec.bootstrap.data.util.Importer;

import org.junit.jupiter.api.TestInstance;

/**
 * Async import test suite.
 * </p>
 * All tests are inherited from the sync suite. Any changes to the tests should
 * be done there. All this suite has to do is to redeclare importer as async.
 * Otherwise, same restrictions apply as for the sync tests.
 */
@SpecTest
@OnlyInStandalone
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncImportSuccessSpecTest extends ImportSuccessSpecTest {

    @Override
    protected Importer getImporter(ApiClient client) {
        return new Importer(client, true);
    }

}
