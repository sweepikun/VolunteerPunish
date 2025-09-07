package cn.popcraft.volunteerpunish.model;

import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

public class Punishment {
    public enum Type {
        BAN,
        MUTE
    }
    
    private int id;
    private UUID targetUuid;
    private String volunteerId;
    private Type type;
    private long duration; // 以秒为单位，0表示永久
    private String reason;
    private Date issuedAt;
    private Date expiresAt; // null表示永久
    private boolean active = true; // 是否仍生效
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public UUID getTargetUuid() {
        return targetUuid;
    }
    
    public void setTargetUuid(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }
    
    public String getVolunteerId() {
        return volunteerId;
    }
    
    public void setVolunteerId(String volunteerId) {
        this.volunteerId = volunteerId;
    }
    
    public Type getType() {
        return type;
    }
    
    public void setType(Type type) {
        this.type = type;
    }
    
    public long getDuration() {
        return duration;
    }
    
    public void setDuration(long duration) {
        this.duration = duration;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public Date getIssuedAt() {
        return issuedAt;
    }
    
    public void setIssuedAt(Date issuedAt) {
        this.issuedAt = issuedAt;
    }
    
    public Date getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
}