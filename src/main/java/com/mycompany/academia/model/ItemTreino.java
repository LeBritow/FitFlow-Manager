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
    
    private boolean progressaoCarga; 

    @ManyToOne
    @JoinColumn(name = "treino_id")
    private Treino treino;

    @ManyToOne
    @JoinColumn(name = "exercicio_id")
    private Exercicio exercicio;

    @OneToMany(mappedBy = "itemTreino", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SerieTreino> seriesTreino = new ArrayList<>();

    public ItemTreino() {
    }

    public void adicionarSerie(SerieTreino serie) {
        seriesTreino.add(serie);
        serie.setItemTreino(this);
    }

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