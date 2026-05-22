package com.mycompany.academia.model;

import com.mycompany.academia.session.SessaoTreino;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

@Entity
public class ComentarioTreino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String mensagem;
    private LocalDateTime dataHoraEnvio;
    private LocalDateTime dataHoraVisu;
    private boolean visualizado;

    @ManyToOne
    @JoinColumn(name = "aluno_id")
    private Aluno aluno;

    @ManyToOne
    @JoinColumn(name = "sessao_treino_id")
    private SessaoTreino sessaoTreino;

    public ComentarioTreino() {
    }

    public void marcarComoLido() {
        this.visualizado = true;
        this.dataHoraVisu = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }

    public LocalDateTime getDataHoraEnvio() { return dataHoraEnvio; }
    public void setDataHoraEnvio(LocalDateTime dataHoraEnvio) { this.dataHoraEnvio = dataHoraEnvio; }

    public LocalDateTime getDataHoraVisu() { return dataHoraVisu; }
    public void setDataHoraVisu(LocalDateTime dataHoraVisu) { this.dataHoraVisu = dataHoraVisu; }

    public boolean isVisualizado() { return visualizado; }
    public void setVisualizado(boolean visualizado) { this.visualizado = visualizado; }

    public Aluno getAluno() { return aluno; }
    public void setAluno(Aluno aluno) { this.aluno = aluno; }

    public SessaoTreino getSessaoTreino() { return sessaoTreino; }
    public void setSessaoTreino(SessaoTreino sessaoTreino) { this.sessaoTreino = sessaoTreino; }
}