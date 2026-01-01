package dev.yalan.live.silencefix;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Optional;
import java.util.UUID;

public class LiveUser {
    private final String clientId;
    private final UUID userId;
    private final JsonObject payload;
    private final Level level;

    public LiveUser(String clientId, UUID userId, JsonObject payload) {
        this.clientId = clientId;
        this.userId = userId;
        this.payload = payload;
        this.level = isSilenceFixUser() ? Level.of(payload.get("level").getAsString()) : Level.FOREIGNER;
    }

    public String getName() {
        return payload.get("username").getAsString();
    }

    public String getRank() {
        return Optional.ofNullable(payload.get("rank"))
                .map(JsonElement::getAsString)
                .orElse(null);
    }

    public String getQQ() {
        if (isSilenceFixUser()) {
            return Optional.ofNullable(payload.get("qq"))
                    .map(JsonElement::getAsString)
                    .orElse("10001");
        }

        return null;
    }

    public Level getLevel() {
        return level;
    }

    public boolean isSilenceFixUser() {
        return "SilenceFix".equals(clientId);
    }

    public String getClientId() {
        return clientId;
    }

    public UUID getUserId() {
        return userId;
    }

    public JsonObject getPayload() {
        return payload;
    }

    public static boolean isBothFreeUser(LiveUser user1, LiveUser user2) {
        return user1.getLevel() == Level.PAID && user2.getLevel() == Level.PAID;
    }

    public enum Level {
        FREE("Free", "FREE", "§a", "§a公益", 0),
        LITTLE_FANS("LittleFans", "LITTLE_FANS", "§d", "§d小粉丝", 0),
        SUPER_FANS("SuperFans", "SUPER_FANS", "§d", "§d大粉丝", 0),
        FREE_KILLER("FreeKiller", "FREE_KILLER", "§4§l", "§4§l公益粉丝杀手", 3),
        PAID("Paid", "PAID", "§e", "§e内部", 5),
        ADMINISTRATOR("Administrator", "ADMINISTRATOR", "§c", "§c管理员", 999),
        FOREIGNER("Foreigner", "FOREIGNER", "", "", -1),
        UNKNOWN("Unknown", "UNKNOWN", "", "", -1);

        private final String formalName;
        private final String jsonName;
        private final String colorCode;
        private final String defaultRank;
        private final int priority;

        Level(String formalName, String jsonName, String colorCode, String defaultRank, int priority) {
            this.formalName = formalName;
            this.jsonName = jsonName;
            this.colorCode = colorCode;
            this.defaultRank = defaultRank;
            this.priority = priority;
        }

        public boolean isFreeOrFans() {
            return this.priority == 0;
        }

        public boolean isGreaterThanFF() {
            return this.priority > 0;
        }

        public boolean isLower(Level target) {
            return this.priority < target.priority;
        }

        public boolean isLowerOrSame(Level target) {
            return this.priority <= target.priority;
        }

        public boolean isHigher(Level target) {
            return this.priority > target.priority;
        }

        public boolean isHigherOrSame(Level target) {
            return this.priority >= target.priority;
        }

        public String getFormalName() {
            return formalName;
        }

        public String getColorCode() {
            return colorCode;
        }

        public String getDefaultRank() {
            return defaultRank;
        }

        public int getPriority() {
            return priority;
        }

        public static Level ofJsonName(String name) {
            for (Level level : values()) {
                if (level.jsonName.equalsIgnoreCase(name)) {
                    return level;
                }
            }

            return UNKNOWN;
        }

        public static Level of(String name) {
            for (Level level : values()) {
                if (level.formalName.equals(name)) {
                    return level;
                }
            }

            return UNKNOWN;
        }

        public static boolean isDefaultRank(String rank) {
            for (Level level : values()) {
                if (level.defaultRank.equals(rank)) {
                    return true;
                }
            }

            return false;
        }
    }
}
