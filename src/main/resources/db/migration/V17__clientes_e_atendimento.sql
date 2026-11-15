-- V17: cadastro de clientes e vinculos explicitos. Nenhum material muda de proprietario ou saldo.
CREATE TABLE clientes (id BIGINT, empresa_id BIGINT, codigo VARCHAR(60), nome VARCHAR(160));
ALTER TABLE estoques ADD COLUMN cliente_id BIGINT;
ALTER TABLE ordens ADD COLUMN cliente_id BIGINT;
-- LEGADO significa cliente desconhecido; nunca deduzir pelo texto do solicitante.
ALTER TABLE ordens ADD COLUMN tipo_atendimento VARCHAR(20) DEFAULT 'LEGADO';
-- Cada estoque CLIENTE preexistente origina um cliente, sem unir nomes semelhantes.
INSERT INTO clientes(id,empresa_id,codigo,nome)
SELECT nextval('sequencia_ids'),empresa_id,codigo,nome FROM estoques WHERE tipo='CLIENTE';
UPDATE estoques SET cliente_id=(SELECT c.id FROM clientes c WHERE c.empresa_id=estoques.empresa_id AND c.codigo=estoques.codigo)
WHERE tipo='CLIENTE';
