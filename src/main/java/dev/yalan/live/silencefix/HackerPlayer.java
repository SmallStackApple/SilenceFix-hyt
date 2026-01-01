package dev.yalan.live.silencefix;

public class HackerPlayer {
    private final ClientId client;
    private final String name;

    public HackerPlayer(ClientId client, String name) {
        this.client = client;
        this.name = name;
    }

    public ClientId getClient() {
        return client;
    }

    public String getName() {
        return name;
    }

    public enum ClientId {
        SOUTHSIDE,
        GUARD_FIX,
        OTHERS
    }
}
