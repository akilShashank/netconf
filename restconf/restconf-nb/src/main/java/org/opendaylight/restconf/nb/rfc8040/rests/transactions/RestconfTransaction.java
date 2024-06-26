/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.restconf.nb.rfc8040.rests.transactions;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.fromInstanceId;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.odlparent.logging.markers.Markers;
import org.opendaylight.restconf.server.api.DatabindContext;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A handle to a set of operations being executed atomically on top of some backing store.
 */
// FIXME: it seems the first two operations deal with lifecycle of a transaction, while others invoke various
//        operations. This should be handled through proper allocation indirection.
abstract class RestconfTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(RestconfTransaction.class);

    final @NonNull DatabindContext databind;

    RestconfTransaction(final DatabindContext databind) {
        this.databind = requireNonNull(databind);
    }

    /**
     * Rollback changes and unlock the datastore.
     */
    // FIXME: this looks synchronous, but it should not be
    abstract void cancel();

    /**
     * Confirm previous operations.
     *
     * @return a FluentFuture containing the result of the commit information
     */
    abstract ListenableFuture<? extends @NonNull CommitInfo> commit();

    /**
     * Delete data from the datastore.
     *
     * @param path the data object path
     */
    final void delete(final YangInstanceIdentifier path) {
        LOG.trace("Delete {}", path);
        deleteImpl(requireNonNull(path));
    }

    abstract void deleteImpl(@NonNull YangInstanceIdentifier path);

    /**
     * Remove data from the datastore.
     *
     * @param path the data object path
     */
    final void remove(final YangInstanceIdentifier path) {
        LOG.trace("Remove {}", path);
        removeImpl(requireNonNull(path));
    }

    abstract void removeImpl(@NonNull YangInstanceIdentifier path);

    /**
     * Merges a piece of data with the existing data at a specified path.
     *
     * @param path the data object path
     * @param data the data object to be merged to the specified path
     */
    final void merge(final YangInstanceIdentifier path, final NormalizedNode data) {
        LOG.trace("Merge {}", path);
        LOG.trace(Markers.confidential(), "Merge with {}", data.prettyTree());
        mergeImpl(requireNonNull(path), data);
    }

    abstract void mergeImpl(@NonNull YangInstanceIdentifier path, @NonNull NormalizedNode data);

    /**
     * Stores a piece of data at the specified path.
     *
     * @param path    the data object path
     * @param data    the data object to be merged to the specified path
     */
    final void create(final YangInstanceIdentifier path, final NormalizedNode data) {
        LOG.trace("Create {}", path);
        LOG.trace(Markers.confidential(), "Create as {}", data.prettyTree());
        createImpl(requireNonNull(path), data);
    }

    abstract void createImpl(@NonNull YangInstanceIdentifier path, @NonNull NormalizedNode data);

    /**
     * Replace a piece of data at the specified path.
     *
     * @param path    the data object path
     * @param data    the data object to be merged to the specified path
     */
    final void replace(final YangInstanceIdentifier path, final NormalizedNode data) {
        LOG.trace("Replace {}", path);
        LOG.trace(Markers.confidential(), "Replace with {}", data.prettyTree());
        replaceImpl(requireNonNull(path), data);
    }

    abstract void replaceImpl(@NonNull YangInstanceIdentifier path, @NonNull NormalizedNode data);

    abstract @Nullable NormalizedNodeContainer<?> readList(@NonNull YangInstanceIdentifier path);

    abstract ListenableFuture<Optional<NormalizedNode>> read(YangInstanceIdentifier path);

    /**
     * Merge parents of data.
     *
     * @param path    path of data
     */
    // FIXME: this method should only be invoked in MdsalRestconfStrategy, and even then only if we are crossing
    //        an implicit list.
    final void ensureParentsByMerge(final YangInstanceIdentifier path) {
        final var normalizedPathWithoutChildArgs = new ArrayList<PathArgument>();
        YangInstanceIdentifier rootNormalizedPath = null;

        final var it = path.getPathArguments().iterator();

        while (it.hasNext()) {
            final var pathArgument = it.next();
            if (rootNormalizedPath == null) {
                rootNormalizedPath = YangInstanceIdentifier.of(pathArgument);
            }

            if (it.hasNext()) {
                normalizedPathWithoutChildArgs.add(pathArgument);
            }
        }

        if (normalizedPathWithoutChildArgs.isEmpty()) {
            return;
        }

        merge(rootNormalizedPath,
            fromInstanceId(databind.modelContext(), YangInstanceIdentifier.of(normalizedPathWithoutChildArgs)));
    }
}
