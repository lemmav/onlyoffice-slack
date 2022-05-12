package com.onlyoffice.slack.configuration.cache.workspaces;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.onlyoffice.slack.model.registry.Workspace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class HazelcastWorkspacesCacheClient {
    public final static String WORKSPACES = "workspaces";
    private final HazelcastInstance hazelcastInstance;

    public void addWorkspace(String wid, Workspace workspace) {
        log.debug("Adding workspace {} to the cache map", wid);
        IMap<String, Workspace> map = hazelcastInstance.getMap(WORKSPACES);
        map.putIfAbsent(wid, workspace);
    }

    public Workspace getWorkspace(String wid) {
        log.debug("Getting workspace {} from the cache map", wid);
        IMap<String, Workspace> map = hazelcastInstance.getMap(WORKSPACES);
        return map.get(wid);
    }

    public Workspace deleteWorkspace(String wid) {
        log.debug("Removing workspace {} from the cache map", wid);
        IMap<String, Workspace> map = hazelcastInstance.getMap(WORKSPACES);
        return map.remove(wid);
    }

    public void updateWorkspace(String wid, Workspace workspace) {
        log.debug("Updating workspace {} in the cache", wid);
        IMap<String, Workspace> map = hazelcastInstance.getMap(WORKSPACES);
        map.set(wid, workspace);
    }
}
