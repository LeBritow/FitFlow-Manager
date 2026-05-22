package com.mycompany.academia.controller;

import com.mycompany.academia.dao.ExercicioDAO;
import com.mycompany.academia.model.Exercicio;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ExerciciosController {

    @FXML private TableView<Exercicio> tabelaExercicios;
    @FXML private Label lblNomeExercicio;
    @FXML private Label lblDescricao;
    @FXML private ImageView imgPreview;

    private ExercicioDAO dao = new ExercicioDAO();
    private ObservableList<Exercicio> listaExerciciosOb;

    @FXML
    public void initialize() {
        carregarDadosTabela();

        tabelaExercicios.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            mostrarDetalhesExercicio(novo);
        });
    }

    private void carregarDadosTabela() {
        List<Exercicio> exercicios = dao.listarTodos();
        listaExerciciosOb = FXCollections.observableArrayList(exercicios);
        tabelaExercicios.setItems(listaExerciciosOb);
    }

    private void mostrarDetalhesExercicio(Exercicio e) {
        if (e != null) {
            lblNomeExercicio.setText(e.getNome() + " (" + e.getGrupoMuscular() + ")");
            lblDescricao.setText(e.getDescricao() != null ? e.getDescricao() : "Sem descrição técnica.");

            if (e.getUrlMidia() != null && !e.getUrlMidia().isEmpty()) {
                try {
                    Image imagemGif = new Image(e.getUrlMidia(), true);
                    imgPreview.setImage(imagemGif);
                } catch (Exception ex) {
                    imgPreview.setImage(null);
                }
            } else {
                imgPreview.setImage(null);
            }
        } else {
            lblNomeExercicio.setText("Selecione um exercício");
            lblDescricao.setText("");
            imgPreview.setImage(null);
        }
    }

    @FXML
    void clicouNovo(ActionEvent event) {
        abrirFormulario(null);
    }

    @FXML
    void clicouEditar(ActionEvent event) {
        Exercicio selecionado = tabelaExercicios.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            mostrarAlerta("Aviso", "Selecione um exercício para editar.");
            return;
        }
        abrirFormulario(selecionado);
    }

    @FXML
    void clicouExcluir(ActionEvent event) {
        Exercicio selecionado = tabelaExercicios.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            mostrarAlerta("Aviso", "Selecione um exercício para excluir.");
            return;
        }

        if (dao.excluir(selecionado)) {
            carregarDadosTabela();
        }
    }

    private void abrirFormulario(Exercicio e) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/FormExercicio.fxml"));
            javafx.scene.Parent raiz = loader.load();

            if (e != null) {
                FormExercicioController contr = loader.getController();
                contr.preencherParaEdicao(e);
            }

            javafx.stage.Stage palcoModal = new javafx.stage.Stage();
            palcoModal.setTitle(e == null ? "Novo Exercício" : "Editar Exercício");
            palcoModal.setScene(new javafx.scene.Scene(raiz));
            palcoModal.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            palcoModal.showAndWait();

            carregarDadosTabela();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void mostrarAlerta(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}