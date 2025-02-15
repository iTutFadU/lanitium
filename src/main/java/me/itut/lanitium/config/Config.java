package me.itut.lanitium.config;

import com.google.gson.annotations.SerializedName;
import net.minecraft.server.ServerLinks;

public class Config {
    @SerializedName("server_brand")
    public String SERVER_BRAND;
    @SerializedName("server_links")
    public ServerLinks.Entry[] SERVER_LINKS;

    public void fillDefaults() {
        if (SERVER_LINKS == null) SERVER_LINKS = new ServerLinks.Entry[0];
    }
}
