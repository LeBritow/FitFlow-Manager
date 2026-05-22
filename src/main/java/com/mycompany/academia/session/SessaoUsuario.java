package com.mycompany.academia.session;

import com.mycompany.academia.model.Usuario;

public class SessaoUsuario {

    private static SessaoUsuario instancia;
    private Usuario usuarioLogado;

    // Construtor privado impede que outras classes criem novas instâncias
    private SessaoUsuario() {
    }

    // Método global para aceder à mesma e única instância na memória
    public static SessaoUsuario getInstancia() {
        if (instancia == null) {
            instancia = new SessaoUsuario();
        }
        return instancia;
    }

    public Usuario getUsuarioLogado() {
        return usuarioLogado;
    }

    public void setUsuarioLogado(Usuario usuarioLogado) {
        this.usuarioLogado = usuarioLogado;
    }

    public void encerrarSessao() {
        this.usuarioLogado = null;
    }
}