package com.syla.core.privacy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Synchronous, in-memory export query. Trusted module listeners add their own data only. */
public final class PersonalDataExport {
    private final UUID actorId;
    private final Map<String, Object> sections = new LinkedHashMap<>();
    public PersonalDataExport(UUID actorId) { this.actorId = actorId; }
    public UUID actorId() { return actorId; }
    public void add(String name, Object data) { sections.put(name, data); }
    public Map<String, Object> sections() { return Map.copyOf(sections); }
}
