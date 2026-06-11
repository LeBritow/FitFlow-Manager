package com.mycompany.academia.treino.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Exercicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String nome;
    private String grupoMuscular;
    private String descricao;
    
    private String urlMidia; 

    public Exercicio() {
    }

    public int getId() {
        return id; 
    }
    public void setId(int id) {
        this.id = id; 
    }

    public String getNome() { 
        return nome; 
    }
    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getGrupoMuscular() {
        return grupoMuscular;
    }
    public void setGrupoMuscular(String grupoMuscular) {
        this.grupoMuscular = grupoMuscular;
    }

    public String getDescricao() {
        return descricao;
    }
    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getUrlMidia() {
        return urlMidia;
    }
    public void setUrlMidia(String urlMidia) {
        this.urlMidia = urlMidia;
    }
}