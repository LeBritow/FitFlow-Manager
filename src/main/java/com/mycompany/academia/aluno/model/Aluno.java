package com.mycompany.academia.aluno.model;

import com.mycompany.academia.admin.model.Usuario;
import jakarta.persistence.Entity;

@Entity
public class Aluno extends Usuario {

    private float peso;
    private float altura;
    private float imc;

    public Aluno() {
    }

    public float getPeso() { return peso; }
    public void setPeso(float peso) { this.peso = peso; }

    public float getAltura() { return altura; }
    public void setAltura(float altura) { this.altura = altura; }

    public float getImc() { return imc; }
    public void setImc(float imc) { this.imc = imc; }
}