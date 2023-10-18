/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.client.mdsal.api;

import com.google.common.annotations.Beta;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.netconf.client.mdsal.impl.BaseSchema;

@Beta
@NonNullByDefault
public interface BaseNetconfSchemas {

    BaseSchema baseSchema();

    BaseSchema baseSchemaWithNotifications();
}
