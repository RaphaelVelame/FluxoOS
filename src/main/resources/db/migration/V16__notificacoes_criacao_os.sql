CREATE TABLE notificacoes (
    id BIGINT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    ordem_id BIGINT NOT NULL,
    destinatario_id BIGINT NOT NULL,
    mensagem VARCHAR(240) NOT NULL CHECK (length(trim(mensagem)) > 0),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    lida BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uq_notificacao_os_destinatario UNIQUE(ordem_id,destinatario_id),
    CONSTRAINT fk_notificacao_empresa FOREIGN KEY(empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_notificacao_os_tenant FOREIGN KEY(ordem_id,empresa_id) REFERENCES ordens(id,empresa_id),
    CONSTRAINT fk_notificacao_destino_tenant FOREIGN KEY(destinatario_id,empresa_id) REFERENCES usuarios(id,empresa_id)
);
CREATE INDEX idx_notificacoes_destino ON notificacoes(empresa_id,destinatario_id,id);
