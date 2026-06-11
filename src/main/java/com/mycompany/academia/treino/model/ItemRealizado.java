package com.mycompany.academia.treino.model;

import com.mycompany.academia.core.session.SessaoTreino;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class ItemRealizado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private float cargaUtilizada;
    private boolean feito;

    @ManyToOne
    @JoinColumn(name = "sessao_treino_id")
    private SessaoTreino sessaoTreino;

    @ManyToOne
    @JoinColumn(name = "item_treino_id")
    private ItemTreino itemTreino;
    
    @Column(name = "tempo_descanso_segundos")
    private Integer tempoDescansoSegundos = 0;

    @Column(name = "tempo_execucao_segundos")
    private Integer tempoExecucaoSegundos = 0;

    @Column(name = "status_carga")
    private String statusCarga;

    public ItemRealizado() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public float getCargaUtilizada() {
        return cargaUtilizada;
    }
    public void setCargaUtilizada(float cargaUtilizada) {
        this.cargaUtilizada = cargaUtilizada;
    }

    public boolean isFeito() {
        return feito;
    }
    public void setFeito(boolean feito) {
        this.feito = feito;
    }

    public SessaoTreino getSessaoTreino() {
        return sessaoTreino;
    }
    public void setSessaoTreino(SessaoTreino sessaoTreino) {
        this.sessaoTreino = sessaoTreino;
    }

    public ItemTreino getItemTreino() {
        return itemTreino;
    }
    public void setItemTreino(ItemTreino itemTreino) {
        this.itemTreino = itemTreino;
    }
    
    public Integer getTempoExecucaoSegundos() {
        return tempoExecucaoSegundos;
    }
    public void setTempoExecucaoSegundos(Integer t) {
        this.tempoExecucaoSegundos = t;
    }

    public Integer getTempoDescansoSegundos() {
        return tempoDescansoSegundos;
    }
    public void setTempoDescansoSegundos(Integer t) {
        this.tempoDescansoSegundos = t;
    }

    public String getStatusCarga() {
        return statusCarga;
    }
    public void setStatusCarga(String s) {
        this.statusCarga = s;
    }
}