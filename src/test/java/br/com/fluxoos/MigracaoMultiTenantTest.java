package br.com.fluxoos;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.util.List;

class MigracaoMultiTenantTest {
    void atualizar(PropriedadeEstoqueTest.Banco b) {
        MigracoesTeste.configurar(b.url,b.usuario,b.senha,b.schema).load().migrate();
    }
    void volta(PropriedadeEstoqueTest.Banco b,int versao,String nome) {
        b.script("db/rollback/"+b.vendor+"/V"+versao+"__"+nome+".down.sql");
    }
    @Test void atualizacaoMantemLegadoEMapeiaRolesSemConcederMasterOuPermissaoAoSolicitante() throws Exception {
        try(var b=new PropriedadeEstoqueTest.Banco()) {
            b.migrar13();
            b.sql("INSERT INTO usuarios(id,nome_usuario,nome_completo,perfil,ativo,hash_senha) SELECT 3,'suporte','Suporte','SUPORTE_TI',true,hash_senha FROM usuarios WHERE id=1");
            b.sql("INSERT INTO usuarios(id,nome_usuario,nome_completo,perfil,ativo,hash_senha) SELECT 4,'consulta','Consulta','SOLICITANTE',true,hash_senha FROM usuarios WHERE id=1");
            var antes=b.legado(); atualizar(b);
            assertThat(b.legado()).isEqualTo(antes);
            assertThat(b.numero("SELECT count(*) FROM empresas WHERE id=1 AND nome='Empresa legada'")).isEqualTo(1);
            assertThat(b.numero("SELECT count(*) FROM usuario_roles")).isEqualTo(4);
            assertThat(b.numero("SELECT count(*) FROM usuario_roles WHERE usuario_id=4 OR role='ROLE_ADMIN_MASTER'")).isZero();
            for(var t:List.of("usuarios","ordens","materiais","estoques","movimentos","itens_os"))
                assertThat(b.numero("SELECT count(*) FROM "+t+" WHERE empresa_id IS DISTINCT FROM 1")).isZero();
        }
    }
    @Test void idaVoltaEReaplicacaoMantemTodosOsDadosLegados() throws Exception {
        try(var b=new PropriedadeEstoqueTest.Banco()) {
            b.migrar13();var antes=b.legado();atualizar(b);
            volta(b,16,"notificacoes_criacao_os");volta(b,15,"integridade_entre_empresas");volta(b,14,"empresas_e_multiplas_roles");
            assertThat(b.legado()).isEqualTo(antes);
            b.script("db/migration/V14__empresas_e_multiplas_roles.sql");
            b.script("db/"+b.vendor+"/V15__integridade_entre_empresas.sql");
            b.script("db/migration/V16__notificacoes_criacao_os.sql");
            assertThat(b.legado()).isEqualTo(antes);
        }
    }
    @Test void reversoesRecusamPerdaDeNotificacoesRolesOuEmpresasNovas() throws Exception {
        try(var b=new PropriedadeEstoqueTest.Banco()) {
            atualizar(b);
            b.sql("INSERT INTO notificacoes(id,empresa_id,ordem_id,destinatario_id,mensagem,criado_em) VALUES(50,1,20,1,'Nova OS',CURRENT_TIMESTAMP)");
            assertThatThrownBy(()->volta(b,16,"notificacoes_criacao_os")).hasStackTraceContaining("rollback_V16_notificacoes_preservadas: 1");
            assertThat(b.numero("SELECT count(*) FROM notificacoes")).isEqualTo(1);
        }
        try(var b=new PropriedadeEstoqueTest.Banco()) {
            atualizar(b);volta(b,16,"notificacoes_criacao_os");volta(b,15,"integridade_entre_empresas");
            b.sql("INSERT INTO usuario_roles(usuario_id,role) VALUES(2,'ROLE_ESTOQUISTA')");
            assertThatThrownBy(()->volta(b,14,"empresas_e_multiplas_roles")).hasStackTraceContaining("rollback_V14_roles_preservadas: 1");
            assertThat(b.numero("SELECT count(*) FROM usuario_roles WHERE role='ROLE_ESTOQUISTA'")).isEqualTo(1);
        }
        try(var b=new PropriedadeEstoqueTest.Banco()) {
            atualizar(b);volta(b,16,"notificacoes_criacao_os");volta(b,15,"integridade_entre_empresas");
            b.sql("INSERT INTO empresas(id,nome) VALUES(90,'Empresa nova')");
            assertThatThrownBy(()->volta(b,14,"empresas_e_multiplas_roles")).hasStackTraceContaining("rollback_V14_tenants_preservados: 1");
            assertThat(b.numero("SELECT count(*) FROM empresas")).isEqualTo(2);
        }
    }
    @Test void preflightContaERecusaDadosCruzadosSemCorrigir() throws Exception {
        try(var b=new PropriedadeEstoqueTest.Banco()) {
            b.migrar13();MigracoesTeste.configurar(b.url,b.usuario,b.senha,b.schema).target("14").load().migrate();
            b.sql("INSERT INTO empresas(id,nome) VALUES(90,'Empresa nova')");
            b.sql("UPDATE usuarios SET empresa_id=90 WHERE id=2");
            assertThatThrownBy(()->b.script("db/"+b.vendor+"/V15__integridade_entre_empresas.sql"))
                    .hasStackTraceContaining("fk_ordens_equipe_id_tenant: 1 violation(s). No data changed.");
            assertThat(b.numero("SELECT empresa_id FROM usuarios WHERE id=2")).isEqualTo(90);
        }
    }
}
