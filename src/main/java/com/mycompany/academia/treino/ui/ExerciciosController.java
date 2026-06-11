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
        List<Exercicio> exercicios = dao.listarTodos();
        listaExerciciosOb = FXCollections.observableArrayList(exercicios);
        tabelaExercicios.setItems(listaExerciciosOb);
        TableUtils.autoFitColumns(tabelaExercicios);
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

    @FXML
    void clicouBuscarGifsMassa(ActionEvent event) {
        List<Exercicio> todos = dao.listarTodos();
        List<Exercicio> semGif = todos.stream()
                .filter(e -> e.getUrlMidia() == null || e.getUrlMidia().isEmpty())
                .toList();

        if (semGif.isEmpty()) {
            mostrarAlertaInfo("Todos os exercícios já possuem GIF.");
            return;
        }

        btnBuscarGifsMassa.setDisable(true);
        lblProgresso.setVisible(true);

        new Thread(() -> {
            int total = semGif.size();
            AtomicInteger sucesso = new AtomicInteger(0);
            AtomicInteger falha = new AtomicInteger(0);

            for (int i = 0; i < total; i++) {
                Exercicio e = semGif.get(i);
                String url = gifService.buscarMelhorGif(e.getNome(), e.getGrupoMuscular());

                if (url != null) {
                    e.setUrlMidia(url);
                    dao.salvar(e);
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

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Busca em Massa Concluída");
                alert.setHeaderText(null);
                alert.setContentText(sucesso.get() + " GIFs encontrados e salvos.\n" + falha.get() + " exercícios sem resultado.");
                alert.showAndWait();
            });
        }).start();
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

    private void mostrarAlertaInfo(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informação");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}