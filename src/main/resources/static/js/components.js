export const escape = valor => String(valor ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
export const numero = valor => new Intl.NumberFormat('pt-BR').format(valor);
export const data = valor => valor ? new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(valor)) : '—';
export const perfis = { CHEFE: 'Chefia', SUPORTE_TI: 'Suporte de TI', TECNICO: 'Técnico', SOLICITANTE: 'Solicitante' };
export const situacoes = { ABERTA: 'Aberta', EM_ANDAMENTO: 'Em andamento', CONCLUIDA: 'Concluída' };
export const prioridades = { BAIXA: 'Baixa', MEDIA: 'Média', ALTA: 'Alta' };
export const movimentos = { ENTRADA: 'Entrada', SAIDA: 'Consumo na OS', DEVOLUCAO: 'Devolução', AJUSTE: 'Ajuste' };
const caminhos = {
  grid: '<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>',
  clipboard: '<rect x="5" y="5" width="14" height="16" rx="2"/><rect x="9" y="3" width="6" height="4" rx="1"/><path d="M9 12h6m-6 4h6"/>',
  box: '<path d="m3 7 9-4 9 4v10l-9 4-9-4V7Zm0 0 9 4 9-4M12 11v10M7.5 5l9 4"/>',
  users: '<circle cx="9" cy="8" r="3"/><path d="M3 21v-3a6 6 0 0 1 12 0v3m1-17a3 3 0 0 1 0 6m3 11v-3a6 6 0 0 0-3-5"/>',
  arrow: '<path d="M5 12h14m-5-5 5 5-5 5"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  search: '<circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/>',
  check: '<path d="m5 12 4 4L19 6"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  pin: '<path d="M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 1 1 14 0Z"/><circle cx="12" cy="10" r="2"/>',
  alert: '<path d="m12 3 10 18H2L12 3Z"/><path d="M12 9v4m0 3v1"/>',
  logout: '<path d="M9 4H4v16h5m5-12 4 4-4 4m-6-4h13"/>',
  user: '<circle cx="12" cy="8" r="4"/><path d="M4 22v-2a8 8 0 0 1 16 0v2"/>',
  close: '<path d="m6 6 12 12M6 18 18 6"/>',
  refresh: '<path d="M20 7v5h-5M4 17v-5h5M6 6a8 8 0 0 1 13 3M5 15a8 8 0 0 0 13 3"/>',
};
export const icon = (nome, classe = '') => `<svg class="icon ${classe}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${caminhos[nome] || caminhos.clipboard}</svg>`;
export const badge = (texto, tipo = '') => `<span class="badge ${tipo.toLowerCase()}"><span class="status-dot"></span>${escape(texto)}</span>`;
export const vazio = (titulo, descricao, simbolo = 'clipboard') => `<div class="empty-state">${icon(simbolo)}<h3>${escape(titulo)}</h3><p>${escape(descricao)}</p></div>`;
export const campo = (rotulo, nome, tipo = 'text', extras = '') => `<label>${rotulo}<input name="${nome}" type="${tipo}" required ${extras}></label>`;

let timer;
export function toast(mensagem, erro = false) {
  const el = document.querySelector('#toast');
  el.textContent = mensagem;
  el.className = `visible ${erro ? 'error' : ''}`;
  clearTimeout(timer);
  timer = setTimeout(() => { el.className = ''; }, 5000);
}

export function modal(titulo, conteudo, { aoEnviar, aoSalvar, textoBotao = 'Salvar', largo = false, aoAbrir } = {}) {
  let enviando = false;
  let confirmado = false;
  const el = document.querySelector('#modal');
  el.className = largo ? 'wide' : '';
  el.innerHTML = `<div class="modal-header"><h2 id="modal-title">${escape(titulo)}</h2><button class="icon-button" type="button" data-close aria-label="Fechar">${icon('close')}</button></div>
    <form class="modal-form"><div class="modal-body">${conteudo}<p class="form-error" role="alert"></p></div>
    ${aoEnviar ? `<div class="modal-footer"><button type="button" class="button secondary" data-close>Cancelar</button><button class="button" type="submit">${escape(textoBotao)}</button></div>` : ''}</form>`;
  el.querySelectorAll('[data-close]').forEach(botao => { botao.onclick = () => { if (!enviando) el.close(); }; });
  el.oncancel = evento => { if (enviando) evento.preventDefault(); };
  if (aoEnviar) {
    const form = el.querySelector('form');
    form.onsubmit = async evento => {
      evento.preventDefault();
      if (enviando || confirmado) return;
      enviando = true;
      const botao = form.querySelector('[type=submit]');
      const erro = form.querySelector('.form-error');
      botao.disabled = true;
      erro.textContent = '';
      let resultado;
      try { resultado = await aoEnviar(Object.fromEntries(new FormData(form)), form); }
      catch (e) {
        erro.textContent = e.message;
        botao.disabled = false;
        enviando = false;
        return;
      }
      confirmado = true;
      enviando = false;
      if (el.contains(form)) el.close();
      await aoSalvar?.(resultado);
    };
  }
  if (!el.open) el.showModal();
  aoAbrir?.(el);
  return el;
}
