package com.mycompany.academia.treino.model;

import com.mycompany.academia.treino.enums.ObjetivoTreino;

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
    private ObjetivoTreino objetivo;
    private boolean fichaPadrao; 

    public Treino() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() {
        return nome; 
    }
    
    public void setNome(String nome) {
        this.nome = nome; 
    }

    public ObjetivoTreino getObjetivo() { 
        return objetivo; 
    }
    public void setObjetivo(ObjetivoTreino objetivo) {
        this.objetivo = objetivo;
    }

    public boolean isFichaPadrao() { return fichaPadrao; }
    public void setFichaPadrao(boolean fichaPadrao) { this.fichaPadrao = fichaPadrao; }
}