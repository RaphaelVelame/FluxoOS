package br.com.fluxoos;

import br.com.fluxoos.seguranca.SenhaPbkdf2;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import java.sql.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class PropriedadeEstoqueTest {
    private static final String HASH = new SenhaPbkdf2().gerarHash("Fixture-proprietario-2026!".toCharArray());

    @Test void atualizacaoPreservaLegadoSemAtribuirProprietario() throws Exception {
        try (Banco b = new Banco()) {
            var antes = b.legado();
            b.migrar13();
            assertThat(b.legado()).isEqualTo(antes);
            assertThat(b.numero("SELECT count(*) FROM estoques")).isZero();
            for(String t:List.of("materiais","movimentos","itens_os"))
                assertThat(b.numero("SELECT count(*) FROM "+t+" WHERE estoque_id IS NOT NULL")).isZero();
        }
    }

    @Test void bancoGaranteProprietarioTipoCodigoEOrigemDosSnapshots() throws Exception {
        try (Banco b = new Banco()) {
            b.migrar13();
            b.sql("INSERT INTO estoques(id,codigo,nome,tipo) VALUES(100,'EMP','Empresa','EMPRESA'),(101,'CLI','Cliente','CLIENTE')");
            b.recusar("INSERT INTO estoques(id,codigo,nome,tipo) VALUES(102,'EMP','Outro','CLIENTE')");
            b.recusar("INSERT INTO estoques(id,codigo,nome,tipo) VALUES(102,'NOVO','Outro','OUTRO')");
            b.recusar("UPDATE estoques SET nome=' ' WHERE id=100");
            b.recusar("UPDATE estoques SET codigo=' EMP' WHERE id=100");
            b.recusar("UPDATE estoques SET nome=NULL WHERE id=100");
            b.recusar("UPDATE materiais SET estoque_id=999 WHERE id=10");
            b.sql("UPDATE materiais SET estoque_id=100 WHERE id=10");
            assertThat(b.numero("SELECT count(*) FROM movimentos WHERE estoque_id IS NOT NULL")).isZero();
            b.sql("INSERT INTO movimentos(id,material_id,usuario_id,estoque_id,tipo,variacao,saldo_apos,observacao) VALUES(32,10,1,100,'ENTRADA',1,9,'Origem conhecida')");
            b.recusar("INSERT INTO movimentos(id,material_id,usuario_id,estoque_id,tipo,variacao,saldo_apos,observacao) VALUES(33,10,1,101,'ENTRADA',1,10,'Cliente incorreto')");
            b.recusar("UPDATE itens_os SET estoque_id=101 WHERE ordem_id=20");
            b.sql("UPDATE itens_os SET estoque_id=100 WHERE ordem_id=20");
            b.recusar("DELETE FROM estoques WHERE id=100");
            assertThat(b.numero("SELECT quantidade FROM materiais WHERE id=10")).isEqualTo(8);
        }
    }

    @Test void reversaoSemUsoPreservaDadosEPermiteReaplicar() throws Exception {
        try (Banco b = new Banco()) {
            var antes = b.legado();
            b.migrar13();
            b.script("db/rollback/"+b.vendor+"/V13__propriedade_estoque.down.sql");
            assertThat(b.legado()).isEqualTo(antes);
            b.script("db/"+b.vendor+"/V13__propriedade_estoque.sql");
            assertThat(b.legado()).isEqualTo(antes);
            assertThat(b.numero("SELECT count(*) FROM estoques")).isZero();
        }
    }

    @Test void reversaoComDadosNovosEhBloqueadaSemPerda() throws Exception {
        try (Banco b = new Banco()) {
            b.migrar13();
            b.sql("INSERT INTO estoques(id,codigo,nome,tipo) VALUES(100,'EMP','Empresa','EMPRESA')");
            assertThatThrownBy(()->b.script("db/rollback/"+b.vendor+"/V13__propriedade_estoque.down.sql"))
                    .hasStackTraceContaining("rollback_V13_propriedade_preservada: 1 violation(s). No data changed.");
            assertThat(b.numero("SELECT count(*) FROM estoques")).isEqualTo(1);
            b.sql("UPDATE materiais SET estoque_id=100 WHERE id=10");
            assertThatThrownBy(()->b.script("db/rollback/"+b.vendor+"/V13__propriedade_estoque.down.sql"))
                    .hasStackTraceContaining("rollback_V13_propriedade_preservada: 2 violation(s). No data changed.");
            assertThat(b.numero("SELECT estoque_id FROM materiais WHERE id=10")).isEqualTo(100);
            assertThat(b.numero("SELECT quantidade FROM materiais WHERE id=10")).isEqualTo(8);
        }
    }

    static final class Banco implements AutoCloseable {
        final String url=System.getenv().getOrDefault("TEST_DATABASE_URL","jdbc:h2:mem:propriedade;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        final String usuario=System.getenv().getOrDefault("TEST_DATABASE_USER","sa");
        final String senha=System.getenv().getOrDefault("TEST_DATABASE_PASSWORD","");
        final String schema="propriedade_"+UUID.randomUUID().toString().replace("-","");
        final String vendor=url.startsWith("jdbc:postgresql:")?"postgresql":"h2";
        final Connection c;
        Banco() throws Exception {
            var flyway=MigracoesTeste.configurar(url,usuario,senha,schema);
            c=DriverManager.getConnection(url,usuario,senha);
            try {
                flyway.target("12").load().migrate(); c.setSchema(schema);
                sql("INSERT INTO usuarios(id,nome_usuario,nome_completo,perfil,ativo,hash_senha) VALUES(1,'admin','Administrador','CHEFE',true,'"+HASH+"'),(2,'tecnico','Tecnico','TECNICO',true,'"+HASH+"')");
                sql("INSERT INTO materiais(id,codigo,nome,unidade,quantidade,quantidade_minima,ativo) VALUES(10,'CABO','Cabo original','m',8,0,true)");
                sql("INSERT INTO ordens(id,solicitante,local,problema,prioridade,situacao,equipe_id,relato) VALUES(20,'Pessoa','Local','Rede','MEDIA','EM_ANDAMENTO',2,'Relato original')");
                sql("INSERT INTO movimentos(id,material_id,usuario_id,ordem_id,tipo,variacao,saldo_apos,observacao) VALUES(30,10,1,NULL,'ENTRADA',10,10,'Entrada original'),(31,10,2,20,'SAIDA',-2,8,'Uso original')");
                sql("INSERT INTO itens_os(ordem_id,material_id,quantidade) VALUES(20,10,2)");
            } catch(Exception | Error e) {close();throw e;}
        }
        void migrar13(){MigracoesTeste.configurar(url,usuario,senha,schema).target("13").load().migrate();}
        void sql(String s)throws SQLException{try(var st=c.createStatement()){st.execute(s);}}
        int numero(String s)throws SQLException{try(var st=c.createStatement();var r=st.executeQuery(s)){r.next();return r.getInt(1);}}
        void recusar(String s){assertThatExceptionOfType(SQLException.class).isThrownBy(()->sql(s)).satisfies(e->assertThat(e.getSQLState()).startsWith("23"));}
        void script(String recurso){
            try {ScriptUtils.executeSqlScript(c,new ClassPathResource(recurso));}
            catch(RuntimeException e){
                if(vendor.equals("postgresql"))try{sql("ROLLBACK");}catch(SQLException rollback){e.addSuppressed(rollback);}
                throw e;
            }
        }
        Map<String,List<List<String>>> legado()throws SQLException{
            var dados=new LinkedHashMap<String,List<List<String>>>();
            for(String t:List.of("usuarios","materiais","ordens","movimentos","itens_os"))try(var s=c.createStatement();var r=s.executeQuery("SELECT * FROM "+t+" ORDER BY 1,2")){
                var linhas=new ArrayList<List<String>>();
                while(r.next()){var linha=new ArrayList<String>();for(int n=1;n<=r.getMetaData().getColumnCount();n++)if(!Set.of("estoque_id","empresa_id").contains(r.getMetaData().getColumnName(n).toLowerCase(Locale.ROOT)))linha.add(r.getString(n));linhas.add(linha);}
                dados.put(t,linhas);
            }
            return dados;
        }
        public void close()throws SQLException{try(var s=c.createStatement()){s.execute("DROP SCHEMA IF EXISTS "+schema+" CASCADE");}finally{c.close();}}
    }
}
