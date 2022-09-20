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

package org.candlepin.spec.bootstrap.data.builder;

import java.io.File;

public class Export {

    private final String consumerUuid;
    private final String consumerName;
    private final String cdnLabel;
    private final File file;

    public Export(String consumerUuid, String consumerName, String cdnLabel, File file) {
        this.consumerUuid = consumerUuid;
        this.consumerName = consumerName;
        this.cdnLabel = cdnLabel;
        this.file = file;
    }

    public String consumerUuid() {
        return consumerUuid;
    }

    public String consumerName() {
        return consumerName;
    }

    public String cdnLabel() {
        return cdnLabel;
    }

    public File file() {
        return file;
    }
}
