package com.mycompany.academia.core.ui;

import com.mycompany.academia.aluno.dao.AlunoDAO;
import com.mycompany.academia.core.config.EventBus;
import com.mycompany.academia.treino.dao.ExercicioDAO;
import com.mycompany.academia.treino.dao.TreinoDAO;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class DashboardInicioController {

    @FXML private GridPane gradeCartoes;
    @FXML private ListView<com.mycompany.academia.treino.model.ComentarioTreino> listaFeedbacks;

    private AlunoDAO alunoDAO = new AlunoDAO();
    private ExercicioDAO exercicioDAO = new ExercicioDAO();
    private TreinoDAO treinoDAO = new TreinoDAO();
    private Consumer<String> onNavegar;

    public void setOnNavegar(Consumer<String> onNavegar) {
        this.onNavegar = onNavegar;
    }

    @FXML
    public void initialize() {
        EventBus.emit("Desktop", "DashboardInicioController.carregar", "Carregando dashboard");
        adicionarCartao(0, 0, String.valueOf(alunoDAO.contarAlunos()), "Total de Alunos", "#3498db", "#ebf5fb", "usuarios");
        adicionarCartao(1, 0, String.valueOf(exercicioDAO.contarExercicios()), "Exercícios Cadastrados", "#2ecc71", "#eafaf1", "exercicios");
        adicionarCartao(2, 0, String.valueOf(treinoDAO.contarFichas()), "Fichas de Treino", "#9b59b6", "#f4ecf7", "fichas");
        adicionarCartao(0, 1, String.valueOf(treinoDAO.contarTreinosHoje()), "Treinos Realizados Hoje", "#e67e22", "#fef5e7", "analise");
        adicionarCartao(1, 1, String.valueOf(treinoDAO.contarAlunosAtivosNoMes()), "Alunos Ativos (Mês)", "#1abc9c", "#e8f8f5", "analise");
        adicionarCartao(2, 1, String.valueOf(treinoDAO.contarFeedbacksNaoLidos()), "Feedbacks Não Lidos", "#e74c3c", "#fdedec", "analise");

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        listaFeedbacks.setItems(FXCollections.observableArrayList(treinoDAO.listarFeedbacksRecentes(30)));
        listaFeedbacks.setCellFactory(lv -> new javafx.scene.control.ListCell<com.mycompany.academia.treino.model.ComentarioTreino>() {
            @Override
            protected void updateItem(com.mycompany.academia.treino.model.ComentarioTreino item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    javafx.scene.layout.VBox oConteudo = new javafx.scene.layout.VBox(4);
                    javafx.scene.layout.HBox oLinha1 = new javafx.scene.layout.HBox(8);
                    Label oLblAluno = new javafx.scene.control.Label(item.getAluno().getNome().split(" ")[0]);
                    oLblAluno.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 13px;");
                    Label oLblTreino = new javafx.scene.control.Label(item.getTreino().getNome());
                    oLblTreino.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
                    Label oLblData = new javafx.scene.control.Label(item.getDataCriacao().format(formatter));
                    oLblData.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px;");
                    javafx.scene.layout.Region oSpacer = new javafx.scene.layout.Region();
                    oSpacer.setMaxWidth(Double.MAX_VALUE);
                    javafx.scene.layout.HBox.setHgrow(oSpacer, javafx.scene.layout.Priority.ALWAYS);
                    oLinha1.getChildren().addAll(oLblAluno, oLblTreino, oSpacer, oLblData);

                    String text = item.getTexto().length() > 100 ? item.getTexto().substring(0, 100) + "..." : item.getTexto();
                    Label oLblTexto = new javafx.scene.control.Label(text);
                    oLblTexto.setWrapText(true);
                    oLblTexto.setStyle("-fx-text-fill: #34495e; -fx-font-size: 12px; -fx-padding: 0 0 0 0;");

                    oConteudo.getChildren().addAll(oLinha1, oLblTexto);

                    if (item.isLido()) {
                        oConteudo.setStyle("-fx-padding: 8 10 8 10; -fx-border-color: transparent transparent #ecf0f1 transparent; -fx-border-width: 0 0 1 0;");
                    } else {
                        oConteudo.setStyle("-fx-background-color: #fff8e1; -fx-padding: 8 10 8 15; -fx-border-color: transparent transparent #f5d76e transparent; -fx-border-width: 0 0 1 0; -fx-background-insets: 0, 0, 0, 0;");
                    }

                    setGraphic(oConteudo);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });
    }

    private void adicionarCartao(int coluna, int linha, String container, String rotulo, String cor, String corFundo, String destino) {
        Label oLblValor = new Label(container);
        oLblValor.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: " + cor + ";");

        Label oLblRotulo = new Label(rotulo);
        oLblRotulo.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

        VBox oCartao = new VBox(5, oLblValor, oLblRotulo);
        oCartao.setStyle("-fx-background-color: " + corFundo + "; -fx-border-color: " + cor + "; -fx-border-width: 2px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 20px;");
        oCartao.setPrefHeight(110);
        oCartao.setCursor(Cursor.HAND);
        oCartao.setOnMouseClicked(e -> {
            if (onNavegar != null) onNavegar.accept(destino);
        });
        oCartao.setOnMouseEntered(e -> oCartao.setStyle("-fx-background-color: derive(" + corFundo + ", -5%); -fx-border-color: " + cor + "; -fx-border-width: 3px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 20px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 8, 0, 0, 2);"));
        oCartao.setOnMouseExited(e -> oCartao.setStyle("-fx-background-color: " + corFundo + "; -fx-border-color: " + cor + "; -fx-border-width: 2px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 20px;"));

        gradeCartoes.add(oCartao, coluna, linha);
    }
}
