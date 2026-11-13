import { api } from '../api.js';
import { escape, icon, modal, campo } from '../components.js';

export function seletorEstoque(ctx, nome = 'estoqueId') {
  return `<label>Estoque proprietário<select name="${nome}" required><option value="" disabled selected>Selecione a empresa ou o cliente</option>${ctx.estado.estoques.map(e => `<option value="${e.id}">${escape(ctx.nomeEstoque(e.id))} · ${escape(e.codigo)}</option>`).join('')}</select></label>`;
}

export function novoEstoque(ctx) {
  modal('Novo estoque', `<p class="form-intro">Separe os materiais da empresa dos materiais de cada cliente. Cada estoque terá seus próprios cadastros e saldos.</p><label>Proprietário<select name="tipo" required><option value="EMPRESA">Empresa</option><option value="CLIENTE">Cliente</option></select></label>${campo('Código do estoque', 'codigo', 'text', 'maxlength="60" placeholder="Ex.: EMPRESA ou CLI-001"')}${campo('Nome da empresa, unidade ou cliente', 'nome', 'text', 'maxlength="160" placeholder="Ex.: Matriz ou São Lucas"')}`, {
    textoBotao: 'Criar estoque',
    aoEnviar: e => api('/estoques', { method: 'POST', body: e }),
    aoSalvar: () => ctx.aposGravar('Estoque criado.'),
  });
}

function classificar(ctx, ids) {
  if (!ctx.estado.estoques.length) { novoEstoque(ctx); return; }
  modal('Vincular materiais ao estoque', `<p class="form-intro">${ids.length} ${ids.length === 1 ? 'material selecionado' : 'materiais selecionados'}. Confirme quem é o proprietário.</p>${seletorEstoque(ctx)}<p class="hint">O saldo permanece igual. Movimentos anteriores mantêm a origem registrada na época. Materiais já classificados não podem mudar de proprietário nesta tela.</p>`, {
    textoBotao: 'Confirmar vínculo',
    aoEnviar: e => api('/materiais/estoque', { method: 'PATCH', body: { estoqueId: Number(e.estoqueId), materialIds: ids } }),
    aoSalvar: () => ctx.aposGravar('Materiais vinculados ao estoque.'),
  });
}

export function controlesProprietarios(ctx) {
  const selecionados = new Set();
  const porId = new Map(ctx.estado.estoques.map(e => [e.id, e]));
  const totais = { EMPRESA: 0, CLIENTE: 0, SEM: 0, PENDENTE: 0 };
  ctx.estado.materiais.filter(m => m.ativo).forEach(m => totais[m.estoqueId == null ? 'SEM' : porId.get(m.estoqueId)?.tipo || 'PENDENTE']++);
  return {
    html: `<section class="ownership-panel panel"><div class="section-header"><div><h2>Estoque por proprietário</h2><p>Empresa, clientes e materiais que aguardam classificação</p></div>${ctx.gestor ? `<button id="new-stock" class="button secondary small">${icon('plus')} Novo estoque</button>` : ''}</div><div class="ownership-counts"><span>Empresa <strong>${totais.EMPRESA}</strong></span><span>Clientes <strong>${totais.CLIENTE}</strong></span><span>Sem classificação <strong>${totais.SEM}</strong></span>${totais.PENDENTE ? `<span>Estoque a confirmar <strong>${totais.PENDENTE}</strong></span>` : ''}</div><div class="ownership-filters"><label>Tipo de proprietário<select id="stock-kind"><option value="">Todos</option><option value="EMPRESA">Empresa</option><option value="CLIENTE">Clientes</option><option value="SEM">Sem classificação</option></select></label><label>Estoque / cliente<select id="stock-owner"><option value="">Todos os estoques</option>${ctx.estado.estoques.map(e => `<option value="${e.id}">${escape(ctx.nomeEstoque(e.id))}</option>`).join('')}</select></label></div><p id="stock-scope" class="hint" aria-live="polite"></p>${ctx.gestor ? '<div class="ownership-selection"><label class="check-filter"><input id="select-unclassified" type="checkbox"> Selecionar sem classificação (até 500)</label><button id="classify-selected" class="button secondary small" disabled>Vincular selecionados</button></div>' : ''}</section>`,
    filtrar(m) {
      const tipo = document.querySelector('#stock-kind').value;
      const id = document.querySelector('#stock-owner').value;
      return (!tipo || (tipo === 'SEM' ? m.estoqueId == null : porId.get(m.estoqueId)?.tipo === tipo))
        && (!id || m.estoqueId === Number(id));
    },
    card(m) {
      const tipo = porId.get(m.estoqueId)?.tipo;
      return `<div class="ownership-card"><span class="owner-badge ${tipo === 'CLIENTE' ? 'owner-client' : tipo === 'EMPRESA' ? 'owner-company' : 'owner-empty'}">${escape(ctx.nomeEstoque(m.estoqueId))}</span>${ctx.gestor && m.estoqueId == null ? `<label class="check-filter owner-select"><input type="checkbox" data-classify-select="${m.id}" aria-label="Selecionar ${escape(m.nome)} (${escape(m.codigo)})" ${selecionados.has(m.id) ? 'checked' : ''}> Selecionar</label><button class="text-button" data-classify="${m.id}">Vincular a um estoque</button>` : ''}</div>`;
    },
    ativar(render) {
      document.querySelector('#new-stock')?.addEventListener('click', () => novoEstoque(ctx));
      document.querySelector('#classify-selected')?.addEventListener('click', () => classificar(ctx, [...selecionados]));
      document.querySelector('#stock-kind').addEventListener('change', () => {
        const tipo = document.querySelector('#stock-kind').value;
        const owner = document.querySelector('#stock-owner');
        owner.innerHTML = '<option value="">Todos os estoques</option>' + ctx.estado.estoques.filter(e => !tipo || e.tipo === tipo).map(e => `<option value="${e.id}">${escape(ctx.nomeEstoque(e.id))}</option>`).join('');
        owner.disabled = tipo === 'SEM';
        selecionados.clear(); render();
      });
      document.querySelector('#stock-owner').addEventListener('change', () => { selecionados.clear(); render(); });
      document.querySelector('#select-unclassified')?.addEventListener('change', e => {
        selecionados.clear();
        if (e.target.checked) [...document.querySelectorAll('[data-classify-select]')].slice(0, 500).forEach(el => selecionados.add(Number(el.dataset.classifySelect)));
        render();
      });
    },
    aposRender(lista) {
      const visiveis = new Set(lista.filter(m => m.estoqueId == null).map(m => m.id));
      for (const id of selecionados) if (!visiveis.has(id)) selecionados.delete(id);
      document.querySelector('#stock-scope').textContent = `${lista.length} ${lista.length === 1 ? 'material nesta seleção' : 'materiais nesta seleção'}`;
      const barra = document.querySelector('.ownership-selection');
      if (barra) barra.hidden = visiveis.size === 0;
      const atualizarSelecao = () => {
        const botao = document.querySelector('#classify-selected');
        if (botao) { botao.disabled = selecionados.size === 0; botao.textContent = `Vincular selecionados${selecionados.size ? ` (${selecionados.size})` : ''}`; }
        const todos = document.querySelector('#select-unclassified');
        if (todos) { todos.disabled = visiveis.size === 0; todos.checked = selecionados.size > 0 && selecionados.size === Math.min(visiveis.size, 500); todos.indeterminate = selecionados.size > 0 && !todos.checked; }
      };
      document.querySelectorAll('[data-classify]').forEach(el => { el.onclick = () => classificar(ctx, [Number(el.dataset.classify)]); });
      document.querySelectorAll('[data-classify-select]').forEach(el => {
        el.onchange = () => {
          const id = Number(el.dataset.classifySelect);
          if (el.checked && selecionados.size < 500) selecionados.add(id); else selecionados.delete(id);
          el.checked = selecionados.has(id);
          atualizarSelecao();
        };
      });
      atualizarSelecao();
    },
  };
}
