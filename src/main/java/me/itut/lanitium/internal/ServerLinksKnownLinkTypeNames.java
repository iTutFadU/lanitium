package me.itut.lanitium.internal;

import net.minecraft.server.ServerLinks;

import static net.minecraft.server.ServerLinks.KnownLinkType.*;

public class ServerLinksKnownLinkTypeNames {
    public static ServerLinks.KnownLinkType from(String name) {
        return switch (name) {
            case "report_bug" -> BUG_REPORT;
            case "community_guidelines" -> COMMUNITY_GUIDELINES;
            case "support" -> SUPPORT;
            case "status" -> STATUS;
            case "feedback" -> FEEDBACK;
            case "community" -> COMMUNITY;
            case "website" -> WEBSITE;
            case "forums" -> FORUMS;
            case "news" -> NEWS;
            case "announcements" -> ANNOUNCEMENTS;
            default -> throw new IllegalArgumentException("Unknown link type: " + name);
        };
    }

    public static String name(ServerLinks.KnownLinkType type) {
        return switch (type) {
            case BUG_REPORT -> "report_bug";
            case COMMUNITY_GUIDELINES -> "community_guidelines";
            case SUPPORT -> "support";
            case STATUS -> "status";
            case FEEDBACK -> "feedback";
            case COMMUNITY -> "community";
            case WEBSITE -> "website";
            case FORUMS -> "forums";
            case NEWS -> "news";
            case ANNOUNCEMENTS -> "announcements";
        };
    }
}
