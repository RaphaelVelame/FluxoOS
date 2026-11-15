import { api } from '../api.js';
import { icon, escape, numero, vazio, badge, campo, modal } from '../components.js';
import { novoUsuario } from './team.js';

export function visaoGlobal(ctx) {
  const { empresas, usuarios, ordens } = ctx.estado;
  const pendentes = ordens.filter(o => o.situacao !== 'CONCLUIDA').length;
  ctx.pagina(`<section class="welcome"><div><span class="eyebrow">ADMINISTRAÇÃO DA PLATAFORMA</span><h2>Olá, ${escape(ctx.estado.usuario.nome)}</h2><p>Cadastre empresas e organize os acessos de seus administradores.</p></div></section>
    <section class="stats-grid" aria-label="Resumo global">${[['Empresas', empresas.length, 'grid'], ['Usuários ativos', usuarios.filter(u => u.ativo).length, 'users'], ['Ordens pendentes', pendentes, 'clock'], ['Total de ordens', ordens.length, 'clipboard']].map(([titulo, valor, simbolo]) => `<article class="stat-card"><span class="stat-icon blue">${icon(simbolo)}</span><span class="stat-label">${titulo}</span><strong>${numero(valor)}</strong></article>`).join('')}</section>
    <section class="panel platform-companies"><div class="section-header"><div><h2>Empresas da plataforma</h2><p>Cadastre a empresa e adicione um administrador para liberar sua operação.</p></div></div>${empresas.length ? `<div class="company-list">${empresas.map(e => {
      const pessoas = usuarios.filter(u => u.empresaId === e.id);
      const admins = pessoas.filter(u => ctx.temFuncao(u, 'ROLE_ADMIN_EMPRESA')).length;
      return `<article class="company-row" data-company="${e.id}"><div class="company-copy"><h3>${escape(e.nome)}</h3><p>${pessoas.length} ${pessoas.length === 1 ? 'usuário' : 'usuários'} · ${ordens.filter(o => o.empresaId === e.id).length} OS</p>${badge(admins ? `${admins} ${admins === 1 ? 'administrador ativo' : 'administradores ativos'}` : 'Sem administrador ativo', admins ? 'concluida' : 'media')}</div><div class="company-actions"><button class="button secondary small" data-add-admin="${e.id}">Adicionar administrador</button><a class="text-link" href="#equipe/${e.id}">Gerenciar usuários ${icon('arrow')}</a></div></article>`;
    }).join('')}</div>` : vazio('Nenhuma empresa cadastrada', 'Cadastre uma empresa para começar.')}</section>
    <div class="action-row"><a class="button secondary" href="#ordens">Consultar ordens</a><a class="button secondary" href="#equipe">Gerenciar usuários</a></div>`, {
    titulo: 'Visão global', subtitulo: 'Empresas, pessoas e atendimentos da plataforma',
    acao: { texto: 'Cadastrar empresa', executar: () => novaEmpresa(ctx) },
  });
  document.querySelectorAll('[data-add-admin]').forEach(el => {
    el.onclick = () => novoUsuario(ctx, { empresaId: Number(el.dataset.addAdmin), administrador: true });
  });
}

function novaEmpresa(ctx) {
  modal('Cadastrar empresa', `<p class="form-intro">Depois do cadastro, adicione um administrador para gerenciar a equipe, as ordens e os materiais dessa empresa.</p>${campo('Nome da empresa', 'nome', 'text', 'maxlength="160" autocomplete="organization" placeholder="Ex.: Oficina Central"')}`, {
    textoBotao: 'Cadastrar empresa',
    aoEnviar: e => api('/plataforma/empresas', { method: 'POST', body: { nome: e.nome } }),
    aoSalvar: () => ctx.aposGravar('Empresa cadastrada. Adicione um administrador para liberar sua operação.', 'inicio'),
  });
}
