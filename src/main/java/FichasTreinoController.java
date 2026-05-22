package com.mycompany.academia;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.FloatStringConverter;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.StringConverter;
import javafx.application.Platform;


public class FichasTreinoController {

    @FXML private ComboBox<Treino> comboTemplates;
    @FXML private ComboBox<Aluno> comboAlunos;
    @FXML private ComboBox<ProgramacaoTreino> comboTreinosExistentes;
    @FXML private TextField campoNomeTreino, campoObjetivo;
    @FXML private CheckBox checkFichaPadrao;
    
    @FXML private TableView<Exercicio> tabelaCatalogo;
    @FXML private TableView<ItemTreino> tabelaFicha;
    @FXML private TableColumn<ItemTreino, String> colunaExercicioFicha;
    @FXML private TableColumn<ItemTreino, Integer> colunaSeries, colunaReps;
    @FXML private TableColumn<ItemTreino, Float> colunaCarga, colunaDescanso;

    private TreinoDAO treinoDAO = new TreinoDAO();
    private ExercicioDAO exercicioDAO = new ExercicioDAO();
    
    private ObservableList<Exercicio> listaCatalogo;
    private ObservableList<ItemTreino> listaFicha;
    
    private Treino treinoEmEdicao = null;

    @FXML
    public void initialize() {
        carregarAlunos();
        carregarTemplates();
        
        listaCatalogo = FXCollections.observableArrayList(exercicioDAO.listarTodos());
        tabelaCatalogo.setItems(listaCatalogo);
        
        listaFicha = FXCollections.observableArrayList();
        tabelaFicha.setItems(listaFicha);
        
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
            campoObjetivo.setText(treinoEmEdicao.getObjetivo());
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
            campoObjetivo.setText(template.getObjetivo());
            checkFichaPadrao.setSelected(false); 
            listaFicha.clear();
            List<ItemTreino> itensTemplate = treinoDAO.listarItensPorTreino(template.getId());
            for (ItemTreino it : itensTemplate) {
                ItemTreino novoItem = new ItemTreino();
                novoItem.setExercicio(it.getExercicio());
                novoItem.setSeries(it.getSeries());
                novoItem.setRepeticoes(it.getRepeticoes());
                novoItem.setCargaSugerida(it.getCargaSugerida());
                novoItem.setIntervaloDescanso(it.getIntervaloDescanso());
                listaFicha.add(novoItem);
            }
        } else if (result.isPresent() && result.get() == btnEditar) {
            treinoEmEdicao = template;
            campoNomeTreino.setText(template.getNome());
            campoObjetivo.setText(template.getObjetivo());
            checkFichaPadrao.setSelected(true); 
            comboAlunos.getSelectionModel().clearSelection(); 
            listaFicha.clear();
            listaFicha.setAll(treinoDAO.listarItensPorTreino(template.getId()));
        } else {
            Platform.runLater(() -> comboTemplates.getSelectionModel().clearSelection());
        }
    }

    // =========================================================================
    // GERADOR INTELIGENTE DE TREINOS
    // =========================================================================

    @FXML
    void clicouGeradorTreino(ActionEvent event) {
        // Novas variações de foco muscular
        List<String> focos = Arrays.asList(
            "Peito e Tríceps", 
            "Costas e Bíceps", 
            "Pernas Completo", 
            "Ombros e Abdômen", 
            "Peito e Costas (Antagonista)", 
            "Braços (Bíceps e Tríceps)"
        );
        
        ChoiceDialog<String> dialogFoco = new ChoiceDialog<>(focos.get(0), focos);
        dialogFoco.setTitle("Gerador de Treinos");
        dialogFoco.setHeaderText("Criação de Treino (Descanso Máximo: 60s)");
        dialogFoco.setContentText("Escolha o foco do treino:");
        
        Optional<String> resultadoFoco = dialogFoco.showAndWait();
        if (resultadoFoco.isPresent()) {
            montarFichaAutomatica(resultadoFoco.get());
        }
    }

    private void montarFichaAutomatica(String foco) {
        listaFicha.clear();
        Random rand = new Random();
        
        List<Exercicio> selecionados = new ArrayList<>();
        
        // Distribuição para garantir volume de treino (10 a 12 exercícios)
        if (foco.equals("Peito e Tríceps")) {
            selecionados.addAll(sortearExercicios("Peitoral", 6));
            selecionados.addAll(sortearExercicios("Triceps", 5));
        } else if (foco.equals("Costas e Bíceps")) {
            selecionados.addAll(sortearExercicios("Costas", 6));
            selecionados.addAll(sortearExercicios("Biceps", 5));
        } else if (foco.equals("Pernas Completo")) {
            selecionados.addAll(sortearExercicios("Pernas", 11));
        } else if (foco.equals("Ombros e Abdômen")) {
            selecionados.addAll(sortearExercicios("Ombros", 5));
            selecionados.addAll(sortearExercicios("Abdomen", 6));
        } else if (foco.equals("Peito e Costas (Antagonista)")) {
            selecionados.addAll(sortearExercicios("Peitoral", 5));
            selecionados.addAll(sortearExercicios("Costas", 5));
        } else { // Braços
            selecionados.addAll(sortearExercicios("Biceps", 5));
            selecionados.addAll(sortearExercicios("Triceps", 5));
        }
        
        for (Exercicio ex : selecionados) {
            ItemTreino item = new ItemTreino();
            item.setExercicio(ex);
            // Randomização de volume para evitar estagnação
            item.setSeries(3 + rand.nextInt(2)); // Varia entre 3 e 4 séries
            item.setRepeticoes(8 + rand.nextInt(8)); // Varia entre 8 e 15 reps
            item.setCargaSugerida(10.0f + (rand.nextFloat() * 20.0f)); // Carga base
            
            // Descanso estrito: de 45s a 60s
            item.setIntervaloDescanso(45.0f + (rand.nextFloat() * 15.0f)); 
            
            listaFicha.add(item);
        }
        
        campoNomeTreino.setText("Treino " + foco + " [Gerado]");
        campoObjetivo.setText("Variação de carga e volume dinâmico");
    }

    private List<Exercicio> sortearExercicios(String grupo, int quantidade) {
        List<Exercicio> doGrupo = exercicioDAO.buscarPorGrupoMuscular(grupo);
        if (doGrupo.isEmpty()) return new ArrayList<>(); 
        Collections.shuffle(doGrupo);
        return doGrupo.subList(0, Math.min(quantidade, doGrupo.size()));
    } 
    
    // =========================================================================

    private void configurarColunasDaFicha() {
        colunaExercicioFicha.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getExercicio().getNome()));
        colunaSeries.setCellValueFactory(new PropertyValueFactory<>("series"));
        colunaSeries.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colunaSeries.setOnEditCommit(event -> event.getRowValue().setSeries(event.getNewValue()));
        colunaReps.setCellValueFactory(new PropertyValueFactory<>("repeticoes"));
        colunaReps.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colunaReps.setOnEditCommit(event -> event.getRowValue().setRepeticoes(event.getNewValue()));
        colunaCarga.setCellValueFactory(new PropertyValueFactory<>("cargaSugerida"));
        colunaCarga.setCellFactory(TextFieldTableCell.forTableColumn(new FloatStringConverter()));
        colunaCarga.setOnEditCommit(event -> event.getRowValue().setCargaSugerida(event.getNewValue()));
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
    @FXML void adicionarNaFicha(ActionEvent event) {
        Exercicio selecionado = tabelaCatalogo.getSelectionModel().getSelectedItem();
        if (selecionado == null) return;
        ItemTreino novoItem = new ItemTreino();
        novoItem.setExercicio(selecionado);
        novoItem.setSeries(3); novoItem.setRepeticoes(12);
        novoItem.setCargaSugerida(10.0f); novoItem.setIntervaloDescanso(60.0f);
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
        if (nomeTreino.isEmpty() || listaFicha.isEmpty()) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Preencha o Nome e adicione pelo menos um exercício.");
            return;
        }
        if (!isTemplate && alunoSelecionado == null) {
            mostrarAlerta(Alert.AlertType.ERROR, "Aviso", "Para guardar um treino pessoal, selecione o Aluno Alvo no topo.");
            return;
        }
        Treino treino = (treinoEmEdicao != null) ? treinoEmEdicao : new Treino();
        treino.setNome(nomeTreino);
        treino.setObjetivo(campoObjetivo.getText());
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
        campoNomeTreino.clear(); campoObjetivo.clear(); listaFicha.clear();
        comboAlunos.getSelectionModel().clearSelection();
        comboTreinosExistentes.getItems().clear();
        comboTemplates.getSelectionModel().clearSelection();
        checkFichaPadrao.setSelected(false);
    }
    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alert = new Alert(tipo); alert.setTitle(titulo); alert.setHeaderText(null); alert.setContentText(mensagem); alert.showAndWait();
    }
}