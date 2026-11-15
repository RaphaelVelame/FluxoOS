package br.com.fluxoos;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class MigracaoClientesTest {
    void v16(PropriedadeEstoqueTest.Banco b) {
        MigracoesTeste.configurar(b.url,b.usuario,b.senha,b.schema).target("16").load().migrate();
    }
    void migrar(PropriedadeEstoqueTest.Banco b) {
        MigracoesTeste.configurar(b.url,b.usuario,b.senha,b.schema).load().migrate();
    }
    void voltar(PropriedadeEstoqueTest.Banco b) {
        b.script("db/rollback/"+b.vendor+"/V18__integridade_clientes.down.sql");
        b.script("db/rollback/"+b.vendor+"/V17__clientes_e_atendimento.down.sql");
    }

    @Test void migraCadaEstoqueClienteSemUnirNomesOuClassificarMateriaisEPermiteVolta() throws Exception {
        try(var b=new PropriedadeEstoqueTest.Banco()) {
            v16(b);
            b.sql("INSERT INTO estoques(id,empresa_id,codigo,nome,tipo) VALUES(100,1,'A','Mesmo nome','CLIENTE'),(101,1,'B','Mesmo nome','CLIENTE'),(102,1,'EMP','Empresa','EMPRESA')");
            b.sql("UPDATE materiais SET estoque_id=100 WHERE id=10");
            var antes=b.legado();
            migrar(b);
            assertThat(b.numero("SELECT count(*) FROM clientes")).isEqualTo(2);
            assertThat(b.numero("SELECT count(*) FROM estoques e JOIN clientes c ON c.id=e.cliente_id AND c.empresa_id=e.empresa_id AND c.codigo=e.codigo AND c.nome=e.nome")).isEqualTo(2);
            assertThat(b.numero("SELECT count(*) FROM ordens WHERE tipo_atendimento='LEGADO' AND cliente_id IS NULL")).isEqualTo(1);
            assertThat(b.numero("SELECT quantidade FROM materiais WHERE id=10 AND estoque_id=100")).isEqualTo(8);
            assertThat(b.numero("SELECT count(*) FROM movimentos WHERE estoque_id IS NULL")).isEqualTo(2);
            voltar(b);
            assertThat(b.legado()).isEqualTo(antes);
            b.script("db/migration/V17__clientes_e_atendimento.sql");
            b.script("db/"+b.vendor+"/V18__integridade_clientes.sql");
            assertThat(b.numero("SELECT count(*) FROM clientes")).isEqualTo(2);
        }
    }

    @Test void bancoImpedeClienteDeOutroTenantTipoIncoerenteNomeVazioEDuplicidade() throws Exception {
        try(var b=new PropriedadeEstoqueTest.Banco()) {
            migrar(b);
            b.sql("INSERT INTO empresas(id,nome) VALUES(90,'Outra empresa')");
            b.sql("INSERT INTO clientes(id,empresa_id,codigo,nome) VALUES(100,1,'A','Cliente A'),(101,90,'A','Cliente A')");
            b.recusar("INSERT INTO clientes(id,empresa_id,codigo,nome) VALUES(102,1,'A','Duplicado')");
            b.recusar("INSERT INTO clientes(id,empresa_id,codigo,nome) VALUES(102,1,'B',' ')");
            b.recusar("INSERT INTO clientes(id,empresa_id,codigo,nome) VALUES(102,999,'B','Órfão')");
            b.recusar("INSERT INTO clientes(id,empresa_id,codigo,nome) VALUES(102,1,NULL,'Sem código')");
            b.recusar("INSERT INTO estoques(id,empresa_id,codigo,nome,tipo) VALUES(200,1,'A','Custódia','CLIENTE')");
            b.recusar("INSERT INTO estoques(id,empresa_id,codigo,nome,tipo,cliente_id) VALUES(200,1,'A','Custódia','CLIENTE',101)");
            b.recusar("INSERT INTO estoques(id,empresa_id,codigo,nome,tipo,cliente_id) VALUES(200,1,'A','Empresa','EMPRESA',100)");
            b.recusar("UPDATE ordens SET tipo_atendimento='CLIENTE',cliente_id=101 WHERE id=20");
            b.recusar("UPDATE ordens SET tipo_atendimento='CLIENTE' WHERE id=20");
            b.recusar("UPDATE ordens SET cliente_id=100 WHERE id=20");
            b.recusar("UPDATE ordens SET tipo_atendimento='OUTRO' WHERE id=20");
            b.sql("UPDATE ordens SET tipo_atendimento='CLIENTE',cliente_id=100 WHERE id=20");
            b.recusar("DELETE FROM clientes WHERE id=100");
        }
    }

    @Test void preflightContaRecusaInconsistenciaSemAlterarDados() throws Exception {
        try(var b=new PropriedadeEstoqueTest.Banco()) {
            v16(b);b.sql("INSERT INTO estoques(id,empresa_id,codigo,nome,tipo) VALUES(100,1,'A','Custódia','CLIENTE')");
            b.script("db/migration/V17__clientes_e_atendimento.sql");
            b.sql("UPDATE estoques SET cliente_id=NULL WHERE id=100");
            assertThatThrownBy(()->b.script("db/"+b.vendor+"/V18__integridade_clientes.sql"))
                    .hasStackTraceContaining("ck_estoques_cliente: 1 violation(s). No data changed.");
            assertThat(b.numero("SELECT count(*) FROM estoques WHERE id=100 AND cliente_id IS NULL")).isEqualTo(1);
            assertThat(b.numero("SELECT count(*) FROM clientes")).isEqualTo(1);
        }
    }

    @Test void voltaRecusaDescartarNovoClienteOuAtendimentoIdentificado() throws Exception {
        try(var b=new PropriedadeEstoqueTest.Banco()) {
            migrar(b);b.sql("INSERT INTO clientes(id,empresa_id,codigo,nome) VALUES(100,1,'A','Novo cliente')");
            assertThatThrownBy(()->voltar(b)).hasStackTraceContaining("rollback_V17_clientes_preservados: 1");
            assertThat(b.numero("SELECT count(*) FROM clientes WHERE id=100")).isEqualTo(1);
        }
        try(var b=new PropriedadeEstoqueTest.Banco()) {
            migrar(b);b.sql("UPDATE ordens SET tipo_atendimento='INTERNO' WHERE id=20");
            assertThatThrownBy(()->voltar(b)).hasStackTraceContaining("rollback_V17_atendimentos_preservados: 1");
            assertThat(b.numero("SELECT count(*) FROM ordens WHERE tipo_atendimento='INTERNO'")).isEqualTo(1);
        }
    }
}
