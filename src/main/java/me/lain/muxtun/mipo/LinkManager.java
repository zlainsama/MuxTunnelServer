package me.lain.muxtun.mipo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class LinkManager
{

    private final SharedResources resources;
    private final Map<UUID, LinkSession> sessions;

    LinkManager(SharedResources resources)
    {
        this.resources = resources;
        this.sessions = new ConcurrentHashMap<>();
    }

    SharedResources getResources()
    {
        return resources;
    }

    Map<UUID, LinkSession> getSessions()
    {
        return sessions;
    }

}
