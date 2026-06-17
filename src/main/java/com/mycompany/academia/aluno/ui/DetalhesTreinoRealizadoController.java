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

    public void carregarDadosReais(com.mycompany.academia.aluno.model.Aluno pAluno, com.mycompany.academia.treino.model.Treino pTreino, java.time.LocalDateTime pDataSessao, String pComentarioTexto) {
        labelTituloTreino.setText(pTreino.getNome() + " (" + pTreino.getObjetivo() + ")");

        java.time.format.DateTimeFormatter formatador = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
        labelDataFinalizacao.setText("Finalizado em: " + pDataSessao.format(formatador));
        labelComentarioAluno.setText(pComentarioTexto != null && !pComentarioTexto.isEmpty() ? "\"" + pComentarioTexto + "\"" : "Sem feedback do aluno");
        labelNotaGeral.setText("Análise de Ritmo e Carga Ativa");

        List<com.mycompany.academia.treino.model.ItemRealizado> oRealizados = treinoDAO.listarItensRealizados(
            pAluno.getId(),
            pTreino.getId(),
            pDataSessao
        );

        ObservableList<LinhaExecucao> oDados = FXCollections.observableArrayList();

        for (com.mycompany.academia.treino.model.ItemRealizado ir : oRealizados) {
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

            oDados.add(new LinhaExecucao(
                ir.getItemTreino().getExercicio().getNome(),
                planejado,
                realizado,
                tempoExecFmt,
                tempoDescFmt,
                tendencia
            ));
        }

        tabelaExecucao.setItems(oDados);
        TableUtils.autoFitColumns(tabelaExecucao);
    }

    public void carregarDadosReais(com.mycompany.academia.treino.model.ComentarioTreino pComentario) {
        labelTituloTreino.setText(pComentario.getTreino().getNome() + " (" + pComentario.getTreino().getObjetivo() + ")");
        
        java.time.format.DateTimeFormatter formatador = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
        labelDataFinalizacao.setText("Finalizado em: " + pComentario.getDataCriacao().format(formatador));
        labelComentarioAluno.setText("\"" + pComentario.getTexto() + "\"");
        labelNotaGeral.setText("Análise de Ritmo e Carga Ativa");

        List<com.mycompany.academia.treino.model.ItemRealizado> oRealizados = treinoDAO.listarItensRealizados(
            pComentario.getAluno().getId(), 
            pComentario.getTreino().getId(), 
            pComentario.getDataCriacao()
        );

        ObservableList<LinhaExecucao> oDados = FXCollections.observableArrayList();
        
        for (com.mycompany.academia.treino.model.ItemRealizado ir : oRealizados) {
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

            oDados.add(new LinhaExecucao(
                ir.getItemTreino().getExercicio().getNome(), 
                planejado, 
                realizado, 
                tempoExecFmt, 
                tempoDescFmt, 
                tendencia
            ));
        }
        
        tabelaExecucao.setItems(oDados);
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