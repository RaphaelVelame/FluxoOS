import { api, salvarCsrf } from './api.js';
import { icon, escape, perfis, toast } from './components.js';
import { dashboard } from './views/dashboard.js';
import { ordens, detalheOrdem, novaOrdem } from './views/orders.js';
import { estoque } from './views/inventory.js';
import { equipe, conta } from './views/team.js';

const estado = { usuario: null, ordens: [], materiais: [], usuarios: [], movimentos: [] };
let sincronizacaoPendente = null;
let sincronizando = null;
const navegacao = [['inicio', 'Visão geral', 'grid'], ['ordens', 'Ordens de serviço', 'clipboard'], ['estoque', 'Estoque', 'box'], ['equipe', 'Equipe', 'users']];
const contexto = {
  estado,
  get gestor() { return estado.usuario?.ativo && ['CHEFE', 'SUPORTE_TI'].includes(estado.usuario.perfil); },
  get chefe() { return estado.usuario?.perfil === 'CHEFE'; },
  nomeUsuario: id => estado.usuarios.find(u => u.id === id)?.nome || 'Não designado',
  iniciais: id => (estado.usuarios.find(u => u.id === id)?.nome || '?').split(' ').filter(Boolean).slice(0, 2).map(p => p[0]).join(''),
  atualizar, pagina, encerrar, aposGravar,
  novaOrdem: () => novaOrdem(contexto),
};

async function atualizar() {
  const usuario = estado.usuario;
  const [ordens, materiais, usuarios, movimentos] = await Promise.all([
    api('/ordens'), api('/materiais'), api('/usuarios'), api('/movimentos'),
  ]);
  if (usuario === estado.usuario) Object.assign(estado, { ordens, materiais, usuarios, movimentos });
}

function mostrarPendencia(carregando = false) {
  pagina(`<div class="empty-state"><h2>${escape(sincronizacaoPendente?.mensagem || 'Dados indisponíveis')}</h2><p>${carregando ? 'Atualizando os dados da tela…' : 'Não foi possível carregar os dados atualizados. Use o botão abaixo para continuar.'}</p><button id="retry-sync" class="button" ${carregando ? 'disabled' : ''}>Atualizar dados</button></div>`,
    { titulo: 'Atualização pendente', subtitulo: sincronizacaoPendente ? 'Operação concluída com sucesso' : 'Tente carregar os dados novamente' });
  document.querySelector('#retry-sync')?.addEventListener('click', () => { mostrarPendencia(true); void sincronizar(); });
}

async function sincronizar() {
  if (sincronizando) return sincronizando;
  const usuario = estado.usuario;
  const pendente = sincronizacaoPendente;
  const tarefa = (async () => {
    try {
      await atualizar();
      if (usuario !== estado.usuario || sincronizacaoPendente !== pendente) return false;
      sincronizacaoPendente = null;
      if (await rota() === false) throw new Error('Não foi possível atualizar esta página.');
      toast(pendente?.mensagem || 'Dados atualizados.');
      return true;
    } catch {
      if (usuario !== estado.usuario) return false;
      if (sincronizacaoPendente && sincronizacaoPendente !== pendente) return false;
      sincronizacaoPendente = pendente;
      mostrarPendencia();
      return false;
    }
  })();
  sincronizando = tarefa;
  try { return await tarefa; }
  finally { if (sincronizando === tarefa) sincronizando = null; }
}

async function aposGravar(mensagem, destino) {
  if (!estado.usuario) return;
  sincronizacaoPendente = { mensagem };
  if (destino) history.pushState(null, '', `#${destino}`);
  mostrarPendencia(true);
  if (sincronizando) await sincronizando;
  await sincronizar();
}

function login() {
  document.querySelector('#modal').close();
  document.querySelector('#app').innerHTML = `<main class="login-layout" id="main"><section class="login-story"><a class="brand" href="#inicio"><span class="brand-mark">F</span>Fluxo<span>OS</span></a><div class="login-message"><span class="eyebrow">MENOS DISTÂNCIA. MAIS SOLUÇÃO.</span><h1>O trabalho flui.<br>A equipe avança.</h1><p>Ordens de serviço, materiais e pessoas conectados do primeiro chamado à última assinatura.</p><div class="login-flow"><span>${icon('clipboard')} Organize</span><span>${icon('box')} Acompanhe</span><span>${icon('check')} Resolva</span></div></div><small>Gestão de serviços feita para o dia a dia.</small></section>
    <section class="login-form-area"><div class="mobile-brand brand"><span class="brand-mark">F</span>Fluxo<span>OS</span></div><div class="login-form-card"><span class="eyebrow">SEU ESPAÇO DE TRABALHO</span><h2>Bom ter você por aqui.</h2><p>Entre para acompanhar sua operação.</p><form id="login-form" class="stack"><label>Usuário<input name="usuario" autocomplete="username" required maxlength="80" placeholder="Seu nome de usuário" autocapitalize="none" spellcheck="false"></label><label>Senha<input name="senha" type="password" autocomplete="current-password" required maxlength="1024" placeholder="Digite sua senha"></label><p class="form-error" role="alert"></p><button class="button full-width" type="submit">Entrar no FluxoOS ${icon('arrow')}</button></form><p class="login-help">Precisa de acesso? Fale com a chefia da sua equipe.</p></div><small class="login-footer">FluxoOS · Cada etapa, sob controle.</small></section></main>`;
  document.querySelector('#login-form').onsubmit = async evento => {
    evento.preventDefault();
    const form = evento.currentTarget;
    const botao = form.querySelector('button');
    botao.disabled = true;
    form.querySelector('.form-error').textContent = '';
    try {
      const dados = await api('/auth/login', { method: 'POST', body: Object.fromEntries(new FormData(form)) });
      salvarCsrf(dados.csrf);
      estado.usuario = dados.usuario;
      form.reset();
      shell();
      await aposGravar('Login realizado.');
    } catch (e) { form.querySelector('.form-error').textContent = e.message; botao.disabled = false; }
  };
}

function shell() {
  const u = estado.usuario;
  document.querySelector('#app').innerHTML = `<div class="app-layout"><aside class="sidebar"><a class="brand" href="#inicio"><span class="brand-mark">F</span>Fluxo<span>OS</span></a><span class="nav-label">ESPAÇO DE TRABALHO</span><nav aria-label="Navegação principal">${navegacao.map(([rota, nome, simbolo]) => `<a href="#${rota}" data-nav="${rota}">${icon(simbolo)}<span>${nome}</span></a>`).join('')}</nav><div class="sidebar-note">${icon('check')}<strong>Do chamado à solução.</strong><p>Tudo conectado em um único fluxo.</p><span class="note-decoration"></span></div><div class="sidebar-bottom"><a class="profile" href="#conta"><span class="avatar">${escape(contexto.iniciais(u.id))}</span><div><strong>${escape(u.nome)}</strong><small>${escape(perfis[u.perfil])}</small></div>${icon('user')}</a><button id="logout" class="logout">${icon('logout')} Sair da conta</button></div></aside>
    <div class="workspace"><header class="topbar"><span class="workspace-label"><span class="online-dot"></span> Gestão de operações</span><div class="topbar-actions"><span id="today"></span><a href="#conta" class="mobile-profile avatar" aria-label="Minha conta">${escape(contexto.iniciais(u.id))}</a><button class="icon-button" id="refresh" aria-label="Atualizar dados">${icon('refresh')}</button></div></header><main id="main" tabindex="-1"><div id="page-header" class="page-header"></div><div id="content"></div><footer class="workspace-footer"><span>FluxoOS</span> Organização que faz o trabalho acontecer.</footer></main></div>
    <nav class="bottom-nav" aria-label="Navegação no celular">${navegacao.map(([rota, nome, simbolo]) => `<a href="#${rota}" data-nav="${rota}">${icon(simbolo)}<span>${rota === 'inicio' ? 'Início' : rota === 'ordens' ? 'Ordens' : nome}</span></a>`).join('')}</nav></div>`;
  document.querySelector('#today').textContent = new Intl.DateTimeFormat('pt-BR', { day: 'numeric', month: 'long', timeZone: 'America/Bahia' }).format(new Date());
  document.querySelector('#logout').onclick = async () => {
    try { await api('/auth/logout', { method: 'POST' }); encerrar(); }
    catch (e) { toast(e.message, true); }
  };
  document.querySelector('#refresh').onclick = async e => {
    const botao = e.currentTarget; botao.disabled = true;
    try { await sincronizar(); }
    finally { botao.disabled = false; }
  };
}

function pagina(html, { titulo, subtitulo, acao } = {}) {
  if (!estado.usuario || !document.querySelector('#page-header')) return;
  document.title = `${titulo} · FluxoOS`;
  document.querySelector('#page-header').innerHTML = `<div><span class="eyebrow">ESPAÇO DE TRABALHO</span><h1>${escape(titulo)}</h1><p>${escape(subtitulo)}</p></div>${acao ? `<button class="button" id="primary-action">${icon('plus')}${escape(acao.texto)}</button>` : ''}`;
  if (acao) document.querySelector('#primary-action').onclick = acao.executar;
  document.querySelector('#content').innerHTML = html;
}

async function rota() {
  if (!estado.usuario) return;
  if (sincronizacaoPendente) { mostrarPendencia(!!sincronizando); return false; }
  const caminho = location.hash.slice(1) || 'inicio';
  const [nome, id] = caminho.split('/');
  document.querySelectorAll('[data-nav]').forEach(el => {
    const ativo = el.dataset.nav === nome;
    el.classList.toggle('active', ativo);
    if (ativo) el.setAttribute('aria-current', 'page'); else el.removeAttribute('aria-current');
  });
  try {
    if (nome === 'ordens' && id && /^\d+$/.test(id)) {
      pagina('<p class="muted">Carregando atendimento…</p>', { titulo: 'Ordem de serviço', subtitulo: 'Consultando os detalhes' });
      await detalheOrdem(contexto, Number(id));
    } else if (nome === 'ordens') ordens(contexto);
    else if (nome === 'estoque') estoque(contexto);
    else if (nome === 'equipe') equipe(contexto);
    else if (nome === 'conta') conta(contexto);
    else dashboard(contexto);
    return true;
  } catch (e) {
    if (estado.usuario) pagina(`<div class="empty-state"><h2>Não foi possível abrir esta página</h2><p>${escape(e.message)}</p><a class="button secondary" href="#inicio">Voltar ao início</a></div>`, { titulo: 'Página indisponível', subtitulo: 'Tente atualizar os dados' });
    return false;
  }
}

function encerrar() {
  sincronizacaoPendente = null;
  sincronizando = null;
  salvarCsrf(null);
  Object.assign(estado, { usuario: null, ordens: [], materiais: [], usuarios: [], movimentos: [] });
  location.hash = 'inicio';
  login();
}

window.addEventListener('hashchange', rota);
window.addEventListener('sessao-expirada', () => {
  const confirmacao = sincronizacaoPendente?.mensagem;
  encerrar();
  toast(`${confirmacao ? confirmacao + ' ' : ''}Sua sessão expirou. Entre novamente.`, true);
});
try {
  estado.usuario = await api('/auth/me');
  if (!sessionStorage.getItem('fluxoos.csrf')) { login(); }
  else { shell(); await sincronizar(); }
} catch { login(); }
