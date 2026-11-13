package br.com.fluxoos;

import br.com.fluxoos.seguranca.SenhaPbkdf2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class IntegridadeSchemaTest {
    private static final String HASH = new SenhaPbkdf2().gerarHash("Senha-de-fixture-valida-2026!".toCharArray());
    private static final String DIGEST = "a".repeat(64);
    private static final String CHAVE = "00000000-0000-0000-0000-000000000001";
    private static final List<String> TABELAS = List.of("usuarios", "materiais", "ordens", "itens_os", "movimentos", "idempotencia", "auditoria");
    private static final Map<Integer, String> ASSUNTOS = Map.of(6,"textos_obrigatorios",7,"integridade_usuarios",
            8,"regras_movimentos",9,"consistencia_ordens",10,"formato_idempotencia",11,"autor_movimento_idempotencia",12,"consumo_unico_por_os");

    @Test void idaVoltaEReaplicacaoPreservamCadaValorDoBancoLegado() throws Exception {
        try (Banco b = new Banco(5)) {
            var antes = b.snapshot();
            b.migrar(12);
            assertThat(b.snapshot()).isEqualTo(antes);
            assertThat(b.novasConstraints()).isEqualTo(31);
            assertThat(b.escalar("SELECT max(installed_rank) FROM flyway_schema_history")).isEqualTo("12");
            for (int v=12; v>=6; v--) b.script("db/rollback/"+b.vendor+"/V"+v+"__"+ASSUNTOS.get(v)+".down.sql");
            assertThat(b.snapshot()).isEqualTo(antes);
            assertThat(b.novasConstraints()).isZero();
            for (int v=6; v<=12; v++) b.script("db/"+b.vendor+"/V"+v+"__"+ASSUNTOS.get(v)+".sql");
            assertThat(b.novasConstraints()).isEqualTo(31);
            assertThat(b.snapshot()).isEqualTo(antes);
            b.flyway().target("12").load().validate();
        }
    }

    @Test void bancoRecusaTextosInvalidosSemDependerDoFormulario() throws Exception {
        try (Banco b = new Banco(12)) {
            var antes = b.snapshot();
            for (String sql : List.of(
                    "UPDATE usuarios SET nome_completo=' ' WHERE id=1",
                    "UPDATE materiais SET codigo='' WHERE id=10",
                    "UPDATE materiais SET nome=' Material' WHERE id=10",
                    "UPDATE materiais SET unidade='un ' WHERE id=10",
                    "UPDATE ordens SET solicitante=' ' WHERE id=20",
                    "UPDATE ordens SET local='' WHERE id=20",
                    "UPDATE ordens SET problema=' ' WHERE id=20",
                    "UPDATE ordens SET relato=' ' WHERE id=21",
                    "UPDATE ordens SET responsavel=' ' WHERE id=22",
                    "UPDATE movimentos SET observacao='' WHERE id=30",
                    "UPDATE auditoria SET tipo='' WHERE id=40",
                    "UPDATE auditoria SET detalhes=' ' WHERE id=40")) b.recusar(sql);
            b.recusar("UPDATE materiais SET nome=NULL WHERE id=10");
            assertThat(b.snapshot()).isEqualTo(antes);
        }
    }

    @Test void whitespaceUnicodeSegueSemanticaJavaETextoInternoNaoEhAparado() throws Exception {
        try (Banco b = new Banco(12)) {
            for (int cp : new int[]{9,10,11,12,13,28,29,30,31,32,0x1680,0x2000,0x2001,0x2002,0x2003,0x2004,
                    0x2005,0x2006,0x2008,0x2009,0x200a,0x2028,0x2029,0x205f,0x3000}) {
                assertThat(Character.isWhitespace(cp)).isTrue();
                b.recusar("UPDATE materiais SET nome=? WHERE id=10", Character.toString(cp));
            }
            b.recusar("UPDATE materiais SET nome=? WHERE id=10", "\u0001Material");
            b.recusar("UPDATE materiais SET nome=? WHERE id=10", "Material\t");
            for (String valido : List.of("Câmera 🔧", "Linha um\nLinha dois", "\u00a0", "\u2007", "\u202f")) {
                b.executar("UPDATE materiais SET nome=? WHERE id=10",valido);
                assertThat(b.escalar("SELECT nome FROM materiais WHERE id=10")).isEqualTo(valido);
            }
            // Não converte silenciosamente limites VARCHAR em limites UTF-16 do DTO.
            b.executar("UPDATE materiais SET nome=? WHERE id=10","🔧".repeat(80));
            b.recusar("UPDATE materiais SET nome=? WHERE id=10","x".repeat(161));
        }
    }

    @Test void validaLoginHashEVersaoSemTrocarCredenciais() throws Exception {
        try (Banco b = new Banco(12)) {
            for (String login : List.of("ABCD","ab","nome com espaco","ábc","abc\n"))
                b.recusar("UPDATE usuarios SET nome_usuario=? WHERE id=1",login);
            for (String hash : List.of("hash-preservado", "pbkdf2_sha256$99999999999999$x$x",
                    HASH.replace("$600000$","$599999$"),HASH.replace("$600000$","$2000001$"),HASH+"\n"))
                b.recusar("UPDATE usuarios SET hash_senha=? WHERE id=1",hash);
            b.recusar("UPDATE usuarios SET versao_sessao=-1 WHERE id=1");
            b.executar("UPDATE usuarios SET nome_usuario=? WHERE id=1","a".repeat(80));
            b.executar("UPDATE usuarios SET hash_senha=? WHERE id=1",HASH.replace("$600000$","$2000000$"));
        }
    }

    @Test void tipoSinalEOrdemDosMovimentosSaoGarantidosPeloBanco() throws Exception {
        try (Banco b = new Banco(12)) {
            for (String sql : List.of(
                    "UPDATE movimentos SET tipo='OUTRO' WHERE id=30",
                    "UPDATE movimentos SET variacao=-10 WHERE id=30",
                    "UPDATE movimentos SET tipo='DEVOLUCAO',variacao=-10 WHERE id=30",
                    "UPDATE movimentos SET variacao=2 WHERE id=31",
                    "UPDATE movimentos SET ordem_id=NULL WHERE id=31",
                    "UPDATE movimentos SET ordem_id=20 WHERE id=30",
                    "UPDATE movimentos SET variacao=0 WHERE id=30")) b.recusar(sql);
            b.executar("UPDATE movimentos SET tipo='AJUSTE',variacao=-1 WHERE id=30");
            b.executar("UPDATE movimentos SET tipo='DEVOLUCAO',variacao=1 WHERE id=30");
            b.executar("INSERT INTO movimentos(id,material_id,usuario_id,tipo,variacao,saldo_apos,observacao) VALUES(32,10,1,'ENTRADA',1,9,'Outra entrada sem OS')");
        }
    }

    @Test void conclusaoPrecisaSerCompletaEAssinaturaTemLimiteDeEnvelope() throws Exception {
        try (Banco b = new Banco(12)) {
            for (String sql : List.of(
                    "UPDATE ordens SET situacao='EM_ANDAMENTO' WHERE id=20",
                    "UPDATE ordens SET situacao='CONCLUIDA' WHERE id=21",
                    "UPDATE ordens SET relato=NULL WHERE id=22",
                    "UPDATE ordens SET responsavel=NULL WHERE id=22",
                    "UPDATE ordens SET assinatura=NULL WHERE id=22",
                    "UPDATE ordens SET assinado_em=NULL WHERE id=22",
                    "UPDATE ordens SET responsavel='Pessoa' WHERE id=20",
                    "UPDATE ordens SET relato='Relato antes do inicio' WHERE id=20")) b.recusar(sql);
            for (String assinatura : List.of("data:image/jpeg;base64,AAAA","data:image/png;base64,@@@",
                    "data:image/png;base64,"+"A".repeat(200000),"data:image/png;base64,AAAA\n"))
                b.recusar("UPDATE ordens SET assinatura=? WHERE id=22",assinatura);
            b.executar("UPDATE ordens SET relato=? WHERE id=21","x".repeat(8000));
            b.recusar("UPDATE ordens SET relato=? WHERE id=21","x".repeat(8001));
            b.executar("UPDATE ordens SET responsavel='Pessoa',assinatura=?,assinado_em=CURRENT_TIMESTAMP,situacao='CONCLUIDA' WHERE id=21",png());
        }
    }

    @Test void chavesDigestsEAutorDoMovimentoSaoConsistentes() throws Exception {
        try (Banco b = new Banco(12)) {
            b.recusar("UPDATE ordens SET autor_abertura_id=1 WHERE id=20");
            for (String chave : List.of("curta","a".repeat(15),"a".repeat(81),"a".repeat(16)+"!","a".repeat(16)+"\n"))
                b.recusar("UPDATE idempotencia SET chave=?",chave);
            for (String digest : List.of("a".repeat(63),"A".repeat(64),"g".repeat(64)))
                b.recusar("UPDATE idempotencia SET conteudo=?",digest);
            b.executar("UPDATE ordens SET autor_abertura_id=1,chave_abertura=?,conteudo_abertura=? WHERE id=20",CHAVE,DIGEST);
            b.recusar("UPDATE ordens SET chave_abertura='curta' WHERE id=20");
            b.recusar("UPDATE ordens SET conteudo_abertura='invalido' WHERE id=20");
            b.recusar("UPDATE idempotencia SET usuario_id=1"); // ambas FKs simples seriam válidas.
            b.recusar("INSERT INTO idempotencia(usuario_id,chave,conteudo,movimento_id) VALUES(2,?,?,31)",CHAVE,DIGEST);
            b.executar("UPDATE idempotencia SET chave=?","a".repeat(16));
            b.executar("UPDATE idempotencia SET chave=?","z".repeat(80));
        }
    }

    @Test void consumoDuplicadoEhRecusadoMasCadastroComZeroPermaneceValido() throws Exception {
        try (Banco b = new Banco(12)) {
            b.recusar("INSERT INTO movimentos(id,material_id,usuario_id,ordem_id,tipo,variacao,saldo_apos,observacao) VALUES(32,10,2,21,'SAIDA',-1,7,'Duplicado')");
            b.recusar("INSERT INTO itens_os(ordem_id,material_id,quantidade) VALUES(21,10,1)");
            b.recusar("UPDATE movimentos SET material_id=999 WHERE id=30");
            b.executar("INSERT INTO materiais(id,codigo,nome,unidade,quantidade,quantidade_minima,ativo) VALUES(11,'ZERO','Sem saldo','kg',0,0,true)");
            b.executar("UPDATE materiais SET quantidade=0 WHERE id=10");
            b.recusar("UPDATE materiais SET quantidade=-1 WHERE id=10");
        }
    }

    record Legado(int anterior, String sql, String constraint, int violacoes) {}
    static Stream<Legado> legadosInvalidos() {
        return Stream.of(
                new Legado(5,"UPDATE usuarios SET nome_completo=' '","ck_usuarios_nome_completo_preenchido",2),
                new Legado(6,"UPDATE usuarios SET hash_senha='hash-preservado' WHERE id=1","ck_usuarios_hash_formato",1),
                new Legado(7,"UPDATE movimentos SET tipo='DESCONHECIDO' WHERE id=30","ck_movimentos_tipo",1),
                new Legado(8,"UPDATE ordens SET situacao='EM_ANDAMENTO' WHERE id=20","ck_ordens_tecnico_apos_inicio",1),
                new Legado(9,"UPDATE idempotencia SET chave='curta'","ck_idempotencia_chave",1),
                new Legado(10,"UPDATE idempotencia SET usuario_id=1","fk_idempotencia_movimento_autor",1),
                new Legado(11,"INSERT INTO movimentos(id,material_id,usuario_id,ordem_id,tipo,variacao,saldo_apos,observacao) VALUES(32,10,2,21,'SAIDA',-1,7,'Duplicado')","uq_movimentos_consumo_os_material",1));
    }

    @ParameterizedTest @MethodSource("legadosInvalidos")
    void migrationRecusaLegadoInvalidoInformaContagemENaoSaneia(Legado caso) throws Exception {
        try (Banco b = new Banco(caso.anterior)) {
            b.executar(caso.sql);
            var antes = b.snapshot();
            assertThatThrownBy(()->b.migrar(12)).hasStackTraceContaining(
                    "Preflight "+caso.constraint+": "+caso.violacoes+" violation(s). No data changed.");
            assertThat(b.snapshot()).isEqualTo(antes);
        }
    }

    private static String png() throws Exception {
        var imagem = new BufferedImage(80,40,BufferedImage.TYPE_INT_ARGB);
        var g=imagem.createGraphics();
        try { g.setColor(Color.BLACK); g.drawLine(5,5,65,30); } finally { g.dispose(); }
        var bytes=new ByteArrayOutputStream();
        ImageIO.write(imagem,"png",bytes);
        return "data:image/png;base64,"+Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    private static final class Banco implements AutoCloseable {
        final String url=System.getenv().getOrDefault("TEST_DATABASE_URL",
                "jdbc:h2:mem:constraints;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        final String usuario=System.getenv().getOrDefault("TEST_DATABASE_USER","sa");
        final String senha=System.getenv().getOrDefault("TEST_DATABASE_PASSWORD","");
        final String schema="constraints_"+UUID.randomUUID().toString().replace("-","");
        final String vendor=url.startsWith("jdbc:postgresql:")?"postgresql":"h2";
        final Connection conexao;

        Banco(int versao) throws Exception {
            // Configurar antes de abrir a conexao tambem verifica o marcador de banco descartavel.
            flyway();
            conexao=DriverManager.getConnection(url,usuario,senha);
            try {
                migrar(versao);
                conexao.setSchema(schema);
                executar("INSERT INTO usuarios(id,nome_usuario,nome_completo,perfil,ativo,hash_senha) VALUES(1,'admin','Pessoa admin','CHEFE',true,?),(2,'tecnico','Pessoa tecnica','TECNICO',true,?)",HASH,HASH);
                executar("INSERT INTO materiais(id,codigo,nome,unidade,quantidade,quantidade_minima,ativo,referencia_fabricante) VALUES(10,'MAT-10','Câmera 🔧','un',8,2,true,'Referência original')");
                executar("INSERT INTO ordens(id,solicitante,local,problema,prioridade,situacao,equipe_id,relato) VALUES(20,'Pessoa','Sala','Rede','MEDIA','ABERTA',NULL,NULL),(21,'Pessoa','Sala','Rede','ALTA','EM_ANDAMENTO',2,'Relato')");
                executar("INSERT INTO ordens(id,solicitante,local,problema,prioridade,situacao,equipe_id,relato,responsavel,assinatura,assinado_em) VALUES(22,'Pessoa','Sala','Rede','BAIXA','CONCLUIDA',2,'Relato original','Pessoa presente',?,CURRENT_TIMESTAMP)",png());
                executar("INSERT INTO movimentos(id,material_id,usuario_id,ordem_id,tipo,variacao,saldo_apos,observacao) VALUES(30,10,1,NULL,'ENTRADA',10,10,'Entrada original'),(31,10,2,21,'SAIDA',-2,8,'Uso original')");
                executar("INSERT INTO itens_os(ordem_id,material_id,quantidade) VALUES(21,10,2)");
                executar("INSERT INTO idempotencia(usuario_id,chave,conteudo,movimento_id) VALUES(2,?,?,31)",CHAVE,DIGEST);
                executar("INSERT INTO auditoria(id,ordem_id,usuario_id,alvo_id,tipo,detalhes) VALUES(40,21,2,1,'CONSUMO','Snapshot original')");
            } catch (Exception | Error erro) { close(); throw erro; }
        }
        org.flywaydb.core.api.configuration.FluentConfiguration flyway() { return MigracoesTeste.configurar(url,usuario,senha,schema); }
        void migrar(int alvo) { flyway().target(Integer.toString(alvo)).load().migrate(); }
        void executar(String sql,Object... parametros) throws SQLException {
            try(var s=conexao.prepareStatement(sql)) {
                for(int i=0;i<parametros.length;i++)s.setObject(i+1,parametros[i]);
                s.execute();
            }
        }
        void recusar(String sql,Object... parametros) {
            assertThatExceptionOfType(SQLException.class).isThrownBy(()->executar(sql,parametros))
                    .satisfies(erro->assertThat(erro.getSQLState().startsWith("23") || erro.getSQLState().equals("22001"))
                            .as("SQLSTATE deve indicar integridade/limite de tipo, nao erro de sintaxe: %s",erro.getSQLState()).isTrue());
        }
        String escalar(String sql) throws SQLException {
            try(var s=conexao.createStatement();var r=s.executeQuery(sql)){r.next();return r.getString(1);}
        }
        int novasConstraints() throws SQLException {
            try(var s=conexao.prepareStatement("SELECT count(*) FROM information_schema.table_constraints WHERE constraint_schema=? AND (constraint_name LIKE 'ck_%' OR constraint_name IN ('uq_movimentos_id_usuario','fk_idempotencia_movimento_autor','uq_movimentos_consumo_os_material'))")) {
                s.setString(1,schema);try(var r=s.executeQuery()){r.next();return r.getInt(1);}
            }
        }
        Map<String,List<List<String>>> snapshot() throws SQLException {
            var resultado=new LinkedHashMap<String,List<List<String>>>();
            for(String tabela:TABELAS)try(var s=conexao.createStatement();var r=s.executeQuery("SELECT * FROM "+tabela+" ORDER BY 1,2")) {
                var linhas=new ArrayList<List<String>>();
                while(r.next()){var linha=new ArrayList<String>();for(int c=1;c<=r.getMetaData().getColumnCount();c++)linha.add(r.getString(c));linhas.add(linha);}
                resultado.put(tabela,linhas);
            }
            return resultado;
        }
        void script(String recurso) { ScriptUtils.executeSqlScript(conexao,new ClassPathResource(recurso)); }
        public void close() throws SQLException {
            try(var s=conexao.createStatement()){s.execute("DROP SCHEMA IF EXISTS "+schema+" CASCADE");}
            finally {conexao.close();}
        }
    }
}
