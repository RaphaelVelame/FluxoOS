import { api } from '../api.js';
import { campo, escape, modal, vazio } from '../components.js';

export const podeCadastrarCliente = ctx => ctx.temFuncao(ctx.estado.usuario,
  'ROLE_ADMIN_EMPRESA', 'ROLE_SUPERVISOR', 'ROLE_SUPORTE_TECNICO', 'ROLE_ESTOQUISTA');

export function seletorCliente(ctx, interno = false) {
  return `<label>${interno ? 'Cliente / atendimento' : 'Cliente cadastrado'}<select name="clienteId" required><option value="" disabled selected>Selecione ${interno ? 'o cliente ou atendimento interno' : 'o cliente'}</option>${interno ? '<option value="INTERNO">Atendimento interno da empresa</option>' : ''}${ctx.estado.clientes.map(c => `<option value="${c.id}">${escape(c.nome)} · ${escape(c.codigo)}</option>`).join('')}</select></label>`;
}

function novoCliente(ctx) {
  modal('Novo cliente', `<p class="form-intro">Cadastre o cliente para vincular suas ordens de serviço e identificar seus materiais. O cadastro pertence à sua empresa.</p>${campo('Código do cliente', 'codigo', 'text', 'maxlength="60" placeholder="Ex.: CLI-001"')}${campo('Nome do cliente', 'nome', 'text', 'maxlength="160" autocomplete="organization"')}`, {
    textoBotao: 'Cadastrar cliente',
    aoEnviar: e => api('/clientes', { method: 'POST', body: e }),
    aoSalvar: () => ctx.aposGravar('Cliente cadastrado.', 'clientes'),
  });
}

export function clientes(ctx) {
  ctx.pagina(`<p class="hint">Use o mesmo cliente nas ordens de serviço e nos estoques que guardam materiais dele.</p><label class="search-field"><input id="client-search" type="search" aria-label="Buscar clientes" placeholder="Buscar cliente por nome ou código"></label><div id="clients-list" class="orders-grid"></div>`, {
    titulo: 'Clientes', subtitulo: 'Clientes atendidos pela sua empresa',
    acao: podeCadastrarCliente(ctx) ? { texto: 'Novo cliente', executar: () => novoCliente(ctx) } : null,
  });
  const render = () => {
    const termo = document.querySelector('#client-search').value.toLocaleLowerCase('pt-BR');
    const lista = ctx.estado.clientes.filter(c => `${c.nome} ${c.codigo}`.toLocaleLowerCase('pt-BR').includes(termo));
    document.querySelector('#clients-list').innerHTML = lista.length ? lista.map(c => `<article class="panel detail-panel"><span class="small-code">${escape(c.codigo)}</span><h2>${escape(c.nome)}</h2><p>${ctx.estado.ordens.filter(o => o.clienteId === c.id).length} ordens de serviço · ${ctx.estado.estoques.filter(e => e.clienteId === c.id).length} estoques</p></article>`).join('') : vazio('Nenhum cliente encontrado', 'Cadastre o cliente antes de abrir seu atendimento ou criar seu estoque.');
  };
  document.querySelector('#client-search').addEventListener('input', render);
  render();
}
