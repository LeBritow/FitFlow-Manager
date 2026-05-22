package com.mycompany.academia.session;

import com.mycompany.academia.model.Usuario;

public class SessaoUsuario {

    private static SessaoUsuario instancia;
    private Usuario usuarioLogado;

    private SessaoUsuario() {
    }

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