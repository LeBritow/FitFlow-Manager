package com.mycompany.academia.treino.ui;

import com.mycompany.academia.treino.dao.ExercicioDAO;
import com.mycompany.academia.treino.model.Exercicio;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class FormExercicioController {

    @FXML private Label labelTitulo;
    @FXML private TextField campoNome;
    @FXML private TextField campoGrupo;
    @FXML private TextField campoUrl;
    @FXML private TextArea campoDescricao;

    private Exercicio exercicioEdicao = null;

    public void preencherParaEdicao(Exercicio e) {
        this.exercicioEdicao = e;
        labelTitulo.setText("Editar Exercício");
        campoNome.setText(e.getNome());
        campoGrupo.setText(e.getGrupoMuscular());
        campoUrl.setText(e.getUrlMidia());
        campoDescricao.setText(e.getDescricao());
    }

    @FXML
    void clicouSalvar(ActionEvent event) {
        Exercicio e = exercicioEdicao;
        if (e == null) {
            e = new Exercicio();
        }

        e.setNome(campoNome.getText());
        e.setGrupoMuscular(campoGrupo.getText());
        e.setUrlMidia(campoUrl.getText());
        e.setDescricao(campoDescricao.getText());

        ExercicioDAO dao = new ExercicioDAO();
        if (dao.salvar(e)) {
            fecharJanela();
        }
    }

    @FXML
    void clicouCancelar(ActionEvent event) {
        fecharJanela();
    }

    private void fecharJanela() {
        Stage stage = (Stage) campoNome.getScene().getWindow();
        stage.close();
    }
}