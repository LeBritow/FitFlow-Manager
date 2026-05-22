package com.mycompany.academia.model;

import jakarta.persistence.Entity;

@Entity
public class Instrutor extends Usuario {

    private String cref; // 

    public Instrutor() {
    }

    public String getCref() {
        return cref;
    }

    public void setCref(String cref) {
        this.cref = cref;
    }
}