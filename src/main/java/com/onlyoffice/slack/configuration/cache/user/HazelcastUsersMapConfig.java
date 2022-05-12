package com.onlyoffice.slack.configuration.cache.user;

import com.hazelcast.config.*;
import com.onlyoffice.slack.configuration.general.IntegrationConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class HazelcastUsersMapConfig {
    public final static String USERS = "users";
    private final IntegrationConfiguration integrationConfiguration;

    @Bean
    public MapConfig usersMapConfig() {
        EvictionConfig evictionConfig = new EvictionConfig()
                .setSize(integrationConfiguration.getUsersSizeMb())
                .setMaxSizePolicy(MaxSizePolicy.USED_HEAP_SIZE)
                .setEvictionPolicy(EvictionPolicy.LFU);

        MapConfig mapConfig = new MapConfig(USERS)
                .setName("SLACK-USERS-CONFIG")
                .setTimeToLiveSeconds(60 * 30)
                .setMaxIdleSeconds(60 * 15)
                .setAsyncBackupCount(1)
                .setBackupCount(1)
                .setReadBackupData(false)
                .setEvictionConfig(evictionConfig)
                .setMetadataPolicy(MetadataPolicy.CREATE_ON_UPDATE);
        mapConfig.getMapStoreConfig().setInitialLoadMode(MapStoreConfig.InitialLoadMode.EAGER);
        return mapConfig;
    }
}
