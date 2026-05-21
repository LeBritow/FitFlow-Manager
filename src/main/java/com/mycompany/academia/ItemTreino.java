package com.mycompany.academia;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class ItemTreino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id; // [cite: 137]

    private int series; // [cite: 139]
    private int repeticoes; // [cite: 140]
    private float intervaloDescanso; // [cite: 141]
    private float cargaSugerida; // [cite: 142]

    // Relacionamento com Treino
    @ManyToOne
    @JoinColumn(name = "treino_id") // Cria a Foreign Key apontando para Treino
    private Treino treino;

    // Relacionamento com Exercicio
    @ManyToOne
    @JoinColumn(name = "exercicio_id") // Cria a Foreign Key apontando para Exercicio
    private Exercicio exercicio;

    public ItemTreino() {
        // Construtor vazio obrigatório para o JPA
    }

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSeries() { return series; }
    public void setSeries(int series) { this.series = series; }

    public int getRepeticoes() { return repeticoes; }
    public void setRepeticoes(int repeticoes) { this.repeticoes = repeticoes; }

    public float getIntervaloDescanso() { return intervaloDescanso; }
    public void setIntervaloDescanso(float intervaloDescanso) { this.intervaloDescanso = intervaloDescanso; }

    public float getCargaSugerida() { return cargaSugerida; }
    public void setCargaSugerida(float cargaSugerida) { this.cargaSugerida = cargaSugerida; }

    public Treino getTreino() { return treino; }
    public void setTreino(Treino treino) { this.treino = treino; }

    public Exercicio getExercicio() { return exercicio; }
    public void setExercicio(Exercicio exercicio) { this.exercicio = exercicio; }
}