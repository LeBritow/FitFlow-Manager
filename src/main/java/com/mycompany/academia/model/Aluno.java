package com.mycompany.academia.model;

import jakarta.persistence.Entity;

@Entity
public class Aluno extends Usuario {

    private float peso; // [cite: 109]
    private float altura; // [cite: 109]
    private float imc; // [cite: 110]

    public Aluno() {
    }

    public float getPeso() { return peso; }
    public void setPeso(float peso) { this.peso = peso; }

    public float getAltura() { return altura; }
    public void setAltura(float altura) { this.altura = altura; }

    public float getImc() { return imc; }
    public void setImc(float imc) { this.imc = imc; }
}