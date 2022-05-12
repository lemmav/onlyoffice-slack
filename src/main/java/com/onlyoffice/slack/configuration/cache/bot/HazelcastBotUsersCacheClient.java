package com.onlyoffice.slack.configuration.cache.bot;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.onlyoffice.slack.model.registry.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class HazelcastBotUsersCacheClient {
    public final static String BOTS = "bots";
    private final HazelcastInstance hazelcastInstance;

    public void addBot(String wid, User user) {
        log.debug("Adding {} bot to the cache map", wid);
        IMap<String, User> map = hazelcastInstance.getMap(BOTS);
        map.putIfAbsent(wid, user);
    }

    public User getBot(String wid) {
        log.debug("Getting bot {} from the cache map", wid);
        IMap<String, User> map = hazelcastInstance.getMap(BOTS);
        return map.get(wid);
    }

    public User deleteBot(String wid) {
        log.debug("Removing bot {} from the cache map", wid);
        IMap<String, User> map = hazelcastInstance.getMap(BOTS);
        return map.remove(wid);
    }
}
