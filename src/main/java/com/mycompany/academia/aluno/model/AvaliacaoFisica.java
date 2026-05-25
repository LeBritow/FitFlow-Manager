package com.mycompany.academia.aluno.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "avaliacao_fisica")
public class AvaliacaoFisica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;

    private float peso;
    private float altura;
    private float imc;
    
    @Column(name = "data_avaliacao")
    private LocalDate dataAvaliacao;

    public AvaliacaoFisica() {}

    public AvaliacaoFisica(Aluno aluno, float peso, float altura, float imc, LocalDate dataAvaliacao) {
        this.aluno = aluno;
        this.peso = peso;
        this.altura = altura;
        this.imc = imc;
        this.dataAvaliacao = dataAvaliacao;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public Aluno getAluno() { return aluno; }
    public void setAluno(Aluno aluno) { this.aluno = aluno; }
    public float getPeso() { return peso; }
    public void setPeso(float peso) { this.peso = peso; }
    public float getAltura() { return altura; }
    public void setAltura(float altura) { this.altura = altura; }
    public float getImc() { return imc; }
    public void setImc(float imc) { this.imc = imc; }
    public LocalDate getDataAvaliacao() { return dataAvaliacao; }
    public void setDataAvaliacao(LocalDate dataAvaliacao) { this.dataAvaliacao = dataAvaliacao; }
}