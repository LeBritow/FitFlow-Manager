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
import java.io.IOException;
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
        
        // Configura um aviso nativo elegante para listas vazias
        Label placeholder = new Label("Nenhum treino ou feedback registrado para este aluno ainda.");
        placeholder.setStyle("-fx-text-fill: #7f8c8d;");
        listaComentarios.setPlaceholder(placeholder);

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

        // CELL FACTORY: Converte ItemHistorico em texto estruturado
        listaComentarios.setCellFactory(lv -> new ListCell<ItemHistorico>() {
            @Override
            protected void updateItem(ItemHistorico item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    java.time.format.DateTimeFormatter formatador = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    setText(String.format("[%s] [%s] %s", item.data.format(formatador), item.treinoNome, item.descricao));

                    if ("feedback".equals(item.tipo) && !item.lido) {
                        setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // Ouvinte de duplo clique para abrir detalhes do treino
        listaComentarios.setOnMouseClicked((MouseEvent event) -> {
            if (event.getClickCount() == 2) {
                ItemHistorico item = listaComentarios.getSelectionModel().getSelectedItem();
                if (item != null) {
                    if (item.comentario != null) {
                        abrirModalDetalhesTreino(item.comentario);
                    } else if (item.sessao != null) {
                        abrirModalDetalhesTreino(item.sessao);
                    }
                }
            }
        });
    }

    private void configurarFiltroBusca() {
        List<Aluno> listaBanco = alunoDAO.buscarTodos();
        todosAlunos = FXCollections.observableArrayList(listaBanco);
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
                
                ObservableList<Aluno> filtrados = FXCollections.observableArrayList();
                for (Aluno a : todosAlunos) {
                    if (a.getNome().toLowerCase().contains(novo.toLowerCase()) || a.getCpf().contains(novo)) {
                        filtrados.add(a);
                    }
                }
                comboBuscaAluno.setItems(filtrados);
                comboBuscaAluno.show();
            }
        });
    }

    private void mostrarDetalhesAluno(Aluno aluno) {
        EventBus.emit("Desktop", "AnaliseAlunoController.mostrarDetalhes", "alunoId=" + aluno.getId());
        labelNomeAluno.setText(aluno.getNome());
        atualizarLabelsMedidas(aluno);
        
        labelFichaAtiva.setText(treinoDAO.buscarNomeFichaAtiva(aluno.getId()));
        labelUltimoTreino.setText(treinoDAO.buscarDataUltimoTreino(aluno.getId()));
        labelTreinosMes.setText(String.valueOf(treinoDAO.buscarQuantidadeTreinosMes(aluno.getId())));
        
        List<String> exerciciosDoAluno = treinoDAO.buscarNomesExerciciosPorAluno(aluno.getId());
        if (exerciciosDoAluno.isEmpty()) {
            comboExercicioGrafico.setItems(FXCollections.observableArrayList("Nenhum treino cadastrado"));
            comboExercicioGrafico.setDisable(true);
        } else {
            comboExercicioGrafico.setItems(FXCollections.observableArrayList(exerciciosDoAluno));
            comboExercicioGrafico.setDisable(false);
        }
        
        renderizarGraficoPesoImcReal(aluno);
        gerarAlertas(aluno);
        
        // ====================================================================
        // HISTÓRICO: mescla treinos realizados + feedbacks
        // ====================================================================
        treinoDAO.marcarComentariosComoLidos(aluno.getId());

        List<ItemHistorico> historico = new ArrayList<>();

        for (com.mycompany.academia.treino.model.ComentarioTreino c : treinoDAO.buscarComentariosPorAluno(aluno.getId())) {
            historico.add(new ItemHistorico(
                c.getDataCriacao(), "feedback", c.getTreino().getNome(),
                c.getTexto(), c.isLido(), c, null, c.getAluno(), c.getTreino()
            ));
        }

        for (com.mycompany.academia.core.session.SessaoTreino s : treinoDAO.buscarSessoesPorAluno(aluno.getId())) {
            com.mycompany.academia.treino.model.Treino t = s.getProgramacaoTreino().getTreino();
            String desc = "Treino realizado";
            if (s.getData() != null) {
                long dias = java.time.temporal.ChronoUnit.DAYS.between(s.getData().toLocalDate(), LocalDate.now());
                if (dias == 0) desc = "Treino realizado hoje";
                else if (dias == 1) desc = "Treino realizado ontem";
                else desc = "Treino realizado";
            }
            historico.add(new ItemHistorico(
                s.getData(), "treino", t.getNome(),
                desc, true, null, s, aluno, t
            ));
        }

        historico.sort((a, b) -> b.data.compareTo(a.data));

        listaComentarios.getItems().clear();
        listaComentarios.setItems(FXCollections.observableArrayList(historico));
    }

    private void atualizarLabelsMedidas(Aluno aluno) {
        float imc = aluno.getImc();
        float altura = aluno.getAltura();

        // 1. Descobrindo a Classificação Oficial da OMS
        String classificacao = "";
        if (imc > 0 && imc < 18.5) classificacao = "(Abaixo do Peso)";
        else if (imc >= 18.5 && imc <= 24.9) classificacao = "(Peso Ideal)";
        else if (imc >= 25.0 && imc <= 29.9) classificacao = "(Sobrepeso)";
        else if (imc >= 30.0 && imc <= 34.9) classificacao = "(Obesidade Grau I)";
        else if (imc >= 35.0 && imc <= 39.9) classificacao = "(Obesidade Grau II)";
        else if (imc >= 40.0) classificacao = "(Obesidade Grau III)";

        // 2. Calculando a faixa de peso ideal exclusivamente para a altura deste aluno
        float pesoIdealMin = 18.5f * (altura * altura);
        float pesoIdealMax = 24.9f * (altura * altura);

        // 3. Atualizando a interface com os dados completos e inteligentes
        if (aluno.getPeso() > 0) {
            labelImcAluno.setText(String.format("Peso: %.1f kg | Altura: %.2f m | IMC: %.1f %s | Alvo Ideal: %.1f kg a %.1f kg", 
                    aluno.getPeso(), altura, imc, classificacao, pesoIdealMin, pesoIdealMax));
        } else {
            // Se o aluno for recém-cadastrado e não tiver peso nenhum
            labelImcAluno.setText("Aluno sem medidas registradas. Realize a primeira avaliação física.");
        }
    }

    // =================================================================
    // GRÁFICO 1: PESO + IMC REAL DO BANCO DE DADOS
    // =================================================================
    private void renderizarGraficoPesoImcReal(Aluno aluno) {
        graficoEvolucao.getData().clear();
        ((CategoryAxis) graficoEvolucao.getXAxis()).getCategories().clear(); 

        List<AvaliacaoFisica> historico = alunoDAO.buscarAvaliacoesPorAluno(aluno.getId());

        XYChart.Series<String, Number> seriesPeso = new XYChart.Series<>();
        seriesPeso.setName("Peso Corporal (kg)");

        XYChart.Series<String, Number> seriesImc = new XYChart.Series<>();
        seriesImc.setName("Índice de IMC");

        DateTimeFormatter formatador = DateTimeFormatter.ofPattern("dd/MM");

        for (AvaliacaoFisica avaliacao : historico) {
            String dataEixoX = avaliacao.getDataAvaliacao().format(formatador);
            seriesPeso.getData().add(new XYChart.Data<>(dataEixoX, avaliacao.getPeso()));
            seriesImc.getData().add(new XYChart.Data<>(dataEixoX, avaliacao.getImc()));
        }

        // ====================================================================
        // UPDATED: Só plota o ponto inicial se o aluno possuir peso (Evita o bug pós-exclusão)
        // ====================================================================
        if (historico.isEmpty() && aluno.getPeso() > 0) {
            String hoje = LocalDate.now().format(formatador);
            seriesPeso.getData().add(new XYChart.Data<>(hoje, aluno.getPeso()));
            seriesImc.getData().add(new XYChart.Data<>(hoje, aluno.getImc()));
            graficoEvolucao.getData().addAll(seriesPeso, seriesImc);
            configurarTooltips(seriesPeso, " kg");
            configurarTooltips(seriesImc, " IMC");
        } else if (!historico.isEmpty()) {
            // Se tem histórico real, adiciona normalmente
            graficoEvolucao.getData().addAll(seriesPeso, seriesImc);
            configurarTooltips(seriesPeso, " kg");
            configurarTooltips(seriesImc, " IMC");
        }
        // Se cair no else (historico vazio e peso 0), o gráfico não recebe nada e fica limpo!
    }

    // =================================================================
    // GRÁFICO 2: PROGRESSÃO DE CARGA POR EXERCÍCIO SELECIONADO
    // =================================================================
    private void renderizarGraficoCargaExercicios(Aluno aluno, String nomeExercicio) {
        graficoCargas.getData().clear();
        ((CategoryAxis) graficoCargas.getXAxis()).getCategories().clear();

        XYChart.Series<String, Number> seriesCarga = new XYChart.Series<>();
        seriesCarga.setName("Carga Máxima (kg) - " + nomeExercicio);

        List<com.mycompany.academia.treino.model.ItemRealizado> historico = treinoDAO.buscarHistoricoCargas(aluno.getId(), nomeExercicio);
        java.time.format.DateTimeFormatter formatador = java.time.format.DateTimeFormatter.ofPattern("dd/MM");

        Map<String, com.mycompany.academia.core.session.SessaoTreino> mapaSessoes = new HashMap<>();

        for (com.mycompany.academia.treino.model.ItemRealizado ir : historico) {
            String dataEixoX = ir.getSessaoTreino().getData().format(formatador);
            mapaSessoes.putIfAbsent(dataEixoX, ir.getSessaoTreino());
            XYChart.Data<String, Number> ponto = new XYChart.Data<>(dataEixoX, ir.getCargaUtilizada());
            seriesCarga.getData().add(ponto);
        }

        if (!historico.isEmpty()) {
            graficoCargas.getData().add(seriesCarga);

            for (XYChart.Data<String, Number> data : seriesCarga.getData()) {
                Tooltip tooltip = new Tooltip(data.getYValue() + " kg");
                tooltip.setShowDelay(Duration.ZERO);
                Tooltip.install(data.getNode(), tooltip);
                data.getNode().setOnMouseEntered(e -> data.getNode().setStyle("-fx-scale-x: 1.4; -fx-scale-y: 1.4;"));
                data.getNode().setOnMouseExited(e -> data.getNode().setStyle("-fx-scale-x: 1; -fx-scale-y: 1;"));
                com.mycompany.academia.core.session.SessaoTreino sessao = mapaSessoes.get(data.getXValue());
                if (sessao != null) {
                    data.getNode().setOnMouseClicked(e -> abrirModalDetalhesTreino(sessao));
                }
            }
        }
    }

    private void configurarTooltips(XYChart.Series<String, Number> series, String sufixo) {
        for (XYChart.Data<String, Number> data : series.getData()) {
            Tooltip tooltip = new Tooltip(data.getYValue() + sufixo);
            tooltip.setShowDelay(Duration.ZERO);
            Tooltip.install(data.getNode(), tooltip);
            data.getNode().setOnMouseEntered(e -> data.getNode().setStyle("-fx-scale-x: 1.4; -fx-scale-y: 1.4;"));
            data.getNode().setOnMouseExited(e -> data.getNode().setStyle("-fx-scale-x: 1; -fx-scale-y: 1;"));
        }
    }

    @FXML
    void clicouAtualizarMedidas(ActionEvent event) {
        if (alunoSelecionado == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Gerenciamento de Avaliações Físicas");
        dialog.setHeaderText("Métricas Corporais - " + alunoSelecionado.getNome().split(" ")[0]);

        // Criamos os 3 botões (O Excluir fica alinhado à esquerda)
        ButtonType btnSalvar = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnExcluir = new ButtonType("Excluir", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(btnExcluir, btnSalvar, ButtonType.CANCEL);

        // Ocultamos o botão Excluir por padrão (ele só vai aparecer se o modo for "Editar")
        Node botaoExcluir = dialog.getDialogPane().lookupButton(btnExcluir);
        botaoExcluir.setVisible(false);
        botaoExcluir.setStyle("-fx-base: #e74c3c; -fx-text-fill: white;"); // Deixa ele vermelho!

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 50, 10, 10));

        ComboBox<String> comboModo = new ComboBox<>(FXCollections.observableArrayList("Nova Avaliação", "Editar Lançamento Anterior"));
        comboModo.setValue("Nova Avaliação");

        ComboBox<AvaliacaoFisica> comboAvaliacoesAnteriores = new ComboBox<>();
        comboAvaliacoesAnteriores.setDisable(true);
        
        List<AvaliacaoFisica> historico = alunoDAO.buscarAvaliacoesPorAluno(alunoSelecionado.getId());
        comboAvaliacoesAnteriores.setItems(FXCollections.observableArrayList(historico));
        comboAvaliacoesAnteriores.setConverter(new StringConverter<AvaliacaoFisica>() {
            @Override public String toString(AvaliacaoFisica af) { 
                return af == null ? "" : af.getDataAvaliacao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " (" + af.getPeso() + "kg)"; 
            }
            @Override public AvaliacaoFisica fromString(String str) { return null; }
        });

        TextField txtPeso = new TextField(String.valueOf(alunoSelecionado.getPeso()));
        TextField txtAltura = new TextField(String.valueOf(alunoSelecionado.getAltura()));
        DatePicker pickerData = new DatePicker(LocalDate.now());

        // TRAVA DO CALENDÁRIO: Desabilita qualquer dia que seja maior que hoje
        pickerData.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate hoje = LocalDate.now();
                setDisable(empty || date.compareTo(hoje) > 0);
            }
        });

        comboModo.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            boolean modoEdicao = novo.equals("Editar Lançamento Anterior");
            comboAvaliacoesAnteriores.setDisable(!modoEdicao);
            pickerData.setDisable(modoEdicao); 
            botaoExcluir.setVisible(modoEdicao); // O botão de apagar acende aqui!
        });

        comboAvaliacoesAnteriores.getSelectionModel().selectedItemProperty().addListener((obs, antigo, selec) -> {
            if (selec != null) {
                txtPeso.setText(String.valueOf(selec.getPeso()));
                txtAltura.setText(String.valueOf(selec.getAltura()));
                pickerData.setValue(selec.getDataAvaliacao());
            }
        });

        grid.add(new Label("Operação:"), 0, 0);
        grid.add(comboModo, 1, 0);
        grid.add(new Label("Selecionar Registro:"), 0, 1);
        grid.add(comboAvaliacoesAnteriores, 1, 1);
        grid.add(new Label("Peso (kg):"), 0, 2);
        grid.add(txtPeso, 1, 2);
        grid.add(new Label("Altura (m):"), 0, 3);
        grid.add(txtAltura, 1, 3);
        grid.add(new Label("Data:"), 0, 4);
        grid.add(pickerData, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(resposta -> {
            try {
                if (resposta == btnExcluir) {
                    AvaliacaoFisica editada = comboAvaliacoesAnteriores.getSelectionModel().getSelectedItem();
                    if (editada == null) throw new IllegalArgumentException("Selecione qual avaliação deseja apagar.");
                    
                    alunoDAO.deletarAvaliacaoFisica(editada);
                    
                    List<AvaliacaoFisica> restante = alunoDAO.buscarAvaliacoesPorAluno(alunoSelecionado.getId());
                    if (!restante.isEmpty()) {
                        AvaliacaoFisica ultima = restante.get(restante.size() - 1);
                        alunoSelecionado.setPeso(ultima.getPeso());
                        alunoSelecionado.setAltura(ultima.getAltura());
                        alunoSelecionado.setImc(ultima.getImc());
                    } else {
                        // ====================================================
                        // NOVO: Se apagou a última medida do banco, zera o modelo
                        // ====================================================
                        alunoSelecionado.setPeso(0);
                        alunoSelecionado.setAltura(0);
                        alunoSelecionado.setImc(0);
                    }
                    alunoDAO.salvarOuAtualizar(alunoSelecionado);
                    
                    atualizarLabelsMedidas(alunoSelecionado);
                    renderizarGraficoPesoImcReal(alunoSelecionado);
                    return; 
                }

                if (resposta == btnSalvar) {
                    float novoPeso = Float.parseFloat(txtPeso.getText());
                    float novaAltura = Float.parseFloat(txtAltura.getText());
                    float novoImc = novoPeso / (novaAltura * novaAltura);
                    
                    if (comboModo.getValue().equals("Nova Avaliação")) {
                        LocalDate dataAvaliacao = pickerData.getValue();
                        if (dataAvaliacao == null) throw new IllegalArgumentException("A data não pode estar vazia.");
                        if (dataAvaliacao.isAfter(LocalDate.now())) throw new IllegalArgumentException("Você não pode registrar uma avaliação no futuro."); // Dupla checagem de segurança

                        AvaliacaoFisica nova = new AvaliacaoFisica(alunoSelecionado, novoPeso, novaAltura, novoImc, dataAvaliacao);
                        alunoDAO.salvarAvaliacaoFisica(nova);
                    } else {
                        AvaliacaoFisica editada = comboAvaliacoesAnteriores.getSelectionModel().getSelectedItem();
                        if (editada == null) throw new IllegalArgumentException("Selecione qual avaliação deseja corrigir.");

                        editada.setPeso(novoPeso);
                        editada.setAltura(novaAltura);
                        editada.setImc(novoImc);
                        alunoDAO.atualizarAvaliacaoFisica(editada);
                    }

                    alunoSelecionado.setPeso(novoPeso);
                    alunoSelecionado.setAltura(novaAltura);
                    alunoSelecionado.setImc(novoImc);
                    alunoDAO.salvarOuAtualizar(alunoSelecionado);

                    atualizarLabelsMedidas(alunoSelecionado);
                    renderizarGraficoPesoImcReal(alunoSelecionado);
                }
            } catch (Exception e) {
                Alert erro = new Alert(Alert.AlertType.ERROR);
                erro.setTitle("Erro na Operação");
                erro.setHeaderText("Falha ao processar");
                erro.setContentText(e.getMessage());
                erro.showAndWait();
            }
        });
    }

    private void abrirModalDetalhesTreino(com.mycompany.academia.core.session.SessaoTreino sessao) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DetalhesTreinoRealizado.fxml"));
            Parent root = loader.load();

            DetalhesTreinoRealizadoController controller = loader.getController();
            com.mycompany.academia.treino.model.Treino treino = sessao.getProgramacaoTreino().getTreino();
            controller.carregarDadosReais(
                alunoSelecionado,
                treino,
                sessao.getData(),
                null
            );

            Stage modal = new Stage();
            modal.setTitle("Detalhes da Execução do Treino - " + treino.getNome());
            modal.setScene(new Scene(root));
            modal.setResizable(false);
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.showAndWait();
        } catch (Exception e) {
            System.err.println("Erro crítico ao abrir modal:");
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText("Não foi possível abrir os detalhes do treino.");
            alert.setContentText(e.getCause() != null ? e.getCause().toString() : e.toString());
            alert.showAndWait();
        }
    }

    private void abrirModalDetalhesTreino(com.mycompany.academia.treino.model.ComentarioTreino comentario) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DetalhesTreinoRealizado.fxml"));
            Parent root = loader.load();

            DetalhesTreinoRealizadoController controller = loader.getController();
            // É nesta linha abaixo que o sistema processa os cálculos de carga e histórico
            controller.carregarDadosReais(comentario); 

            Stage modal = new Stage();
            modal.setTitle("Detalhes da Execução do Treino");
            modal.setScene(new Scene(root));
            modal.setResizable(false);
            modal.initModality(Modality.APPLICATION_MODAL); 
            modal.showAndWait();
            
            // Força a atualização da lista após fechar a janela
            if (alunoSelecionado != null) {
                mostrarDetalhesAluno(alunoSelecionado);
            }
        } catch (Exception e) {
            // =================================================================
            // MODO INVESTIGAÇÃO: Força o erro oculto a aparecer na tela!
            // =================================================================
            System.err.println("Erro crítico capturado ao tentar abrir o modal:");
            e.printStackTrace(); // Imprime o rastro no console do NetBeans
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Modo Investigação - Bug Capturado");
            alert.setHeaderText("Um erro oculto impediu a tela de abrir!");
            
            // Pega a mensagem exata de erro que o Java tentou esconder
            String causa = e.getCause() != null ? e.getCause().toString() : e.toString();
            alert.setContentText("Motivo da falha:\n" + causa + "\n\nOlhe o console (Output) do NetBeans para ver a linha exata do código onde isso quebrou.");
            alert.showAndWait();
        }
    }

    private void gerarAlertas(Aluno aluno) {
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

        List<com.mycompany.academia.treino.model.Treino> fichasPadrao = treinoDAO.listarFichasPadrao();
        if (fichasPadrao.isEmpty()) {
            Alert aviso = new Alert(Alert.AlertType.WARNING);
            aviso.setTitle("Nenhuma Ficha Padrão");
            aviso.setHeaderText("Não há fichas padrão cadastradas.");
            aviso.setContentText("Crie uma ficha padrão primeiro em Treinos > Nova Ficha.");
            aviso.showAndWait();
            return;
        }

        ChoiceDialog<com.mycompany.academia.treino.model.Treino> dialog = new ChoiceDialog<>(fichasPadrao.get(0), fichasPadrao);
        dialog.setTitle("Trocar Ficha de Treino");
        dialog.setHeaderText("Selecione a nova ficha para " + alunoSelecionado.getNome());
        dialog.setContentText("Ficha:");

        dialog.showAndWait().ifPresent(ficha -> {
            com.mycompany.academia.treino.model.ProgramacaoTreino programacao = new com.mycompany.academia.treino.model.ProgramacaoTreino();
            programacao.setAluno(alunoSelecionado);
            programacao.setTreino(ficha);
            programacao.setDataInicioSemanas(LocalDateTime.now());
            programacao.setDataFimSemanas(LocalDateTime.now().plusWeeks(4));

            boolean ok = treinoDAO.salvarProgramacao(programacao);
            if (ok) {
                Alert sucesso = new Alert(Alert.AlertType.INFORMATION);
                sucesso.setTitle("Ficha Trocada");
                sucesso.setHeaderText("Programação salva com sucesso!");
                sucesso.setContentText("Ficha \"" + ficha.getNome() + "\" atribuída de "
                    + java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy").format(LocalDate.now())
                    + " até "
                    + java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy").format(LocalDate.now().plusWeeks(4)));
                sucesso.showAndWait();
                mostrarDetalhesAluno(alunoSelecionado);
            } else {
                Alert erro = new Alert(Alert.AlertType.ERROR);
                erro.setTitle("Erro");
                erro.setHeaderText("Falha ao salvar programação.");
                erro.showAndWait();
            }
        });
    }

    private Label criarEtiquetaAlerta(String texto, String corTexto, String corFundo) {
        Label etiqueta = new Label(texto);
        etiqueta.setStyle("-fx-text-fill: " + corTexto + "; -fx-background-color: " + corFundo + "; -fx-padding: 8px 12px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-font-size: 13px;");
        etiqueta.setMaxWidth(Double.MAX_VALUE);
        return etiqueta;
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