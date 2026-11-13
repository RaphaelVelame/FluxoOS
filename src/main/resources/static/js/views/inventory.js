import { api, chaveOperacao } from '../api.js';
import { icon, escape, numero, badge, vazio, modal, campo, data, movimentos } from '../components.js';
import { controlesProprietarios, novoEstoque, seletorEstoque } from './inventory-owners.js';

export function estoque(ctx) {
  const materiais = ctx.estado.materiais;
  const proprietarios = controlesProprietarios(ctx);
  const alertas = materiais.filter(m => m.ativo && m.abaixoDoMinimo).length;
  ctx.pagina(`<div class="inventory-summary"><span>${icon('box')}<strong>${materiais.filter(m => m.ativo).length}</strong> materiais ativos</span><span class="warning-text">${icon('alert')}<strong>${alertas}</strong> abaixo do mínimo</span></div>
    ${proprietarios.html}<div class="toolbar"><label class="search-field">${icon('search')}<input id="stock-search" type="search" placeholder="Buscar material, código ou cliente" aria-label="Buscar material"></label><label class="check-filter"><input type="checkbox" id="low-only"> Apenas em alerta</label><label class="check-filter"><input type="checkbox" id="inactive"> Mostrar inativos</label></div><div id="stock-list" class="stock-grid"></div>`,
    { titulo: 'Estoque de materiais', subtitulo: 'Disponibilidade para manter o trabalho em dia', acao: ctx.gestor ? { texto: 'Cadastrar material', executar: () => novoMaterial(ctx) } : null });
  const render = () => {
    const busca = document.querySelector('#stock-search').value.toLocaleLowerCase('pt-BR');
    const lista = materiais.filter(m => (m.ativo || document.querySelector('#inactive').checked)
      && (!document.querySelector('#low-only').checked || (m.ativo && m.abaixoDoMinimo))
      && proprietarios.filtrar(m)
      && `${m.nome} ${m.codigo} ${m.referenciaFabricante ?? ''} ${ctx.nomeEstoque(m.estoqueId)}`.toLocaleLowerCase('pt-BR').includes(busca));
    document.querySelector('#stock-list').innerHTML = lista.length ? lista.map(m => `<article class="stock-card ${!m.ativo ? 'inactive' : ''}"><div class="card-top"><span class="material-icon">${icon('box')}</span>${!m.ativo ? badge('Inativo', 'neutral') : m.abaixoDoMinimo ? badge('Repor estoque', 'alta') : badge('Disponível', 'concluida')}</div><span class="small-code">${escape(m.codigo)}</span><h2>${escape(m.nome)}</h2>${m.referenciaFabricante ? `<p class="muted">Ref. fabricante: ${escape(m.referenciaFabricante)}</p>` : ''}<div class="stock-amount"><strong>${numero(m.quantidade)}</strong><span>${escape(m.unidade)}</span><small>Mínimo: ${numero(m.quantidadeMinima)} ${escape(m.unidade)}</small></div><div class="stock-progress"><progress max="${Math.max(m.quantidade, m.quantidadeMinima * 2, 1)}" value="${m.quantidade}" aria-label="Saldo em relação ao mínimo"></progress></div><div class="card-bottom"><button class="text-button" data-history="${m.id}">Ver movimentos ${icon('arrow')}</button>${ctx.gestor ? `<button class="button secondary small" data-move="${m.id}" ${!m.ativo ? 'disabled' : ''}>Movimentar</button>` : ''}</div>${ctx.gestor ? `<button class="text-button material-status" data-status="${m.id}">${m.ativo ? 'Desativar' : 'Ativar'} material</button>` : ''}</article>`).join('') : vazio('Nenhum material encontrado', 'Ajuste a busca ou cadastre um novo material.', 'box');
    document.querySelectorAll('[data-move]').forEach(el => { el.onclick = () => movimentar(ctx, Number(el.dataset.move)); });
    document.querySelectorAll('[data-history]').forEach(el => { el.onclick = () => historicoMaterial(ctx, Number(el.dataset.history)); });
    document.querySelectorAll('[data-status]').forEach(el => { el.onclick = () => statusMaterial(ctx, Number(el.dataset.status)); });
    lista.forEach(m => document.querySelector(`[data-history="${m.id}"]`).closest('.stock-card').querySelector('h2').insertAdjacentHTML('afterend', proprietarios.card(m)));
    proprietarios.aposRender(lista);
  };
  document.querySelectorAll('#stock-search, #low-only, #inactive').forEach(el => el.addEventListener('input', render));
  proprietarios.ativar(render);
  render();
}

function novoMaterial(ctx) {
  if (!ctx.estado.estoques.length) { novoEstoque(ctx); return; }
  modal('Cadastrar material', `<p class="form-intro">O material começa com saldo zero. Registre uma entrada para repor o estoque.</p>${campo('Código único', 'codigo', 'text', 'maxlength="60" placeholder="Ex.: MAT-004"')}${campo('Nome do material', 'nome', 'text', 'maxlength="160"')}<div class="form-grid">${campo('Unidade', 'unidade', 'text', 'maxlength="20" placeholder="un, m, kg…"')}${campo('Quantidade mínima', 'minimo', 'number', 'min="0" step="1" value="0"')}</div>`, { textoBotao: 'Cadastrar material', aoEnviar: async e => {
    return api('/materiais', { method: 'POST', body: { ...e, minimo: Number(e.minimo), estoqueId: Number(e.estoqueId) } });
  }, aoAbrir: el => el.querySelector('.modal-body').insertAdjacentHTML('afterbegin', seletorEstoque(ctx)), aoSalvar: () => ctx.aposGravar('Material cadastrado.') });
}

function movimentar(ctx, id) {
  const material = ctx.estado.materiais.find(m => m.id === id);
  const key = chaveOperacao();
  modal(`Movimentar · ${material.nome}`, `<p class="form-intro">Saldo atual: <strong>${material.quantidade} ${escape(material.unidade)}</strong>. O consumo de atendimento deve ser registrado dentro da OS.</p><label>Tipo de movimento<select name="tipo"><option value="ENTRADA">Entrada de material</option><option value="DEVOLUCAO">Devolução ao estoque</option><option value="AJUSTE">Ajuste de inventário</option></select></label>${campo('Variação da quantidade', 'variacao', 'number', 'step="1" min="1"')}<p class="hint" id="variation-hint">Informe a quantidade que será adicionada ao estoque.</p>${campo('Motivo da movimentação', 'observacao', 'text', 'maxlength="500"')}`, {
    textoBotao: 'Confirmar movimentação', aoAbrir: el => {
      el.querySelector('[name=tipo]').onchange = e => {
        const ajuste = e.target.value === 'AJUSTE';
        const quantidade = el.querySelector('[name=variacao]');
        if (ajuste) quantidade.removeAttribute('min'); else quantidade.min = '1';
        el.querySelector('#variation-hint').textContent = ajuste ? 'Informe a diferença: +5 adiciona cinco; -5 retira cinco. Não informe o saldo final.' : 'Informe a quantidade que será adicionada ao estoque.';
      };
    }, aoEnviar: async e => {
      return api(`/materiais/${id}/movimentos`, { method: 'POST', key, body: { ...e, variacao: Number(e.variacao) } });
    },
    aoSalvar: () => ctx.aposGravar('Estoque atualizado.'),
  });
}

function historicoMaterial(ctx, id) {
  const material = ctx.estado.materiais.find(m => m.id === id);
  const lista = ctx.estado.movimentos.filter(m => m.materialId === id);
  modal(`Movimentos · ${material.nome}`, lista.length ? `<div class="movement-list">${lista.map(m => `<article class="movement"><span class="movement-number ${m.variacao > 0 ? 'positive' : 'negative'}">${m.variacao > 0 ? '+' : ''}${m.variacao}</span><div><strong>${escape(movimentos[m.tipo])}${m.ordemId ? ` · OS #${m.ordemId}` : ''}</strong><p>${escape(m.observacao)}</p><small class="item-owner">${escape(ctx.nomeEstoque(m.estoqueId))}</small><small>${data(m.criadoEm)} · ${escape(ctx.nomeUsuario(m.usuarioId))}</small></div><small>Saldo<br><strong>${m.saldoApos}</strong></small></article>`).join('')}</div>` : vazio('Sem movimentos', 'As entradas e os consumos deste material aparecerão aqui.', 'box'), { largo: true });
}

function statusMaterial(ctx, id) {
  const material = ctx.estado.materiais.find(m => m.id === id);
  modal(`${material.ativo ? 'Desativar' : 'Ativar'} material`, `<p>${escape(material.nome)}</p><p class="muted">${material.ativo ? 'O material continuará no histórico e ficará indisponível para novas movimentações.' : 'O material ficará disponível para novas movimentações.'}</p>`, { textoBotao: 'Confirmar', aoEnviar: async () => {
    return api(`/materiais/${id}/status`, { method: 'PATCH', body: { ativo: !material.ativo } });
  }, aoSalvar: () => ctx.aposGravar('Status do material atualizado.') });
}
