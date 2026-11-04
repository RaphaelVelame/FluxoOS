package br.com.fluxoos.application.porta;

import br.com.fluxoos.domain.modelo.Usuario;

public interface AdministracaoUsuarios {
    void inserir(Usuario usuario, String hash);
    void alterarStatus(long id, boolean ativo);
}
