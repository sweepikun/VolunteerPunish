package cn.popcraft.volunteerpunish.model;

import java.sql.Timestamp;
import java.util.UUID;

public class Volunteer {
    private int id;
    private UUID uuid;
    private String groupName;
    private String volunteerId;
    private int dailyBanUsed;
    private int dailyMuteUsed;
    private Timestamp lastReset;
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public UUID getUuid() {
        return uuid;
    }
    
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    
    public String getVolunteerId() {
        return volunteerId;
    }
    
    public void setVolunteerId(String volunteerId) {
        this.volunteerId = volunteerId;
    }
    
    public int getDailyBanUsed() {
        return dailyBanUsed;
    }
    
    public void setDailyBanUsed(int dailyBanUsed) {
        this.dailyBanUsed = dailyBanUsed;
    }
    
    public int getDailyMuteUsed() {
        return dailyMuteUsed;
    }
    
    public void setDailyMuteUsed(int dailyMuteUsed) {
        this.dailyMuteUsed = dailyMuteUsed;
    }
    
    public Timestamp getLastReset() {
        return lastReset;
    }
    
    public void setLastReset(Timestamp lastReset) {
        this.lastReset = lastReset;
    }
}