package com.mycompany.academia.aluno.ui;

import com.mycompany.academia.aluno.dao.AlunoDAO;
import com.mycompany.academia.aluno.model.Aluno;
import com.mycompany.academia.core.config.EventBus;
import com.mycompany.academia.aluno.model.AvaliacaoFisica;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.*;
import java.time.LocalDateTime;
import javafx.geometry.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.util.Duration;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.control.DateCell;
import javafx.scene.Node;

public class AnaliseAlunoController {

    @FXML private ComboBox<Aluno> comboBuscaAluno;
    @FXML private VBox painelDireito;
    @FXML private Label labelNomeAluno;
    @FXML private Label labelImcAluno;
    @FXML private Label labelFichaAtiva;
    @FXML private Label labelUltimoTreino;
    @FXML private Label labelTreinosMes;
    @FXML private VBox caixaAlertas;
    @FXML private LineChart<String, Number> graficoEvolucao;
    @FXML private LineChart<String, Number> graficoCargas;
    @FXML private ComboBox<String> comboExercicioGrafico;
    @FXML private ListView<ItemHistorico> listaComentarios;

    private AlunoDAO alunoDAO = new AlunoDAO();
    private com.mycompany.academia.treino.dao.TreinoDAO treinoDAO = new com.mycompany.academia.treino.dao.TreinoDAO(); 
    private Aluno alunoSelecionado;
    private ObservableList<Aluno> todosAlunos;

    @FXML
    public void initialize() {
        graficoEvolucao.setAnimated(false);
        graficoCargas.setAnimated(false);
        configurarFiltroBusca();
        
        Label oPlaceholder = new Label("Nenhum treino ou feedback registrado para este aluno ainda.");
        oPlaceholder.setStyle("-fx-text-fill: #7f8c8d;");
        listaComentarios.setPlaceholder(oPlaceholder);

        comboBuscaAluno.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) {
                alunoSelecionado = novo;
                painelDireito.setDisable(false);
                mostrarDetalhesAluno(novo);
            }
        });

        comboExercicioGrafico.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null && alunoSelecionado != null) {
                renderizarGraficoCargaExercicios(alunoSelecionado, novo);
            }
        });

        listaComentarios.setCellFactory(lv -> new ListCell<ItemHistorico>() {
            @Override
            protected void updateItem(ItemHistorico item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    java.time.format.DateTimeFormatter oFormatador = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    setText(String.format("[%s] [%s] %s", item.data.format(oFormatador), item.treinoNome, item.descricao));

                    if ("feedback".equals(item.tipo) && !item.lido) {
                        setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        listaComentarios.setOnMouseClicked((MouseEvent event) -> {
            if (event.getClickCount() == 2) {
                ItemHistorico oItem = listaComentarios.getSelectionModel().getSelectedItem();
                if (oItem != null) {
                    if (oItem.comentario != null) {
                        abrirModalDetalhesTreino(oItem.comentario);
                    } else if (oItem.sessao != null) {
                        abrirModalDetalhesTreino(oItem.sessao);
                    }
                }
            }
        });
    }

    private void configurarFiltroBusca() {
        List<Aluno> oListaBanco = alunoDAO.listarTodos();
        todosAlunos = FXCollections.observableArrayList(oListaBanco);
        comboBuscaAluno.setItems(todosAlunos);

        comboBuscaAluno.setConverter(new StringConverter<Aluno>() {
            @Override
            public String toString(Aluno aluno) {
                return aluno == null ? "" : aluno.getNome() + " (CPF: " + aluno.getCpf() + ")";
            }
            @Override
            public Aluno fromString(String string) {
                return null; 
            }
        });

        comboBuscaAluno.setEditable(true);
        comboBuscaAluno.getEditor().textProperty().addListener((obs, antigo, novo) -> {
            if (comboBuscaAluno.getSelectionModel().getSelectedItem() == null || 
                !comboBuscaAluno.getConverter().toString(comboBuscaAluno.getSelectionModel().getSelectedItem()).equals(novo)) {
                
                ObservableList<Aluno> oFiltrados = FXCollections.observableArrayList();
                for (Aluno a : todosAlunos) {
                    if (a.getNome().toLowerCase().contains(novo.toLowerCase()) || a.getCpf().contains(novo)) {
                        oFiltrados.add(a);
                    }
                }
                comboBuscaAluno.setItems(oFiltrados);
                comboBuscaAluno.show();
            }
        });
    }

    private void mostrarDetalhesAluno(Aluno pAluno) {
        EventBus.emit("Desktop", "AnaliseAlunoController.mostrarDetalhes", "alunoId=" + pAluno.getId());
        labelNomeAluno.setText(pAluno.getNome());
        atualizarLabelsMedidas(pAluno);
        
        labelFichaAtiva.setText(treinoDAO.buscarNomeFichaAtiva(pAluno.getId()));
        labelUltimoTreino.setText(treinoDAO.buscarDataUltimoTreino(pAluno.getId()));
        labelTreinosMes.setText(String.valueOf(treinoDAO.contarTreinosNoMes(pAluno.getId())));
        
        List<String> oExerciciosDoAluno = treinoDAO.listarNomesExerciciosDoAluno(pAluno.getId());
        if (oExerciciosDoAluno.isEmpty()) {
            comboExercicioGrafico.setItems(FXCollections.observableArrayList("Nenhum treino cadastrado"));
            comboExercicioGrafico.setDisable(true);
        } else {
            comboExercicioGrafico.setItems(FXCollections.observableArrayList(oExerciciosDoAluno));
            comboExercicioGrafico.setDisable(false);
        }
        
        renderizarGraficoPesoImcReal(pAluno);
        gerarAlertas(pAluno);

        List<ItemHistorico> oHistorico = new ArrayList<>();

        for (com.mycompany.academia.treino.model.ComentarioTreino c : treinoDAO.listarComentariosDoAluno(pAluno.getId())) {
            oHistorico.add(new ItemHistorico(
                c.getDataCriacao(), "feedback", c.getTreino().getNome(),
                c.getTexto(), c.isLido(), c, null, c.getAluno(), c.getTreino()
            ));
        }

        for (com.mycompany.academia.core.session.SessaoTreino s : treinoDAO.listarSessoesDoAluno(pAluno.getId())) {
            com.mycompany.academia.treino.model.Treino oT = s.getProgramacaoTreino().getTreino();
            String desc = "Treino realizado";
            if (s.getData() != null) {
                long dias = java.time.temporal.ChronoUnit.DAYS.between(s.getData().toLocalDate(), LocalDate.now());
                if (dias == 0) desc = "Treino realizado hoje";
                else if (dias == 1) desc = "Treino realizado ontem";
                else desc = "Treino realizado";
            }
            oHistorico.add(new ItemHistorico(
                s.getData(), "treino", oT.getNome(),
                desc, true, null, s, pAluno, oT
            ));
        }

        oHistorico.sort((a, b) -> b.data.compareTo(a.data));

        listaComentarios.getItems().clear();
        listaComentarios.setItems(FXCollections.observableArrayList(oHistorico));
    }

    private void atualizarLabelsMedidas(Aluno pAluno) {
        float imc = pAluno.getImc();
        float altura = pAluno.getAltura();

        String classificacao = "";
        if (imc > 0 && imc < 18.5) classificacao = "(Abaixo do Peso)";
        else if (imc >= 18.5 && imc <= 24.9) classificacao = "(Peso Ideal)";
        else if (imc >= 25.0 && imc <= 29.9) classificacao = "(Sobrepeso)";
        else if (imc >= 30.0 && imc <= 34.9) classificacao = "(Obesidade Grau I)";
        else if (imc >= 35.0 && imc <= 39.9) classificacao = "(Obesidade Grau II)";
        else         if (imc >= 40.0) classificacao = "(Obesidade Grau III)";

        float pesoIdealMin = 18.5f * (altura * altura);
        float pesoIdealMax = 24.9f * (altura * altura);

        if (pAluno.getPeso() > 0) {
            labelImcAluno.setText(String.format("Peso: %.1f kg | Altura: %.2f m | IMC: %.1f %s | Alvo Ideal: %.1f kg a %.1f kg", 
                    pAluno.getPeso(), altura, imc, classificacao, pesoIdealMin, pesoIdealMax));
        } else {
            labelImcAluno.setText("Aluno sem medidas registradas. Realize a primeira avaliação física.");
        }
    }

    private void renderizarGraficoPesoImcReal(Aluno pAluno) {
        graficoEvolucao.getData().clear();
        ((CategoryAxis) graficoEvolucao.getXAxis()).getCategories().clear(); 

        List<AvaliacaoFisica> oHistorico = alunoDAO.listarAvaliacoesDoAluno(pAluno.getId());

        XYChart.Series<String, Number> oSeriesPeso = new XYChart.Series<>();
        oSeriesPeso.setName("Peso Corporal (kg)");

        XYChart.Series<String, Number> oSeriesImc = new XYChart.Series<>();
        oSeriesImc.setName("Índice de IMC");

        DateTimeFormatter oFormatador = DateTimeFormatter.ofPattern("dd/MM");

        for (AvaliacaoFisica avaliacao : oHistorico) {
            String dataEixoX = avaliacao.getDataAvaliacao().format(oFormatador);
            oSeriesPeso.getData().add(new XYChart.Data<>(dataEixoX, avaliacao.getPeso()));
            oSeriesImc.getData().add(new XYChart.Data<>(dataEixoX, avaliacao.getImc()));
        }
        if (oHistorico.isEmpty() && pAluno.getPeso() > 0) {
            String hoje = LocalDate.now().format(oFormatador);
            oSeriesPeso.getData().add(new XYChart.Data<>(hoje, pAluno.getPeso()));
            oSeriesImc.getData().add(new XYChart.Data<>(hoje, pAluno.getImc()));
            graficoEvolucao.getData().addAll(oSeriesPeso, oSeriesImc);
            configurarTooltips(oSeriesPeso, " kg");
            configurarTooltips(oSeriesImc, " IMC");
        } else if (!oHistorico.isEmpty()) {
            graficoEvolucao.getData().addAll(oSeriesPeso, oSeriesImc);
            configurarTooltips(oSeriesPeso, " kg");
            configurarTooltips(oSeriesImc, " IMC");
        }
    }

    private void renderizarGraficoCargaExercicios(Aluno pAluno, String pNomeExercicio) {
        graficoCargas.getData().clear();
        ((CategoryAxis) graficoCargas.getXAxis()).getCategories().clear();

        XYChart.Series<String, Number> oSeriesCarga = new XYChart.Series<>();
        oSeriesCarga.setName("Carga Máxima (kg) - " + pNomeExercicio);

        List<com.mycompany.academia.treino.model.ItemRealizado> oHistorico = treinoDAO.listarHistoricoCargas(pAluno.getId(), pNomeExercicio);
        java.time.format.DateTimeFormatter oFormatador = java.time.format.DateTimeFormatter.ofPattern("dd/MM");

        Map<String, com.mycompany.academia.core.session.SessaoTreino> oMapaSessoes = new HashMap<>();

        for (com.mycompany.academia.treino.model.ItemRealizado ir : oHistorico) {
            String dataEixoX = ir.getSessaoTreino().getData().format(oFormatador);
            oMapaSessoes.putIfAbsent(dataEixoX, ir.getSessaoTreino());
            XYChart.Data<String, Number> ponto = new XYChart.Data<>(dataEixoX, ir.getCargaUtilizada());
            oSeriesCarga.getData().add(ponto);
        }

        if (!oHistorico.isEmpty()) {
            graficoCargas.getData().add(oSeriesCarga);

            for (XYChart.Data<String, Number> data : oSeriesCarga.getData()) {
                Tooltip oTooltip = new Tooltip(data.getYValue() + " kg");
                oTooltip.setShowDelay(Duration.ZERO);
                Tooltip.install(data.getNode(), oTooltip);
                data.getNode().setOnMouseEntered(e -> data.getNode().setStyle("-fx-scale-x: 1.4; -fx-scale-y: 1.4;"));
                data.getNode().setOnMouseExited(e -> data.getNode().setStyle("-fx-scale-x: 1; -fx-scale-y: 1;"));
                com.mycompany.academia.core.session.SessaoTreino oSessao = oMapaSessoes.get(data.getXValue());
                if (oSessao != null) {
                    data.getNode().setOnMouseClicked(e -> abrirModalDetalhesTreino(oSessao));
                }
            }
        }
    }

    private void configurarTooltips(XYChart.Series<String, Number> series, String sufixo) {
        for (XYChart.Data<String, Number> data : series.getData()) {
            Tooltip oTooltip = new Tooltip(data.getYValue() + sufixo);
            oTooltip.setShowDelay(Duration.ZERO);
            Tooltip.install(data.getNode(), oTooltip);
            data.getNode().setOnMouseEntered(e -> data.getNode().setStyle("-fx-scale-x: 1.4; -fx-scale-y: 1.4;"));
            data.getNode().setOnMouseExited(e -> data.getNode().setStyle("-fx-scale-x: 1; -fx-scale-y: 1;"));
        }
    }

    @FXML
    void clicouAtualizarMedidas(ActionEvent event) {
        if (alunoSelecionado == null) return;

        Dialog<ButtonType> oDialog = new Dialog<>();
        oDialog.setTitle("Gerenciamento de Avaliações Físicas");
        oDialog.setHeaderText("Métricas Corporais - " + alunoSelecionado.getNome().split(" ")[0]);

        ButtonType oBtnSalvar = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        ButtonType oBtnExcluir = new ButtonType("Excluir", ButtonBar.ButtonData.LEFT);
        oDialog.getDialogPane().getButtonTypes().addAll(oBtnExcluir, oBtnSalvar, ButtonType.CANCEL);

        Node oBotaoExcluir = oDialog.getDialogPane().lookupButton(oBtnExcluir);
        oBotaoExcluir.setVisible(false);
        oBotaoExcluir.setStyle("-fx-base: #e74c3c; -fx-text-fill: white;");

        GridPane oGrid = new GridPane();
        oGrid.setHgap(10);
        oGrid.setVgap(10);
        oGrid.setPadding(new Insets(20, 50, 10, 10));

        ComboBox<String> oComboModo = new ComboBox<>(FXCollections.observableArrayList("Nova Avaliação", "Editar Lançamento Anterior"));
        oComboModo.setValue("Nova Avaliação");

        ComboBox<AvaliacaoFisica> oComboAvaliacoesAnteriores = new ComboBox<>();
        oComboAvaliacoesAnteriores.setDisable(true);
        
        List<AvaliacaoFisica> oHistorico = alunoDAO.listarAvaliacoesDoAluno(alunoSelecionado.getId());
        oComboAvaliacoesAnteriores.setItems(FXCollections.observableArrayList(oHistorico));
        oComboAvaliacoesAnteriores.setConverter(new StringConverter<AvaliacaoFisica>() {
            @Override public String toString(AvaliacaoFisica af) { 
                return af == null ? "" : af.getDataAvaliacao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " (" + af.getPeso() + "kg)"; 
            }
            @Override public AvaliacaoFisica fromString(String str) { return null; }
        });

        TextField oTxtPeso = new TextField(String.valueOf(alunoSelecionado.getPeso()));
        TextField oTxtAltura = new TextField(String.valueOf(alunoSelecionado.getAltura()));
        DatePicker oPickerData = new DatePicker(LocalDate.now());

        oPickerData.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate oHoje = LocalDate.now();
                setDisable(empty || date.compareTo(oHoje) > 0);
            }
        });

        oComboModo.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            boolean modoEdicao = novo.equals("Editar Lançamento Anterior");
            oComboAvaliacoesAnteriores.setDisable(!modoEdicao);
            oPickerData.setDisable(modoEdicao); 
            oBotaoExcluir.setVisible(modoEdicao);
        });

        oComboAvaliacoesAnteriores.getSelectionModel().selectedItemProperty().addListener((obs, antigo, selec) -> {
            if (selec != null) {
                oTxtPeso.setText(String.valueOf(selec.getPeso()));
                oTxtAltura.setText(String.valueOf(selec.getAltura()));
                oPickerData.setValue(selec.getDataAvaliacao());
            }
        });

        oGrid.add(new Label("Operação:"), 0, 0);
        oGrid.add(oComboModo, 1, 0);
        oGrid.add(new Label("Selecionar Registro:"), 0, 1);
        oGrid.add(oComboAvaliacoesAnteriores, 1, 1);
        oGrid.add(new Label("Peso (kg):"), 0, 2);
        oGrid.add(oTxtPeso, 1, 2);
        oGrid.add(new Label("Altura (m):"), 0, 3);
        oGrid.add(oTxtAltura, 1, 3);
        oGrid.add(new Label("Data:"), 0, 4);
        oGrid.add(oPickerData, 1, 4);

        oDialog.getDialogPane().setContent(oGrid);

        oDialog.showAndWait().ifPresent(resposta -> {
            try {
                if (resposta == oBtnExcluir) {
                    AvaliacaoFisica oEditada = oComboAvaliacoesAnteriores.getSelectionModel().getSelectedItem();
                    if (oEditada == null) throw new IllegalArgumentException("Selecione qual avaliação deseja apagar.");
                    
                    alunoDAO.excluirAvaliacaoFisica(oEditada);
                    
                    List<AvaliacaoFisica> oRestante = alunoDAO.listarAvaliacoesDoAluno(alunoSelecionado.getId());
                    if (!oRestante.isEmpty()) {
                        AvaliacaoFisica oUltima = oRestante.get(oRestante.size() - 1);
                        alunoSelecionado.setPeso(oUltima.getPeso());
                        alunoSelecionado.setAltura(oUltima.getAltura());
                        alunoSelecionado.setImc(oUltima.getImc());
                    } else {
                        alunoSelecionado.setPeso(0);
                        alunoSelecionado.setAltura(0);
                        alunoSelecionado.setImc(0);
                    }
                    alunoDAO.inserirOuAtualizar(alunoSelecionado);
                    
                    atualizarLabelsMedidas(alunoSelecionado);
                    renderizarGraficoPesoImcReal(alunoSelecionado);
                    return; 
                }

                if (resposta == oBtnSalvar) {
                    float novoPeso = Float.parseFloat(oTxtPeso.getText());
                    float novaAltura = Float.parseFloat(oTxtAltura.getText());
                    float novoImc = novoPeso / (novaAltura * novaAltura);
                    
                    if (oComboModo.getValue().equals("Nova Avaliação")) {
                        LocalDate oDataAvaliacao = oPickerData.getValue();
                        if (oDataAvaliacao == null) throw new IllegalArgumentException("A data não pode estar vazia.");
                        if (oDataAvaliacao.isAfter(LocalDate.now())) throw new IllegalArgumentException("Você não pode registrar uma avaliação no futuro."); // Dupla checagem de segurança

                        AvaliacaoFisica oNova = new AvaliacaoFisica(alunoSelecionado, novoPeso, novaAltura, novoImc, oDataAvaliacao);
                        alunoDAO.inserirAvaliacaoFisica(oNova);
                    } else {
                        AvaliacaoFisica oEditada = oComboAvaliacoesAnteriores.getSelectionModel().getSelectedItem();
                        if (oEditada == null) throw new IllegalArgumentException("Selecione qual avaliação deseja corrigir.");

                        oEditada.setPeso(novoPeso);
                        oEditada.setAltura(novaAltura);
                        oEditada.setImc(novoImc);
                        alunoDAO.atualizarAvaliacaoFisica(oEditada);
                    }

                    alunoSelecionado.setPeso(novoPeso);
                    alunoSelecionado.setAltura(novaAltura);
                    alunoSelecionado.setImc(novoImc);
                    alunoDAO.inserirOuAtualizar(alunoSelecionado);

                    atualizarLabelsMedidas(alunoSelecionado);
                    renderizarGraficoPesoImcReal(alunoSelecionado);
                }
            } catch (Exception e) {
                Alert oErro = new Alert(Alert.AlertType.ERROR);
                oErro.setTitle("Erro na Operação");
                oErro.setHeaderText("Falha ao processar");
                oErro.setContentText(e.getMessage());
                oErro.showAndWait();
            }
        });
    }

    private void abrirModalDetalhesTreino(com.mycompany.academia.core.session.SessaoTreino pSessao) {
        try {
            FXMLLoader oLoader = new FXMLLoader(getClass().getResource("/fxml/DetalhesTreinoRealizado.fxml"));
            Parent oRoot = oLoader.load();

            DetalhesTreinoRealizadoController oController = oLoader.getController();
            com.mycompany.academia.treino.model.Treino oTreino = pSessao.getProgramacaoTreino().getTreino();
            oController.carregarDadosReais(
                alunoSelecionado,
                oTreino,
                pSessao.getData(),
                null
            );

            Stage oModal = new Stage();
            oModal.setTitle("Detalhes da Execução do Treino - " + oTreino.getNome());
            oModal.setScene(new Scene(oRoot));
            oModal.setResizable(false);
            oModal.initModality(Modality.APPLICATION_MODAL);
            oModal.showAndWait();
        } catch (Exception e) {
            System.err.println("Erro crítico ao abrir modal:");
            e.printStackTrace();
            Alert oAlert = new Alert(Alert.AlertType.ERROR);
            oAlert.setTitle("Erro");
            oAlert.setHeaderText("Não foi possível abrir os detalhes do treino.");
            oAlert.setContentText(e.getCause() != null ? e.getCause().toString() : e.toString());
            oAlert.showAndWait();
        }
    }

    private void abrirModalDetalhesTreino(com.mycompany.academia.treino.model.ComentarioTreino pComentario) {
        try {
            FXMLLoader oLoader = new FXMLLoader(getClass().getResource("/fxml/DetalhesTreinoRealizado.fxml"));
            Parent oRoot = oLoader.load();

            DetalhesTreinoRealizadoController oController = oLoader.getController();
            oController.carregarDadosReais(pComentario);

            Stage oModal = new Stage();
            oModal.setTitle("Detalhes da Execução do Treino");
            oModal.setScene(new Scene(oRoot));
            oModal.setResizable(false);
            oModal.initModality(Modality.APPLICATION_MODAL);
            oModal.showAndWait();

            if (alunoSelecionado != null) {
                mostrarDetalhesAluno(alunoSelecionado);
            }
        } catch (Exception e) {
            System.err.println("Erro crítico capturado ao tentar abrir o modal:");
            e.printStackTrace();
            
            Alert oAlert = new Alert(Alert.AlertType.ERROR);
            oAlert.setTitle("Modo Investigação - Bug Capturado");
            oAlert.setHeaderText("Um erro oculto impediu a tela de abrir!");

            String causa = e.getCause() != null ? e.getCause().toString() : e.toString();
            oAlert.setContentText("Motivo da falha:\n" + causa + "\n\nOlhe o console (Output) do NetBeans para ver a linha exata do código onde isso quebrou.");
            oAlert.showAndWait();
        }
    }

    private void gerarAlertas(Aluno pAluno) {
        caixaAlertas.getChildren().clear();

        String fichaAtiva = labelFichaAtiva.getText();
        String ultimoTreino = labelUltimoTreino.getText();
        int treinosMes = Integer.parseInt(labelTreinosMes.getText().replaceAll("\\D+", "0"));

        boolean temAlerta = false;

        if ("Nenhuma Ativa".equals(fichaAtiva)) {
            caixaAlertas.getChildren().add(criarEtiquetaAlerta(
                "⚠️ Sem Ficha Ativa: O aluno não possui uma ficha de treino vigente. Atribua uma nova programação.",
                "#d35400", "#fdebd0"));
            temAlerta = true;
        }

        if (treinosMes == 0) {
            caixaAlertas.getChildren().add(criarEtiquetaAlerta(
                "🚨 Ausência: O aluno não registrou nenhum treino este mês.",
                "#c0392b", "#fadbd8"));
            temAlerta = true;
        } else if (treinosMes <= 4) {
            caixaAlertas.getChildren().add(criarEtiquetaAlerta(
                "⚠️ Frequência Baixa: Apenas " + treinosMes + " treinos no mês. Considere reforçar a frequência.",
                "#e67e22", "#fef5e7"));
            temAlerta = true;
        }

        if (!temAlerta) {
            caixaAlertas.getChildren().add(criarEtiquetaAlerta(
                "✅ Desempenho excelente! " + treinosMes + " treinos no mês e ficha ativa em dia.",
                "#27ae60", "#d5f5e3"));
        }
    }

    @FXML
    void clicouTrocarFicha(ActionEvent event) {
        if (alunoSelecionado == null) return;

        List<com.mycompany.academia.treino.model.Treino> oFichasPadrao = treinoDAO.listarFichasPadrao();
        if (oFichasPadrao.isEmpty()) {
            Alert oAviso = new Alert(Alert.AlertType.WARNING);
            oAviso.setTitle("Nenhuma Ficha Padrão");
            oAviso.setHeaderText("Não há fichas padrão cadastradas.");
            oAviso.setContentText("Crie uma ficha padrão primeiro em Treinos > Nova Ficha.");
            oAviso.showAndWait();
            return;
        }

        ChoiceDialog<com.mycompany.academia.treino.model.Treino> oDialog = new ChoiceDialog<>(oFichasPadrao.get(0), oFichasPadrao);
        oDialog.setTitle("Trocar Ficha de Treino");
        oDialog.setHeaderText("Selecione a nova ficha para " + alunoSelecionado.getNome());
        oDialog.setContentText("Ficha:");

        oDialog.showAndWait().ifPresent(ficha -> {
            com.mycompany.academia.treino.model.ProgramacaoTreino oProgramacao = new com.mycompany.academia.treino.model.ProgramacaoTreino();
            oProgramacao.setAluno(alunoSelecionado);
            oProgramacao.setTreino(ficha);
            oProgramacao.setDataInicioSemanas(LocalDateTime.now());
            oProgramacao.setDataFimSemanas(LocalDateTime.now().plusWeeks(4));

            boolean ok = treinoDAO.inserirProgramacao(oProgramacao);
            if (ok) {
                Alert oSucesso = new Alert(Alert.AlertType.INFORMATION);
                oSucesso.setTitle("Ficha Trocada");
                oSucesso.setHeaderText("Programação salva com sucesso!");
                oSucesso.setContentText("Ficha \"" + ficha.getNome() + "\" atribuída de "
                    + java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy").format(LocalDate.now())
                    + " até "
                    + java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy").format(LocalDate.now().plusWeeks(4)));
                oSucesso.showAndWait();
                mostrarDetalhesAluno(alunoSelecionado);
            } else {
                Alert oErro = new Alert(Alert.AlertType.ERROR);
                oErro.setTitle("Erro");
                oErro.setHeaderText("Falha ao salvar programação.");
                oErro.showAndWait();
            }
        });
    }

    private Label criarEtiquetaAlerta(String texto, String corTexto, String corFundo) {
        Label oEtiqueta = new Label(texto);
        oEtiqueta.setStyle("-fx-text-fill: " + corTexto + "; -fx-background-color: " + corFundo + "; -fx-padding: 8px 12px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-font-size: 13px;");
        oEtiqueta.setMaxWidth(Double.MAX_VALUE);
        return oEtiqueta;
    }

    // Wrapper para mesclar sessões e feedbacks no histórico
    public static class ItemHistorico {
        public final LocalDateTime data;
        public final String tipo;
        public final String treinoNome;
        public final String descricao;
        public final boolean lido;
        public final com.mycompany.academia.treino.model.ComentarioTreino comentario;
        public final com.mycompany.academia.core.session.SessaoTreino sessao;
        public final com.mycompany.academia.aluno.model.Aluno aluno;
        public final com.mycompany.academia.treino.model.Treino treino;

        public ItemHistorico(LocalDateTime data, String tipo, String treinoNome, String descricao,
                             boolean lido, com.mycompany.academia.treino.model.ComentarioTreino comentario,
                             com.mycompany.academia.core.session.SessaoTreino sessao,
                             com.mycompany.academia.aluno.model.Aluno aluno,
                             com.mycompany.academia.treino.model.Treino treino) {
            this.data = data;
            this.tipo = tipo;
            this.treinoNome = treinoNome;
            this.descricao = descricao;
            this.lido = lido;
            this.comentario = comentario;
            this.sessao = sessao;
            this.aluno = aluno;
            this.treino = treino;
        }
    }
}