import { api } from '../api.js';
import { nomesFuncoes, funcoes, escape, icon, badge, campo, modal, data, toast, vazio } from '../components.js';

export function equipe(ctx, empresaId = null) {
  const empresaInicial = ctx.master && ctx.estado.empresas.some(e => e.id === empresaId) ? empresaId : null;
  ctx.pagina(`${ctx.master ? `<div class="toolbar company-user-filters"><label>Empresa<select id="team-company"><option value="">Todas as empresas</option>${ctx.estado.empresas.map(e => `<option value="${e.id}" ${e.id === empresaInicial ? 'selected' : ''}>${escape(e.nome)}</option>`).join('')}</select></label><label class="search-field">${icon('search')}<input id="team-search" type="search" placeholder="Buscar nome ou login" aria-label="Buscar usuários"></label></div>` : ''}<section class="team-grid"></section>${ctx.chefe ? '<div class="action-row"><button class="button secondary" id="security-events">Consultar eventos de segurança</button></div>' : ''}`, {
    titulo: ctx.master ? 'Usuários da plataforma' : 'Equipe', subtitulo: ctx.master ? 'Administradores e funcionários de cada empresa' : 'Pessoas e responsabilidades da operação',
    acao: ctx.master || ctx.chefe ? { texto: ctx.master ? 'Adicionar usuário' : 'Adicionar pessoa', executar: () => novoUsuario(ctx, { empresaId: ctx.master ? Number(document.querySelector('#team-company').value) || null : null }) } : null,
  });
  const podeGerenciar = u => u.id !== ctx.estado.usuario.id && u.empresaId != null && !u.roles.includes('ROLE_ADMIN_MASTER') &&
    (ctx.master || (ctx.chefe && u.empresaId === ctx.estado.usuario.empresaId && !u.roles.includes('ROLE_ADMIN_EMPRESA')));
  const ultimoAdmin = u => ctx.temFuncao(u, 'ROLE_ADMIN_EMPRESA') && ctx.estado.usuarios.filter(v => v.empresaId === u.empresaId && ctx.temFuncao(v, 'ROLE_ADMIN_EMPRESA')).length === 1;
  const render = () => {
    const empresa = document.querySelector('#team-company')?.value || '';
    const busca = (document.querySelector('#team-search')?.value || '').toLocaleLowerCase('pt-BR');
    const pessoas = ctx.estado.usuarios.filter(u => (!empresa || u.empresaId === Number(empresa)) && `${u.nome} ${u.login}`.toLocaleLowerCase('pt-BR').includes(busca));
    document.querySelector('.team-grid').innerHTML = pessoas.length ? pessoas.map(u => `<article class="panel person-card" data-user="${u.id}"><div class="card-top"><span class="person-avatar">${escape(ctx.iniciais(u.id))}</span>${badge(u.ativo ? 'Ativo' : 'Inativo', u.ativo ? 'concluida' : 'neutral')}</div><h2>${escape(u.nome)}</h2><p>@${escape(u.login)}</p>${ctx.master ? `<p class="person-company">${escape(nomeEmpresa(ctx, u.empresaId))}</p>` : ''}<span class="role-label">${escape(nomesFuncoes(u))}</span>${podeGerenciar(u) ? `<button class="button secondary full-width" data-edit-user="${u.id}">Editar funções</button><button class="button secondary full-width" data-user-status="${u.id}" ${ultimoAdmin(u) ? 'disabled title="Adicione outro administrador ativo antes de desativar esta conta."' : ''}>${u.ativo ? 'Desativar' : 'Ativar'} conta</button>${ultimoAdmin(u) ? '<p class="hint">Mantenha um administrador ativo. Adicione outro antes de desativar esta conta.</p>' : ''}` : ''}</article>`).join('') : vazio('Nenhum usuário encontrado', 'Ajuste a empresa ou a busca.');
    document.querySelectorAll('[data-edit-user]').forEach(el => {
      el.onclick = () => {
        const u = ctx.estado.usuarios.find(u => u.id === Number(el.dataset.editUser));
        modal('Editar pessoa e funções', `${ctx.master ? `<p class="form-intro">Empresa: <strong>${escape(nomeEmpresa(ctx, u.empresaId))}</strong></p>` : ''}${campo('Nome completo', 'nome', 'text', 'maxlength="160"')}${seletorFuncoes(u.roles, { permitirAdmin: ctx.master, fixarAdmin: ultimoAdmin(u) })}<p class="hint">Alterar funções encerra as sessões anteriores desta pessoa.</p>`, {
          aoAbrir: el => { el.querySelector('[name=nome]').value = u.nome; },
          aoEnviar: (e, form) => api(`/usuarios/${u.id}`, { method: 'PUT', body: { nome: e.nome, roles: new FormData(form).getAll('roles') } }),
          aoSalvar: () => ctx.aposGravar('Pessoa e funções atualizadas.'),
        });
      };
    });
    document.querySelectorAll('[data-user-status]').forEach(el => {
      el.onclick = () => {
        const u = ctx.estado.usuarios.find(u => u.id === Number(el.dataset.userStatus));
        modal(`${u.ativo ? 'Desativar' : 'Ativar'} conta`, `<p>${escape(u.nome)}</p>${ctx.master ? `<p class="muted">${escape(nomeEmpresa(ctx, u.empresaId))}</p>` : ''}<p class="muted">${u.ativo ? 'Esta pessoa perderá o acesso ao sistema. O histórico será preservado.' : 'Esta pessoa poderá acessar o sistema novamente.'}</p>`, { textoBotao: 'Confirmar',
          aoEnviar: () => api(`/usuarios/${u.id}/status`, { method: 'PATCH', body: { ativo: !u.ativo } }),
          aoSalvar: () => ctx.aposGravar('Status da conta atualizado.'),
        });
      };
    });
  };
  document.querySelector('#team-company')?.addEventListener('change', e => {
    history.replaceState(null, '', `#equipe${e.target.value ? '/' + e.target.value : ''}`); render();
  });
  document.querySelector('#team-search')?.addEventListener('input', render);
  render();
  document.querySelector('#security-events')?.addEventListener('click', async () => {
    const usuario = ctx.estado.usuario;
    try {
      const eventos = await api('/seguranca/eventos');
      if (usuario !== ctx.estado.usuario || location.hash !== '#equipe') return;
      modal('Eventos de segurança', eventos.length ? eventos.slice(0, 100).map(e => `<div class="security-event"><strong>${escape(e.tipoEvento.replaceAll('_', ' '))}</strong><p>${escape(e.detalhes)}</p><small>${data(e.criadoEm)}</small></div>`).join('') : '<p>Nenhum evento registrado.</p>', { largo: true });
    } catch (e) { if (usuario === ctx.estado.usuario) toast(e.message, true); }
  });
}

function nomeEmpresa(ctx, id) { return id == null ? 'Plataforma' : ctx.estado.empresas.find(e => e.id === id)?.nome || `Empresa #${id}`; }

function seletorFuncoes(selecionadas = ['ROLE_SUPORTE_EXTERNO'], { permitirAdmin = false, fixarAdmin = false } = {}) {
  return '<fieldset><legend>Funções de acesso</legend>' + Object.entries(funcoes).filter(([r]) => r !== 'ROLE_ADMIN_MASTER' && (permitirAdmin || r !== 'ROLE_ADMIN_EMPRESA')).map(([r, nome]) => `<label class="check-filter"><input type="checkbox" name="roles" value="${r}" ${selecionadas.includes(r) ? 'checked' : ''} ${fixarAdmin && r === 'ROLE_ADMIN_EMPRESA' ? 'disabled' : ''}> ${escape(nome)}</label>${fixarAdmin && r === 'ROLE_ADMIN_EMPRESA' ? '<input type="hidden" name="roles" value="ROLE_ADMIN_EMPRESA">' : ''}`).join('') + '</fieldset>';
}

export function novoUsuario(ctx, { empresaId = null, administrador = false } = {}) {
  if (ctx.master && !ctx.estado.empresas.length) { toast('Cadastre uma empresa antes de adicionar usuários.', true); return; }
  const cadastroAdmin = ctx.master && administrador;
  const empresa = cadastroAdmin ? `<p class="form-intro">Empresa: <strong>${escape(nomeEmpresa(ctx, empresaId))}</strong></p>`
    : ctx.master ? `<label>Empresa<select name="empresaId" required><option value="" disabled ${empresaId == null ? 'selected' : ''}>Selecione a empresa</option>${ctx.estado.empresas.map(e => `<option value="${e.id}" ${e.id === empresaId ? 'selected' : ''}>${escape(e.nome)}</option>`).join('')}</select></label>` : '';
  modal(cadastroAdmin ? 'Adicionar administrador' : ctx.master ? 'Adicionar usuário' : 'Adicionar pessoa à equipe', `${empresa}${campo('Nome completo', 'nome', 'text', 'maxlength="160" autocomplete="name"')}${campo('Login', 'login', 'text', 'minlength="3" maxlength="80" pattern="[a-z0-9._-]+" autocomplete="off" placeholder="Ex.: maria.santos"')}${seletorFuncoes(cadastroAdmin ? ['ROLE_ADMIN_EMPRESA'] : undefined, { permitirAdmin: ctx.master, fixarAdmin: cadastroAdmin })}${campo('Senha inicial', 'senha', 'password', 'minlength="15" maxlength="1024" autocomplete="new-password"')}<p class="hint">O login é único na plataforma. Use uma senha com pelo menos 15 caracteres e compartilhe-a por um canal privado.</p>`, { textoBotao: cadastroAdmin ? 'Criar administrador' : 'Criar conta',
    aoEnviar: (e, form) => api('/usuarios', { method: 'POST', body: { login: e.login, nome: e.nome, senha: e.senha, roles: new FormData(form).getAll('roles'), ...(ctx.master ? { empresaId: cadastroAdmin ? empresaId : Number(e.empresaId) } : {}) } }),
    aoSalvar: u => ctx.aposGravar(cadastroAdmin ? 'Administrador criado.' : 'Conta criada.', ctx.master ? `equipe/${u.empresaId}` : undefined),
  });
}

export function conta(ctx) {
  const u = ctx.estado.usuario;
  ctx.pagina(`<section class="panel account-panel"><span class="person-avatar">${escape(ctx.iniciais(u.id))}</span><h2>${escape(u.nome)}</h2><p>@${escape(u.login)} · ${escape(nomesFuncoes(u))}</p><hr><h3>Alterar minha senha</h3><p class="muted">Ao trocar a senha, todas as suas sessões serão encerradas.</p><form id="password-form" class="stack">${campo('Senha atual', 'atual', 'password', 'autocomplete="current-password" maxlength="1024"')}${campo('Nova senha', 'nova', 'password', 'minlength="15" maxlength="1024" autocomplete="new-password"')}${campo('Confirme a nova senha', 'confirmacao', 'password', 'minlength="15" maxlength="1024" autocomplete="new-password"')}<p class="hint">Pelo menos 15 caracteres. Uma frase longa é uma boa opção.</p><p class="form-error" role="alert"></p><button class="button" type="submit">Alterar senha</button></form></section>`, { titulo: 'Minha conta', subtitulo: 'Seu perfil e a segurança do seu acesso' });
  document.querySelector('#password-form').onsubmit = async e => {
    e.preventDefault();
    const form = e.currentTarget;
    const valores = Object.fromEntries(new FormData(form));
    const erro = form.querySelector('.form-error');
    if (valores.nova !== valores.confirmacao) { erro.textContent = 'As novas senhas precisam ser iguais.'; return; }
    const botao = form.querySelector('button'); botao.disabled = true; erro.textContent = '';
    try { await api('/auth/senha', { method: 'POST', body: { atual: valores.atual, nova: valores.nova } }); ctx.encerrar(); toast('Senha alterada. Entre novamente.'); }
    catch (e) { erro.textContent = e.message; botao.disabled = false; }
  };
}
