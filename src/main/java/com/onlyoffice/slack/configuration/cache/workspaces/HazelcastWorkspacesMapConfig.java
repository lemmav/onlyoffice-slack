package com.onlyoffice.slack.configuration.cache.workspaces;

import com.hazelcast.config.*;
import com.onlyoffice.slack.configuration.general.IntegrationConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class HazelcastWorkspacesMapConfig {
    public final static String WORKSPACES = "workspaces";
    private final IntegrationConfiguration integrationConfiguration;

    @Bean
    public MapConfig workspacesMapConfig() {
        EvictionConfig evictionConfig = new EvictionConfig()
                .setSize(integrationConfiguration.getWorkspaceSizeMb())
                .setMaxSizePolicy(MaxSizePolicy.USED_HEAP_SIZE)
                .setEvictionPolicy(EvictionPolicy.LFU);

        MapConfig mapConfig = new MapConfig(WORKSPACES)
                .setName("SLACK-WORKSPACES-CONFIG")
                .setTimeToLiveSeconds(60 * 60 * 3)
                .setMaxIdleSeconds(60 * 60)
                .setAsyncBackupCount(1)
                .setBackupCount(1)
                .setReadBackupData(false)
                .setEvictionConfig(evictionConfig)
                .setMetadataPolicy(MetadataPolicy.CREATE_ON_UPDATE);
        mapConfig.getMapStoreConfig().setInitialLoadMode(MapStoreConfig.InitialLoadMode.EAGER);
        return mapConfig;
    }
}
