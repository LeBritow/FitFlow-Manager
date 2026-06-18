package com.mycompany.academia.treino.model;

import com.mycompany.academia.core.config.IEntidadeNomeada;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Treino implements IEntidadeNomeada {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  private String nome;

  @ManyToOne
  private Objetivo objetivo;
  private boolean fichaPadrao;

  public Treino() {
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

  public Objetivo getObjetivo() {
    return objetivo;
  }
  public void setObjetivo(Objetivo objetivo) {
    this.objetivo = objetivo;
  }

  public boolean isFichaPadrao() {
    return fichaPadrao;
  }
  public void setFichaPadrao(boolean fichaPadrao) {
    this.fichaPadrao = fichaPadrao;
  }

  @Override
  public String toString() {
    return nome;
  }
}