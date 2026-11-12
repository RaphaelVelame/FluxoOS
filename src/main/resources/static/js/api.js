let csrf = sessionStorage.getItem('fluxoos.csrf');

export function salvarCsrf(valor) {
  csrf = valor;
  if (valor) sessionStorage.setItem('fluxoos.csrf', valor);
  else sessionStorage.removeItem('fluxoos.csrf');
}

export async function api(caminho, { method = 'GET', body, key } = {}) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (method !== 'GET' && csrf) headers['X-CSRF-Token'] = csrf;
  if (key) headers['Idempotency-Key'] = key;
  const resposta = await fetch(`/api${caminho}`, {
    method, headers, credentials: 'same-origin', body: body === undefined ? undefined : JSON.stringify(body),
  });
  const tipo = resposta.headers.get('content-type') || '';
  const dados = tipo.includes('application/json') ? await resposta.json() : null;
  if (!resposta.ok) {
    if (resposta.status === 401 && caminho !== '/auth/login') {
      const tinhaSessao = !!csrf;
      salvarCsrf(null);
      if (tinhaSessao) window.dispatchEvent(new Event('sessao-expirada'));
    }
    throw new Error(dados?.mensagem || 'Não foi possível conectar ao servidor. Tente novamente.');
  }
  return dados;
}

export function chaveOperacao() {
  if (globalThis.crypto?.randomUUID) return crypto.randomUUID();
  const valores = crypto.getRandomValues(new Uint8Array(24));
  return Array.from(valores, n => n.toString(16).padStart(2, '0')).join('');
}
