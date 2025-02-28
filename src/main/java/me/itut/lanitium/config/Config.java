package me.itut.lanitium.config;

import com.mojang.authlib.GameProfile;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerLinks;

import java.util.List;

public class Config {
    public String modName;
    public List<ServerLinks.Entry> links;
    public Component displayMotd;
    public Integer displayPlayersOnline, displayPlayersMax;
    private List<String> displayPlayersSample;
    public transient List<GameProfile> displayPlayersSampleProfiles;
    public boolean disableJoinMessages, disableLeaveMessages;

    public void fillDefaults() {
        if (displayPlayersSample != null)
            displayPlayersSampleProfiles = displayPlayersSample.stream().map(v -> new GameProfile(Util.NIL_UUID, v)).toList();
    }
}
