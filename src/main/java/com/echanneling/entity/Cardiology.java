package com.echanneling.entity;

public class Cardiology implements Specialization {
    @Override
    public String getSpecialtyName() {
        return "Cardiology";
    }

    @Override
    public String getDescription() {
        return "Focuses on heart and blood vessel disorders.";
    }
}
