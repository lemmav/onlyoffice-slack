package com.onlyoffice.slack.configuration.cache.bot;

import com.hazelcast.config.*;
import com.onlyoffice.slack.configuration.general.IntegrationConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class HazelcastBotUsersMapConfig {
    public final static String BOTS = "bots";
    private final IntegrationConfiguration integrationConfiguration;

    @Bean
    public MapConfig botsMapConfig() {
        EvictionConfig evictionConfig = new EvictionConfig()
                .setSize(integrationConfiguration.getBotsSizeMb())
                .setMaxSizePolicy(MaxSizePolicy.USED_HEAP_SIZE)
                .setEvictionPolicy(EvictionPolicy.LFU);

        MapConfig mapConfig = new MapConfig(BOTS)
                .setName("SLACK-BOTS-CONFIG")
                .setTimeToLiveSeconds(60 * 60 * 2)
                .setMaxIdleSeconds(60 * 30)
                .setAsyncBackupCount(1)
                .setBackupCount(1)
                .setReadBackupData(false)
                .setEvictionConfig(evictionConfig)
                .setMetadataPolicy(MetadataPolicy.CREATE_ON_UPDATE);
        mapConfig.getMapStoreConfig().setInitialLoadMode(MapStoreConfig.InitialLoadMode.EAGER);
        return mapConfig;
    }
}
