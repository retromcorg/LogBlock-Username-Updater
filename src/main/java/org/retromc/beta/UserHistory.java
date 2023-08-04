package org.retromc.beta;

import java.util.ArrayList;
import java.util.UUID;

public class UserHistory {
    private final UUID uuid;
    private ArrayList<UsernameEntry> history = new ArrayList<>();

    public UserHistory(UUID uuid) {
        this.uuid = uuid;
    }

    public String getLatestUsername() {
        //Find UsernameEntry with newest expiresOn
        UsernameEntry newestEntry = null;
        for (UsernameEntry entry : history) {
            if (newestEntry == null || entry.getExpiresOn() > newestEntry.getExpiresOn()) {
                newestEntry = entry;
            }
        }
        return newestEntry.getUsername();
    }

    public String[] getHistory() {
        //Get all usernames except for the one with the newest expiresOn
        ArrayList<String> usernames = new ArrayList<>();
        String latestUsername = getLatestUsername();
        for (UsernameEntry entry : history) {
            if (!entry.getUsername().equals(latestUsername)) {
                usernames.add(entry.getUsername());
            }
        }
        return usernames.toArray(new String[0]);
    }

    public void addEntry(UsernameEntry entry) {
        history.add(entry);
    }

    public UUID getUUID() {
        return uuid;
    }

}
