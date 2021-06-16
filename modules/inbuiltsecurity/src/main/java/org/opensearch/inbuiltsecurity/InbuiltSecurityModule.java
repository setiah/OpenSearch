/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.inbuiltsecurity;

import org.opensearch.plugins.OverridablePlugin;
import org.opensearch.plugins.Plugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InbuiltSecurityModule extends Plugin implements OverridablePlugin {

    private static final Logger logger = LogManager.getLogger(InbuiltSecurityModule.class);

    public InbuiltSecurityModule() {
        logger.info("Creating instance of InbuiltSecurityModule");
    }
}
