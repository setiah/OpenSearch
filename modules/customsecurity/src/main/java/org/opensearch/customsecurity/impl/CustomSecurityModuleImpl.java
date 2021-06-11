/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.customsecurity.impl;

import org.opensearch.plugins.ClusterPlugin;
import org.opensearch.plugins.MapperPlugin;
import org.opensearch.inbuiltsecurity.spi.SecurityModule;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CustomSecurityModuleImpl extends SecurityModule implements ClusterPlugin, MapperPlugin {
    private static final Logger logger = LogManager.getLogger(CustomSecurityModuleImpl.class);
    public CustomSecurityModuleImpl() {
        logger.info("Initializing CustomSecurityModuleImpl...");
    }
}
