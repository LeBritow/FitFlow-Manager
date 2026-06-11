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
        Usuario atual = SessaoUsuario.getInstancia().getUsuarioLogado();
        if (atual instanceof Admin) {
            comboTipoPerfil.getItems().addAll("Admin", "Instrutor", "Aluno");
        } else {
            comboTipoPerfil.getItems().addAll("Instrutor", "Aluno");
        }
        comboTipoPerfil.setValue("Aluno"); // Padrão
        
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

    public void preencherParaEdicao(Usuario u) {
        this.usuarioParaEditar = u;
        labelTitulo.setText("Editar Usuário");
        
        campoNome.setText(u.getNome());
        campoCpf.setText(u.getCpf());
        campoEmail.setText(u.getEmail());
        
        comboTipoPerfil.setDisable(true); 
        
        if (u instanceof Aluno) {
            comboTipoPerfil.setValue("Aluno");
            campoPeso.setText(String.valueOf(((Aluno) u).getPeso()));
            campoAltura.setText(String.valueOf(((Aluno) u).getAltura()));
        } else if (u instanceof Instrutor) {
            comboTipoPerfil.setValue("Instrutor");
            campoCref.setText(((Instrutor) u).getCref());
        } else {
            comboTipoPerfil.setValue("Admin");
        }
    }

    @FXML
    void clicouSalvar(ActionEvent event) {
        String tipo = comboTipoPerfil.getValue();
        Usuario objSalvar = usuarioParaEditar;
        
        if (objSalvar == null) {
            if (tipo.equals("Aluno")) objSalvar = new Aluno();
            else if (tipo.equals("Instrutor")) objSalvar = new Instrutor();
            else objSalvar = new Admin();
            
            objSalvar.setSenha("123456");
        }
        
        objSalvar.setNome(campoNome.getText());
        objSalvar.setCpf(campoCpf.getText());
        objSalvar.setEmail(campoEmail.getText());
        
        try {
            if (objSalvar instanceof Aluno) {
                float p = Float.parseFloat(campoPeso.getText());
                float a = Float.parseFloat(campoAltura.getText());
                ((Aluno) objSalvar).setPeso(p);
                ((Aluno) objSalvar).setAltura(a);
                ((Aluno) objSalvar).setImc(p / (a * a));
            } else if (objSalvar instanceof Instrutor) {
                ((Instrutor) objSalvar).setCref(campoCref.getText());
            }
        } catch (NumberFormatException e) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Peso e Altura devem ser números válidos (ex: 75.5)");
            a.showAndWait();
            return;
        }
        
        UsuarioDAO dao = new UsuarioDAO();
        if (dao.salvar(objSalvar)) {
            fecharJanela();
        } else {
            Alert a = new Alert(Alert.AlertType.ERROR, "Erro ao salvar no banco de dados.");
            a.showAndWait();
        }
    }

    @FXML
    void clicouCancelar(ActionEvent event) {
        fecharJanela();
    }

    private void fecharJanela() {
        Stage palco = (Stage) campoNome.getScene().getWindow();
        palco.close();
    }
}