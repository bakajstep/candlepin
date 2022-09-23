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

package org.candlepin.spec.bootstrap.assertions;

import static org.assertj.core.api.Assertions.assertThat;

import org.candlepin.dto.api.client.v1.ConsumerDTO;
import org.candlepin.dto.api.client.v1.UpstreamConsumerDTO;

import java.util.function.Supplier;

public final class ConsumerAssert {

    private ConsumerAssert() {
        throw new UnsupportedOperationException();
    }

    public static void assertSameUuid(Supplier<String> uuid, Supplier<String> other) {
        assertThat(uuid).isNotNull();
        assertThat(other).isNotNull();
        assertSameUuid(uuid.get(), other.get());
    }

    public static void assertSameUuid(String uuid, String other) {
        assertThat(uuid).isNotNull();
        assertThat(other).isNotNull();
        assertThat(uuid).isEqualTo(other);
    }

}
