package com.echanneling.entity;

public class Pediatrics implements Specialization {
    @Override
    public String getSpecialtyName() {
        return "Pediatrics";
    }

    @Override
    public String getDescription() {
        return "Focuses on the medical care of infants, children, and adolescents.";
    }
}
