package com.echanneling.entity;
@jakarta.persistence.Entity
public class AttendeeLock {
    @jakarta.persistence.Id @jakarta.persistence.Column(length=64)
    private String id;
    public AttendeeLock() {}
    public AttendeeLock(String id) { this.id = id; }
    public String getId() { return id; }
}
