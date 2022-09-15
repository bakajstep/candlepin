/**
 * Copyright (c) 2009 - 2016 Red Hat, Inc.
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
package org.candlepin.resteasy.interceptors;

import org.candlepin.exceptions.BadRequestException;
import org.candlepin.model.AbstractHibernateObject;
import org.candlepin.paging.Page;
import org.candlepin.paging.PageRequest;

import com.google.inject.Inject;
import com.google.inject.Provider;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.jboss.resteasy.core.ResteasyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;



/**
 * The PagedStreamInterceptor handles the paging of entity streams, in the clunkiest, most inefficient
 * way possible.
 */
@javax.ws.rs.ext.Provider
public class PagedStreamInterceptor implements ContainerResponseFilter {
    private static final Logger log = LoggerFactory.getLogger(PagedStreamInterceptor.class);

    public PagedStreamInterceptor() {
        // Intentionally left empty
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        PageRequest pageRequest = ResteasyContext.getContextData(PageRequest.class);

        if (pageRequest == null) {
            // No page request, do nothing
            log.trace("no page request in context data; skipping stream paging");
            return;
        }

        Type type = responseContext.getEntityType();
        if (!(type instanceof ParameterizedType)) {
            // If it's not a parameterized type, we won't be able to fetch a method on the underlying
            // class without a great deal of pain and inconsistency (on-the-fly reflection)
            log.trace("response type isn't parameterized; cannot sort or page results: {}", type);
            return;
        }

        ParameterizedType ptype = (ParameterizedType) type;
        Type[] etypes = ptype.getActualTypeArguments();

        if (!Stream.class.isAssignableFrom((Class) ptype.getRawType()) || etypes == null || etypes.length < 1) {
            log.trace("response base type isn't a stream, or doesn't have any parameterized types: {}, {}, {}",
                ptype, etypes, etypes.length);
            return;
        }

        Class elementClass = (Class) etypes[0];

        // Impl note:
        // Sorting will always be required (for consistency) if a page request object is
        // present -- either isPaging() will be true, or we'll have ordering config.
        Stream output = this.applyOrdering(pageRequest, elementClass, (Stream) responseContext.getEntity());

        // Apply paging, if necessary
        output = this.applyPaging(pageRequest, output);

        // Set the response output
        responseContext.setEntity(output);
    }

    private Stream applyOrdering(PageRequest pageRequest, Class elementClass, Stream stream) {
        String sortField = pageRequest.getSortBy() != null ?
            pageRequest.getSortBy() :
            AbstractHibernateObject.DEFAULT_SORT_FIELD;

        PageRequest.Order order = pageRequest.getOrder() != null ?
            pageRequest.getOrder() :
            PageRequest.DEFAULT_ORDER;

        String keyMethodName = new StringBuilder("get")
            .append(sortField.substring(0, 1).toUpperCase())
            .append(sortField.substring(1))
            .toString();

        try {
            Method keyMethod = elementClass.getMethod(keyMethodName);
            Class keyRetType = keyMethod.getReturnType();

            if (keyRetType == null || !(keyRetType.isPrimitive() || Comparable.class.isAssignableFrom(keyRetType))) {
                throw new BadRequestException("cannot sort on non-comparable field: " + sortField + ", " + keyRetType);
            }

            Comparator comparator = Comparator.comparing(obj -> {
                try {
                    return (Comparable) keyMethod.invoke(obj);
                }
                catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });

            if (order == PageRequest.Order.DESCENDING) {
                comparator = comparator.reversed();
            }

            return stream.sorted(comparator);
        }
        catch (NoSuchMethodException ex) {
            // TODO: Translate this
            throw new BadRequestException("cannot sort on non-existent field: " + sortField);
        }
    }

    private Stream applyPaging(PageRequest pageRequest, Stream<Object> stream) {
        if (!pageRequest.isPaging()) {
            // If we're not actually paging, do nothing
            return stream;
        }

        // This is pain -- we have to collect the page so we can have an accurate
        // count for the page headers.
        List<Object> elements = stream.skip((pageRequest.getPage() - 1) * pageRequest.getPerPage())
            .limit(pageRequest.getPerPage())
            .collect(Collectors.toList());

        // Create a page object for the link header response
        Page page = new Page();
        page.setMaxRecords(elements.size());
        page.setPageRequest(pageRequest);
        // Note: we don't need to store the page data in the page

        ResteasyContext.pushContext(Page.class, page);

        // Return our butchered stream
        return elements.stream();
    }

}
