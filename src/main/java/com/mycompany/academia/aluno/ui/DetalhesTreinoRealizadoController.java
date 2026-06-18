package com.mycompany.academia.aluno.ui;

import com.mycompany.academia.aluno.model.Aluno;
import com.mycompany.academia.core.config.EventBus;
import com.mycompany.academia.core.util.TableUtils;
import com.mycompany.academia.treino.model.Exercicio;
import com.mycompany.academia.treino.model.ItemTreino;
import com.mycompany.academia.treino.model.SerieTreino;
import com.mycompany.academia.treino.model.Treino;
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
        labelTituloTreino.setText(treino.getNome() + " (" + treino.getObjetivo().getNome() + ")");

        java.time.format.DateTimeFormatter formatador = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
        labelDataFinalizacao.setText("Finalizado em: " + dataSessao.format(formatador));
        if (comentarioTexto != null && !comentarioTexto.isEmpty()) {
            labelComentarioAluno.setText("\"" + comentarioTexto + "\"");
        } else {
            labelComentarioAluno.setText("Sem feedback do aluno");
        }
        labelNotaGeral.setText("Análise de Ritmo e Carga Ativa");

        List<com.mycompany.academia.treino.model.ItemRealizado> oRealizados = treinoDAO.listarItensRealizados(
            aluno.getId(),
            treino.getId(),
            dataSessao
        );

        ObservableList<LinhaExecucao> oDados = FXCollections.observableArrayList();

        for (com.mycompany.academia.treino.model.ItemRealizado ir : oRealizados) {
            ItemTreino oItemIr = ir.getItemTreino();
            List<SerieTreino> oSeries = oItemIr.getSeriesTreino();
            int totalSeries = oSeries.size();
            String planejado = totalSeries + "x ";
            if (!oSeries.isEmpty()) {
                SerieTreino oPrimeiraSerie = oSeries.get(0);
                planejado += oPrimeiraSerie.getRepeticoes() + " reps / " +
                             oPrimeiraSerie.getCarga() + "kg";
            }

            String realizado;
            if (ir.isFeito()) {
                realizado = String.format("%.1f kg", ir.getCargaUtilizada());
            } else {
                realizado = "Não realizado";
            }

            String tempoExecFmt;
            if (ir.isFeito()) {
                Integer oTempoExecSegundos = ir.getTempoExecucaoSegundos();
                int oTempoExecValor = 0;
                if (oTempoExecSegundos != null) {
                    oTempoExecValor = oTempoExecSegundos;
                }
                tempoExecFmt = formatarTempo(oTempoExecValor);
            } else {
                tempoExecFmt = "--:--";
            }
            String tempoDescFmt;
            if (ir.isFeito()) {
                Integer oTempoDescSegundos = ir.getTempoDescansoSegundos();
                int oTempoDescValor = 0;
                if (oTempoDescSegundos != null) {
                    oTempoDescValor = oTempoDescSegundos;
                }
                tempoDescFmt = formatarTempo(oTempoDescValor);
            } else {
                tempoDescFmt = "--:--";
            }

            String tendencia = "➡️ Manteve";
            if ("SUBIU".equalsIgnoreCase(ir.getStatusCarga())) tendencia = "🔺 Aumentou";
            if ("DIMINUIU".equalsIgnoreCase(ir.getStatusCarga())) tendencia = "🔻 Diminuiu";
            if (!ir.isFeito()) tendencia = "❌ Pulado";

            Exercicio oExercicio = oItemIr.getExercicio();
            oDados.add(new LinhaExecucao(
                oExercicio.getNome(),
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

    public void carregarDadosReais(com.mycompany.academia.treino.model.ComentarioTreino comentario) {
        Treino oTreino = comentario.getTreino();
        labelTituloTreino.setText(oTreino.getNome() + " (" + oTreino.getObjetivo().getNome() + ")");
        
        java.time.format.DateTimeFormatter formatador = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
        labelDataFinalizacao.setText("Finalizado em: " + comentario.getDataCriacao().format(formatador));
        labelComentarioAluno.setText("\"" + comentario.getTexto() + "\"");
        labelNotaGeral.setText("Análise de Ritmo e Carga Ativa");

        Aluno oAluno = comentario.getAluno();
        List<com.mycompany.academia.treino.model.ItemRealizado> oRealizados = treinoDAO.listarItensRealizados(
            oAluno.getId(), 
            oTreino.getId(), 
            comentario.getDataCriacao()
        );

        ObservableList<LinhaExecucao> oDados = FXCollections.observableArrayList();
        
        for (com.mycompany.academia.treino.model.ItemRealizado ir : oRealizados) {
            ItemTreino oItemTreino = ir.getItemTreino();
            int totalSeries = oItemTreino.getSeriesTreino().size();
            String planejado = totalSeries + "x ";
            if (!oItemTreino.getSeriesTreino().isEmpty()) {
                SerieTreino oPrimeiraSerie = oItemTreino.getSeriesTreino().get(0);
                planejado += oPrimeiraSerie.getRepeticoes() + " reps / " +
                             oPrimeiraSerie.getCarga() + "kg";
            }

            String realizado;
            if (ir.isFeito()) {
                realizado = String.format("%.1f kg", ir.getCargaUtilizada());
            } else {
                realizado = "Não realizado";
            }

            String tempoExecFmt;
            if (ir.isFeito()) {
                Integer oTempoExecSegundos = ir.getTempoExecucaoSegundos();
                int oTempoExecValor = 0;
                if (oTempoExecSegundos != null) {
                    oTempoExecValor = oTempoExecSegundos;
                }
                tempoExecFmt = formatarTempo(oTempoExecValor);
            } else {
                tempoExecFmt = "--:--";
            }
            String tempoDescFmt;
            if (ir.isFeito()) {
                Integer oTempoDescSegundos = ir.getTempoDescansoSegundos();
                int oTempoDescValor = 0;
                if (oTempoDescSegundos != null) {
                    oTempoDescValor = oTempoDescSegundos;
                }
                tempoDescFmt = formatarTempo(oTempoDescValor);
            } else {
                tempoDescFmt = "--:--";
            }

            String tendencia = "➡️ Manteve";
            if ("SUBIU".equalsIgnoreCase(ir.getStatusCarga())) tendencia = "🔺 Aumentou";
            if ("DIMINUIU".equalsIgnoreCase(ir.getStatusCarga())) tendencia = "🔻 Diminuiu";
            if (!ir.isFeito()) tendencia = "❌ Pulado";

            Exercicio oExercicio = oItemTreino.getExercicio();
            oDados.add(new LinhaExecucao(
                oExercicio.getNome(), 
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