package com.mycompany.academia.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class SerieTreino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int numeroDaSerie; // Ex: 1, 2, 3
    private int repeticoes;    // Ex: 12 (na primeira), 10 (na segunda)
    private float carga;       // Ex: 10.0 (na primeira), 12.0 (na segunda)

    // Relacionamento com a tabela ItemTreino (Mãe)
    @ManyToOne
    @JoinColumn(name = "item_treino_id")
    private ItemTreino itemTreino;

    public SerieTreino() {
        // Construtor vazio para o JPA
    }

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getNumeroDaSerie() { return numeroDaSerie; }
    public void setNumeroDaSerie(int numeroDaSerie) { this.numeroDaSerie = numeroDaSerie; }

    public int getRepeticoes() { return repeticoes; }
    public void setRepeticoes(int repeticoes) { this.repeticoes = repeticoes; }

    public float getCarga() { return carga; }
    public void setCarga(float carga) { this.carga = carga; }

    public ItemTreino getItemTreino() { return itemTreino; }
    public void setItemTreino(ItemTreino itemTreino) { this.itemTreino = itemTreino; }
}