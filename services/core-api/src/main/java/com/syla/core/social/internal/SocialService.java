package com.syla.core.social.internal;

import com.syla.core.identity.*;
import com.syla.core.platform.ApiProblem;
import com.syla.core.social.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
class SocialService implements SocialData {
    private final AthleteDirectory directory;
    private final JdbcTemplate jdbc;
    SocialService(AthleteDirectory directory, JdbcTemplate jdbc) { this.directory=directory; this.jdbc=jdbc; }
    private boolean blocked(UUID a, UUID b) {
        return jdbc.queryForObject("select exists(select 1 from social.block where (blocker_id=? and blocked_id=?) or (blocker_id=? and blocked_id=?))", Boolean.class, a,b,b,a);
    }
    private Set<UUID> excluded(UUID viewer) {
        return new HashSet<>(jdbc.queryForList("select blocked_id from social.block where blocker_id=? union select blocker_id from social.block where blocked_id=?", UUID.class, viewer,viewer));
    }
    List<ProfileView> search(Authentication auth, String query) {
        UUID viewer=directory.actor(auth);
        return directory.search(query,viewer,excluded(viewer));
    }
    AthleteView get(Authentication auth, String handle) {
        UUID viewer=directory.actor(auth);
        var profile=directory.byHandle(handle);
        if (blocked(viewer,profile.id())) throw missing();
        return new AthleteView(profile, viewer.equals(profile.id()) ? "SELF" : relationship(viewer,profile.id()), count(profile.id(),true),count(profile.id(),false));
    }
    private long count(UUID id, boolean followers) {
        return jdbc.queryForObject("select count(*) from social.follow where " + (followers ? "athlete_id" : "follower_id") + "=? and status='ACCEPTED'", Long.class,id);
    }
    private String relationship(UUID viewer, UUID owner) {
        return jdbc.queryForList("select status from social.follow where follower_id=? and athlete_id=?", String.class, viewer,owner).stream().findFirst().orElse("NONE");
    }
    void change(Authentication auth, UUID target, String action) {
        UUID actor=directory.actor(auth);
        if (actor.equals(target)) throw new ApiProblem(400,"Choose another athlete.");
        directory.lockParticipants(actor,target);
        var profile=directory.profile(target);
        if (action.equals("block")) {
            jdbc.update("insert into social.block(blocker_id,blocked_id) values (?,?) on conflict do nothing",actor,target);
            jdbc.update("delete from social.follow where (follower_id=? and athlete_id=?) or (follower_id=? and athlete_id=?)",actor,target,target,actor);
            return;
        }
        if (action.equals("unblock")) {
            jdbc.update("delete from social.block where blocker_id=? and blocked_id=?",actor,target); return;
        }
        if (blocked(actor,target)) throw missing();
        switch (action) {
            case "follow" -> jdbc.update("insert into social.follow(follower_id,athlete_id,status) values (?,?,?) on conflict do nothing",actor,target,profile.privateProfile() ? "PENDING" : "ACCEPTED");
            case "unfollow" -> jdbc.update("delete from social.follow where follower_id=? and athlete_id=?",actor,target);
            case "accept" -> jdbc.update("update social.follow set status='ACCEPTED' where follower_id=? and athlete_id=? and status='PENDING'",target,actor);
            case "remove" -> jdbc.update("delete from social.follow where follower_id=? and athlete_id=?",target,actor);
            default -> throw new ApiProblem(400,"Unknown relationship action.");
        }
    }
    List<ProfileView> list(Authentication auth, String kind, int page) {
        UUID actor=directory.actor(auth);
        if (page<0 || page>100000) throw new ApiProblem(400,"Invalid page.");
        String sql=switch(kind) {
            case "following" -> "select athlete_id from social.follow where follower_id=? and status='ACCEPTED' order by created_at desc,athlete_id";
            case "followers" -> "select follower_id from social.follow where athlete_id=? and status='ACCEPTED' order by created_at desc,follower_id";
            case "requests" -> "select follower_id from social.follow where athlete_id=? and status='PENDING' order by created_at desc,follower_id";
            case "pending" -> "select athlete_id from social.follow where follower_id=? and status='PENDING' order by created_at desc,athlete_id";
            case "blocks" -> "select blocked_id from social.block where blocker_id=? order by created_at desc,blocked_id";
            default -> throw new ApiProblem(400,"Unknown relationship list.");
        };
        List<ProfileView> result=new ArrayList<>();
        // Bound each page while retaining stable relationship ordering.
        for (UUID id:jdbc.queryForList(sql + " limit 20 offset ?",UUID.class,actor,page*20)) {
            try { result.add(directory.profile(id)); }
            catch (ApiProblem missing) { if (missing.status()!=404) throw missing; }
        }
        return result;
    }
    public boolean canRead(UUID viewer, UUID owner) {
        directory.profile(owner);
        return !blocked(viewer,owner) && (viewer.equals(owner) || relationship(viewer,owner).equals("ACCEPTED"));
    }
    public List<Relationship> export(UUID owner) {
        List<Relationship> rows=new ArrayList<>(jdbc.query("select athlete_id, status from social.follow where follower_id=?",(rs,n)->new Relationship(rs.getObject(1,UUID.class),"OUTGOING",rs.getString(2)),owner));
        rows.addAll(jdbc.query("select follower_id, status from social.follow where athlete_id=?",(rs,n)->new Relationship(rs.getObject(1,UUID.class),"INCOMING",rs.getString(2)),owner));
        rows.addAll(jdbc.query("select blocked_id from social.block where blocker_id=?",(rs,n)->new Relationship(rs.getObject(1,UUID.class),"OUTGOING","BLOCKED"),owner));
        return rows;
    }
    public void deleteAllForAccount(UUID owner) {
        jdbc.update("delete from social.follow where follower_id=? or athlete_id=?",owner,owner);
        jdbc.update("delete from social.block where blocker_id=? or blocked_id=?",owner,owner);
    }
    private static ApiProblem missing() { return new ApiProblem(404,"Athlete not found."); }
}
