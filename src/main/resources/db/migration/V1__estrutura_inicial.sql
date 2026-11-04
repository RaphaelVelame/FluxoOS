CREATE SEQUENCE sequencia_ids START WITH 100;

CREATE TABLE usuarios (
    id BIGINT PRIMARY KEY,
    nome_usuario VARCHAR(80) NOT NULL UNIQUE,
    nome_completo VARCHAR(160) NOT NULL,
    perfil VARCHAR(30) NOT NULL CHECK (perfil IN ('CHEFE','SUPORTE_TI','TECNICO','SOLICITANTE')),
    ativo BOOLEAN NOT NULL,
    hash_senha VARCHAR(300) NOT NULL
);

CREATE TABLE materiais (
    id BIGINT PRIMARY KEY,
    codigo VARCHAR(60) NOT NULL UNIQUE,
    nome VARCHAR(160) NOT NULL,
    unidade VARCHAR(20) NOT NULL,
    quantidade INTEGER NOT NULL CHECK (quantidade >= 0),
    quantidade_minima INTEGER NOT NULL CHECK (quantidade_minima >= 0),
    ativo BOOLEAN NOT NULL
);

CREATE TABLE ordens (
    id BIGINT PRIMARY KEY,
    solicitante VARCHAR(160) NOT NULL,
    local VARCHAR(240) NOT NULL,
    problema VARCHAR(4000) NOT NULL,
    prioridade VARCHAR(20) NOT NULL CHECK (prioridade IN ('BAIXA','MEDIA','ALTA')),
    situacao VARCHAR(30) NOT NULL CHECK (situacao IN ('ABERTA','EM_ANDAMENTO','CONCLUIDA')),
    equipe_id BIGINT REFERENCES usuarios(id),
    relato VARCHAR(8000),
    responsavel VARCHAR(160),
    assinatura TEXT,
    assinado_em TIMESTAMP WITH TIME ZONE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE itens_os (
    ordem_id BIGINT NOT NULL REFERENCES ordens(id),
    material_id BIGINT NOT NULL REFERENCES materiais(id),
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    PRIMARY KEY (ordem_id, material_id)
);

CREATE TABLE movimentos (
    id BIGINT PRIMARY KEY,
    material_id BIGINT NOT NULL REFERENCES materiais(id),
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    ordem_id BIGINT REFERENCES ordens(id),
    tipo VARCHAR(30) NOT NULL,
    variacao INTEGER NOT NULL CHECK (variacao <> 0),
    saldo_apos INTEGER NOT NULL CHECK (saldo_apos >= 0),
    observacao VARCHAR(500) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE idempotencia (
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    chave VARCHAR(80) NOT NULL,
    conteudo VARCHAR(64) NOT NULL,
    movimento_id BIGINT NOT NULL REFERENCES movimentos(id),
    PRIMARY KEY (usuario_id, chave)
);

CREATE TABLE auditoria (
    id BIGINT PRIMARY KEY,
    ordem_id BIGINT REFERENCES ordens(id),
    usuario_id BIGINT REFERENCES usuarios(id),
    tipo VARCHAR(60) NOT NULL,
    detalhes VARCHAR(1000) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ordens_equipe ON ordens(equipe_id);
CREATE INDEX idx_movimentos_material ON movimentos(material_id, criado_em);
CREATE INDEX idx_auditoria_ordem ON auditoria(ordem_id, criado_em);
