package com.mycompany.academia.aluno.ui;

import com.mycompany.academia.admin.ui.FormUsuarioController;
import com.mycompany.academia.admin.dao.UsuarioDAO;
import com.mycompany.academia.admin.model.Usuario;
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

    @FXML
    public void initialize() {
        carregarDadosTabela();
    }

    private void carregarDadosTabela() {
        List<Usuario> usuariosBanco = dao.listarTodos();
        
        listaUsuariosOb = FXCollections.observableArrayList(usuariosBanco);
        
        tabelaUsuarios.setItems(listaUsuariosOb);
    }

    @FXML
    void clicouNovo(ActionEvent event) {
        abrirFormulario(null);
    }

    @FXML
    void clicouEditar(ActionEvent event) {
        Usuario selecionado = tabelaUsuarios.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atenção", "Selecione um usuário para editar.");
            return;
        }
        abrirFormulario(selecionado);
    }

    private void abrirFormulario(Usuario usuario) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/FormUsuario.fxml"));
            javafx.scene.Parent raiz = loader.load();
            
            if (usuario != null) {
                FormUsuarioController controller = loader.getController();
                controller.preencherParaEdicao(usuario);
            }
            
            javafx.stage.Stage palcoModal = new javafx.stage.Stage();
            palcoModal.setTitle(usuario == null ? "Novo Usuário" : "Editar Usuário");
            palcoModal.setScene(new javafx.scene.Scene(raiz));
            
            palcoModal.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            palcoModal.showAndWait();
            
            carregarDadosTabela();
            
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Não foi possível abrir o formulário.");
        }
    }

    @FXML
    void clicouExcluir(ActionEvent event) {
        Usuario selecionado = tabelaUsuarios.getSelectionModel().getSelectedItem();
        
        if (selecionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atenção", "Você precisa clicar em um usuário na tabela antes de excluir.");
            return;
        }
        
        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacao.setTitle("Confirmação de Exclusão");
        confirmacao.setHeaderText("Você está prestes a deletar: " + selecionado.getNome());
        confirmacao.setContentText("Tem certeza disso? Essa ação não pode ser desfeita.");
        
        if (confirmacao.showAndWait().get() == javafx.scene.control.ButtonType.OK) {
            boolean sucesso = dao.excluir(selecionado);
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
        Usuario selecionado = tabelaUsuarios.getSelectionModel().getSelectedItem();
        
        if (selecionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atenção", "Você precisa clicar em um usuário na tabela para resetar a senha.");
            return;
        }

        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacao.setTitle("Resetar Senha");
        confirmacao.setHeaderText("Resetar senha de " + selecionado.getNome() + "?");
        confirmacao.setContentText("A senha será alterada para o padrão '123456'. O usuário deverá trocá-la depois.");

        if (confirmacao.showAndWait().get() == javafx.scene.control.ButtonType.OK) {
            boolean sucesso = dao.atualizarSenhaPorEmail(selecionado.getEmail(), "123456");
            
            if (sucesso) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "A senha foi resetada para 123456.");
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Falha ao resetar a senha no banco de dados.");
            }
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}