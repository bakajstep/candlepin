/**
 * Copyright (c) 2009 - 2020 Red Hat, Inc.
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
package org.candlepin.service.impl;

import org.candlepin.service.CloudRegistrationAdapter;
import org.candlepin.service.model.CloudRegistrationInfo;
import org.candlepin.service.exception.CloudRegistrationAuthorizationException;
import org.candlepin.service.exception.MalformedCloudRegistrationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The custom implementation of the {@link CloudRegistrationAdapter}.
 *
 * This implementation always returns organization id "snowwhite"
 */
public class CustomCloudRegistrationAdapter implements CloudRegistrationAdapter {

    private static Logger log = LoggerFactory.getLogger(CustomCloudRegistrationAdapter.class);

    @Override
    public String resolveCloudRegistrationData(CloudRegistrationInfo cloudRegInfo)
        throws CloudRegistrationAuthorizationException, MalformedCloudRegistrationException {

        if (cloudRegInfo == null) {
            throw new MalformedCloudRegistrationException("No cloud registration information provided");
        } else {
            String cloudType = cloudRegInfo.getType();
            log.debug("Cloud registration information provided");

            if (cloudType == null) {
                throw new MalformedCloudRegistrationException("No cloud type specified");
            } else {
                log.debug("Cloud type: {}", cloudType);
            }
        }

        String metadata = cloudRegInfo.getMetadata();
        if (metadata == null) {
            throw new MalformedCloudRegistrationException(
                "No metadata provided with the cloud registration info");
        } else {
            log.debug("Cloud metadata: {}", metadata);
        }

        String signature = cloudRegInfo.getSignature();
        if (signature != null) {
            log.debug("Signature of cloud metadata: {}", signature);
        }

        return "snowwhite";
    }
}