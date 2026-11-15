package com.syla.core.social;
import java.util.*;
public interface SocialData {
    record Relationship(UUID athleteId, String direction, String status) {}
    List<Relationship> export(UUID owner);
    void deleteAllForAccount(UUID owner);
    boolean canRead(UUID viewer, UUID owner);
}
