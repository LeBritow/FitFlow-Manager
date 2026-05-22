package com.mycompany.academia.model;

import jakarta.persistence.Entity;

@Entity
public class Instrutor extends Usuario {

    private String cref; // 

    public Instrutor() {
        // Construtor vazio obrigatório para o JPA
    }

    public String getCref() {
        return cref;
    }

    public void setCref(String cref) {
        this.cref = cref;
    }
}