package com.onlyoffice.slack.shared.configuration.cache;

import com.hazelcast.config.*;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.onlyoffice.slack.shared.persistence.entity.id.InstallerUserId;
import com.onlyoffice.slack.shared.transfer.cache.DocumentSessionKey;
import com.onlyoffice.slack.shared.transfer.cache.EditorSession;
import com.slack.api.bolt.model.builtin.DefaultBot;
import com.slack.api.bolt.model.builtin.DefaultInstaller;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class HazelcastConfiguration {
  private static final String BOTS_MAP_KEY = "bots";
  private static final String STATES_MAP_KEY = "states";
  private static final String SESSIONS_MAP_KEY = "sessions";
  private static final String DOCUMENT_KEYS_MAP_KEY = "keys";
  private static final String INSTALLERS_MAP_KEY = "installers";

  private final HazelcastFileSessionMapStoreFactory mapStoreFactory;
  private final HazelcastInstance hazelcastInstance;

  @PostConstruct
  public void configureMapStore() {
    if (!hazelcastInstance.getConfig().getMapConfigs().containsKey(DOCUMENT_KEYS_MAP_KEY)) {
      var fileSessionsMapConfig = new MapConfig(DOCUMENT_KEYS_MAP_KEY);
      var fileSessionsEvictionConfig =
          new EvictionConfig()
              .setEvictionPolicy(EvictionPolicy.LRU)
              .setMaxSizePolicy(MaxSizePolicy.USED_HEAP_SIZE)
              .setSize(100);

      var mapStoreConfig = new MapStoreConfig();

      fileSessionsMapConfig.setEvictionConfig(fileSessionsEvictionConfig);

      mapStoreConfig.setEnabled(true);
      mapStoreConfig.setFactoryImplementation(mapStoreFactory);
      mapStoreConfig.setWriteDelaySeconds(0);
      mapStoreConfig.setWriteBatchSize(1);
      mapStoreConfig.setWriteCoalescing(true);

      fileSessionsMapConfig.setMapStoreConfig(mapStoreConfig);

      hazelcastInstance.getConfig().addMapConfig(fileSessionsMapConfig);
    }
  }

  @Bean
  public IMap<String, Instant> states(final HazelcastInstance instance) {
    return instance.getMap(STATES_MAP_KEY);
  }

  @Bean
  IMap<String, EditorSession> sessions(final HazelcastInstance instance) {
    return instance.getMap(SESSIONS_MAP_KEY);
  }

  @Bean
  public IMap<String, DocumentSessionKey> documentKeys(final HazelcastInstance instance) {
    return instance.getMap(DOCUMENT_KEYS_MAP_KEY);
  }

  @Bean
  public IMap<String, DefaultBot> bot(final HazelcastInstance instance) {
    return instance.getMap(BOTS_MAP_KEY);
  }

  @Bean
  public IMap<InstallerUserId, DefaultInstaller> installerUser(final HazelcastInstance instance) {
    return instance.getMap(INSTALLERS_MAP_KEY);
  }
}
