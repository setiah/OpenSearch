/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.inbuiltsecurity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.bootstrap.BootstrapCheck;
import org.opensearch.client.Client;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.metadata.IndexTemplateMetadata;
import org.opensearch.cluster.node.DiscoveryNodeRole;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.component.LifecycleComponent;
import org.opensearch.common.inject.Module;
import org.opensearch.common.io.stream.NamedWriteable;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.SettingUpgrader;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.env.Environment;
import org.opensearch.env.NodeEnvironment;
import org.opensearch.index.IndexModule;
import org.opensearch.index.shard.IndexSettingProvider;
import org.opensearch.plugins.ExtensiblePlugin;
import org.opensearch.plugins.Plugin;
import org.opensearch.repositories.RepositoriesService;
import org.opensearch.script.ScriptService;
import org.opensearch.inbuiltsecurity.spi.SecurityModule;
import org.opensearch.threadpool.ExecutorBuilder;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.watcher.ResourceWatcherService;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Depends on SPI implementation.
 * It uses ServiceLoader to load the desired implementation at runtime.
 *
 * CHALLENGES
 * 1.
 *
 * NOTES
 * 1. impl classes load ~~before~~ after this class as SPI does lazy loading.
 *
 */
public class SecurityProvider extends Plugin implements ExtensiblePlugin {

//    private final ServiceLoader<SecurityModule> loader;
    private SecurityModule securityModuleImpl = null;
    private static final Logger logger = LogManager.getLogger(SecurityProvider.class);

    public SecurityProvider() {
        logger.info("Initializing SecurityProvider");
//        loader = ServiceLoader.load(SecurityModule.class);
//        Iterator<SecurityModule> securityModuleIterator = loader.iterator();
//        while (securityModuleIterator.hasNext()) {
//            SecurityModule sm = securityModuleIterator.next();
//            logger.info("Detected SecurityModule implementation - " + sm.getClass().getName());
//            if(null == securityModuleImpl || !sm.getClass().getName().equals(DefaultSecurityModuleImpl.class.getName())) {
//                securityModuleImpl = sm;
//            }
//        }
    }

    @Override
    public void loadExtensions(ExtensionLoader loader) {
        for (SecurityModule extension: loader.loadExtensions(SecurityModule.class)) {
            logger.info("Reading SecurityModule extension " + extension.getClass().getSimpleName());

            /**
             * Hacks for overriding default extension with OpenSearchSecurityPlugin if available
             */
            if(null == securityModuleImpl) {
                securityModuleImpl = extension;
                logger.info("Loading SecurityModule with " + extension.getClass().getSimpleName());
            }

            if(!extension.getClass().getSimpleName().equals("DefaultSecurityModuleImpl")) {
                securityModuleImpl = extension;
                logger.info("Overriding SecurityModule with " + extension.getClass().getSimpleName());
            }

            if(extension.getClass().getSimpleName().equals("OpenSearchSecurityPlugin")) {
                securityModuleImpl = extension;
                logger.info("Overriding SecurityModule with " + extension.getClass().getSimpleName());
            }
        }
    }

    //TODO - check
    @Override
    protected Optional<String> getFeature() {
        return Optional.empty();
    }

    /**
     * Node level guice modules.
     */
    @Override
    public Collection<Module> createGuiceModules() {
        return securityModuleImpl.createGuiceModules();
    }

    /**
     * Node level services that will be automatically started/stopped/closed. This classes must be constructed
     * by injection with guice.
     */
    @Override
    public Collection<Class<? extends LifecycleComponent>> getGuiceServiceClasses() {
        return securityModuleImpl.getGuiceServiceClasses();
    }

    /**
     * Returns components added by this plugin.
     *
     * Any components returned that implement {@link LifecycleComponent} will have their lifecycle managed.
     * Note: To aid in the migration away from guice, all objects returned as components will be bound in guice
     * to themselves.
     *
     * @param client A client to make requests to the system
     * @param clusterService A service to allow watching and updating cluster state
     * @param threadPool A service to allow retrieving an executor to run an async action
     * @param resourceWatcherService A service to watch for changes to node local files
     * @param scriptService A service to allow running scripts on the local node
     * @param xContentRegistry the registry for extensible xContent parsing
     * @param environment the environment for path and setting configurations
     * @param nodeEnvironment the node environment used coordinate access to the data paths
     * @param namedWriteableRegistry the registry for {@link NamedWriteable} object parsing
     * @param indexNameExpressionResolver A service that resolves expression to index and alias names
     * @param repositoriesServiceSupplier A supplier for the service that manages snapshot repositories; will return null when this method
     *                                   is called, but will return the repositories service once the node is initialized.
     */
    @Override
    public Collection<Object> createComponents(Client client, ClusterService clusterService, ThreadPool threadPool,
                                               ResourceWatcherService resourceWatcherService, ScriptService scriptService,
                                               NamedXContentRegistry xContentRegistry, Environment environment,
                                               NodeEnvironment nodeEnvironment, NamedWriteableRegistry namedWriteableRegistry,
                                               IndexNameExpressionResolver indexNameExpressionResolver,
                                               Supplier<RepositoriesService> repositoriesServiceSupplier) {

        return securityModuleImpl.createComponents(client, clusterService, threadPool, resourceWatcherService, scriptService, xContentRegistry,
            environment, nodeEnvironment, namedWriteableRegistry, indexNameExpressionResolver, repositoriesServiceSupplier);
    }

    /**
     * Additional node settings loaded by the plugin. Note that settings that are explicit in the nodes settings can't be
     * overwritten with the additional settings. These settings added if they don't exist.
     */
    @Override
    public Settings additionalSettings() {
        return securityModuleImpl.additionalSettings();
    }

    /**
     * Returns parsers for {@link NamedWriteable} this plugin will use over the transport protocol.
     * @see NamedWriteableRegistry
     */
    @Override
    public List<NamedWriteableRegistry.Entry> getNamedWriteables() {
        return securityModuleImpl.getNamedWriteables();
    }

    /**
     * Returns parsers for named objects this plugin will parse from {@link XContentParser#namedObject(Class, String, Object)}.
     * @see NamedWriteableRegistry
     */
    @Override
    public List<NamedXContentRegistry.Entry> getNamedXContent() {
        return securityModuleImpl.getNamedXContent();
    }

    /**
     * Called before a new index is created on a node. The given module can be used to register index-level
     * extensions.
     */
    @Override
    public void onIndexModule(IndexModule indexModule) {
        securityModuleImpl.onIndexModule(indexModule);
    }

    /**
     * Returns a list of additional {@link Setting} definitions for this plugin.
     */
    @Override
    public List<Setting<?>> getSettings() {
        return securityModuleImpl.getSettings();
    }

    /**
     * Returns a list of additional settings filter for this plugin
     */
    @Override
    public List<String> getSettingsFilter() {
        return securityModuleImpl.getSettingsFilter();
    }

    /**
     * Get the setting upgraders provided by this plugin.
     *
     * @return the settings upgraders
     */
    @Override
    public List<SettingUpgrader<?>> getSettingUpgraders() {
        return securityModuleImpl.getSettingUpgraders();
    }

    /**
     * Provides a function to modify index template meta data on startup.
     * <p>
     * Plugins should return the input template map via {@link UnaryOperator#identity()} if no upgrade is required.
     * <p>
     * The order of the template upgrader calls is undefined and can change between runs so, it is expected that
     * plugins will modify only templates owned by them to avoid conflicts.
     * <p>
     * @return Never {@code null}. The same or upgraded {@code IndexTemplateMetadata} map.
     * @throws IllegalStateException if the node should not start because at least one {@code IndexTemplateMetadata}
     *                               cannot be upgraded
     */
    @Override
    public UnaryOperator<Map<String, IndexTemplateMetadata>> getIndexTemplateMetadataUpgrader() {
        return securityModuleImpl.getIndexTemplateMetadataUpgrader();
    }

    /**
     * Provides the list of this plugin's custom thread pools, empty if
     * none.
     *
     * @param settings the current settings
     * @return executors builders for this plugin's custom thread pools
     */
    @Override
    public List<ExecutorBuilder<?>> getExecutorBuilders(Settings settings) {
        return securityModuleImpl.getExecutorBuilders(settings);
    }

    /**
     * Returns a list of checks that are enforced when a node starts up once a node has the transport protocol bound to a non-loopback
     * interface. In this case we assume the node is running in production and all bootstrap checks must pass. This allows plugins
     * to provide a better out of the box experience by pre-configuring otherwise (in production) mandatory settings or to enforce certain
     * configurations like OS settings or 3rd party resources.
     */
    @Override
    public List<BootstrapCheck> getBootstrapChecks() {
        return securityModuleImpl.getBootstrapChecks();
    }

    @Override
    public Set<DiscoveryNodeRole> getRoles() {
        return securityModuleImpl.getRoles();
    }

    /**
     * Close the resources opened by this plugin.
     *
     * @throws IOException if the plugin failed to close its resources
     */
    @Override
    public void close() throws IOException {
        securityModuleImpl.close();
    }

    /**
     * An {@link IndexSettingProvider} allows hooking in to parts of an index
     * lifecycle to provide explicit default settings for newly created indices. Rather than changing
     * the default values for an index-level setting, these act as though the setting has been set
     * explicitly, but still allow the setting to be overridden by a template or creation request body.
     */
    @Override
    public Collection<IndexSettingProvider> getAdditionalIndexSettingProviders() {
        return securityModuleImpl.getAdditionalIndexSettingProviders();
    }
}
