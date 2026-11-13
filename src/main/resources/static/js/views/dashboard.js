import { icon, escape, numero, vazio, situacoes, badge } from '../components.js';

export function dashboard(ctx) {
  const { ordens, materiais, usuario } = ctx.estado;
  const abertas = ordens.filter(o => o.situacao === 'ABERTA');
  const andamento = ordens.filter(o => o.situacao === 'EM_ANDAMENTO');
  const concluidas = ordens.filter(o => o.situacao === 'CONCLUIDA');
  const alertas = materiais.filter(m => m.ativo && m.abaixoDoMinimo);
  const pendentes = ordens.filter(o => o.situacao !== 'CONCLUIDA').sort((a, b) =>
    ({ ALTA: 0, MEDIA: 1, BAIXA: 2 }[a.prioridade] - { ALTA: 0, MEDIA: 1, BAIXA: 2 }[b.prioridade])).slice(0, 4);
  ctx.pagina(`<section class="welcome"><div><span class="eyebrow">TUDO PRONTO PARA MAIS UM DIA</span><h2>Olá, ${escape(usuario.nome.split(' ')[0])}<span class="greeting-dot">.</span></h2><p>Acompanhe os atendimentos e mantenha tudo em movimento.</p></div><div class="welcome-symbol">${icon('clipboard')}</div></section>
    <section class="stats-grid" aria-label="Resumo das operações">
      ${[['Ordens abertas', abertas.length, 'clipboard', 'amber', 'Aguardando atendimento'], ['Em andamento', andamento.length, 'clock', 'blue', 'Serviços em execução'], ['Concluídas', concluidas.length, 'check', 'green', 'Atendimentos finalizados'], ['Estoque em alerta', alertas.length, 'box', 'orange', 'Materiais abaixo do mínimo']].map(([titulo, valor, simbolo, cor, detalhe]) =>
        `<article class="stat-card"><span class="stat-icon ${cor}">${icon(simbolo)}</span><span class="stat-label">${titulo}</span><strong>${numero(valor).padStart(2, '0')}</strong><small>${detalhe}</small></article>`).join('')}
    </section>
    <div class="dashboard-grid"><section class="panel"><div class="section-header"><div><h2>Próximos atendimentos</h2><p>As prioridades da sua operação</p></div><a class="text-link" href="#ordens">Ver todas ${icon('arrow')}</a></div>
      ${pendentes.length ? `<div class="task-list">${pendentes.map(o => `<a class="task-row" href="#ordens/${o.id}"><div class="task-symbol ${o.prioridade.toLowerCase()}">${icon('clipboard')}</div><div class="task-copy"><span class="small-code">OS #${o.id}</span><h3>${escape(o.problema)}</h3><p>${icon('pin')}${escape(o.local)}</p></div><div class="task-meta">${badge(situacoes[o.situacao], o.situacao)}${icon('arrow')}</div></a>`).join('')}</div>` : vazio('Nenhum atendimento pendente', 'As novas ordens de serviço aparecerão aqui.')}</section>
      <section class="panel stock-alert"><div class="section-header"><div><h2>Atenção ao estoque</h2><p>Planeje a próxima reposição</p></div><span class="count-badge">${alertas.length}</span></div>
        ${alertas.length ? alertas.slice(0, 4).map(m => `<div class="alert-item"><span class="alert-icon">${icon('box')}</span><div><h3>${escape(m.nome)}</h3><p>Mínimo: ${m.quantidadeMinima} ${escape(m.unidade)}</p></div><strong>${m.quantidade}<small>${escape(m.unidade)}</small></strong></div>`).join('') : vazio('Estoque em dia', 'Nenhum material está abaixo do mínimo.', 'check')}
        <a class="button secondary full-width" href="#estoque">Consultar estoque ${icon('arrow')}</a></section></div>
    <section class="flow-note">${icon('check')}<div><strong>Cada atendimento, um histórico completo.</strong><p>Materiais utilizados, responsáveis e assinatura registrados na mesma ordem.</p></div><span class="flow-label">FLUXOOS</span></section>`, { titulo: 'Visão geral', subtitulo: 'Sua operação, em um só lugar', acao: ctx.podeCriarOs ? { texto: 'Nova ordem de serviço', executar: ctx.novaOrdem } : null });
}
