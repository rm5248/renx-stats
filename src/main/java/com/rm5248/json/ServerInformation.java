/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rm5248.json;

import com.owlike.genson.annotation.JsonProperty;
import java.util.Map;

/**
 *
 * @author rob
 */
public class ServerInformation {
    /*
       {
      "Name":"Constructive Tyranny AOW",
      "Current Map":"CNC-Whiteout",
      "Bots":0,
      "Players":8,
      "Game Version":"Open Beta 5.468",
      "Variables":{
         "Mine Limit":28,
         "bSteamRequired":false,
         "bPrivateMessageTeamOnly":false,
         "bPassworded":false,
         "bAllowPrivateMessaging":true,
         "bRanked":true,
         "Game Type":1,
         "Player Limit":64,
         "Vehicle Limit":14,
         "bAutoBalanceTeams":true,
         "Team Mode":3,
         "bSpawnCrates":true,
         "CrateRespawnAfterPickup":30.000000,
         "Time Limit":0
      },
      "Port":7991,
      "IP":"148.251.214.18"
   },
    */
    
    public String Name;
    @JsonProperty("Current Map" )
    public String currentMap;
    public int Bots;
    public int Players;
    @JsonProperty("Game Version")
    public String gameVersion;
    public Map<String,Object> Variables;
    public int Port;
    public String IP;
}
