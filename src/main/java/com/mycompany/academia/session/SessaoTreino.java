package com.mycompany.academia.session;

import com.mycompany.academia.model.ProgramacaoTreino;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

@Entity
public class SessaoTreino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private LocalDateTime data;
    private boolean concluido;

    @ManyToOne
    @JoinColumn(name = "programacao_treino_id")
    private ProgramacaoTreino programacaoTreino;

    public SessaoTreino() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime data) { this.data = data; }

    public boolean isConcluido() { return concluido; }
    public void setConcluido(boolean concluido) { this.concluido = concluido; }

    public ProgramacaoTreino getProgramacaoTreino() { return programacaoTreino; }
    public void setProgramacaoTreino(ProgramacaoTreino programacaoTreino) { this.programacaoTreino = programacaoTreino; }
}