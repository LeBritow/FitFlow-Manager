package com.mycompany.academia.aluno.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class DetalhesTreinoRealizadoController {

    @FXML private Label labelTituloTreino;
    @FXML private Label labelDataFinalizacao;
    
    @FXML private TableView<LinhaExecucao> tabelaExecucao;
    @FXML private TableColumn<LinhaExecucao, String> colExercicio;
    @FXML private TableColumn<LinhaExecucao, String> colPlanejado;
    @FXML private TableColumn<LinhaExecucao, String> colRealizado;
    
    @FXML private Label labelNotaGeral;
    @FXML private Label labelComentarioAluno;

    @FXML
    public void initialize() {
        // Configura as colunas da tabela para ler as propriedades da nossa classe interna
        colExercicio.setCellValueFactory(cellData -> cellData.getValue().exercicio);
        colPlanejado.setCellValueFactory(cellData -> cellData.getValue().planejado);
        colRealizado.setCellValueFactory(cellData -> cellData.getValue().realizado);
    }

    // Método que a tela principal vai chamar para "injetar" os dados na janelinha
    public void carregarDadosMockados(String tituloDinamico) {
        labelTituloTreino.setText(tituloDinamico);
        
        // Simulando a diferença entre o que o app mandou fazer e o que o aluno anotou
        ObservableList<LinhaExecucao> dados = FXCollections.observableArrayList(
            new LinhaExecucao("Supino Reto com Barra", "4x10 - 20kg", "4x (10,10,8,8) - 22kg"),
            new LinhaExecucao("Crucifixo Inclinado", "3x12 - 12kg", "3x12 - 12kg (OK)"),
            new LinhaExecucao("Tríceps Pulley", "4x12 - 40kg", "4x (12,12,10,8) - 40kg")
        );
        
        tabelaExecucao.setItems(dados);
    }

    // Classe auxiliar interna apenas para popular a Tabela facilmente
    public static class LinhaExecucao {
        private final SimpleStringProperty exercicio;
        private final SimpleStringProperty planejado;
        private final SimpleStringProperty realizado;

        public LinhaExecucao(String exercicio, String planejado, String realizado) {
            this.exercicio = new SimpleStringProperty(exercicio);
            this.planejado = new SimpleStringProperty(planejado);
            this.realizado = new SimpleStringProperty(realizado);
        }
    }
}