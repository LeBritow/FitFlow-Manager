package com.mycompany.academia.treino.ui;

import com.mycompany.academia.core.config.EventBus;
import com.mycompany.academia.core.config.GifSearchService;
import com.mycompany.academia.core.util.TableUtils;
import com.mycompany.academia.treino.dao.ExercicioDAO;
import com.mycompany.academia.treino.model.Exercicio;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ExerciciosController {

    @FXML private TableView<Exercicio> tabelaExercicios;
    @FXML private Label lblNomeExercicio;
    @FXML private Label lblDescricao;
    @FXML private Label lblProgresso;
    @FXML private ImageView imgPreview;
    @FXML private Button btnBuscarGifsMassa;

    private ExercicioDAO dao = new ExercicioDAO();
    private GifSearchService gifService = new GifSearchService();
    private ObservableList<Exercicio> listaExerciciosOb;

    @FXML
    public void initialize() {
        EventBus.emit("Desktop", "ExerciciosController.listarExercicios", "Carregando exercícios");
        carregarDadosTabela();

        tabelaExercicios.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            mostrarDetalhesExercicio(novo);
        });
    }

    private void carregarDadosTabela() {
        List<Exercicio> oExercicios = dao.listarTodos();
        listaExerciciosOb = FXCollections.observableArrayList(oExercicios);
        tabelaExercicios.setItems(listaExerciciosOb);
        TableUtils.autoFitColumns(tabelaExercicios);
    }

    private void mostrarDetalhesExercicio(Exercicio exercicio) {
        if (exercicio != null) {
            lblNomeExercicio.setText(exercicio.getNome() + " (" + exercicio.getGrupoMuscular() + ")");
            if (exercicio.getDescricao() != null) {
                lblDescricao.setText(exercicio.getDescricao());
            } else {
                lblDescricao.setText("Sem descrição técnica.");
            }

            if (exercicio.getUrlMidia() != null && !exercicio.getUrlMidia().isEmpty()) {
                try {
                    Image imagemGif = new Image(exercicio.getUrlMidia(), true);
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
        Exercicio oSelecionado = tabelaExercicios.getSelectionModel().getSelectedItem();
        if (oSelecionado == null) {
            mostrarAlerta("Aviso", "Selecione um exercício para editar.");
            return;
        }
        abrirFormulario(oSelecionado);
    }

    @FXML
    void clicouExcluir(ActionEvent event) {
        Exercicio oSelecionado = tabelaExercicios.getSelectionModel().getSelectedItem();
        if (oSelecionado == null) {
            mostrarAlerta("Aviso", "Selecione um exercício para excluir.");
            return;
        }

        if (dao.excluir(oSelecionado)) {
            carregarDadosTabela();
        }
    }

    @FXML
    void clicouBuscarGifsMassa(ActionEvent event) {
        List<Exercicio> oTodos = dao.listarTodos();
        List<Exercicio> oSemGif = new ArrayList<>();
        for (Exercicio e : oTodos) {
            if (e.getUrlMidia() == null || e.getUrlMidia().isEmpty()) {
                oSemGif.add(e);
            }
        }

        if (oSemGif.isEmpty()) {
            mostrarAlertaInfo("Todos os exercícios já possuem GIF.");
            return;
        }

        btnBuscarGifsMassa.setDisable(true);
        lblProgresso.setVisible(true);

        new Thread(() -> {
            int total = oSemGif.size();
            AtomicInteger sucesso = new AtomicInteger(0);
            AtomicInteger falha = new AtomicInteger(0);

            for (int i = 0; i < total; i++) {
                Exercicio oE = oSemGif.get(i);
                String url = gifService.buscarMelhorGif(oE.getNome(), oE.getGrupoMuscular());

                if (url != null) {
                    oE.setUrlMidia(url);
                    dao.inserir(oE);
                    sucesso.incrementAndGet();
                } else {
                    falha.incrementAndGet();
                }

                int progresso = i + 1;
                Platform.runLater(() ->
                    lblProgresso.setText("Buscando GIFs: " + progresso + "/" + total)
                );
            }

            Platform.runLater(() -> {
                btnBuscarGifsMassa.setDisable(false);
                lblProgresso.setVisible(false);
                carregarDadosTabela();

                Alert oAlert = new Alert(Alert.AlertType.INFORMATION);
                oAlert.setTitle("Busca em Massa Concluída");
                oAlert.setHeaderText(null);
                oAlert.setContentText(sucesso.get() + " GIFs encontrados e salvos.\n" + falha.get() + " exercícios sem resultado.");
                oAlert.showAndWait();
            });
        }).start();
    }

    private void abrirFormulario(Exercicio exercicio) {
        try {
            javafx.fxml.FXMLLoader oLoader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/FormExercicio.fxml"));
            javafx.scene.Parent oRaiz = oLoader.load();

            if (exercicio != null) {
                FormExercicioController oContr = oLoader.getController();
                oContr.preencherParaEdicao(exercicio);
            }

            javafx.stage.Stage oPalcoModal = new javafx.stage.Stage();
            if (exercicio == null) {
                oPalcoModal.setTitle("Novo Exercício");
            } else {
                oPalcoModal.setTitle("Editar Exercício");
            }
            oPalcoModal.setScene(new javafx.scene.Scene(oRaiz));
            oPalcoModal.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            oPalcoModal.showAndWait();

            carregarDadosTabela();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void mostrarAlerta(String titulo, String mensagem) {
        Alert oAlert = new Alert(Alert.AlertType.WARNING);
        oAlert.setTitle(titulo);
        oAlert.setHeaderText(null);
        oAlert.setContentText(mensagem);
        oAlert.showAndWait();
    }

    private void mostrarAlertaInfo(String mensagem) {
        Alert oAlert = new Alert(Alert.AlertType.INFORMATION);
        oAlert.setTitle("Informação");
        oAlert.setHeaderText(null);
        oAlert.setContentText(mensagem);
        oAlert.showAndWait();
    }
}