/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.restconf.server.api;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Result of a {@code POST} request as defined in
 * <a href="https://www.rfc-editor.org/rfc/rfc8040#section-4.4">RFC8040 section 4.4</a>.
 */
@NonNullByDefault
public sealed interface DataPostResult {
    /**
     * Result of a {@code POST} request as defined in
     * <a href="https://www.rfc-editor.org/rfc/rfc8040#section-4.4.1">RFC8040 Create ResourceMode</a>.
     *
     * @param createdPath API path of the newly-created resource
     */
    // FIXME: use ApiPath instead of String
    record CreateResource(String createdPath) implements DataPostResult {
        public CreateResource {
            requireNonNull(createdPath);
        }
    }
}
