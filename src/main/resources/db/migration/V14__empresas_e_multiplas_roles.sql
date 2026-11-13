-- Atualizacao de negocio: o legado e vinculado a "Empresa legada", conforme escolha do usuario.
-- Nao redefine senha, perfil original, saldo, propriedade de material ou historico.
CREATE TABLE empresas (
    id BIGINT PRIMARY KEY,
    nome VARCHAR(160) NOT NULL CHECK (length(trim(nome)) > 0)
);
INSERT INTO empresas(id,nome) VALUES(1,'Empresa legada');
ALTER TABLE usuarios ADD COLUMN empresa_id BIGINT DEFAULT 1;
ALTER TABLE ordens ADD COLUMN empresa_id BIGINT DEFAULT 1 NOT NULL;
ALTER TABLE materiais ADD COLUMN empresa_id BIGINT DEFAULT 1 NOT NULL;
ALTER TABLE estoques ADD COLUMN empresa_id BIGINT DEFAULT 1 NOT NULL;
ALTER TABLE movimentos ADD COLUMN empresa_id BIGINT DEFAULT 1 NOT NULL;
ALTER TABLE itens_os ADD COLUMN empresa_id BIGINT DEFAULT 1 NOT NULL;

CREATE TABLE usuario_roles (
    usuario_id BIGINT NOT NULL,
    role VARCHAR(40) NOT NULL,
    PRIMARY KEY(usuario_id,role)
);
INSERT INTO usuario_roles(usuario_id,role) SELECT id,'ROLE_ADMIN_EMPRESA' FROM usuarios WHERE perfil='CHEFE';
INSERT INTO usuario_roles(usuario_id,role) SELECT id,'ROLE_SUPORTE_TECNICO' FROM usuarios WHERE perfil='SUPORTE_TI';
INSERT INTO usuario_roles(usuario_id,role) SELECT id,'ROLE_ESTOQUISTA' FROM usuarios WHERE perfil='SUPORTE_TI';
INSERT INTO usuario_roles(usuario_id,role) SELECT id,'ROLE_SUPORTE_EXTERNO' FROM usuarios WHERE perfil='TECNICO';
-- SOLICITANTE permanece com consulta legada; nao recebe papel operacional por inferencia.
