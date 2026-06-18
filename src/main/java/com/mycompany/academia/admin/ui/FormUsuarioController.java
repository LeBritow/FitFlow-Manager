package com.mycompany.academia.admin.ui;

import com.mycompany.academia.admin.dao.UsuarioDAO;
import com.mycompany.academia.admin.model.Admin;
import com.mycompany.academia.core.config.EventBus;
import com.mycompany.academia.aluno.model.Aluno;
import com.mycompany.academia.admin.model.Instrutor;
import com.mycompany.academia.admin.model.Usuario;
import com.mycompany.academia.core.session.SessaoUsuario;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class FormUsuarioController {

    @FXML private Label labelTitulo;
    @FXML private ComboBox<String> comboTipoPerfil;
    @FXML private TextField campoNome, campoCpf, campoEmail;
    
    @FXML private HBox caixaAluno;
    @FXML private TextField campoPeso, campoAltura;
    @FXML private VBox caixaInstrutor;
    @FXML private TextField campoCref;

    private Usuario usuarioParaEditar = null;

    @FXML
    public void initialize() {
        EventBus.emit("Desktop", "FormUsuarioController.salvar", "Abrindo formulário de usuário");
        Usuario oAtual = SessaoUsuario.getInstancia().getUsuarioLogado();
        if (oAtual instanceof Admin) {
            comboTipoPerfil.getItems().addAll("Admin", "Instrutor", "Aluno");
        } else {
            comboTipoPerfil.getItems().addAll("Instrutor", "Aluno");
        }
        comboTipoPerfil.setValue("Aluno");
        
        comboTipoPerfil.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            ajustarCamposDinamicos(novo);
        });
        
        ajustarCamposDinamicos("Aluno");
    }

    private void ajustarCamposDinamicos(String tipo) {
        caixaAluno.setVisible(tipo.equals("Aluno"));
        caixaAluno.setManaged(tipo.equals("Aluno"));
        
        caixaInstrutor.setVisible(tipo.equals("Instrutor"));
        caixaInstrutor.setManaged(tipo.equals("Instrutor"));
    }

    public void preencherParaEdicao(Usuario usuario) {
        this.usuarioParaEditar = usuario;
        labelTitulo.setText("Editar Usuário");
        
        campoNome.setText(usuario.getNome());
        campoCpf.setText(usuario.getCpf());
        campoEmail.setText(usuario.getEmail());
        
        comboTipoPerfil.setDisable(true); 
        
        if (usuario instanceof Aluno) {
            comboTipoPerfil.setValue("Aluno");
            campoPeso.setText(String.valueOf(((Aluno) usuario).getPeso()));
            campoAltura.setText(String.valueOf(((Aluno) usuario).getAltura()));
        } else if (usuario instanceof Instrutor) {
            comboTipoPerfil.setValue("Instrutor");
            campoCref.setText(((Instrutor) usuario).getCref());
        } else {
            comboTipoPerfil.setValue("Admin");
        }
    }

    @FXML
    void clicouSalvar(ActionEvent event) {
        String tipo = comboTipoPerfil.getValue();
        Usuario oObjSalvar = usuarioParaEditar;
        
        if (oObjSalvar == null) {
            if (tipo.equals("Aluno")) oObjSalvar = new Aluno();
            else if (tipo.equals("Instrutor")) oObjSalvar = new Instrutor();
            else oObjSalvar = new Admin();
            
            oObjSalvar.setSenha("123456");
        }
        
        oObjSalvar.setNome(campoNome.getText());
        oObjSalvar.setCpf(campoCpf.getText());
        oObjSalvar.setEmail(campoEmail.getText());
        
        try {
            if (oObjSalvar instanceof Aluno) {
                float p = Float.parseFloat(campoPeso.getText());
                float a = Float.parseFloat(campoAltura.getText());
                ((Aluno) oObjSalvar).setPeso(p);
                ((Aluno) oObjSalvar).setAltura(a);
                ((Aluno) oObjSalvar).setImc(p / (a * a));
            } else if (oObjSalvar instanceof Instrutor) {
                ((Instrutor) oObjSalvar).setCref(campoCref.getText());
            }
        } catch (NumberFormatException e) {
            Alert oA = new Alert(Alert.AlertType.ERROR, "Peso e Altura devem ser números válidos (ex: 75.5)");
            oA.showAndWait();
            return;
        }
        
        UsuarioDAO oDao = new UsuarioDAO();
        if (oDao.inserir(oObjSalvar)) {
            fecharJanela();
        } else {
            Alert oA = new Alert(Alert.AlertType.ERROR, "Erro ao salvar no banco de dados.");
            oA.showAndWait();
        }
    }

    @FXML
    void clicouCancelar(ActionEvent event) {
        fecharJanela();
    }

    private void fecharJanela() {
        Stage oPalco = (Stage) campoNome.getScene().getWindow();
        oPalco.close();
    }
}