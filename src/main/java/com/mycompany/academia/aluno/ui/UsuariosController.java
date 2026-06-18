package com.mycompany.academia.aluno.ui;

import com.mycompany.academia.admin.ui.FormUsuarioController;
import com.mycompany.academia.admin.dao.UsuarioDAO;
import com.mycompany.academia.core.config.EventBus;
import com.mycompany.academia.admin.model.Admin;
import com.mycompany.academia.admin.model.Usuario;
import com.mycompany.academia.core.session.SessaoUsuario;
import com.mycompany.academia.core.util.TableUtils;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;

public class UsuariosController {

    @FXML
    private TableView<Usuario> tabelaUsuarios;

    private UsuarioDAO dao = new UsuarioDAO();
    private ObservableList<Usuario> listaUsuariosOb;
    private boolean usuarioLogadoEhAdmin;

    @FXML
    public void initialize() {
        Usuario oAtual = SessaoUsuario.getInstancia().getUsuarioLogado();
        usuarioLogadoEhAdmin = oAtual instanceof Admin;
        EventBus.emit("Desktop", "UsuariosController.listarUsuarios", "Carregando gerenciamento de usuários");
        carregarDadosTabela();
    }

    private void carregarDadosTabela() {
        List<Usuario> oUsuariosBanco = dao.listarTodos();
        
        listaUsuariosOb = FXCollections.observableArrayList(oUsuariosBanco);
        
        tabelaUsuarios.setItems(listaUsuariosOb);
        TableUtils.autoFitColumns(tabelaUsuarios);
    }

    @FXML
    void clicouNovo(ActionEvent event) {
        abrirFormulario(null);
    }

    @FXML
    void clicouEditar(ActionEvent event) {
        Usuario oSelecionado = tabelaUsuarios.getSelectionModel().getSelectedItem();
        if (oSelecionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atenção", "Selecione um usuário para editar.");
            return;
        }
        if (!usuarioLogadoEhAdmin && oSelecionado instanceof Admin) {
            mostrarAlerta(Alert.AlertType.ERROR, "Acesso Negado", "Apenas administradores podem editar outros administradores.");
            return;
        }
        abrirFormulario(oSelecionado);
    }

    private void abrirFormulario(Usuario usuario) {
        try {
            javafx.fxml.FXMLLoader oLoader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/FormUsuario.fxml"));
            javafx.scene.Parent oRaiz = oLoader.load();
            
            if (usuario != null) {
                FormUsuarioController oController = oLoader.getController();
                oController.preencherParaEdicao(usuario);
            }
            
            javafx.stage.Stage oPalcoModal = new javafx.stage.Stage();
            if (usuario == null) {
                oPalcoModal.setTitle("Novo Usuário");
            } else {
                oPalcoModal.setTitle("Editar Usuário");
            }
            oPalcoModal.setScene(new javafx.scene.Scene(oRaiz));
            
            oPalcoModal.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            oPalcoModal.showAndWait();
            
            carregarDadosTabela();
            
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Não foi possível abrir o formulário.");
        }
    }

    @FXML
    void clicouExcluir(ActionEvent event) {
        Usuario oSelecionado = tabelaUsuarios.getSelectionModel().getSelectedItem();
        
        if (oSelecionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atenção", "Você precisa clicar em um usuário na tabela antes de excluir.");
            return;
        }
        
        if (!usuarioLogadoEhAdmin && oSelecionado instanceof Admin) {
            mostrarAlerta(Alert.AlertType.ERROR, "Acesso Negado", "Apenas administradores podem excluir outros administradores.");
            return;
        }
        
        Alert oConfirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        oConfirmacao.setTitle("Confirmação de Exclusão");
        oConfirmacao.setHeaderText("Você está prestes a deletar: " + oSelecionado.getNome());
        oConfirmacao.setContentText("Tem certeza disso? Essa ação não pode ser desfeita.");
        
        if (oConfirmacao.showAndWait().get() == javafx.scene.control.ButtonType.OK) {
            boolean sucesso = dao.excluir(oSelecionado);
            if (sucesso) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Usuário excluído com sucesso!");
                carregarDadosTabela();
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Não foi possível excluir o usuário. Ele pode estar vinculado a outros dados (ex: Fichas de Treino).");
            }
        }
    }

    @FXML
    void clicouResetarSenha(ActionEvent event) {
        Usuario oSelecionado = tabelaUsuarios.getSelectionModel().getSelectedItem();
        
        if (oSelecionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atenção", "Você precisa clicar em um usuário na tabela para resetar a senha.");
            return;
        }
        
        if (!usuarioLogadoEhAdmin && oSelecionado instanceof Admin) {
            mostrarAlerta(Alert.AlertType.ERROR, "Acesso Negado", "Apenas administradores podem resetar a senha de outros administradores.");
            return;
        }

        Alert oConfirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        oConfirmacao.setTitle("Resetar Senha");
        oConfirmacao.setHeaderText("Resetar senha de " + oSelecionado.getNome() + "?");
        oConfirmacao.setContentText("A senha será alterada para o padrão '123456'. O usuário deverá trocá-la depois.");

        if (oConfirmacao.showAndWait().get() == javafx.scene.control.ButtonType.OK) {
            boolean sucesso = dao.atualizarSenhaPorEmail(oSelecionado.getEmail(), "123456");
            
            if (sucesso) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "A senha foi resetada para 123456.");
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Falha ao resetar a senha no banco de dados.");
            }
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert oAlert = new Alert(tipo);
        oAlert.setTitle(titulo);
        oAlert.setHeaderText(null);
        oAlert.setContentText(mensagem);
        oAlert.showAndWait();
    }
}