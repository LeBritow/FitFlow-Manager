package com.mycompany.academia.aluno.ui;

import com.mycompany.academia.aluno.dao.AlunoDAO;
import com.mycompany.academia.aluno.model.Aluno;
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
import java.util.List;
import java.util.Random;
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
    @FXML private ListView<String> listaComentarios;
    @FXML private ComboBox<String> comboExercicioGrafico;

    private AlunoDAO alunoDAO = new AlunoDAO();
    private com.mycompany.academia.treino.dao.TreinoDAO treinoDAO = new com.mycompany.academia.treino.dao.TreinoDAO(); 
    private Aluno alunoSelecionado;
    private ObservableList<Aluno> todosAlunos;

    @FXML
    public void initialize() {
        graficoEvolucao.setAnimated(false);
        graficoCargas.setAnimated(false);
        configurarFiltroBusca();
        
        // Ouvinte para a seleção do ComboBox superior de Alunos
        comboBuscaAluno.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) {
                alunoSelecionado = novo;
                painelDireito.setDisable(false);
                mostrarDetalhesAluno(novo);
            }
        });

        // NOVO: Ouvinte que redesenha o gráfico de carga quando o instrutor muda o exercício
        comboExercicioGrafico.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null && alunoSelecionado != null) {
                renderizarGraficoCargaExercicios(alunoSelecionado, novo);
            }
        });

        // Ouvinte de duplo clique no histórico de feedbacks
        listaComentarios.setOnMouseClicked((MouseEvent event) -> {
            if (event.getClickCount() == 2) { 
                String itemSelecionado = listaComentarios.getSelectionModel().getSelectedItem();
                if (itemSelecionado != null) {
                    abrirModalDetalhesTreino(itemSelecionado);
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
        labelNomeAluno.setText(aluno.getNome());
        atualizarLabelsMedidas(aluno);
        
        // ====================================================================
        // CARDS REAIS: Substituindo os dados fakes pelas consultas do DAO
        // ====================================================================
        labelFichaAtiva.setText(treinoDAO.buscarNomeFichaAtiva(aluno.getId()));
        labelUltimoTreino.setText(treinoDAO.buscarDataUltimoTreino(aluno.getId()));
        labelTreinosMes.setText(String.valueOf(treinoDAO.buscarQuantidadeTreinosMes(aluno.getId())));
        // ====================================================================
        
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
        
        listaComentarios.getItems().clear();
        List<String> feedbacksReais = treinoDAO.buscarComentariosPorAluno(aluno.getId());
        if (feedbacksReais.isEmpty()) {
            listaComentarios.getItems().add("Nenhum feedback registrado para este aluno ainda.");
        } else {
            listaComentarios.setItems(FXCollections.observableArrayList(feedbacksReais));
        }
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

        // Simulação inteligente que será trocada pelo SELECT do ItemRealizado no futuro
        if (nomeExercicio.contains("Supino")) {
            seriesCarga.getData().add(new XYChart.Data<>("05/05", 50));
            seriesCarga.getData().add(new XYChart.Data<>("12/05", 50));
            seriesCarga.getData().add(new XYChart.Data<>("19/05", 54));
            seriesCarga.getData().add(new XYChart.Data<>("26/05", 54));
        } else if (nomeExercicio.contains("Leg Press")) {
            seriesCarga.getData().add(new XYChart.Data<>("05/05", 160));
            seriesCarga.getData().add(new XYChart.Data<>("12/05", 180));
            seriesCarga.getData().add(new XYChart.Data<>("19/05", 180));
            seriesCarga.getData().add(new XYChart.Data<>("26/05", 200));
        } else {
            seriesCarga.getData().add(new XYChart.Data<>("05/05", 30));
            seriesCarga.getData().add(new XYChart.Data<>("26/05", 35));
        }

        graficoCargas.getData().add(seriesCarga);
        configurarTooltips(seriesCarga, " kg");
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

    private void abrirModalDetalhesTreino(String itemSelecionado) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DetalhesTreinoRealizado.fxml"));
            Parent root = loader.load();

            DetalhesTreinoRealizadoController controller = loader.getController();
            String tituloDinamico = itemSelecionado.split("-")[0].trim();
            controller.carregarDadosMockados(tituloDinamico);

            Stage modal = new Stage();
            modal.setTitle("Detalhes da Execução do Treino");
            modal.setScene(new Scene(root));
            modal.setResizable(false);
            modal.initModality(Modality.APPLICATION_MODAL); 
            modal.showAndWait();
        } catch (IOException e) {
            System.err.println("Erro ao abrir a tela de Detalhes do Treino.");
            e.printStackTrace();
        }
    }

    private void gerarAlertas(Aluno aluno) {
        caixaAlertas.getChildren().clear();
        Random rand = new Random();
        if (rand.nextBoolean()) {
            caixaAlertas.getChildren().add(criarEtiquetaAlerta("⚠️ Ficha Vencida: A ficha atual tem mais de 30 dias. Considere atualizar o treino.", "#d35400", "#fdebd0"));
        }
        if (rand.nextBoolean()) {
            caixaAlertas.getChildren().add(criarEtiquetaAlerta("🚨 Ausência: O aluno não registra treinos há mais de 7 dias.", "#c0392b", "#fadbd8"));
        }
        if (caixaAlertas.getChildren().isEmpty()) {
            caixaAlertas.getChildren().add(criarEtiquetaAlerta("✅ Desempenho excelente! Nenhuma pendência encontrada.", "#27ae60", "#d5f5e3"));
        }
    }

    private Label criarEtiquetaAlerta(String texto, String corTexto, String corFundo) {
        Label etiqueta = new Label(texto);
        etiqueta.setStyle("-fx-text-fill: " + corTexto + "; -fx-background-color: " + corFundo + "; -fx-padding: 8px 12px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-font-size: 13px;");
        etiqueta.setMaxWidth(Double.MAX_VALUE);
        return etiqueta;
    }
    
    public List<String> buscarNomesExerciciosPorAluno(Long alunoId) {
        jakarta.persistence.EntityManager em = com.mycompany.academia.core.config.JPAUtil.getEntityManager();
        try {
            // ATENÇÃO: A string JPQL abaixo pressupõe que o seu 'ItemTreino' tem um relacionamento com 'Exercicio' e com 'Treino', e que 'Treino' tem relacionamento com 'Aluno'.
            // Se os nomes dos seus atributos na classe forem diferentes, basta ajustar os nomes após o JOIN.
            String jpql = "SELECT DISTINCT e.nome FROM ItemTreino it " +
                          "JOIN it.exercicio e " +
                          "JOIN it.treino t " +
                          "WHERE t.aluno.id = :alunoId";
                          
            jakarta.persistence.TypedQuery<String> query = em.createQuery(jpql, String.class);
            query.setParameter("alunoId", alunoId);
            return query.getResultList();
        } catch (Exception e) {
            System.err.println("Erro ao buscar exercícios do aluno: " + e.getMessage());
            e.printStackTrace();
            return java.util.Collections.emptyList(); // Retorna lista vazia para não quebrar a tela
        } finally {
            em.close();
        }
    }
}