/*
 * Copyright (c) 2009 - 2025 Red Hat, Inc.
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

package org.candlepin.resource;

import org.candlepin.dto.api.server.v1.ConsumerFeedDTO;
import org.candlepin.exceptions.BadRequestException;
import org.candlepin.model.Owner;
import org.candlepin.model.OwnerCurator;
import org.candlepin.model.RhsmApiCompatCurator;
import org.candlepin.paging.Page;
import org.candlepin.paging.PageRequest;
import org.candlepin.resource.server.v1.RhsmapiApi;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import org.jboss.resteasy.core.ResteasyContext;
import org.xnap.commons.i18n.I18n;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Entry point for endpoints that are dedicated for RHSM API for compatibility reasons.
 */
public class RhsmApiCompatResource implements RhsmapiApi {

    private I18n i18n;
    OwnerCurator ownerCurator;
    RhsmApiCompatCurator rhsmApiCurator;

    @Inject
    public RhsmApiCompatResource(I18n i18n, OwnerCurator ownerCurator, RhsmApiCompatCurator rhsmApiCurator) {
        this.i18n = i18n;
        this.ownerCurator = ownerCurator;
        this.rhsmApiCurator = Objects.requireNonNull(rhsmApiCurator);
    }

    @Override
    @Transactional
    public Stream<ConsumerFeedDTO> getConsumerFeed(String orgKey, String afterId, String afterUuid,
        OffsetDateTime afterCheckin, Integer page, Integer perPage) {

        if (orgKey == null || orgKey.trim().isEmpty()) {
            // TODO: We need to agree what we want to return 404 or empty list depends on what we use in
            // rest of the RHSM API replacement
            return Stream.empty();
        }

        Owner owner = this.ownerCurator.getByKey(orgKey);
        if (owner == null) {
            return Stream.empty();
        }

        //Paging bit
        int offset = 1;
        int limit = 1000;
        PageRequest pageRequest = ResteasyContext.getContextData(PageRequest.class);
        if (pageRequest != null) {
            Page<Stream<ConsumerFeedDTO>> pageResponse = new Page<>();
            pageResponse.setPageRequest(pageRequest);

            if (pageRequest.isPaging()) {
                offset = pageRequest.getPage();
                limit = pageRequest.getPerPage();
                if (offset < 1) {
                    throw new BadRequestException(i18n.tr("Parameter page must be positive integer"));
                }
                if (limit < 1 || limit > 1000) {
                    throw new BadRequestException(i18n.tr("Parameter per_page must be in range 1-1000"));
                }
            }

            // Store the page for the LinkHeaderResponseFilter
            ResteasyContext.pushContext(Page.class, pageResponse);
        }

        // TODO: rename orgKey to match codebase to owner
        rhsmApiCurator.getConsumerFeed(owner, afterId, afterUuid, afterCheckin, offset, limit);

        return Stream.empty();
    }
}
