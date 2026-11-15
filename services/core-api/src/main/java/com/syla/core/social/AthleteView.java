package com.syla.core.social;
import com.syla.core.identity.ProfileView;
public record AthleteView(ProfileView profile, String relationship, long followers, long following) {}
