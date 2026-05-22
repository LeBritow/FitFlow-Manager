package com.mycompany.academia.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
public class ItemTreino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private float intervaloDescanso; 
    
    // Flag para sabermos se o instrutor quis fazer progressão ou se é um treino normal
    private boolean progressaoCarga; 

    // Relacionamento com Treino
    @ManyToOne
    @JoinColumn(name = "treino_id")
    private Treino treino;

    // Relacionamento com Exercicio
    @ManyToOne
    @JoinColumn(name = "exercicio_id")
    private Exercicio exercicio;

    // A MÁGICA ACONTECE AQUI: Uma lista contendo todas as séries deste exercício
    // CascadeType.ALL significa que se salvar ou apagar o ItemTreino, as Séries vão junto!
    @OneToMany(mappedBy = "itemTreino", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SerieTreino> seriesTreino = new ArrayList<>();

    public ItemTreino() {
        // Construtor vazio obrigatório para o JPA
    }

    // Método utilitário para facilitar adicionar séries
    public void adicionarSerie(SerieTreino serie) {
        seriesTreino.add(serie);
        serie.setItemTreino(this);
    }

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public float getIntervaloDescanso() { return intervaloDescanso; }
    public void setIntervaloDescanso(float intervaloDescanso) { this.intervaloDescanso = intervaloDescanso; }

    public boolean isProgressaoCarga() { return progressaoCarga; }
    public void setProgressaoCarga(boolean progressaoCarga) { this.progressaoCarga = progressaoCarga; }

    public Treino getTreino() { return treino; }
    public void setTreino(Treino treino) { this.treino = treino; }

    public Exercicio getExercicio() { return exercicio; }
    public void setExercicio(Exercicio exercicio) { this.exercicio = exercicio; }

    public List<SerieTreino> getSeriesTreino() { return seriesTreino; }
    public void setSeriesTreino(List<SerieTreino> seriesTreino) { this.seriesTreino = seriesTreino; }
}