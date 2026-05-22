package com.mycompany.academia.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Treino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String nome;
    private String objetivo;
    
    private boolean fichaPadrao; 

    public Treino() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getObjetivo() { return objetivo; }
    public void setObjetivo(String objetivo) { this.objetivo = objetivo; }

    public boolean isFichaPadrao() { return fichaPadrao; }
    public void setFichaPadrao(boolean fichaPadrao) { this.fichaPadrao = fichaPadrao; }
}