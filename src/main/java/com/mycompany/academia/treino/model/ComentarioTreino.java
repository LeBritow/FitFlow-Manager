package com.mycompany.academia.treino.model;

import com.mycompany.academia.aluno.model.Aluno;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ComentarioTreino {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    @ManyToOne
    private Aluno aluno;
    
    @ManyToOne
    private Treino treino;
    
    private String texto;
    private LocalDateTime dataCriacao;
    private boolean lido;
    
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public com.mycompany.academia.aluno.model.Aluno getAluno() {
        return aluno;
    }
    public void setAluno(com.mycompany.academia.aluno.model.Aluno aluno) {
        this.aluno = aluno;
    }

    public Treino getTreino() {
        return treino;
    }
    public void setTreino(Treino treino) {
        this.treino = treino;
    }

    public String getTexto() {
        return texto;
    }
    public void setTexto(String texto) {
        this.texto = texto;
    }

    public java.time.LocalDateTime getDataCriacao() {
        return dataCriacao;
    }
    public void setDataCriacao(java.time.LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public boolean isLido() {
        return lido;
    }
    public void setLido(boolean lido) {
        this.lido = lido;
    }
}