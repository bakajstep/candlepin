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
package org.candlepin.spec.bootstrap.client;

import org.candlepin.ApiClient;
import org.candlepin.ApiException;
import java.util.Collections;
import java.util.Map;

public class HostedTestResourceClient {

    ApiClient apiClient = null;

    public HostedTestResourceClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public Object isHostedTestResourceAlive() throws ApiException {
        okhttp3.Call call = this.apiClient.buildCall("https://localhost:8443/candlepin", "/hostedtest/alive", "GET",
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, null, Map.of("Accept", "text/plain"),
                Collections.EMPTY_MAP, Collections.EMPTY_MAP, new String[]{}, null);
        return this.apiClient.execute(call, String.class).getData();
    }
}
