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
import com.mycompany.academia.treino.enums.ObjetivoTreino;

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
    @FXML private ComboBox<ObjetivoTreino> comboObjetivo;
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
            @Override public String toString(Aluno a) {
                if (a == null) return "";
                return a.getNome();
            }
            @Override public Aluno fromString(String s) { return null; }
        });
        comboTreinosExistentes.setConverter(new StringConverter<ProgramacaoTreino>() {
            @Override public String toString(ProgramacaoTreino p) {
                if (p == null) return "";
                Treino oTreinoP = p.getTreino();
                return oTreinoP.getNome();
            }
            @Override public ProgramacaoTreino fromString(String s) { return null; }
        });
    }

    private void carregarTemplates() {
        comboTemplates.setItems(FXCollections.observableArrayList(treinoDAO.listarFichasPadrao()));
        comboTemplates.setConverter(new StringConverter<Treino>() {
            @Override public String toString(Treino t) {
                if (t == null) return "";
                return t.getNome();
            }
            @Override public Treino fromString(String s) { return null; }
        });
    }

    private void atualizarComboTreinosDoAluno(Aluno aluno) {
        if (aluno != null) {
            List<ProgramacaoTreino> oProgs = treinoDAO.listarProgramacoesDoAluno(aluno.getId());
            comboTreinosExistentes.setItems(FXCollections.observableArrayList(oProgs));
            checkFichaPadrao.setSelected(false);
        } else {
            comboTreinosExistentes.getItems().clear();
        }
    }

    private void carregarTreinoParaEdicao(ProgramacaoTreino prog) {
        if (prog != null && prog.getTreino() != null) {
            treinoEmEdicao = prog.getTreino();
            campoNomeTreino.setText(treinoEmEdicao.getNome());
            comboObjetivo.setValue(treinoEmEdicao.getObjetivo());
            checkFichaPadrao.setSelected(false);
            listaFicha.setAll(treinoDAO.listarItensDoTreino(treinoEmEdicao.getId()));
        }
    }

    private void importarTemplate(Treino template) {
        if (template == null) return;
        Alert oAlert = new Alert(Alert.AlertType.CONFIRMATION);
        oAlert.setTitle("Opções de Template");
        oAlert.setHeaderText("Ficha: " + template.getNome());
        oAlert.setContentText("O que deseja fazer com esta ficha padrão?");
        ButtonType oBtnImportar = new ButtonType("Importar Cópia para Aluno");
        ButtonType oBtnEditar = new ButtonType("Editar Template Original");
        ButtonType oBtnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        oAlert.getButtonTypes().setAll(oBtnImportar, oBtnEditar, oBtnCancelar);
        Optional<ButtonType> oResult = oAlert.showAndWait();

        if (oResult.isPresent() && oResult.get() == oBtnImportar) {
            treinoEmEdicao = null; 
            campoNomeTreino.setText(template.getNome());
            comboObjetivo.setValue(template.getObjetivo());
            checkFichaPadrao.setSelected(false); 
            listaFicha.clear();
            
            List<ItemTreino> oItensTemplate = treinoDAO.listarItensDoTreino(template.getId());
            for (ItemTreino oIt : oItensTemplate) {
                ItemTreino oNovoItem = new ItemTreino();
                oNovoItem.setExercicio(oIt.getExercicio());
                oNovoItem.setIntervaloDescanso(oIt.getIntervaloDescanso());
                oNovoItem.setProgressaoCarga(oIt.isProgressaoCarga());
                
                for (SerieTreino oSOrig : oIt.getSeriesTreino()) {
                    SerieTreino oNovaSerie = new SerieTreino();
                    oNovaSerie.setNumeroDaSerie(oSOrig.getNumeroDaSerie());
                    oNovaSerie.setRepeticoes(oSOrig.getRepeticoes());
                    oNovaSerie.setCarga(oSOrig.getCarga());
                    oNovoItem.adicionarSerie(oNovaSerie);
                }
                listaFicha.add(oNovoItem);
            }
        } else if (oResult.isPresent() && oResult.get() == oBtnEditar) {
            treinoEmEdicao = template;
            campoNomeTreino.setText(template.getNome());
            comboObjetivo.setValue(template.getObjetivo());
            checkFichaPadrao.setSelected(true); 
            comboAlunos.getSelectionModel().clearSelection(); 
            listaFicha.clear();
            listaFicha.setAll(treinoDAO.listarItensDoTreino(template.getId()));
        } else {
            Platform.runLater(() -> comboTemplates.getSelectionModel().clearSelection());
        }
    }

    @FXML
    void clicouGeradorTreino(ActionEvent event) {
        Dialog<List<String>> oDialog = new Dialog<>();
        oDialog.setTitle("Gerador Inteligente");
        oDialog.setHeaderText("Configure os parâmetros exatos do treino");

        ButtonType oBtnGerar = new ButtonType("Gerar Treino", ButtonBar.ButtonData.OK_DONE);
        oDialog.getDialogPane().getButtonTypes().addAll(oBtnGerar, ButtonType.CANCEL);

        GridPane oGrid = new GridPane();
        oGrid.setHgap(10); oGrid.setVgap(10);
        oGrid.setPadding(new Insets(20, 20, 10, 10));

        ComboBox<String> oComboFoco = new ComboBox<>(FXCollections.observableArrayList(
            "Peito e Tríceps", "Costas e Bíceps", "Pernas Completo", "Ombros e Abdômen", "Peito e Costas (Antagonista)", "Braços (Bíceps e Tríceps)"
        ));
        oComboFoco.getSelectionModel().selectFirst();
        
        TextField oCampoQtd = new TextField("6");
        TextField oCampoSeries = new TextField("4");
        TextField oCampoReps = new TextField("15");
        
        CheckBox oCheckVariar = new CheckBox("Gostaria de séries e repetições alternadas?");
        CheckBox oCheckProgressao = new CheckBox("Aplicar método de Progressão de Carga (Pirâmide)?");

        Label oLblSeries = new Label("Séries Base:");
        Label oLblReps = new Label("Reps Base:");

        oCheckVariar.setOnAction(e -> {
            if (oCheckVariar.isSelected()) {
                oLblSeries.setText("Séries Máx:"); oLblReps.setText("Reps Máx:");
            } else {
                oLblSeries.setText("Séries Base:"); oLblReps.setText("Reps Base:");
            }
        });

        oGrid.add(new Label("Foco Muscular:"), 0, 0); oGrid.add(oComboFoco, 1, 0);
        oGrid.add(new Label("Qtd. Exercícios:"), 0, 1); oGrid.add(oCampoQtd, 1, 1);
        oGrid.add(oLblSeries, 0, 2); oGrid.add(oCampoSeries, 1, 2);
        oGrid.add(oLblReps, 0, 3); oGrid.add(oCampoReps, 1, 3);
        oGrid.add(oCheckVariar, 0, 4, 2, 1);
        oGrid.add(oCheckProgressao, 0, 5, 2, 1);

        oDialog.getDialogPane().setContent(oGrid);
        Platform.runLater(() -> oCampoQtd.requestFocus());

        oDialog.setResultConverter(dialogButton -> {
            if (dialogButton == oBtnGerar) {
                return Arrays.asList(
                    oComboFoco.getValue(), oCampoQtd.getText(), oCampoSeries.getText(), oCampoReps.getText(),
                    String.valueOf(oCheckVariar.isSelected()), String.valueOf(oCheckProgressao.isSelected())
                );
            }
            return null;
        });

        Optional<List<String>> oResultado = oDialog.showAndWait();
        oResultado.ifPresent(dados -> {
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
        Random oRand = new Random();
        List<Exercicio> oSelecionados = new ArrayList<>();
        
        int qtdPrincipal = (int) Math.ceil(totalExercicios * 0.6); 
        int qtdSecundario = totalExercicios - qtdPrincipal;        
        int qtdMetade = totalExercicios / 2;
        int qtdOutraMetade = totalExercicios - qtdMetade;

        if (foco.equals("Peito e Tríceps")) {
            oSelecionados.addAll(sortearExercicios("Peitoral", qtdPrincipal));
            oSelecionados.addAll(sortearExercicios("Triceps", qtdSecundario));
        } else if (foco.equals("Costas e Bíceps")) {
            oSelecionados.addAll(sortearExercicios("Costas", qtdPrincipal));
            oSelecionados.addAll(sortearExercicios("Biceps", qtdSecundario));
        } else if (foco.equals("Pernas Completo")) {
            oSelecionados.addAll(sortearExercicios("Pernas", totalExercicios));
        } else if (foco.equals("Ombros e Abdômen")) {
            oSelecionados.addAll(sortearExercicios("Ombros", qtdMetade));
            oSelecionados.addAll(sortearExercicios("Abdomen", qtdOutraMetade));
        } else if (foco.equals("Peito e Costas (Antagonista)")) {
            oSelecionados.addAll(sortearExercicios("Peitoral", qtdMetade));
            oSelecionados.addAll(sortearExercicios("Costas", qtdOutraMetade));
        } else {
            oSelecionados.addAll(sortearExercicios("Biceps", qtdMetade));
            oSelecionados.addAll(sortearExercicios("Triceps", qtdOutraMetade));
        }
        
        for (Exercicio oEx : oSelecionados) {
            ItemTreino oItem = new ItemTreino();
            oItem.setExercicio(oEx);
            oItem.setProgressaoCarga(progredir);
            oItem.setIntervaloDescanso(45.0f + (oRand.nextInt(4) * 5.0f));
            
            int baseCarga = 10 + oRand.nextInt(21); 
            
            int seriesDoExercicio;
            int repsDoExercicio;
            
            if (variar) {
                seriesDoExercicio = Math.max(2, qtdSeries - oRand.nextInt(3)); 
                int diferencaParaOMinimo = seriesDoExercicio - 2;
                int penalidadeReps = (diferencaParaOMinimo * 2) + oRand.nextInt(2); 
                repsDoExercicio = Math.max(6, qtdReps - penalidadeReps);
            } else {
                seriesDoExercicio = qtdSeries;
                repsDoExercicio = qtdReps;
            }
            
            for (int i = 1; i <= seriesDoExercicio; i++) {
                SerieTreino oSerie = new SerieTreino();
                oSerie.setNumeroDaSerie(i);
                
                if (progredir) {
                    oSerie.setRepeticoes(Math.max(6, repsDoExercicio - ((i - 1) * 2)));
                    oSerie.setCarga(baseCarga + ((i - 1) * 2.0f));
                } else {
                    oSerie.setRepeticoes(repsDoExercicio);
                    oSerie.setCarga(baseCarga);
                }
                oItem.adicionarSerie(oSerie);
            }
            listaFicha.add(oItem);
        }
        
        campoNomeTreino.setText("Treino " + foco + " [" + totalExercicios + " Ex]");
    }

    private List<Exercicio> sortearExercicios(String group, int quantidade) {
        List<Exercicio> oDoGrupo = exercicioDAO.listarPorGrupoMuscular(group);
        if (oDoGrupo.isEmpty()) return new ArrayList<>(); 
        Collections.shuffle(oDoGrupo);
        return oDoGrupo.subList(0, Math.min(quantidade, oDoGrupo.size()));
    } 
    
    private void configurarColunasDaFicha() {
        colunaExercicioFicha.setCellValueFactory(cellData -> {
            ItemTreino oItem = cellData.getValue();
            Exercicio oExercicio = oItem.getExercicio();
            return new SimpleStringProperty(oExercicio.getNome());
        });
        colunaSeries.setCellValueFactory(cellData -> {
            ItemTreino oItem = cellData.getValue();
            return new SimpleIntegerProperty(oItem.getSeriesTreino().size()).asObject();
        });
        
        colunaReps.setCellValueFactory(cellData -> {
            ItemTreino oItem = cellData.getValue();
            if (oItem.getSeriesTreino().isEmpty()) return new SimpleStringProperty("-");
            List<SerieTreino> oSeries = oItem.getSeriesTreino();
            if (!oItem.isProgressaoCarga() && oSeries.get(0).getRepeticoes() == oSeries.get(oSeries.size()-1).getRepeticoes()) {
                return new SimpleStringProperty(String.valueOf(oSeries.get(0).getRepeticoes()));
            }
            StringBuilder reps = new StringBuilder();
            for (SerieTreino oS : oItem.getSeriesTreino()) reps.append(oS.getRepeticoes()).append("-");
            return new SimpleStringProperty(reps.substring(0, reps.length() - 1));
        });
        colunaReps.setCellFactory(TextFieldTableCell.forTableColumn());
        colunaReps.setOnEditCommit(event -> {
            ItemTreino oItem = event.getRowValue();
            String[] parts = event.getNewValue().split("-");
            
            if (parts.length == 1) {
                try {
                    int rep = Integer.parseInt(parts[0].trim());
                    for (SerieTreino oS : oItem.getSeriesTreino()) oS.setRepeticoes(rep);
                    oItem.setProgressaoCarga(false);
                } catch (Exception e) {}
            } else {
                oItem.setProgressaoCarga(true);
                List<SerieTreino> oSeries = oItem.getSeriesTreino();
                for (int i = 0; i < Math.min(parts.length, oSeries.size()); i++) {
                    try { oSeries.get(i).setRepeticoes(Integer.parseInt(parts[i].trim())); } catch (Exception e) {}
                }
            }
            tabelaFicha.refresh();
        });

        colunaCarga.setCellValueFactory(cellData -> {
            ItemTreino oItem = cellData.getValue();
            if (oItem.getSeriesTreino().isEmpty()) return new SimpleStringProperty("-");
            if (!oItem.isProgressaoCarga()) {
                SerieTreino oPrimeiraSerie = oItem.getSeriesTreino().get(0);
                return new SimpleStringProperty(oPrimeiraSerie.getCarga() + "kg");
            }
            StringBuilder cargas = new StringBuilder();
            for (SerieTreino oS : oItem.getSeriesTreino()) cargas.append(oS.getCarga()).append("/");
            return new SimpleStringProperty(cargas.substring(0, cargas.length() - 1) + "kg");
        });
        colunaCarga.setCellFactory(TextFieldTableCell.forTableColumn());
        colunaCarga.setOnEditCommit(event -> {
            ItemTreino oItem = event.getRowValue();
            String limpo = event.getNewValue().replace("kg", "").trim();
            String[] parts = limpo.split("/");
            
            if (parts.length == 1) {
                try {
                    float carga = Float.parseFloat(parts[0].trim());
                    for (SerieTreino oS : oItem.getSeriesTreino()) oS.setCarga(carga);
                } catch (Exception e) {}
            } else {
                List<SerieTreino> oSeries = oItem.getSeriesTreino();
                for (int i = 0; i < Math.min(parts.length, oSeries.size()); i++) {
                    try { oSeries.get(i).setCarga(Float.parseFloat(parts[i].trim())); } catch (Exception e) {}
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
        ProgramacaoTreino oProgSelecionada = comboTreinosExistentes.getSelectionModel().getSelectedItem();
        if (oProgSelecionada == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Aviso", "Selecione uma Ficha Pessoal do Aluno no topo para poder excluí-la.");
            return;
        }
        
        Alert oConfirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        oConfirmacao.setTitle("Excluir Ficha");
        Treino oTreinoProg = oProgSelecionada.getTreino();
        oConfirmacao.setHeaderText("Atenção: Você está prestes a apagar a ficha '" + oTreinoProg.getNome() + "'");
        oConfirmacao.setContentText("Tem certeza? O aluno não verá mais esse treino no aplicativo.");
        
        if (oConfirmacao.showAndWait().get() == ButtonType.OK) {
            if (treinoDAO.excluirProgramacao(oProgSelecionada)) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Ficha excluída com sucesso!");
                limparEcra();
                Aluno oAlunoAtual = comboAlunos.getValue();
                if (oAlunoAtual != null) atualizarComboTreinosDoAluno(oAlunoAtual);
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Não foi possível excluir a ficha do banco de dados.");
            }
        }
    }
    
    @FXML void adicionarNaFicha(ActionEvent event) {
        Exercicio oSelecionado = tabelaCatalogo.getSelectionModel().getSelectedItem();
        if (oSelecionado == null) return;
        
        ItemTreino oNovoItem = new ItemTreino();
        oNovoItem.setExercicio(oSelecionado);
        oNovoItem.setIntervaloDescanso(60.0f);
        oNovoItem.setProgressaoCarga(false);
        
        for(int i = 1; i <= 3; i++){
            SerieTreino oSerie = new SerieTreino();
            oSerie.setNumeroDaSerie(i);
            oSerie.setRepeticoes(12);
            oSerie.setCarga(10.0f);
            oNovoItem.adicionarSerie(oSerie);
        }
        listaFicha.add(oNovoItem);
    }
    
    @FXML void removerDaFicha(ActionEvent event) {
        ItemTreino oSelecionado = tabelaFicha.getSelectionModel().getSelectedItem();
        if (oSelecionado != null) listaFicha.remove(oSelecionado);
    }
    
    @FXML void salvarFichaCompleta(ActionEvent event) {
        boolean isTemplate = checkFichaPadrao.isSelected();
        Aluno oAlunoSelecionado = comboAlunos.getValue();
        String nomeTreino = campoNomeTreino.getText();
        
        if (nomeTreino.isEmpty() || listaFicha.isEmpty() || comboObjetivo.getValue() == null) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Preencha o Nome, adicione pelo menos um exercício e defina o Objetivo.");
            return;
        }
        if (!isTemplate && oAlunoSelecionado == null) {
            mostrarAlerta(Alert.AlertType.ERROR, "Aviso", "Para guardar um treino pessoal, selecione o Aluno Alvo no topo.");
            return;
        }
        
        Treino oTreino;
        if (treinoEmEdicao != null) {
            oTreino = treinoEmEdicao;
        } else {
            oTreino = new Treino();
        }
        oTreino.setNome(nomeTreino);
        oTreino.setObjetivo(comboObjetivo.getValue());
        oTreino.setFichaPadrao(isTemplate);
        
        Treino oTreinoSalvo = treinoDAO.inserirTreino(oTreino);
        if (oTreinoSalvo != null) {
            for (ItemTreino oItem : listaFicha) {
                if (treinoEmEdicao == null) oItem.setId(0); 
                oItem.setTreino(oTreinoSalvo);
                treinoDAO.inserirItemTreino(oItem);
            }
            if (!isTemplate && treinoEmEdicao == null) {
                ProgramacaoTreino oProg = new ProgramacaoTreino();
                oProg.setAluno(oAlunoSelecionado);
                oProg.setTreino(oTreinoSalvo);
                oProg.setDataInicioSemanas(LocalDateTime.now());
                oProg.setDataFimSemanas(LocalDateTime.now().plusWeeks(4));
                oProg.setDiaDaSemana("A definir");
                treinoDAO.inserirProgramacao(oProg);
            }
            mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Ficha guardada com sucesso!");
            carregarTemplates(); 
            if (oAlunoSelecionado != null) atualizarComboTreinosDoAluno(oAlunoSelecionado);
            limparEcra();
        }
    }
    
    private void limparEcra() {
        treinoEmEdicao = null;
        campoNomeTreino.clear(); 
        comboObjetivo.getSelectionModel().clearSelection();
        listaFicha.clear();
        comboAlunos.getSelectionModel().clearSelection();
        comboTreinosExistentes.getItems().clear();
        comboTemplates.getSelectionModel().clearSelection();
        checkFichaPadrao.setSelected(false);
    }
    
    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert oAlert = new Alert(tipo); oAlert.setTitle(titulo); oAlert.setHeaderText(null); oAlert.setContentText(mensagem); oAlert.showAndWait();
    }
}