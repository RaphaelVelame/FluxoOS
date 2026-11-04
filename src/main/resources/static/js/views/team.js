import { api } from '../api.js';
import { escape, icon, badge, campo, modal, perfis, data, toast } from '../components.js';

export function equipe(ctx) {
  ctx.pagina(`<section class="team-grid">${ctx.estado.usuarios.map(u => `<article class="panel person-card"><div class="card-top"><span class="person-avatar">${escape(ctx.iniciais(u.id))}</span>${badge(u.ativo ? 'Ativo' : 'Inativo', u.ativo ? 'concluida' : 'neutral')}</div><h2>${escape(u.nome)}</h2><p>@${escape(u.login)}</p><span class="role-label">${escape(perfis[u.perfil])}</span>${ctx.chefe && u.id !== ctx.estado.usuario.id ? `<button class="button secondary full-width" data-user-status="${u.id}">${u.ativo ? 'Desativar' : 'Ativar'} conta</button>` : ''}</article>`).join('')}</section>${ctx.chefe ? '<div class="action-row"><button class="button secondary" id="security-events">Consultar eventos de segurança</button></div>' : ''}`, { titulo: 'Equipe', subtitulo: 'Pessoas e responsabilidades da operação', acao: ctx.chefe ? { texto: 'Adicionar pessoa', executar: () => novoUsuario(ctx) } : null });
  document.querySelectorAll('[data-user-status]').forEach(el => {
    el.onclick = () => {
      const u = ctx.estado.usuarios.find(u => u.id === Number(el.dataset.userStatus));
      modal(`${u.ativo ? 'Desativar' : 'Ativar'} conta`, `<p>${escape(u.nome)}</p><p class="muted">${u.ativo ? 'Esta pessoa perderá o acesso ao sistema. O histórico será preservado.' : 'Esta pessoa poderá acessar o sistema novamente.'}</p>`, { textoBotao: 'Confirmar', aoEnviar: async () => {
        await api(`/usuarios/${u.id}/status`, { method: 'PATCH', body: { ativo: !u.ativo } }); await ctx.atualizar(); equipe(ctx); toast('Status da conta atualizado.');
      } });
    };
  });
  document.querySelector('#security-events')?.addEventListener('click', async () => {
    try {
      const eventos = await api('/seguranca/eventos');
      modal('Eventos de segurança', eventos.length ? eventos.slice(0, 100).map(e => `<div class="security-event"><strong>${escape(e.tipoEvento.replaceAll('_', ' '))}</strong><p>${escape(e.detalhes)}</p><small>${data(e.criadoEm)}</small></div>`).join('') : '<p>Nenhum evento registrado.</p>', { largo: true });
    } catch (e) { toast(e.message, true); }
  });
}

function novoUsuario(ctx) {
  modal('Adicionar pessoa à equipe', `${campo('Nome completo', 'nome', 'text', 'maxlength="160" autocomplete="name"')}${campo('Login', 'login', 'text', 'minlength="3" maxlength="80" pattern="[a-z0-9._-]+" autocomplete="off" placeholder="Ex.: maria.santos"')}<label>Perfil de acesso<select name="perfil">${Object.entries(perfis).map(([v, t]) => `<option value="${v}" ${v === 'TECNICO' ? 'selected' : ''}>${t}</option>`).join('')}</select></label>${campo('Senha inicial', 'senha', 'password', 'minlength="15" maxlength="1024" autocomplete="new-password"')}<p class="hint">Use pelo menos 15 caracteres. Compartilhe a senha por um canal privado e peça a troca no primeiro acesso.</p>`, { textoBotao: 'Criar conta', aoEnviar: async e => {
    await api('/usuarios', { method: 'POST', body: e }); await ctx.atualizar(); equipe(ctx); toast('Conta criada.');
  } });
}

export function conta(ctx) {
  const u = ctx.estado.usuario;
  ctx.pagina(`<section class="panel account-panel"><span class="person-avatar">${escape(ctx.iniciais(u.id))}</span><h2>${escape(u.nome)}</h2><p>@${escape(u.login)} · ${escape(perfis[u.perfil])}</p><hr><h3>Alterar minha senha</h3><p class="muted">Ao trocar a senha, todas as suas sessões serão encerradas.</p><form id="password-form" class="stack">${campo('Senha atual', 'atual', 'password', 'autocomplete="current-password" maxlength="1024"')}${campo('Nova senha', 'nova', 'password', 'minlength="15" maxlength="1024" autocomplete="new-password"')}${campo('Confirme a nova senha', 'confirmacao', 'password', 'minlength="15" maxlength="1024" autocomplete="new-password"')}<p class="hint">Pelo menos 15 caracteres. Uma frase longa é uma boa opção.</p><p class="form-error" role="alert"></p><button class="button" type="submit">Alterar senha</button></form></section>`, { titulo: 'Minha conta', subtitulo: 'Seu perfil e a segurança do seu acesso' });
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
