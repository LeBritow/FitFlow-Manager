package com.mycompany.academia.treino.ui;

import com.mycompany.academia.core.config.EventBus;
import com.mycompany.academia.core.config.GifSearchService;
import com.mycompany.academia.core.util.TableUtils;
import com.mycompany.academia.treino.dao.ExercicioDAO;
import com.mycompany.academia.treino.model.Exercicio;
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

    private void mostrarDetalhesExercicio(Exercicio pExercicio) {
        if (pExercicio != null) {
            lblNomeExercicio.setText(pExercicio.getNome() + " (" + pExercicio.getGrupoMuscular() + ")");
            lblDescricao.setText(pExercicio.getDescricao() != null ? pExercicio.getDescricao() : "Sem descrição técnica.");

            if (pExercicio.getUrlMidia() != null && !pExercicio.getUrlMidia().isEmpty()) {
                try {
                    Image imagemGif = new Image(pExercicio.getUrlMidia(), true);
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
        List<Exercicio> oSemGif = oTodos.stream()
                .filter(e -> e.getUrlMidia() == null || e.getUrlMidia().isEmpty())
                .toList();

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
                Exercicio e = oSemGif.get(i);
                String url = gifService.buscarMelhorGif(e.getNome(), e.getGrupoMuscular());

                if (url != null) {
                    e.setUrlMidia(url);
                    dao.inserir(e);
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

    private void abrirFormulario(Exercicio pExercicio) {
        try {
            javafx.fxml.FXMLLoader oLoader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/FormExercicio.fxml"));
            javafx.scene.Parent oRaiz = oLoader.load();

            if (pExercicio != null) {
                FormExercicioController oContr = oLoader.getController();
                oContr.preencherParaEdicao(pExercicio);
            }

            javafx.stage.Stage oPalcoModal = new javafx.stage.Stage();
            oPalcoModal.setTitle(pExercicio == null ? "Novo Exercício" : "Editar Exercício");
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