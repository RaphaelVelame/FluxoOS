import { api, chaveOperacao } from '../api.js';
import { icon, escape, badge, situacoes, prioridades, vazio, modal, campo, data, toast } from '../components.js';
import { prepararAssinatura } from '../signature.js';

export function novaOrdem(ctx) {
  modal('Nova ordem de serviço', `<p class="form-intro">Conte o que precisa ser resolvido. A equipe pode ser designada em seguida.</p>
    ${campo('Solicitante', 'solicitante', 'text', 'maxlength="160" autocomplete="name"')}
    ${campo('Local do atendimento', 'local', 'text', 'maxlength="240" placeholder="Unidade, setor e sala"')}
    <label>Problema relatado<textarea name="problema" required maxlength="4000" rows="4" placeholder="Descreva a necessidade do atendimento"></textarea></label>
    <label>Prioridade<select name="prioridade"><option value="MEDIA">Média</option><option value="ALTA">Alta</option><option value="BAIXA">Baixa</option></select></label>`, {
    textoBotao: 'Criar ordem de serviço', aoEnviar: async valores => {
      const ordem = await api('/ordens', { method: 'POST', body: valores });
      await ctx.atualizar();
      location.hash = `ordens/${ordem.id}`;
      toast('Ordem de serviço criada.');
    },
  });
}

export function ordens(ctx) {
  ctx.pagina(`<div class="toolbar"><label class="search-field">${icon('search')}<input id="search" type="search" placeholder="Buscar por OS, problema ou local" aria-label="Buscar ordens"></label><label class="sr-only" for="status-filter">Situação</label><select id="status-filter"><option value="">Todas as situações</option>${Object.entries(situacoes).map(([v, t]) => `<option value="${v}">${t}</option>`).join('')}</select><label class="check-filter"><input id="mine" type="checkbox" ${ctx.estado.usuario.perfil === 'TECNICO' ? 'checked' : ''}> Minhas ordens</label></div><div id="orders-list" class="orders-grid"></div>`,
    { titulo: 'Ordens de serviço', subtitulo: 'Do chamado à solução, acompanhe cada etapa', acao: ctx.gestor ? { texto: 'Nova ordem', executar: ctx.novaOrdem } : null });
  const render = () => {
    const termo = document.querySelector('#search').value.toLocaleLowerCase('pt-BR');
    const status = document.querySelector('#status-filter').value;
    const minhas = document.querySelector('#mine').checked;
    const lista = ctx.estado.ordens.filter(o => (!status || o.situacao === status) && (!minhas || o.tecnicoId === ctx.estado.usuario.id)
      && `${o.id} ${o.problema} ${o.local} ${o.solicitante}`.toLocaleLowerCase('pt-BR').includes(termo));
    document.querySelector('#orders-list').innerHTML = lista.length ? lista.map(o => `<a class="order-card" href="#ordens/${o.id}"><div class="card-top"><span class="small-code">OS #${o.id}</span>${badge(prioridades[o.prioridade], o.prioridade)}</div><h2>${escape(o.problema)}</h2><p class="card-location">${icon('pin')}${escape(o.local)}</p><div class="card-requester">Solicitante: ${escape(o.solicitante)}</div><div class="card-bottom">${badge(situacoes[o.situacao], o.situacao)}<span class="assigned-avatar" title="${escape(ctx.nomeUsuario(o.tecnicoId))}">${escape(ctx.iniciais(o.tecnicoId))}</span></div></a>`).join('') : vazio('Nenhuma ordem encontrada', 'Ajuste os filtros ou crie uma nova ordem de serviço.');
  };
  document.querySelectorAll('#search, #status-filter, #mine').forEach(el => el.addEventListener('input', render));
  render();
}

export async function detalheOrdem(ctx, id) {
  const [ordem, historico] = await Promise.all([api(`/ordens/${id}`), api(`/ordens/${id}/historico`)]);
  if (location.hash !== `#ordens/${id}`) return;
  const podeAtender = ctx.gestor || (ctx.estado.usuario.perfil === 'TECNICO' && ordem.tecnicoId === ctx.estado.usuario.id);
  const emAndamento = ordem.situacao === 'EM_ANDAMENTO';
  const finalizada = ordem.situacao === 'CONCLUIDA';
  ctx.pagina(`<a class="back-link" href="#ordens">← Voltar para ordens</a><div class="detail-heading"><div class="badge-row">${badge(situacoes[ordem.situacao], ordem.situacao)}${badge(`Prioridade ${prioridades[ordem.prioridade].toLowerCase()}`, ordem.prioridade)}</div><h2>${escape(ordem.problema)}</h2></div>
    <div class="detail-grid"><div class="detail-main"><section class="panel detail-panel"><h3>Informações do atendimento</h3><dl class="info-grid"><div><dt>Solicitante</dt><dd>${escape(ordem.solicitante)}</dd></div><div><dt>Local</dt><dd>${escape(ordem.local)}</dd></div><div><dt>Técnico responsável</dt><dd>${escape(ctx.nomeUsuario(ordem.tecnicoId))}</dd></div><div><dt>Identificação</dt><dd>OS #${ordem.id}</dd></div></dl><div class="action-row">${ctx.gestor && !finalizada ? '<button id="assign" class="button secondary">Designar técnico</button>' : ''}${podeAtender && ordem.situacao === 'ABERTA' ? '<button id="start" class="button">Iniciar atendimento</button>' : ''}</div></section>
    <section class="panel detail-panel"><div class="section-header"><div><h3>Relato do serviço</h3><p>O que foi realizado no local</p></div></div>${podeAtender && emAndamento ? `<form id="report-form"><label class="sr-only" for="report">Relato do atendimento</label><textarea id="report" name="relato" required maxlength="8000" rows="5" placeholder="Descreva as atividades realizadas…">${escape(ordem.relato || '')}</textarea><div class="action-row"><button class="button secondary" type="submit">Salvar relato</button></div></form>` : `<p class="pre-wrap">${escape(ordem.relato || 'O relato será registrado durante o atendimento.')}</p>`}</section>
    <section class="panel detail-panel"><div class="section-header"><div><h3>Materiais utilizados</h3><p>Consumo vinculado à baixa de estoque</p></div>${podeAtender && emAndamento ? `<button id="consume" class="button secondary small">${icon('plus')} Material</button>` : ''}</div>${ordem.itens.length ? `<div class="item-list">${ordem.itens.map(i => `<div class="line-item"><span>${escape(i.nome)}</span><strong>${i.quantidade} ${escape(i.unidade)}</strong></div>`).join('')}</div>` : '<p class="muted">Nenhum material utilizado.</p>'}</section>
    ${finalizada ? `<section class="panel detail-panel"><h3>Atendimento assinado</h3><p>Responsável: <strong>${escape(ordem.responsavel)}</strong></p><p class="muted">${data(ordem.assinadoEm)}</p><img class="saved-signature" src="/api/ordens/${id}/assinatura" alt="Assinatura de ${escape(ordem.responsavel)}"></section>` : podeAtender && emAndamento ? `<section class="completion-callout"><div>${icon('check')}<h3>Pronto para concluir?</h3><p>Salve o relato e solicite a assinatura do responsável no local.</p></div><button id="complete" class="button">Concluir com assinatura</button></section>` : ''}</div>
    <aside class="panel detail-panel timeline"><h3>Histórico da ordem</h3>${historico.length ? historico.map(h => `<div class="timeline-entry"><span class="timeline-dot"></span><strong>${escape(h.tipo.replaceAll('_', ' '))}</strong><p>${escape(h.detalhes)}</p><small>${data(h.criadoEm)} · ${escape(ctx.nomeUsuario(h.usuarioId))}</small></div>`).join('') : '<p class="muted">Nenhum evento registrado.</p>'}</aside></div>`, { titulo: `Ordem de serviço #${id}`, subtitulo: 'Detalhes, materiais e histórico do atendimento' });
  const recarregar = async mensagem => { await ctx.atualizar(); await detalheOrdem(ctx, id); toast(mensagem); };
  document.querySelector('#assign')?.addEventListener('click', () => {
    const tecnicos = ctx.estado.usuarios.filter(u => u.ativo && u.perfil === 'TECNICO' && u.id !== ordem.tecnicoId);
    if (!tecnicos.length) { toast('Cadastre um técnico ativo ou escolha uma OS com outro responsável.', true); return; }
    modal('Designar técnico', `<label>Técnico responsável<select name="tecnicoId" required>${tecnicos.map(u => `<option value="${u.id}">${escape(u.nome)}</option>`).join('')}</select></label><p class="hint">A atribuição será registrada no histórico da ordem.</p>`, { aoEnviar: async e => {
      await api(`/ordens/${id}/atribuicao`, { method: 'POST', body: { tecnicoId: Number(e.tecnicoId) } }); await recarregar('Técnico designado.');
    } });
  });
  document.querySelector('#start')?.addEventListener('click', async e => {
    e.currentTarget.disabled = true;
    try { await api(`/ordens/${id}/inicio`, { method: 'POST' }); await recarregar('Atendimento iniciado.'); }
    catch (erro) { toast(erro.message, true); e.target.disabled = false; }
  });
  document.querySelector('#report-form')?.addEventListener('submit', async e => {
    e.preventDefault(); const botao = e.currentTarget.querySelector('button'); botao.disabled = true;
    try { await api(`/ordens/${id}/relato`, { method: 'PUT', body: { relato: document.querySelector('#report').value } }); await recarregar('Relato salvo.'); }
    catch (erro) { toast(erro.message, true); botao.disabled = false; }
  });
  document.querySelector('#consume')?.addEventListener('click', () => {
    const materiais = ctx.estado.materiais.filter(m => m.ativo && m.quantidade > 0 && !ordem.itens.some(i => i.materialId === m.id));
    if (!materiais.length) { toast('Não há materiais disponíveis para adicionar.', true); return; }
    const key = chaveOperacao();
    modal('Registrar material utilizado', `<p class="form-intro">O saldo será baixado assim que você confirmar o consumo.</p><label>Material<select name="materialId" required>${materiais.map(m => `<option value="${m.id}">${escape(m.nome)} · ${m.quantidade} ${escape(m.unidade)} disponíveis</option>`).join('')}</select></label>${campo('Quantidade utilizada', 'quantidade', 'number', 'min="1" step="1"')}${campo('Observação', 'observacao', 'text', 'maxlength="500" placeholder="Onde o material foi utilizado?"')}`, { textoBotao: 'Confirmar consumo', aoEnviar: async e => {
      await api(`/ordens/${id}/materiais`, { method: 'POST', key, body: { ...e, materialId: Number(e.materialId), quantidade: Number(e.quantidade) } }); await recarregar('Material registrado e estoque atualizado.');
    } });
  });
  document.querySelector('#complete')?.addEventListener('click', () => {
    const relatoAtual = document.querySelector('#report')?.value;
    if (!ordem.relato || relatoAtual !== ordem.relato) { toast('Salve o relato do serviço antes de concluir.', true); return; }
    let assinatura;
    modal('Concluir atendimento', `${campo('Nome do responsável presente', 'responsavel', 'text', 'maxlength="160" autocomplete="name"')}<div class="signature-label"><span>Assinatura do responsável</span><button id="clear-signature" class="text-button" type="button">Limpar</button></div><canvas id="signature" aria-label="Área para desenhar a assinatura com o dedo, caneta ou mouse"></canvas><p class="hint">Assine com o dedo, caneta ou mouse. A assinatura e a data ficarão vinculadas a esta OS.</p>`, {
      textoBotao: 'Assinar e concluir', aoAbrir: el => { assinatura = prepararAssinatura(el.querySelector('canvas')); el.querySelector('#clear-signature').onclick = () => assinatura.limpar(); },
      aoEnviar: async e => { await api(`/ordens/${id}/conclusao`, { method: 'POST', body: { responsavel: e.responsavel, assinatura: assinatura.imagem() } }); await recarregar('Atendimento concluído com assinatura.'); },
    });
  });
}
