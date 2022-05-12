package com.onlyoffice.slack.configuration.cache;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.UUID;

//TODO: Prod configuration
@Configuration
@Slf4j
public class HazelcastCacheConfiguration {
    @Bean
    public HazelcastInstance hazelcastInstance(Config config) {
        return Hazelcast.newHazelcastInstance(config);
    }

    @Profile(value = {"prod", "production"})
    @Bean
    public Config createProductionConfig(
            List<MapConfig> mapConfigs,
            NetworkConfig networkConfig,
            PartitionGroupConfig groupConfig
    ) {
        log.info("Initializing a new production hazelcast configuration");
        Config config = new Config();
        config.setClusterName("ONLYOFFICE x Slack Cache");
        config.setInstanceName("ONLYOFFICE x Slack Cache - " + UUID.randomUUID());
        config.setPartitionGroupConfig(groupConfig);
        config.setNetworkConfig(networkConfig);
        config.setProperty("hazelcast.socket.server.bind.any", "false");
        config.setProperty("hazelcast.heartbeat.failuredetector.type", "deadline");
        config.setProperty("hazelcast.heartbeat.interval.seconds", "60");
        config.setProperty("hazelcast.max.no.heartbeat.seconds", "180");
        config.getJetConfig().setEnabled(false);
        mapConfigs.forEach(mapConfig -> config.addMapConfig(mapConfig));
        return config;
    }

    @Profile(value = {"dev", "development"})
    @Bean
    public Config createDevelopmentConfig(
            List<MapConfig> mapConfigs,
            NetworkConfig networkConfig,
            PartitionGroupConfig groupConfig
    ) {
        log.info("Initializing a new development hazelcast configuration");
        Config config = new Config();
        config.setClusterName("DEV Cache");
        config.setInstanceName("DEV Cache - " + UUID.randomUUID());
        config.setPartitionGroupConfig(groupConfig);
        config.setNetworkConfig(networkConfig);
        config.setProperty("hazelcast.health.monitoring.level","NOISY");
        config.setProperty("hazelcast.socket.server.bind.any", "false");
        config.setProperty("hazelcast.logging.type", "slf4j");
        config.setProperty("hazelcast.heartbeat.failuredetector.type", "deadline");
        config.setProperty("hazelcast.heartbeat.interval.seconds", "30");
        config.setProperty("hazelcast.max.no.heartbeat.seconds", "180");
        config.getJetConfig().setEnabled(false);
        mapConfigs.forEach(mapConfig -> config.addMapConfig(mapConfig));
        return config;
    }

    @Bean
    public PartitionGroupConfig partitionGroupConfig() {
        log.info("Initializing a new partitioning config");
        return new PartitionGroupConfig().setEnabled(true)
                .setGroupType(PartitionGroupConfig.MemberGroupType.PER_MEMBER);
    }

    @Profile(value = {"dev", "development"})
    @Bean
    public NetworkConfig developmentNetworkConfig() {
        log.info("Initializing a new development network config");
        JoinConfig joinConfig = new JoinConfig()
                .setTcpIpConfig(new TcpIpConfig()
                .setEnabled(true)
                .setConnectionTimeoutSeconds(30)
                .setMembers(List.of("127.0.0.1:5901")));

        return new NetworkConfig()
                .setJoin(joinConfig)
                .setPort(5901)
                .setPortAutoIncrement(true)
                .setReuseAddress(true);
    }

    @Profile(value = {"prod", "production"})
    @Bean
    public NetworkConfig productionNetworkConfig() {
        log.info("Initializing a new production network config");
        JoinConfig joinConfig = new JoinConfig()
                .setKubernetesConfig(
                        new KubernetesConfig()
                                .setEnabled(true)
                );
        joinConfig.getMulticastConfig().setEnabled(false);

        return new NetworkConfig()
                .setJoin(joinConfig)
                .setPort(5701)
                .setPortAutoIncrement(false);
    }
}
