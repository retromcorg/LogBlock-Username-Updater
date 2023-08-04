package org.retromc.beta;

public class UsernameEntry {
    private final String username;
    private final long expiresOn;
    private final boolean onlineUUID;

    public UsernameEntry(String username, long expiresOn, boolean onlineUUID) {
        this.username = username;
        this.expiresOn = expiresOn;
        this.onlineUUID = onlineUUID;
    }


    public String getUsername() {
        return username;
    }

    public long getExpiresOn() {
        return expiresOn;
    }

    public boolean isOnlineUUID() {
        return onlineUUID;
    }
}
