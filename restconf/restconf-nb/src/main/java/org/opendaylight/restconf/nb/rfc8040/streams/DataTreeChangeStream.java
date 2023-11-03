/*
 * Copyright (c) 2014, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.restconf.nb.rfc8040.streams;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects.ToStringHelper;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.restconf.nb.rfc8040.databind.DatabindProvider;
import org.opendaylight.yang.gen.v1.urn.sal.restconf.event.subscription.rev231103.NotificationOutputTypeGrouping.NotificationOutputType;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link RestconfStream} reporting changes on a particular data tree.
 */
public class DataTreeChangeStream extends RestconfStream<List<DataTreeCandidate>>
        implements ClusteredDOMDataTreeChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(DataTreeChangeStream.class);

    private final DatabindProvider databindProvider;
    private final @NonNull LogicalDatastoreType datastore;
    private final @NonNull YangInstanceIdentifier path;

    DataTreeChangeStream(final ListenersBroker listenersBroker, final String name,
            final NotificationOutputType outputType, final DatabindProvider databindProvider,
            final LogicalDatastoreType datastore, final YangInstanceIdentifier path) {
        super(listenersBroker, name, outputType, switch (outputType) {
            case JSON -> JSONDataTreeCandidateFormatter.FACTORY;
            case XML -> XMLDataTreeCandidateFormatter.FACTORY;
        });
        this.databindProvider = requireNonNull(databindProvider);
        this.datastore = requireNonNull(datastore);
        this.path = requireNonNull(path);
    }

    @Override
    public void onInitialData() {
        // No-op
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void onDataTreeChanged(final List<DataTreeCandidate> dataTreeCandidates) {
        final var now = Instant.now();
        final String data;
        try {
            data = formatter().eventData(databindProvider.currentContext().modelContext(), dataTreeCandidates, now);
        } catch (final Exception e) {
            LOG.error("Failed to process notification {}",
                    dataTreeCandidates.stream().map(Object::toString).collect(Collectors.joining(",")), e);
            return;
        }
        if (data != null) {
            post(data);
        }
    }

    /**
     * Get path pointed to data in data store.
     *
     * @return Path pointed to data in data store.
     */
    public YangInstanceIdentifier getPath() {
        return path;
    }

    /**
     * Register data change listener in DOM data broker and set it to listener on stream.
     *
     * @param domDataBroker data broker for register data change listener
     */
    public final synchronized void listen(final DOMDataBroker domDataBroker) {
        if (!isListening()) {
            final var changeService = domDataBroker.getExtensions().getInstance(DOMDataTreeChangeService.class);
            if (changeService == null) {
                throw new UnsupportedOperationException("DOMDataBroker does not support the DOMDataTreeChangeService");
            }

            setRegistration(changeService.registerDataTreeChangeListener(
                new DOMDataTreeIdentifier(datastore, path), this));
        }
    }

    @Override
    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return super.addToStringAttributes(helper.add("path", path));
    }
}