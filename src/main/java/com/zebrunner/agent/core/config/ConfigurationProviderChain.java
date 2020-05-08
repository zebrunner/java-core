package com.zebrunner.agent.core.config;

import com.zebrunner.agent.core.exception.TestAgentException;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;

@Slf4j
public class ConfigurationProviderChain implements ConfigurationProvider {

    private final List<ConfigurationProvider> providers = new LinkedList<>();

    public ConfigurationProviderChain(List<? extends ConfigurationProvider> credentialsProviders) {
        if (credentialsProviders == null || credentialsProviders.size() == 0) {
            throw new IllegalArgumentException("No credential providers specified");
        }
        this.providers.addAll(credentialsProviders);
    }

    @Override
    public ReportingConfiguration getConfiguration() {
        ReportingConfiguration config = ReportingConfiguration.builder()
                                                              .server(new ReportingConfiguration.ServerConfiguration())
                                                              .rerun(new ReportingConfiguration.RerunConfiguration())
                                                              .build();
        assembleConfiguration(config);
        if (areMandatoryArgsSet(config)) {
            return config;
        } else {
            throw new TestAgentException("Mandatory agent properties are missing - double-check agent configuration");
        }
    }

    /**
     * Iterates over all configuration providers and assembles agent configuration. Configuration property
     * supplied by provider with highest priority always takes precedence.
     *
     * @param config configuration to be assembled
     */
    private void assembleConfiguration(ReportingConfiguration config) {
        for (ConfigurationProvider provider : providers) {
            try {
                ReportingConfiguration providedConfig = provider.getConfiguration();
                normalize(providedConfig);
                merge(config, providedConfig);
                // no need to iterate further to provider with lower priority if all args are provided already
                if (areAllArgsSet(providedConfig)) {
                    break;
                }
            } catch (TestAgentException e) {
                log.warn(e.getMessage());
            }
        }
    }

    private static void normalize(ReportingConfiguration config) {
        normalizeServerConfiguration(config);
        normalizeRerunConfiguration(config);
    }

    private static void normalizeServerConfiguration(ReportingConfiguration config) {
        if (config.getServer() == null) {
            config.setServer(new ReportingConfiguration.ServerConfiguration());
        } else {
            ReportingConfiguration.ServerConfiguration serverConfig = config.getServer();
            String hostname = serverConfig.getHostname();
            String accessToken = serverConfig.getHostname();
            if (hostname != null && accessToken.isBlank()) {
                serverConfig.setHostname(null);
            }
            if (accessToken != null && accessToken.isBlank()) {
                serverConfig.setAccessToken(null);
            }
        }
    }

    private static void normalizeRerunConfiguration(ReportingConfiguration config) {
        if (config.getRerun() == null) {
            config.setRerun(new ReportingConfiguration.RerunConfiguration());
        } else {
            ReportingConfiguration.RerunConfiguration rerunConfig = config.getRerun();
            String runId = rerunConfig.getRunId();
            if (runId != null && runId.isBlank()) {
                rerunConfig.setRunId(null);
            }
        }
    }

    /**
     * Sets values coming from provided configuration that were not set previously by providers with higher priority
     *
     * @param config configuration assembled by previous configuration providers
     * @param providedConfig configuration from current configuration provider
     */
    private static void merge(ReportingConfiguration config, ReportingConfiguration providedConfig) {
        if (config.getEnabled() == null) {
            config.setEnabled(providedConfig.getEnabled());
        }

        ReportingConfiguration.ServerConfiguration server = config.getServer();
        if (server.getHostname() == null) {
            server.setHostname(providedConfig.getServer().getHostname());
        }
        if (server.getAccessToken() == null) {
            server.setAccessToken(providedConfig.getServer().getAccessToken());
        }

        ReportingConfiguration.RerunConfiguration rerun = config.getRerun();
        if (rerun.getRunId() == null) {
            rerun.setRunId(providedConfig.getRerun().getRunId());
        }
    }

    private static boolean areMandatoryArgsSet(ReportingConfiguration config) {
        ReportingConfiguration.ServerConfiguration server = config.getServer();

        // no need to check anything if reporting is disabled
        return !config.isEnabled() || (server.getHostname() != null && server.getAccessToken() != null);
    }

    private static boolean areAllArgsSet(ReportingConfiguration config) {
        Boolean enabled = config.getEnabled();
        String hostname = config.getServer().getHostname();
        String accessToken = config.getServer().getAccessToken();
        String runId = config.getRerun().getRunId();

        boolean allArgsExist = enabled != null && hostname != null && accessToken != null && runId != null;

        return !config.isEnabled() || allArgsExist;
    }

}