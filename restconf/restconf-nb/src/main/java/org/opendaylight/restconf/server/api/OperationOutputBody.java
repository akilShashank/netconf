/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.restconf.server.api;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.IOException;
import java.io.OutputStream;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.restconf.api.FormatParameters;
import org.opendaylight.restconf.api.FormattableBody;
import org.opendaylight.restconf.nb.rfc8040.jersey.providers.ParameterAwareNormalizedNodeWriter;
import org.opendaylight.restconf.nb.rfc8040.jersey.providers.api.RestconfNormalizedNodeWriter;
import org.opendaylight.restconf.server.api.DatabindPath.OperationPath;
import org.opendaylight.restconf.server.spi.FormattableBodySupport;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XMLStreamNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;

/**
 * A {@link FormattableBody} corresponding to a {@code rpc} or {@code action} invocation.
 */
@NonNullByDefault
public final class OperationOutputBody extends FormattableBody {
    private final OperationPath path;
    private final ContainerNode output;

    public OperationOutputBody(final FormatParameters format, final OperationPath path, final ContainerNode output) {
        super(format);
        this.path = requireNonNull(path);
        this.output = requireNonNull(output);
        if (output.isEmpty()) {
            throw new IllegalArgumentException("output may not be empty");
        }
    }

    @VisibleForTesting
    public ContainerNode output() {
        return output;
    }

    @Override
    protected void formatToJSON(final OutputStream out, final FormatParameters format) throws IOException {
        final var stack = prepareStack();

        // RpcDefinition/ActionDefinition is not supported as initial codec in JSONStreamWriter, so we need to emit
        // initial output declaration
        try (var jsonWriter = FormattableBodySupport.createJsonWriter(out, format)) {
            final var module = stack.currentModule();
            jsonWriter.beginObject().name(module.argument().getLocalName() + ":output").beginObject();

            final var nnWriter = ParameterAwareNormalizedNodeWriter.forStreamWriter(
                JSONNormalizedNodeStreamWriter.createNestedWriter(path.databind().jsonCodecs(), stack.toInference(),
                    module.namespace().argument(), jsonWriter), null, null);
            for (var child : output.body()) {
                nnWriter.write(child);
            }
            nnWriter.flush();

            jsonWriter.endObject().endObject();
        }
    }

    @Override
    protected void formatToXML(final OutputStream out, final FormatParameters format) throws IOException {
        final var stack = prepareStack();

        // RpcDefinition/ActionDefinition is not supported as initial codec in XMLStreamWriter, so we need to emit
        // initial output declaration.
        final var xmlWriter = FormattableBodySupport.createXmlWriter(out, format);
        final var nnWriter = ParameterAwareNormalizedNodeWriter.forStreamWriter(
            XMLStreamNormalizedNodeStreamWriter.create(xmlWriter, stack.toInference()), null, null);

        writeElements(xmlWriter, nnWriter, output);
        nnWriter.flush();
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return super.addToStringAttributes(helper.add("path", path).add("output", output.prettyTree()));
    }

    private SchemaInferenceStack prepareStack() {
        final var stack = path.inference().toSchemaInferenceStack();
        stack.enterSchemaTree(path.outputStatement().argument());
        return stack;
    }

    private static void writeElements(final XMLStreamWriter xmlWriter, final RestconfNormalizedNodeWriter nnWriter,
            final ContainerNode data) throws IOException {
        final QName nodeType = data.name().getNodeType();
        final String namespace = nodeType.getNamespace().toString();
        try {
            xmlWriter.writeStartElement(XMLConstants.DEFAULT_NS_PREFIX, nodeType.getLocalName(), namespace);
            xmlWriter.writeDefaultNamespace(namespace);
            for (var child : data.body()) {
                nnWriter.write(child);
            }
            nnWriter.flush();
            xmlWriter.writeEndElement();
            xmlWriter.flush();
        } catch (final XMLStreamException e) {
            throw new IOException("Failed to write elements", e);
        }
    }
}
