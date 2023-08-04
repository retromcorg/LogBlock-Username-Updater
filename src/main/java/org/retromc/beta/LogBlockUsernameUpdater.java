package org.retromc.beta;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.UUID;

public class LogBlockUsernameUpdater {
    private String username;
    private String password;
    private String database;
    private String host;
    private String port;
    private String table; //Normally LB-Players
    private boolean skipUserInput; //If true, will skip user confirmation and update all users

    private HashMap<UUID, UserHistory> userHistories = new HashMap<>();

    private Gson gson = new Gson();


    private LogBlockUsernameUpdater(String[] args) throws IOException {
        System.out.println("Starting LogBlock Username Updater...");

        handleCommandlineInput(args);

        //Read table properties
        readTableList();

        //Print connection info
        System.out.println("Username: " + username + ", Database: " + database + ", Host: " + host + ", Port: " + port + ", Table: " + table);

        //Read UUIDCache
        readUUIDCache();


        //Add jetpackingwolf as old username of JohnyMuffin
        //addUsernameEntry(UUID.fromString("2cfc6452-a6b4-4c49-982e-492eaa3a14ec"), new UsernameEntry("jetpackingwolf", 0, true));


        //Get UUIDs to check
        UUID[] uuidsToCheck = getUUIDsToCheck();

        System.out.println("Detected " + uuidsToCheck.length + " users to check for username updates.");


        if (!skipUserInput) {
            Scanner scanner = new Scanner(System.in);

            System.out.println("\nDo you want to print user info? (Y/N)");
            char input = scanner.nextLine().charAt(0);
            if (input == 'Y' || input == 'y') {
                for (UUID uuid : uuidsToCheck) {
                    printUserInfo(userHistories.get(uuid));
                }
            }

            // Ask for confirmation
            System.out.println("\nDo you want to update the usernames? (Y/N)");
            input = scanner.nextLine().charAt(0);
            if (input != 'Y' && input != 'y') {
                System.out.println("Aborting...");
                System.exit(0);
            }
        }

        //Connect to database with HikariCP
        connectToDatabase();
//
//        //Test Database
        try (Connection connection = dataSource.getConnection()) {
            System.out.println("Database Connection Successful!");
        } catch (SQLException exception) {
            System.out.println("Database Connection Failed! Check your settings! Plugin will now disable!");
            exception.printStackTrace();
            System.exit(1);
        }

//        //Update usernames
        for (UUID uuid : uuidsToCheck) {
            UserHistory userHistory = userHistories.get(uuid);
            String newUsername = userHistory.getLatestUsername();
            String[] oldUsernames = userHistory.getHistory();

            updateUserData(newUsername, oldUsernames);
        }



    }

    String tables[] = new String[0];

    private void readTableList() throws IOException {
        ArrayList<String> tableList = new ArrayList<>();
        //If tableList.txt doesn't exist, create it and add lb-world to file and exit
        File file = new File("tableList.txt");
        if (!file.exists()) {
            file.createNewFile();
            System.out.println("Created tableList.txt. Please add the table name to the file and restart the plugin.");
            System.exit(0);
            return;
        }
        Scanner scanner = new Scanner(file);
        while (scanner.hasNextLine()) {
            tableList.add(scanner.nextLine());
        }
        scanner.close();

        //If tableList.txt is empty, exit
        if (tableList.isEmpty()) {
            System.out.println("tableList.txt is empty. Please add the table names on new lines and restart the plugin.");
            System.exit(0);
            return;
        }

        System.out.println("Will attempt to update playerid in the following tables: " + String.join(", ", tableList));

        this.tables = tableList.toArray(new String[0]);
    }

    private Integer getPlayerID(String username) {
        String sql = "SELECT playerid FROM `lb-players` WHERE playername = ?";

        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("playerid");
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return null;
    }


    private void updateUserData(String newUsername, String[] oldUsernames) {
        int rowsUpdated = 0;
        Integer newPlayerID = getPlayerID(newUsername);

        if (newPlayerID == null) {
            System.out.println("Could not find playerid for new username " + newUsername + ". Skipping...");
            return;
        }

        //Loop for each old username
        for (String oldUsername : oldUsernames) {
            Integer oldPlayerID = getPlayerID(oldUsername);

            if (oldPlayerID == null) {
                System.out.println("Could not find playerid for old username " + oldUsername + ". Skipping...");
                continue;
            }


            //Skip if old username is the same as new username
            if (oldUsername.equalsIgnoreCase(newUsername)) {
                continue;
            }

            //Loop for each table
            for (String table : tables) {
                String sql = "UPDATE `" + table + "` SET playerid = ? WHERE playerid = ?";

                try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, newPlayerID);
                    pstmt.setInt(2, oldPlayerID);


                    //Print SQL statement
//                    System.out.println(pstmt.toString());

                    int updatedRows = pstmt.executeUpdate();

                    rowsUpdated += updatedRows;

                    if (updatedRows > 0) {
                        //System.out.println("Updated " + updatedRows + " rows in table " + table + " for old username " + oldUsername + " to new username " + newUsername);
                    } else {
                        //nSystem.out.println("No rows updated in table " + table + " for old username " + oldUsername);
                    }
                } catch (SQLException exception) {
                    exception.printStackTrace();
                }
            }
        }

        System.out.println("Updated " + rowsUpdated + " for new username " + newUsername + ". Detected " + oldUsernames.length + " old usernames.");
    }


    private HikariDataSource dataSource;

    private void connectToDatabase() {
        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(10);
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);

        dataSource = new HikariDataSource(config);
    }

    private void printUserInfo(UserHistory userHistory) {
        System.out.println("UUID: " + userHistory.getUUID() + ", Latest Username: " + userHistory.getLatestUsername() + ", History: " + String.join(", ", userHistory.getHistory()));
    }

    private UUID[] getUUIDsToCheck() {
        ArrayList<UUID> uuidsToCheck = new ArrayList<>();
        for (UserHistory history : userHistories.values()) {
            if (history.getHistory().length > 0) {
                uuidsToCheck.add(history.getUUID());
            }
        }
        return uuidsToCheck.toArray(new UUID[0]);
    }

    private void addUsernameEntry(UUID uuid, UsernameEntry entry) {
        if (userHistories.containsKey(uuid)) {
            userHistories.get(uuid).addEntry(entry);
        } else {
            UserHistory history = new UserHistory(uuid);
            history.addEntry(entry);
            userHistories.put(uuid, history);
        }
    }

    private void readUUIDCache() throws IOException {
        File uuidCacheFile = new File("UUIDCache.json");

        if (!uuidCacheFile.exists()) {
            System.out.println("UUIDCache.json not found! Please copy it to the same directory as this jar file.");
            System.exit(0);
        }

        //Read UUIDCache.json to GSON JsonArray
        //For each entry in the JsonArray, create a UserHistory object and add it to userHistories
        JsonReader reader = new JsonReader(new FileReader(uuidCacheFile));
        Gson gson = new Gson();
        JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);

        for (JsonElement jsonElement : jsonArray) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            boolean onlineUUID = jsonObject.get("onlineUUID").getAsBoolean();
            String username = jsonObject.get("name").getAsString();
            long expiresOn = jsonObject.get("expiresOn").getAsLong();
            UUID uuid = UUID.fromString(jsonObject.get("uuid").getAsString());
            addUsernameEntry(uuid, new UsernameEntry(username, expiresOn, onlineUUID));
        }

        reader.close();

        System.out.println("Read " + userHistories.size() + " entries from UUIDCache.json");

    }

    private void handleCommandlineInput(String[] args) {
        //Parameters: <username> <password> <database> <host> <port> <table> [skipUserInput]

        if (args.length < 6 || args.length > 7) {
            System.out.println("Invalid arguments! Please use the following format:");
            System.out.println("java -jar LogBlockUsernameUpdater.jar <username> <password> <database> <host> <port> <table> [skipUserInput]");
            System.out.println("Example: java -jar LogBlockUsernameUpdater.jar root password logblock localhost 3306 lb-players false");
            System.exit(0);
        }

        if (args.length == 7) {
            skipUserInput = Boolean.parseBoolean(args[6]);
        }


        username = args[0];
        password = args[1];
        database = args[2];
        host = args[3];
        port = args[4];
        table = args[5];
    }


    public static void main(String[] args) throws IOException {
        new LogBlockUsernameUpdater(args);
    }
}
