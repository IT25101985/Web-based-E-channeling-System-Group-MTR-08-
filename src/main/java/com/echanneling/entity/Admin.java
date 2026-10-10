package com.echanneling.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("ADMIN")
public class Admin extends User {

    private String department;

    public Admin() {
        super();
        this.setRole("ROLE_ADMIN");
    }

    public Admin(String username, String password, String fullName, String department) {
        super(username, password, fullName);
        this.department = department;
        this.setRole("ROLE_ADMIN");
    }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}
