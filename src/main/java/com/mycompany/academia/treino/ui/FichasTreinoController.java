package com.mycompany.academia.treino.ui;

import com.mycompany.academia.core.config.EventBus;
import com.mycompany.academia.core.util.TableUtils;
import com.mycompany.academia.treino.dao.ExercicioDAO;
import com.mycompany.academia.treino.dao.TreinoDAO;
import com.mycompany.academia.aluno.model.Aluno;
import com.mycompany.academia.treino.model.Exercicio;
import com.mycompany.academia.treino.model.ItemTreino;
import com.mycompany.academia.treino.model.ProgramacaoTreino;
import com.mycompany.academia.treino.model.SerieTreino;
import com.mycompany.academia.treino.model.Treino;
import com.mycompany.academia.treino.enums.ObjetivoTreino; // 1. IMPORT DO ENUM

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import javafx.util.converter.FloatStringConverter;

public class FichasTreinoController {

    @FXML private ComboBox<Treino> comboTemplates;
    @FXML private ComboBox<Aluno> comboAlunos;
    @FXML private ComboBox<ProgramacaoTreino> comboTreinosExistentes;
    @FXML private TextField campoNomeTreino;
    @FXML private ComboBox<ObjetivoTreino> comboObjetivo; // 2. TROCADO DE TEXTFIELD PARA COMBOBOX
    @FXML private CheckBox checkFichaPadrao;
    
    @FXML private TableView<Exercicio> tabelaCatalogo;
    @FXML private TableView<ItemTreino> tabelaFicha;
    
    @FXML private TableColumn<ItemTreino, String> colunaExercicioFicha;
    @FXML private TableColumn<ItemTreino, Integer> colunaSeries;
    @FXML private TableColumn<ItemTreino, String> colunaReps;
    @FXML private TableColumn<ItemTreino, String> colunaCarga;
    @FXML private TableColumn<ItemTreino, Float> colunaDescanso;

    private TreinoDAO treinoDAO = new TreinoDAO();
    private ExercicioDAO exercicioDAO = new ExercicioDAO();
    
    private ObservableList<Exercicio> listaCatalogo;
    private ObservableList<ItemTreino> listaFicha;
    
    private Treino treinoEmEdicao = null;

    @FXML
    public void initialize() {
        EventBus.emit("Desktop", "FichasTreinoController.listarProgramacoes", "Carregando fichas de treino");
        carregarAlunos();
        carregarTemplates();
        
        // 3. POPULAR O COMBOBOX DE OBJETIVOS
        comboObjetivo.setItems(FXCollections.observableArrayList(ObjetivoTreino.values()));
        
        listaCatalogo = FXCollections.observableArrayList(exercicioDAO.listarTodos());
        tabelaCatalogo.setItems(listaCatalogo);
        TableUtils.autoFitColumns(tabelaCatalogo);
        
        listaFicha = FXCollections.observableArrayList();
        tabelaFicha.setItems(listaFicha);
        TableUtils.autoFitColumns(tabelaFicha);
        
        tabelaFicha.setEditable(true);
        configurarColunasDaFicha();

        comboAlunos.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novoAluno) -> atualizarComboTreinosDoAluno(novoAluno));
        comboTreinosExistentes.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novaProg) -> carregarTreinoParaEdicao(novaProg));
        comboTemplates.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novoTemplate) -> importarTemplate(novoTemplate));
    }

    private void carregarAlunos() {
        comboAlunos.setItems(FXCollections.observableArrayList(treinoDAO.listarAlunos()));
        comboAlunos.setConverter(new StringConverter<Aluno>() {
            @Override public String toString(Aluno a) { return a != null ? a.getNome() : ""; }
            @Override public Aluno fromString(String s) { return null; }
        });
        comboTreinosExistentes.setConverter(new StringConverter<ProgramacaoTreino>() {
            @Override public String toString(ProgramacaoTreino p) { return p != null ? p.getTreino().getNome() : ""; }
            @Override public ProgramacaoTreino fromString(String s) { return null; }
        });
    }

    private void carregarTemplates() {
        comboTemplates.setItems(FXCollections.observableArrayList(treinoDAO.listarFichasPadrao()));
        comboTemplates.setConverter(new StringConverter<Treino>() {
            @Override public String toString(Treino t) { return t != null ? t.getNome() : ""; }
            @Override public Treino fromString(String s) { return null; }
        });
    }

    private void atualizarComboTreinosDoAluno(Aluno aluno) {
        if (aluno != null) {
            List<ProgramacaoTreino> progs = treinoDAO.listarProgramacoesPorAluno(aluno.getId());
            comboTreinosExistentes.setItems(FXCollections.observableArrayList(progs));
            checkFichaPadrao.setSelected(false);
        } else {
            comboTreinosExistentes.getItems().clear();
        }
    }

    private void carregarTreinoParaEdicao(ProgramacaoTreino prog) {
        if (prog != null && prog.getTreino() != null) {
            treinoEmEdicao = prog.getTreino();
            campoNomeTreino.setText(treinoEmEdicao.getNome());
            comboObjetivo.setValue(treinoEmEdicao.getObjetivo()); // SETAR VALOR DO ENUM
            checkFichaPadrao.setSelected(false);
            listaFicha.setAll(treinoDAO.listarItensPorTreino(treinoEmEdicao.getId()));
        }
    }

    private void importarTemplate(Treino template) {
        if (template == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Opções de Template");
        alert.setHeaderText("Ficha: " + template.getNome());
        alert.setContentText("O que deseja fazer com esta ficha padrão?");
        ButtonType btnImportar = new ButtonType("Importar Cópia para Aluno");
        ButtonType btnEditar = new ButtonType("Editar Template Original");
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnImportar, btnEditar, btnCancelar);
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == btnImportar) {
            treinoEmEdicao = null; 
            campoNomeTreino.setText(template.getNome());
            comboObjetivo.setValue(template.getObjetivo()); // SETAR VALOR DO ENUM
            checkFichaPadrao.setSelected(false); 
            listaFicha.clear();
            
            List<ItemTreino> itensTemplate = treinoDAO.listarItensPorTreino(template.getId());
            for (ItemTreino it : itensTemplate) {
                ItemTreino novoItem = new ItemTreino();
                novoItem.setExercicio(it.getExercicio());
                novoItem.setIntervaloDescanso(it.getIntervaloDescanso());
                novoItem.setProgressaoCarga(it.isProgressaoCarga());
                
                for (SerieTreino sOrig : it.getSeriesTreino()) {
                    SerieTreino novaSerie = new SerieTreino();
                    novaSerie.setNumeroDaSerie(sOrig.getNumeroDaSerie());
                    novaSerie.setRepeticoes(sOrig.getRepeticoes());
                    novaSerie.setCarga(sOrig.getCarga());
                    novoItem.adicionarSerie(novaSerie);
                }
                listaFicha.add(novoItem);
            }
        } else if (result.isPresent() && result.get() == btnEditar) {
            treinoEmEdicao = template;
            campoNomeTreino.setText(template.getNome());
            comboObjetivo.setValue(template.getObjetivo()); // SETAR VALOR DO ENUM
            checkFichaPadrao.setSelected(true); 
            comboAlunos.getSelectionModel().clearSelection(); 
            listaFicha.clear();
            listaFicha.setAll(treinoDAO.listarItensPorTreino(template.getId()));
        } else {
            Platform.runLater(() -> comboTemplates.getSelectionModel().clearSelection());
        }
    }

    @FXML
    void clicouGeradorTreino(ActionEvent event) {
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle("Gerador Inteligente");
        dialog.setHeaderText("Configure os parâmetros exatos do treino");

        ButtonType btnGerar = new ButtonType("Gerar Treino", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGerar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        ComboBox<String> comboFoco = new ComboBox<>(FXCollections.observableArrayList(
            "Peito e Tríceps", "Costas e Bíceps", "Pernas Completo", "Ombros e Abdômen", "Peito e Costas (Antagonista)", "Braços (Bíceps e Tríceps)"
        ));
        comboFoco.getSelectionModel().selectFirst();
        
        TextField campoQtd = new TextField("6");
        TextField campoSeries = new TextField("4");
        TextField campoReps = new TextField("15");
        
        CheckBox checkVariar = new CheckBox("Gostaria de séries e repetições alternadas?");
        CheckBox checkProgressao = new CheckBox("Aplicar método de Progressão de Carga (Pirâmide)?");

        Label lblSeries = new Label("Séries Base:");
        Label lblReps = new Label("Reps Base:");

        checkVariar.setOnAction(e -> {
            if (checkVariar.isSelected()) {
                lblSeries.setText("Séries Máx:"); lblReps.setText("Reps Máx:");
            } else {
                lblSeries.setText("Séries Base:"); lblReps.setText("Reps Base:");
            }
        });

        grid.add(new Label("Foco Muscular:"), 0, 0); grid.add(comboFoco, 1, 0);
        grid.add(new Label("Qtd. Exercícios:"), 0, 1); grid.add(campoQtd, 1, 1);
        grid.add(lblSeries, 0, 2); grid.add(campoSeries, 1, 2);
        grid.add(lblReps, 0, 3); grid.add(campoReps, 1, 3);
        grid.add(checkVariar, 0, 4, 2, 1);
        grid.add(checkProgressao, 0, 5, 2, 1);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(() -> campoQtd.requestFocus());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnGerar) {
                return Arrays.asList(
                    comboFoco.getValue(), campoQtd.getText(), campoSeries.getText(), campoReps.getText(),
                    String.valueOf(checkVariar.isSelected()), String.valueOf(checkProgressao.isSelected())
                );
            }
            return null;
        });

        Optional<List<String>> resultado = dialog.showAndWait();
        resultado.ifPresent(dados -> {
            try {
                String foco = dados.get(0);
                int qtd = Integer.parseInt(dados.get(1));
                int series = Integer.parseInt(dados.get(2));
                int reps = Integer.parseInt(dados.get(3));
                boolean variar = Boolean.parseBoolean(dados.get(4));
                boolean progredir = Boolean.parseBoolean(dados.get(5));
                
                if (qtd <= 0 || series <= 0 || reps <= 0) throw new NumberFormatException();
                
                montarFichaAutomatica(foco, qtd, series, reps, variar, progredir);
                
            } catch (NumberFormatException e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Por favor, digite apenas números inteiros maiores que zero.");
            }
        });
    }

    private void montarFichaAutomatica(String foco, int totalExercicios, int qtdSeries, int qtdReps, boolean variar, boolean progredir) {
        listaFicha.clear();
        Random rand = new Random();
        List<Exercicio> selecionados = new ArrayList<>();
        
        int qtdPrincipal = (int) Math.ceil(totalExercicios * 0.6); 
        int qtdSecundario = totalExercicios - qtdPrincipal;        
        int qtdMetade = totalExercicios / 2;
        int qtdOutraMetade = totalExercicios - qtdMetade;

        if (foco.equals("Peito e Tríceps")) {
            selecionados.addAll(sortearExercicios("Peitoral", qtdPrincipal));
            selecionados.addAll(sortearExercicios("Triceps", qtdSecundario));
        } else if (foco.equals("Costas e Bíceps")) {
            selecionados.addAll(sortearExercicios("Costas", qtdPrincipal));
            selecionados.addAll(sortearExercicios("Biceps", qtdSecundario));
        } else if (foco.equals("Pernas Completo")) {
            selecionados.addAll(sortearExercicios("Pernas", totalExercicios));
        } else if (foco.equals("Ombros e Abdômen")) {
            selecionados.addAll(sortearExercicios("Ombros", qtdMetade));
            selecionados.addAll(sortearExercicios("Abdomen", qtdOutraMetade));
        } else if (foco.equals("Peito e Costas (Antagonista)")) {
            selecionados.addAll(sortearExercicios("Peitoral", qtdMetade));
            selecionados.addAll(sortearExercicios("Costas", qtdOutraMetade));
        } else {
            selecionados.addAll(sortearExercicios("Biceps", qtdMetade));
            selecionados.addAll(sortearExercicios("Triceps", qtdOutraMetade));
        }
        
        for (Exercicio ex : selecionados) {
            ItemTreino item = new ItemTreino();
            item.setExercicio(ex);
            item.setProgressaoCarga(progredir);
            item.setIntervaloDescanso(45.0f + (rand.nextInt(4) * 5.0f));
            
            int baseCarga = 10 + rand.nextInt(21); 
            
            int seriesDoExercicio;
            int repsDoExercicio;
            
            if (variar) {
                seriesDoExercicio = Math.max(2, qtdSeries - rand.nextInt(3)); 
                int diferencaParaOMinimo = seriesDoExercicio - 2;
                int penalidadeReps = (diferencaParaOMinimo * 2) + rand.nextInt(2); 
                repsDoExercicio = Math.max(6, qtdReps - penalidadeReps);
            } else {
                seriesDoExercicio = qtdSeries;
                repsDoExercicio = qtdReps;
            }
            
            for (int i = 1; i <= seriesDoExercicio; i++) {
                SerieTreino serie = new SerieTreino();
                serie.setNumeroDaSerie(i);
                
                if (progredir) {
                    serie.setRepeticoes(Math.max(6, repsDoExercicio - ((i - 1) * 2)));
                    serie.setCarga(baseCarga + ((i - 1) * 2.0f));
                } else {
                    serie.setRepeticoes(repsDoExercicio);
                    serie.setCarga(baseCarga);
                }
                item.adicionarSerie(serie);
            }
            listaFicha.add(item);
        }
        
        campoNomeTreino.setText("Treino " + foco + " [" + totalExercicios + " Ex]");
    }

    private List<Exercicio> sortearExercicios(String group, int quantidade) {
        List<Exercicio> doGrupo = exercicioDAO.buscarPorGrupoMuscular(group);
        if (doGrupo.isEmpty()) return new ArrayList<>(); 
        Collections.shuffle(doGrupo);
        return doGrupo.subList(0, Math.min(quantidade, doGrupo.size()));
    } 
    
    private void configurarColunasDaFicha() {
        colunaExercicioFicha.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getExercicio().getNome()));
        colunaSeries.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getSeriesTreino().size()).asObject());
        
        colunaReps.setCellValueFactory(cellData -> {
            ItemTreino item = cellData.getValue();
            if (item.getSeriesTreino().isEmpty()) return new SimpleStringProperty("-");
            if (!item.isProgressaoCarga() && item.getSeriesTreino().get(0).getRepeticoes() == item.getSeriesTreino().get(item.getSeriesTreino().size()-1).getRepeticoes()) {
                return new SimpleStringProperty(String.valueOf(item.getSeriesTreino().get(0).getRepeticoes()));
            }
            StringBuilder reps = new StringBuilder();
            for (SerieTreino s : item.getSeriesTreino()) reps.append(s.getRepeticoes()).append("-");
            return new SimpleStringProperty(reps.substring(0, reps.length() - 1));
        });
        colunaReps.setCellFactory(TextFieldTableCell.forTableColumn());
        colunaReps.setOnEditCommit(event -> {
            ItemTreino item = event.getRowValue();
            String[] parts = event.getNewValue().split("-");
            
            if (parts.length == 1) {
                try {
                    int rep = Integer.parseInt(parts[0].trim());
                    for (SerieTreino s : item.getSeriesTreino()) s.setRepeticoes(rep);
                    item.setProgressaoCarga(false);
                } catch (Exception e) {}
            } else {
                item.setProgressaoCarga(true);
                for (int i = 0; i < Math.min(parts.length, item.getSeriesTreino().size()); i++) {
                    try { item.getSeriesTreino().get(i).setRepeticoes(Integer.parseInt(parts[i].trim())); } catch (Exception e) {}
                }
            }
            tabelaFicha.refresh();
        });

        colunaCarga.setCellValueFactory(cellData -> {
            ItemTreino item = cellData.getValue();
            if (item.getSeriesTreino().isEmpty()) return new SimpleStringProperty("-");
            if (!item.isProgressaoCarga()) {
                return new SimpleStringProperty(item.getSeriesTreino().get(0).getCarga() + "kg");
            }
            StringBuilder cargas = new StringBuilder();
            for (SerieTreino s : item.getSeriesTreino()) cargas.append(s.getCarga()).append("/");
            return new SimpleStringProperty(cargas.substring(0, cargas.length() - 1) + "kg");
        });
        colunaCarga.setCellFactory(TextFieldTableCell.forTableColumn());
        colunaCarga.setOnEditCommit(event -> {
            ItemTreino item = event.getRowValue();
            String limpo = event.getNewValue().replace("kg", "").trim();
            String[] parts = limpo.split("/");
            
            if (parts.length == 1) {
                try {
                    float carga = Float.parseFloat(parts[0].trim());
                    for (SerieTreino s : item.getSeriesTreino()) s.setCarga(carga);
                } catch (Exception e) {}
            } else {
                for (int i = 0; i < Math.min(parts.length, item.getSeriesTreino().size()); i++) {
                    try { item.getSeriesTreino().get(i).setCarga(Float.parseFloat(parts[i].trim())); } catch (Exception e) {}
                }
            }
            tabelaFicha.refresh();
        });

        colunaDescanso.setCellValueFactory(new PropertyValueFactory<>("intervaloDescanso"));
        colunaDescanso.setCellFactory(TextFieldTableCell.forTableColumn(new FloatStringConverter()));
        colunaDescanso.setOnEditCommit(event -> event.getRowValue().setIntervaloDescanso(event.getNewValue()));
    }

    @FXML void clicouNovoTreino(ActionEvent event) { limparEcra(); }
    
    @FXML void clicouDuplicar(ActionEvent event) {
        if (treinoEmEdicao == null || listaFicha.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Aviso", "Selecione um treino existente para copiar.");
            return;
        }
        treinoEmEdicao = null;
        comboTreinosExistentes.getSelectionModel().clearSelection();
        campoNomeTreino.setText(campoNomeTreino.getText() + " (Cópia)");
    }
    
    @FXML 
    void clicouExcluirTreino(ActionEvent event) {
        ProgramacaoTreino progSelecionada = comboTreinosExistentes.getSelectionModel().getSelectedItem();
        if (progSelecionada == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Aviso", "Selecione uma Ficha Pessoal do Aluno no topo para poder excluí-la.");
            return;
        }
        
        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacao.setTitle("Excluir Ficha");
        confirmacao.setHeaderText("Atenção: Você está prestes a apagar a ficha '" + progSelecionada.getTreino().getNome() + "'");
        confirmacao.setContentText("Tem certeza? O aluno não verá mais esse treino no aplicativo.");
        
        if (confirmacao.showAndWait().get() == ButtonType.OK) {
            if (treinoDAO.excluirProgramacao(progSelecionada)) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Ficha excluída com sucesso!");
                limparEcra();
                Aluno alunoAtual = comboAlunos.getValue();
                if (alunoAtual != null) atualizarComboTreinosDoAluno(alunoAtual);
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Não foi possível excluir a ficha do banco de dados.");
            }
        }
    }
    
    @FXML void adicionarNaFicha(ActionEvent event) {
        Exercicio selecionado = tabelaCatalogo.getSelectionModel().getSelectedItem();
        if (selecionado == null) return;
        
        ItemTreino novoItem = new ItemTreino();
        novoItem.setExercicio(selecionado);
        novoItem.setIntervaloDescanso(60.0f);
        novoItem.setProgressaoCarga(false);
        
        for(int i = 1; i <= 3; i++){
            SerieTreino serie = new SerieTreino();
            serie.setNumeroDaSerie(i);
            serie.setRepeticoes(12);
            serie.setCarga(10.0f);
            novoItem.adicionarSerie(serie);
        }
        listaFicha.add(novoItem);
    }
    
    @FXML void removerDaFicha(ActionEvent event) {
        ItemTreino selecionado = tabelaFicha.getSelectionModel().getSelectedItem();
        if (selecionado != null) listaFicha.remove(selecionado);
    }
    
    @FXML void salvarFichaCompleta(ActionEvent event) {
        boolean isTemplate = checkFichaPadrao.isSelected();
        Aluno alunoSelecionado = comboAlunos.getValue();
        String nomeTreino = campoNomeTreino.getText();
        
        if (nomeTreino.isEmpty() || listaFicha.isEmpty() || comboObjetivo.getValue() == null) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Preencha o Nome, adicione pelo menos um exercício e defina o Objetivo.");
            return;
        }
        if (!isTemplate && alunoSelecionado == null) {
            mostrarAlerta(Alert.AlertType.ERROR, "Aviso", "Para guardar um treino pessoal, selecione o Aluno Alvo no topo.");
            return;
        }
        
        Treino treino = (treinoEmEdicao != null) ? treinoEmEdicao : new Treino();
        treino.setNome(nomeTreino);
        treino.setObjetivo(comboObjetivo.getValue()); // 5. SALVAR O VALOR DO ENUM DIRETAMENTE
        treino.setFichaPadrao(isTemplate);
        
        Treino treinoSalvo = treinoDAO.salvarTreino(treino);
        if (treinoSalvo != null) {
            for (ItemTreino item : listaFicha) {
                if (treinoEmEdicao == null) item.setId(0); 
                item.setTreino(treinoSalvo);
                treinoDAO.salvarItemTreino(item);
            }
            if (!isTemplate && treinoEmEdicao == null) {
                ProgramacaoTreino prog = new ProgramacaoTreino();
                prog.setAluno(alunoSelecionado);
                prog.setTreino(treinoSalvo);
                prog.setDataInicioSemanas(LocalDateTime.now());
                prog.setDataFimSemanas(LocalDateTime.now().plusWeeks(4));
                prog.setDiaDaSemana("A definir");
                treinoDAO.salvarProgramacao(prog);
            }
            mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Ficha guardada com sucesso!");
            carregarTemplates(); 
            if (alunoSelecionado != null) atualizarComboTreinosDoAluno(alunoSelecionado);
            limparEcra();
        }
    }
    
    private void limparEcra() {
        treinoEmEdicao = null;
        campoNomeTreino.clear(); 
        comboObjetivo.getSelectionModel().clearSelection(); // 6. LIMPAR SELEÇÃO DO COMBO
        listaFicha.clear();
        comboAlunos.getSelectionModel().clearSelection();
        comboTreinosExistentes.getItems().clear();
        comboTemplates.getSelectionModel().clearSelection();
        checkFichaPadrao.setSelected(false);
    }
    
    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alert = new Alert(tipo); alert.setTitle(titulo); alert.setHeaderText(null); alert.setContentText(mensagem); alert.showAndWait();
    }
}