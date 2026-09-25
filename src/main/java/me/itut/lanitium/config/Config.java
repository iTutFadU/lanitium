package me.itut.lanitium.config;

import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.players.NameAndId;

import java.util.List;

import static net.minecraft.util.Util.NIL_UUID;

public class Config {
    public String modName;
    public List<ServerLinks.Entry> links;
    public Component displayMotd;
    public Integer displayPlayersOnline, displayPlayersMax;
    private List<String> displayPlayersSample;
    public transient List<NameAndId> displayPlayersSampleProfiles;
    public boolean disableJoinMessages, disableLeaveMessages;

    public void fillDefaults() {
        if (displayPlayersSample != null)
            displayPlayersSampleProfiles = displayPlayersSample.stream().map(v -> new NameAndId(NIL_UUID, v)).toList();
    }
}
