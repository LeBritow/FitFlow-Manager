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

    // AQUI É O PULO DO GATO: VOCÊ PRECISA TER OS MÉTODOS ABAIXO!
    
    public void setAluno(Aluno aluno) { this.aluno = aluno; }
    
    public void setTreino(Treino treino) { this.treino = treino; }
    
    public void setTexto(String texto) { this.texto = texto; }
    
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
    
    public void setLido(boolean lido) { this.lido = lido; }
}