package com.mycompany.academia.aluno.ui;

import com.mycompany.academia.core.config.EventBus;
import com.mycompany.academia.core.util.TableUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import java.util.List;

public class DetalhesTreinoRealizadoController {

    @FXML private Label labelTituloTreino;
    @FXML private Label labelDataFinalizacao;
    
    @FXML private TableView<LinhaExecucao> tabelaExecucao;
    @FXML private TableColumn<LinhaExecucao, String> colExercicio;
    @FXML private TableColumn<LinhaExecucao, String> colPlanejado;
    @FXML private TableColumn<LinhaExecucao, String> colRealizado;
    
    @FXML private TableColumn<LinhaExecucao, String> colTempoExecucao;
    @FXML private TableColumn<LinhaExecucao, String> colTempoDescanso;
    @FXML private TableColumn<LinhaExecucao, String> colTendenciaCarga;
    
    @FXML private Label labelNotaGeral;
    @FXML private Label labelComentarioAluno;

    private com.mycompany.academia.treino.dao.TreinoDAO treinoDAO = new com.mycompany.academia.treino.dao.TreinoDAO();

    @FXML
    public void initialize() {
        EventBus.emit("Desktop", "DetalhesTreinoRealizadoController.carregarDados", "Carregando detalhes do treino");
        colExercicio.setCellValueFactory(cellData -> cellData.getValue().exercicio);
        colPlanejado.setCellValueFactory(cellData -> cellData.getValue().planejado);
        colRealizado.setCellValueFactory(cellData -> cellData.getValue().realizado);
        
        colTempoExecucao.setCellValueFactory(cellData -> cellData.getValue().tempoExecucao);
        colTempoDescanso.setCellValueFactory(cellData -> cellData.getValue().tempoDescanso);
        colTendenciaCarga.setCellValueFactory(cellData -> cellData.getValue().tendenciaCarga);
    }

    public void carregarDadosReais(com.mycompany.academia.aluno.model.Aluno aluno, com.mycompany.academia.treino.model.Treino treino, java.time.LocalDateTime dataSessao, String comentarioTexto) {
        labelTituloTreino.setText(treino.getNome() + " (" + treino.getObjetivo() + ")");

        java.time.format.DateTimeFormatter formatador = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
        labelDataFinalizacao.setText("Finalizado em: " + dataSessao.format(formatador));
        labelComentarioAluno.setText(comentarioTexto != null && !comentarioTexto.isEmpty() ? "\"" + comentarioTexto + "\"" : "Sem feedback do aluno");
        labelNotaGeral.setText("Análise de Ritmo e Carga Ativa");

        List<com.mycompany.academia.treino.model.ItemRealizado> realizados = treinoDAO.buscarItensRealizados(
            aluno.getId(),
            treino.getId(),
            dataSessao
        );

        ObservableList<LinhaExecucao> dados = FXCollections.observableArrayList();

        for (com.mycompany.academia.treino.model.ItemRealizado ir : realizados) {
            int totalSeries = ir.getItemTreino().getSeriesTreino().size();
            String planejado = totalSeries + "x ";
            if (!ir.getItemTreino().getSeriesTreino().isEmpty()) {
                planejado += ir.getItemTreino().getSeriesTreino().get(0).getRepeticoes() + " reps / " +
                             ir.getItemTreino().getSeriesTreino().get(0).getCarga() + "kg";
            }

            String realizado = ir.isFeito() ? String.format("%.1f kg", ir.getCargaUtilizada()) : "Não realizado";

            String tempoExecFmt = ir.isFeito() ? formatarTempo(ir.getTempoExecucaoSegundos() != null ? ir.getTempoExecucaoSegundos() : 0) : "--:--";
            String tempoDescFmt = ir.isFeito() ? formatarTempo(ir.getTempoDescansoSegundos() != null ? ir.getTempoDescansoSegundos() : 0) : "--:--";

            String tendencia = "➡️ Manteve";
            if ("SUBIU".equalsIgnoreCase(ir.getStatusCarga())) tendencia = "🔺 Aumentou";
            if ("DIMINUIU".equalsIgnoreCase(ir.getStatusCarga())) tendencia = "🔻 Diminuiu";
            if (!ir.isFeito()) tendencia = "❌ Pulado";

            dados.add(new LinhaExecucao(
                ir.getItemTreino().getExercicio().getNome(),
                planejado,
                realizado,
                tempoExecFmt,
                tempoDescFmt,
                tendencia
            ));
        }

        tabelaExecucao.setItems(dados);
        TableUtils.autoFitColumns(tabelaExecucao);
    }

    public void carregarDadosReais(com.mycompany.academia.treino.model.ComentarioTreino comentario) {
        labelTituloTreino.setText(comentario.getTreino().getNome() + " (" + comentario.getTreino().getObjetivo() + ")");
        
        java.time.format.DateTimeFormatter formatador = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
        labelDataFinalizacao.setText("Finalizado em: " + comentario.getDataCriacao().format(formatador));
        labelComentarioAluno.setText("\"" + comentario.getTexto() + "\"");
        labelNotaGeral.setText("Análise de Ritmo e Carga Ativa");

        List<com.mycompany.academia.treino.model.ItemRealizado> realizados = treinoDAO.buscarItensRealizados(
            comentario.getAluno().getId(), 
            comentario.getTreino().getId(), 
            comentario.getDataCriacao()
        );

        ObservableList<LinhaExecucao> dados = FXCollections.observableArrayList();
        
        for (com.mycompany.academia.treino.model.ItemRealizado ir : realizados) {
            int totalSeries = ir.getItemTreino().getSeriesTreino().size();
            String planejado = totalSeries + "x ";
            if (!ir.getItemTreino().getSeriesTreino().isEmpty()) {
                planejado += ir.getItemTreino().getSeriesTreino().get(0).getRepeticoes() + " reps / " + 
                             ir.getItemTreino().getSeriesTreino().get(0).getCarga() + "kg";
            }

            String realizado = ir.isFeito() ? String.format("%.1f kg", ir.getCargaUtilizada()) : "Não realizado";

            String tempoExecFmt = ir.isFeito() ? formatarTempo(ir.getTempoExecucaoSegundos() != null ? ir.getTempoExecucaoSegundos() : 0) : "--:--";
            String tempoDescFmt = ir.isFeito() ? formatarTempo(ir.getTempoDescansoSegundos() != null ? ir.getTempoDescansoSegundos() : 0) : "--:--";

            String tendencia = "➡️ Manteve";
            if ("SUBIU".equalsIgnoreCase(ir.getStatusCarga())) tendencia = "🔺 Aumentou";
            if ("DIMINUIU".equalsIgnoreCase(ir.getStatusCarga())) tendencia = "🔻 Diminuiu";
            if (!ir.isFeito()) tendencia = "❌ Pulado";

            dados.add(new LinhaExecucao(
                ir.getItemTreino().getExercicio().getNome(), 
                planejado, 
                realizado, 
                tempoExecFmt, 
                tempoDescFmt, 
                tendencia
            ));
        }
        
        tabelaExecucao.setItems(dados);
        TableUtils.autoFitColumns(tabelaExecucao);
    }

    private String formatarTempo(int totalSegundos) {
        int minutos = totalSegundos / 60;
        int segundos = totalSegundos % 60;
        return String.format("%02d:%02d", minutos, segundos);
    }

    public static class LinhaExecucao {
        private final SimpleStringProperty exercicio;
        private final SimpleStringProperty planejado;
        private final SimpleStringProperty realizado;
        private final SimpleStringProperty tempoExecucao;
        private final SimpleStringProperty tempoDescanso;
        private final SimpleStringProperty tendenciaCarga;

        public LinhaExecucao(String exercicio, String planejado, String realizado, String tempoExec, String tempoDesc, String tendencia) {
            this.exercicio = new SimpleStringProperty(exercicio);
            this.planejado = new SimpleStringProperty(planejado);
            this.realizado = new SimpleStringProperty(realizado);
            this.tempoExecucao = new SimpleStringProperty(tempoExec);
            this.tempoDescanso = new SimpleStringProperty(tempoDesc);
            this.tendenciaCarga = new SimpleStringProperty(tendencia);
        }
    }
}