# LogBlock Username Updater

LogBlock Username Updater is a utility program designed to update the usernames of players in LogBlock upon a name
change. This is helpful as pre-existing block change data will be automatically updated to reflect the new username.

# Features
- Updates the username of a player in LogBlock
- Connects to a MySQL database with HikariCP
- Uses the uuidcache.json file from Poseidon to get usernames and UUIDs
- Can be automated with Task Scheduler or a cron job

# Requirements
- Java 8 or higher
- Low-latency connection to the LogBlock MySQL database
- Running Project Poseidon server

# Compile
Compile the program with Maven.
```bash
mvn clean package
```

# Configuration
1. Copy the uuidcache.json file from your Poseidon server to the same directory as the jar file. The program will
   automatically detect the file and use it to update the usernames of players in LogBlock.
2. Create a file called tablelist.txt in the same directory as the jar file. This file should contain a list of all
   tables in the LogBlock database that contain usernames. Each table name should be on a new line. The program will
   automatically detect this file and use it to update the usernames of players in LogBlock.
   Example File:
```
lb-chat
lb-farlands
lb-retromc
lb-retromc_nether
lb-skylands
```

# Run
```bash
java -jar LogBlockUsernameUpdater.jar <username> <password> <database> <host> <port> <table> [skipUserInput]
```
Example:
```bash
java -jar LogBlockUsernameUpdater.jar root password logblock localhost 3306 lb-players false
```

# Automation
The program can be automated with Task Scheduler on Windows or a cron job on Linux. The program will automatically exit
after it has finished updating the usernames of all players in the uuidcache.json file.

## Example Windows batch file
```batch
@echo off
title LogBlock Updater
xcopy "C:\MinecraftServer\uuidcache.json" "C:\Utilities\LogBlock Updater\" /y
java -jar "LogBlock-Username-Updater-1.0-SNAPSHOT.jar" LBUser Password LogBlockDB 127.0.0.1 3306 lb-players true
del "C:\Utilities\LogBlock Updater\uuidcache.json"
```