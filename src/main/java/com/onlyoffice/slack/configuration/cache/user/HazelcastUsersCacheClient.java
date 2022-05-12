package com.onlyoffice.slack.configuration.cache.user;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.onlyoffice.slack.model.registry.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class HazelcastUsersCacheClient {
    public final static String USERS = "users";
    private final HazelcastInstance hazelcastInstance;

    public void addUser(String wid, String uid, User user) {
        log.debug("Adding user {}-{} to the cache map", wid, uid);
        IMap<String, User> map = hazelcastInstance.getMap(USERS);
        map.putIfAbsent(wid+uid, user);
    }

    public User getUser(String wid, String uid) {
        log.debug("Getting user {}-{} from the cache map", wid, uid);
        IMap<String, User> map = hazelcastInstance.getMap(USERS);
        return map.get(wid+uid);
    }

    public User deleteUser(String wid, String uid) {
        log.debug("Removing user {}-{} from the cache map", wid, uid);
        IMap<String, User> map = hazelcastInstance.getMap(USERS);
        return map.remove(wid+uid);
    }

    public void deleteAll() {
        log.debug("Removing all users");
        IMap<String, User> map = hazelcastInstance.getMap(USERS);
        map.clear();
    }
}
