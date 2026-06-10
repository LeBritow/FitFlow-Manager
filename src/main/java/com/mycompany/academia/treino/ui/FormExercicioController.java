package com.mycompany.academia.treino.ui;

import com.mycompany.academia.core.config.GifSearchService;
import com.mycompany.academia.treino.dao.ExercicioDAO;
import com.mycompany.academia.treino.model.Exercicio;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
    @FXML private Button btnBuscarGif;

    private Exercicio exercicioEdicao = null;
    private GifSearchService gifService = new GifSearchService();

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

    @FXML
    void clicouBuscarGif(ActionEvent event) {
        String nome = campoNome.getText().trim();
        if (nome.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Aviso");
            alert.setHeaderText(null);
            alert.setContentText("Preencha o nome do exercício primeiro.");
            alert.showAndWait();
            return;
        }

        btnBuscarGif.setDisable(true);
        btnBuscarGif.setText("Buscando...");

        String grupo = campoGrupo.getText().trim();

        new Thread(() -> {
            String url = gifService.buscarMelhorGif(nome, grupo);

            javafx.application.Platform.runLater(() -> {
                btnBuscarGif.setDisable(false);
                btnBuscarGif.setText("Buscar GIF");

                if (url != null) {
                    campoUrl.setText(url);
                } else {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Resultado");
                    alert.setHeaderText(null);
                    alert.setContentText("Nenhum GIF encontrado para \"" + nome + "\". Configure uma chave de API do Giphy em GifSearchService.java ou insira a URL manualmente.");
                    alert.showAndWait();
                }
            });
        }).start();
    }

    private void fecharJanela() {
        Stage stage = (Stage) campoNome.getScene().getWindow();
        stage.close();
    }
}