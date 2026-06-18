package com.mycompany.academia.treino.model;

import com.mycompany.academia.aluno.model.Aluno;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

@Entity
public class ProgramacaoTreino {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  private LocalDateTime dataInicioSemanas;
  private LocalDateTime dataFimSemanas;
  private String diaDaSemana;

  @ManyToOne
  @JoinColumn(name = "aluno_id")
  private Aluno aluno;

  @ManyToOne
  @JoinColumn(name = "treino_id")
  private Treino treino;

  public ProgramacaoTreino() {
  }

  public int getId() {
    return id;
  }
  public void setId(int id) {
    this.id = id;
  }

  public LocalDateTime getDataInicioSemanas() {
    return dataInicioSemanas;
  }
  public void setDataInicioSemanas(LocalDateTime dataInicioSemanas) {
    this.dataInicioSemanas = dataInicioSemanas;
  }

  public LocalDateTime getDataFimSemanas() {
    return dataFimSemanas;
  }
  public void setDataFimSemanas(LocalDateTime dataFimSemanas) {
    this.dataFimSemanas = dataFimSemanas;
  }

  public String getDiaDaSemana() {
    return diaDaSemana;
  }
  public void setDiaDaSemana(String diaDaSemana) {
    this.diaDaSemana = diaDaSemana;
  }

  public Aluno getAluno() {
    return aluno;
  }
  public void setAluno(Aluno aluno) {
    this.aluno = aluno;
  }

  public Treino getTreino() {
    return treino;
  }
  public void setTreino(Treino treino) {
    this.treino = treino;
  }
}