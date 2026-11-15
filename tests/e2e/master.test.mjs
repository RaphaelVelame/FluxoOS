import {after, before, test} from 'node:test';
import assert from 'node:assert/strict';
import {spawn} from 'node:child_process';
import {createServer} from 'node:net';
import {createWriteStream} from 'node:fs';
import {mkdir} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
import {setTimeout as delay} from 'node:timers/promises';
import {chromium} from 'playwright';

const root=fileURLToPath(new URL('../../',import.meta.url));
const password='Senha-master-e2e-isolado-2026!';
let app,browser,base,log,empresas=[];
before(async()=>{
  await mkdir(path.join(root,'target/e2e'),{recursive:true});
  const port=await new Promise(resolve=>{const s=createServer();s.listen(0,'127.0.0.1',()=>{const p=s.address().port;s.close(()=>resolve(p));});});
  base=`http://127.0.0.1:${port}`;log=createWriteStream(path.join(root,'target/e2e/master-server.log'));
  app=spawn(process.env.JAVA_HOME?path.join(process.env.JAVA_HOME,'bin','java'):'java',[
    '-jar',process.env.FLUXOOS_TEST_JAR || path.join(root,'target/fluxoos-1.0.0-SNAPSHOT.jar'),
    '--spring.profiles.active=default','--spring.datasource.url=jdbc:h2:mem:e2e-master;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE',
    '--spring.datasource.username=sa','--spring.datasource.password=',`--fluxoos.bootstrap-password=${password}`,
    '--fluxoos.bootstrap-admin-master=true','--fluxoos.demo=false','--fluxoos.cookie-secure=false',
    '--server.address=127.0.0.1',`--server.port=${port}`,'--debug=false'
  ],{cwd:root,windowsHide:true,stdio:['ignore','pipe','pipe']});
  app.stdout.pipe(log);app.stderr.pipe(log);
  let ready=false;
  for(let i=0;i<120;i++){
    if(app.exitCode!==null)throw new Error('Servidor master encerrou; consulte target/e2e/master-server.log');
    try{if((await fetch(base,{signal:AbortSignal.timeout(1000)})).ok){ready=true;break;}}catch{}
    await delay(500);
  }
  assert.ok(ready,'Servidor master não iniciou');
  const master=await acesso('admin',true);
  try{
    for(const sufixo of ['a','b']){
      const empresa=await api(master,'/plataforma/empresas',{nome:`Empresa ${sufixo.toUpperCase()}`});
      await api(master,'/usuarios',{login:`admin-${sufixo}`,nome:`Admin ${sufixo.toUpperCase()}`,senha:password,empresaId:empresa.id,roles:['ROLE_ADMIN_EMPRESA']});
      const admin=await acesso(`admin-${sufixo}`);
      try{empresa.ordem=await api(admin,'/ordens',{solicitante:'Pessoa',local:`Unidade ${sufixo}`,problema:`Chamado da empresa ${sufixo.toUpperCase()}`,prioridade:'MEDIA'});}
      finally{await sair(admin);}
      empresas.push(empresa);
    }
  }finally{await sair(master);}
  browser=await chromium.launch({headless:true,...(process.env.BROWSER_CHANNEL?{channel:process.env.BROWSER_CHANNEL}:{})});
},{timeout:90000});
after(async()=>{await browser?.close();if(app?.pid&&app.exitCode===null){const closed=new Promise(r=>app.once('exit',r));app.kill();await closed;}log?.end();});
async function acesso(usuario,aguardarBootstrap=false){
  let r;
  // O HTTP pode abrir antes do ApplicationRunner confirmar o primeiro administrador.
  // Somente essa sondagem inicial admite até 3 tentativas, abaixo do limite de bloqueio.
  const tentativas=aguardarBootstrap?3:1;
  for(let i=0;i<tentativas;i++){
    r=await fetch(base+'/api/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({usuario,senha:password})});
    if(r.status!==401||i===tentativas-1)break;
    await r.arrayBuffer();await delay(500);
  }
  assert.equal(r.status,200);return {cookie:r.headers.get('set-cookie').split(';')[0],...(await r.json())};
}
async function api(ac,rota,body){
  const r=await fetch(base+'/api'+rota,{method:body?'POST':'GET',headers:{Cookie:ac.cookie,'X-CSRF-Token':ac.csrf,'Content-Type':'application/json','Idempotency-Key':crypto.randomUUID()},body:body?JSON.stringify(body):undefined});
  assert.ok(r.ok,`HTTP ${r.status}: ${rota}`);return r.json();
}
async function sair(ac){await fetch(base+'/api/auth/logout',{method:'POST',headers:{Cookie:ac.cookie,'X-CSRF-Token':ac.csrf}});}
async function login(page,usuario='admin'){
  await page.locator('#login-form').waitFor();await page.locator('#login-form [name=usuario]').fill(usuario);await page.locator('#login-form [name=senha]').fill(password);
  await page.locator('#login-form button').click();
}
async function sessao(fn){
  const context=await browser.newContext({locale:'pt-BR'}),page=await context.newPage();page.setDefaultTimeout(7000);
  const erros=[];page.on('pageerror',e=>erros.push(e.message));
  try{await page.goto(base);await login(page);await fn(page);assert.deepEqual(erros,[]);}
  finally{try{await page.evaluate(async()=>{const csrf=sessionStorage.getItem('fluxoos.csrf');if(csrf)await fetch('/api/auth/logout',{method:'POST',headers:{'X-CSRF-Token':csrf}});});}catch{}await context.close();}
}
async function dados(page,rota){const r=await page.request.get(base+'/api'+rota);assert.equal(r.status(),200);return r.json();}
async function gravar(page,rota,body,method='POST'){
  const csrf=await page.evaluate(()=>sessionStorage.getItem('fluxoos.csrf'));
  const r=await page.request.fetch(base+'/api'+rota,{method,data:body,headers:{'X-CSRF-Token':csrf,'Idempotency-Key':crypto.randomUUID()}});
  assert.ok(r.ok(),`HTTP ${r.status()}: ${rota}`);return (await r.body()).length?r.json():null;
}
async function aguardarInicio(page){await page.getByRole('heading',{name:'Visão global',exact:true}).waitFor();}
async function preencherUsuario(page,nome,usuario){
  await page.locator('#modal [name=nome]').fill(nome);await page.locator('#modal [name=login]').fill(usuario);await page.locator('#modal [name=senha]').fill(password);
}
async function falharConsultas(page){await page.route('**/api/**',r=>r.request().method()==='GET'?r.fulfill({status:503,contentType:'application/json',body:'{"mensagem":"Consulta indisponível no ensaio"}'}):r.continue());}
async function recuperar(page){await page.locator('#retry-sync:not([disabled])').waitFor();await page.unrouteAll({behavior:'wait'});await page.locator('#retry-sync').click();await page.locator('#retry-sync').waitFor({state:'hidden'});}
test('Master entra, atualiza e recarrega sem requisitar estoque de uma empresa',()=>sessao(async page=>{
  const proibidas=[];page.on('request',r=>{if(['/api/materiais','/api/movimentos','/api/estoques'].includes(new URL(r.url()).pathname))proibidas.push(r.url());});
  await page.getByRole('heading',{name:'Visão global',exact:true}).waitFor();
  assert.equal(await page.locator('.mobile-profile').textContent(),'Ad');
  assert.equal(await page.locator('#retry-sync').count(),0);
  assert.equal(await page.locator('[data-nav=estoque]').count(),0);
  assert.equal(await page.getByText('Estoque em dia',{exact:true}).count(),0);
  await page.locator('#refresh').click();await page.getByRole('heading',{name:'Visão global',exact:true}).waitFor();
  await page.reload();await page.getByRole('heading',{name:'Visão global',exact:true}).waitFor();
  assert.deepEqual(proibidas,[]);
  await page.screenshot({path:path.join(root,'target/e2e/master-desktop.png'),fullPage:true});
  await page.setViewportSize({width:390,height:844});
  assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'A visão global não deve ultrapassar a tela do celular');
  await page.screenshot({path:path.join(root,'target/e2e/master-mobile.png'),fullPage:true});
  await page.evaluate(()=>location.hash='ordens');await page.locator('#orders-list .order-card').first().waitFor();
  assert.equal(await page.locator('#orders-list .order-card').count(),2);
  await page.evaluate(()=>location.hash='estoque');
  await page.getByRole('heading',{name:'Estoque por empresa',exact:true}).waitFor();
  assert.equal(await page.locator('#stock-list').count(),0);
  assert.deepEqual(proibidas,[]);
}));

test('Resposta de OS da conta anterior não aparece após entrar em outra empresa',()=>sessao(async page=>{
  await page.getByRole('heading',{name:'Visão global',exact:true}).waitFor();
  const antiga=empresas[0].ordem.id;
  let liberar,recebida,retendo=true;
  const bloqueio=new Promise(r=>liberar=r),retida=new Promise(r=>recebida=r);
  await page.route(`**/api/ordens/${antiga}`,async route=>{if(!retendo){await route.continue();return;}retendo=false;const resposta=await route.fetch();recebida();await bloqueio;await route.fulfill({response:resposta});});
  try{
    await page.evaluate(id=>location.hash=`ordens/${id}`,antiga);await Promise.race([retida,delay(5000).then(()=>{throw new Error('Detalhe não interceptado');})]);
    await page.locator('#logout').click();await login(page,'admin-b');
    await page.getByRole('heading',{name:'Visão geral',exact:true}).waitFor();
    // Mesmo hash do pedido antigo: o controle também precisa conferir a sessão.
    await page.evaluate(id=>location.hash=`ordens/${id}`,antiga);
  await page.getByRole('heading',{name:'Página indisponível',exact:true}).waitFor();
    const antigaConcluida=page.waitForResponse(r=>r.url()===base+`/api/ordens/${antiga}`&&r.status()===200);
    liberar();const respostaAntiga=await antigaConcluida;await respostaAntiga.finished();
    await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
    assert.equal(await page.getByText('Chamado da empresa A',{exact:true}).count(),0);
    assert.equal(await page.getByRole('heading',{name:'Página indisponível',exact:true}).count(),1);
  }finally{liberar();await page.unrouteAll({behavior:'wait'});}
}));

test('Master cadastra empresa e administrador pela interface e o novo acesso fica isolado',()=>sessao(async page=>{
  await aguardarInicio(page);
  const nome='Oficina & Rede '+crypto.randomUUID(),usuario='admin-ui-'+crypto.randomUUID();
  await page.locator('#primary-action').click();await page.locator('#modal [name=nome]').fill(nome);
  await page.locator('#modal button[type=submit]').click();await page.locator('#modal').waitFor({state:'hidden'});
  await aguardarInicio(page);
  const empresa=(await dados(page,'/plataforma/empresas')).find(e=>e.nome===nome);assert.ok(empresa);
  const card=page.locator(`[data-company="${empresa.id}"]`);await card.waitFor();
  assert.ok((await card.textContent()).includes('Sem administrador ativo'));
  await card.locator('[data-add-admin]').click();await preencherUsuario(page,'Administrador da oficina','admin-a');
  assert.equal(await page.locator('#modal input[value="ROLE_ADMIN_MASTER"]').count(),0);
  assert.equal(await page.locator('#modal select[name=empresaId]').count(),0);
  assert.equal(await page.locator('#modal input[type=checkbox][value="ROLE_ADMIN_EMPRESA"]').isDisabled(),true);
  await page.locator('#modal button[type=submit]').click();await page.locator('#modal .form-error').filter({hasText:'duplicado'}).waitFor();
  assert.equal(await page.locator('#modal [name=nome]').inputValue(),'Administrador da oficina');
  assert.ok((await page.locator('#modal').textContent()).includes(nome));
  await page.locator('#modal [name=login]').fill(usuario);await page.locator('#modal button[type=submit]').click();
  await page.locator('#modal').waitFor({state:'hidden'});await page.locator('#team-company').waitFor();
  assert.equal(await page.locator('#modal [name=senha]').count(),0);
  assert.equal(await page.locator('#team-company').inputValue(),String(empresa.id));
  const conta=(await dados(page,'/usuarios')).find(u=>u.login===usuario);
  assert.equal(conta.empresaId,empresa.id);assert.deepEqual(conta.roles,['ROLE_ADMIN_EMPRESA']);
  assert.equal(await page.locator('.person-card').count(),1);
  await page.locator('#logout').click();await login(page,usuario);
  await page.getByRole('heading',{name:'Visão geral',exact:true}).waitFor();
  assert.equal(await page.locator('.company-list').count(),0);
  const usuarios=await dados(page,'/usuarios');assert.ok(usuarios.every(u=>u.empresaId===empresa.id));
  assert.equal((await dados(page,'/ordens')).length,0);
  await page.evaluate(()=>location.hash='equipe');await page.locator('.team-grid').waitFor();
  assert.equal(await page.locator('#team-company').count(),0);
  await page.locator('#primary-action').click();
  assert.equal(await page.locator('#modal input[value="ROLE_ADMIN_EMPRESA"]').count(),0);
  assert.equal(await page.locator('#modal input[value="ROLE_ADMIN_MASTER"]').count(),0);
}));

test('Master filtra, edita e altera status de administradores mantendo o último ativo',()=>sessao(async page=>{
  await aguardarInicio(page);
  const empresa=await gravar(page,'/plataforma/empresas',{nome:'Gestão de acessos '+crypto.randomUUID()});
  const logins=['controle-a-'+crypto.randomUUID(),'controle-b-'+crypto.randomUUID()];
  const contas=[];
  for(const usuario of logins)contas.push(await gravar(page,'/usuarios',{login:usuario,nome:usuario,senha:password,empresaId:empresa.id,roles:['ROLE_ADMIN_EMPRESA']}));
  const sessaoAntiga=await acesso(logins[0]);
  await page.locator('#refresh').click();await aguardarInicio(page);
  await page.locator(`[data-company="${empresa.id}"] a`).click();await page.locator('#team-company').waitFor();
  assert.equal(await page.locator('.person-card').count(),2);
  await page.locator(`[data-edit-user="${contas[0].id}"]`).click();
  assert.equal(await page.locator('#modal select[name=empresaId]').count(),0);
  await page.locator('#modal [name=nome]').fill('Administrador atualizado');await page.locator('#modal input[value="ROLE_SUPORTE_TECNICO"]').check();
  await page.locator('#modal button[type=submit]').click();await page.locator('#modal').waitFor({state:'hidden'});
  await page.locator(`[data-user-status="${contas[0].id}"]`).waitFor();
  const atual=(await dados(page,'/usuarios')).find(u=>u.id===contas[0].id);
  assert.equal(atual.empresaId,empresa.id);assert.ok(atual.roles.includes('ROLE_SUPORTE_TECNICO'));
  assert.equal((await fetch(base+'/api/auth/me',{headers:{Cookie:sessaoAntiga.cookie}})).status,401);
  await page.locator(`[data-user-status="${contas[0].id}"]`).click();await page.locator('#modal button[type=submit]').click();await page.locator('#modal').waitFor({state:'hidden'});
  await page.locator(`[data-user-status="${contas[1].id}"]:disabled`).waitFor();
  assert.equal((await dados(page,'/usuarios')).find(u=>u.id===contas[0].id).ativo,false);
  await page.locator(`[data-edit-user="${contas[1].id}"]`).click();assert.equal(await page.locator('#modal input[type=checkbox][value="ROLE_ADMIN_EMPRESA"]').isDisabled(),true);
  await page.locator('#modal').getByRole('button',{name:'Fechar',exact:true}).click();
  await page.locator(`[data-user-status="${contas[0].id}"]`).click();await page.locator('#modal button[type=submit]').click();await page.locator('#modal').waitFor({state:'hidden'});
  await page.locator(`[data-user-status="${contas[1].id}"]:not([disabled])`).waitFor();
  await page.locator('#team-company').selectOption('');
  const eu=await dados(page,'/auth/me');assert.equal(await page.locator(`[data-user="${eu.id}"] button`).count(),0);
  await page.setViewportSize({width:390,height:844});
  assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
  await page.screenshot({path:path.join(root,'target/e2e/master-usuarios-mobile.png'),fullPage:true});
}));

test('Cadastro de empresa confirmado oferece recuperação sem repetir o POST',()=>sessao(async page=>{
  await aguardarInicio(page);const nome='Empresa confirmada '+crypto.randomUUID();let posts=0;
  page.on('request',r=>{if(r.method()==='POST'&&r.url()===base+'/api/plataforma/empresas')posts++;});
  await page.locator('#primary-action').click();await page.locator('#modal [name=nome]').fill(nome);await falharConsultas(page);
  await page.locator('#modal button[type=submit]').click();await page.locator('#modal').waitFor({state:'hidden'});
  await recuperar(page);await aguardarInicio(page);
  assert.equal(posts,1);assert.equal((await dados(page,'/plataforma/empresas')).filter(e=>e.nome===nome).length,1);
  await page.setViewportSize({width:390,height:844});assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
  await page.screenshot({path:path.join(root,'target/e2e/master-empresas-mobile.png'),fullPage:true});
}));

test('Administrador confirmado não é cadastrado novamente após falha de consulta',()=>sessao(async page=>{
  await aguardarInicio(page);const usuario='admin-confirmado-'+crypto.randomUUID();let posts=0;
  page.on('request',r=>{if(r.method()==='POST'&&r.url()===base+'/api/usuarios')posts++;});
  await page.locator(`[data-add-admin="${empresas[1].id}"]`).click();await preencherUsuario(page,'Administrador confirmado',usuario);await falharConsultas(page);
  await page.locator('#modal button[type=submit]').click();await page.locator('#modal').waitFor({state:'hidden'});await recuperar(page);
  await page.locator('#team-company').waitFor();
  assert.equal(posts,1);const contas=(await dados(page,'/usuarios')).filter(u=>u.login===usuario);assert.equal(contas.length,1);assert.equal(contas[0].empresaId,empresas[1].id);
}));

test('Master escolhe a empresa do funcionário e cancelar limpa as credenciais',()=>sessao(async page=>{
  await aguardarInicio(page);
  await page.evaluate(id=>location.hash=`equipe/${id}`,empresas[0].id);await page.locator('#team-company').waitFor();
  await page.locator('#primary-action').click();await preencherUsuario(page,'Cadastro cancelado','cancelado-'+crypto.randomUUID());
  assert.equal(await page.locator('#modal [name=empresaId]').inputValue(),String(empresas[0].id));
  await page.keyboard.press('Escape');await page.locator('#modal').waitFor({state:'hidden'});
  assert.equal(await page.locator('#modal input').count(),0);
  await page.locator('#primary-action').click();
  const usuario='funcionario-master-'+crypto.randomUUID();
  await preencherUsuario(page,'Funcionário de outra empresa',usuario);
  await page.locator('#modal [name=empresaId]').selectOption(String(empresas[1].id));
  await page.locator('#modal input[value="ROLE_ESTOQUISTA"]').check();
  assert.equal(await page.locator('#modal input[value="ROLE_ADMIN_MASTER"]').count(),0);
  await page.locator('#modal button[type=submit]').click();await page.locator('#modal').waitFor({state:'hidden'});
  await page.waitForFunction(id=>document.querySelector('#team-company')?.value===String(id),empresas[1].id);
  const criada=(await dados(page,'/usuarios')).find(u=>u.login===usuario);
  assert.equal(criada.empresaId,empresas[1].id);
  assert.deepEqual(new Set(criada.roles),new Set(['ROLE_SUPORTE_EXTERNO','ROLE_ESTOQUISTA']));
}));
