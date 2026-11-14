import { icon, escape, numero, vazio, situacoes, badge } from '../components.js';

export function dashboard(ctx) {
  if (ctx.master) { global(ctx); return; }
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

function global(ctx) {
  const { empresas, usuarios, ordens } = ctx.estado;
  const pendentes = ordens.filter(o => o.situacao !== 'CONCLUIDA').length;
  ctx.pagina(`<section class="welcome"><div><span class="eyebrow">ADMINISTRAÇÃO DA PLATAFORMA</span><h2>Olá, ${escape(ctx.estado.usuario.nome)}</h2><p>Acompanhe empresas, usuários e ordens de serviço em seu escopo global.</p></div></section>
    <section class="stats-grid" aria-label="Resumo global">${[['Empresas', empresas.length, 'grid'], ['Usuários ativos', usuarios.filter(u => u.ativo).length, 'users'], ['Ordens pendentes', pendentes, 'clock'], ['Total de ordens', ordens.length, 'clipboard']].map(([titulo, valor, simbolo]) => `<article class="stat-card"><span class="stat-icon blue">${icon(simbolo)}</span><span class="stat-label">${titulo}</span><strong>${numero(valor)}</strong></article>`).join('')}</section>
    <section class="panel"><div class="section-header"><div><h2>Empresas da plataforma</h2><p>O estoque e a execução dos atendimentos são consultados pelas contas operacionais de cada empresa.</p></div></div>${empresas.length ? `<div class="item-list">${empresas.map(e => `<div class="line-item"><span>${escape(e.nome)}<small class="item-owner">Empresa #${e.id}</small></span><strong>${usuarios.filter(u => u.empresaId === e.id).length} usuários · ${ordens.filter(o => o.empresaId === e.id).length} OS</strong></div>`).join('')}</div>` : vazio('Nenhuma empresa cadastrada', 'As empresas provisionadas aparecerão aqui.')}</section>
    <div class="action-row"><a class="button secondary" href="#ordens">Consultar ordens</a><a class="button secondary" href="#equipe">Consultar usuários</a></div>`, { titulo: 'Visão global', subtitulo: 'Empresas, pessoas e atendimentos da plataforma' });
}
