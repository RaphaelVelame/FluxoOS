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
const password='Senha-e2e-gravacao-2026!';
let app,browser,base,log;
before(async()=>{
  await mkdir(path.join(root,'target/e2e'),{recursive:true});
  const port=await new Promise(resolve=>{const server=createServer();server.listen(0,'127.0.0.1',()=>{const p=server.address().port;server.close(()=>resolve(p));});});
  base=`http://127.0.0.1:${port}`;
  log=createWriteStream(path.join(root,'target/e2e/server.log'));
  app=spawn(process.env.JAVA_HOME?path.join(process.env.JAVA_HOME,'bin','java'):'java',[
    '-jar',path.join(root,'target/fluxoos-1.0.0-SNAPSHOT.jar'),
    '--spring.profiles.active=default','--spring.datasource.url=jdbc:h2:mem:e2e-gravacao;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE',
    '--spring.datasource.username=sa','--spring.datasource.password=',`--fluxoos.bootstrap-password=${password}`,
    '--fluxoos.demo=true','--fluxoos.cookie-secure=false','--fluxoos.seguranca.max-sessoes=20',
    '--server.address=127.0.0.1',`--server.port=${port}`,'--debug=false'
  ],{cwd:root,windowsHide:true,stdio:['ignore','pipe','pipe']});
  app.stdout.pipe(log);app.stderr.pipe(log);app.on('error',error=>{console.error(error);});
  let ready=false;
  for(let i=0;i<120;i++){
    if(app.exitCode!==null)throw new Error('Servidor de teste terminou; consulte target/e2e/server.log');
    try{if((await fetch(base,{signal:AbortSignal.timeout(1000)})).ok){ready=true;break;}}catch{}
    await delay(500);
  }
  assert.ok(ready,'Servidor não iniciou');
  browser=await chromium.launch({headless:true,...(process.env.BROWSER_CHANNEL?{channel:process.env.BROWSER_CHANNEL}:{})});
}, {timeout:90000});
after(async()=>{await browser?.close();if(app?.pid&&app.exitCode===null){const closed=new Promise(resolve=>app.once('exit',resolve));app.kill();await closed;}log?.end();});

async function session(fn){
  const context=await browser.newContext({locale:'pt-BR'}),page=await context.newPage();
  const errors=[];page.on('pageerror',error=>errors.push(error.message));
  page.setDefaultTimeout(8000);
  try{
    await page.goto(base);await page.locator('#login-form').waitFor();
    await page.locator('[name=usuario]').fill('admin');await page.locator('[name=senha]').fill(password);
    await page.locator('#login-form button').click();await page.locator('#page-header h1').waitFor();
    await fn(page);
    assert.deepEqual(errors,[],'Erros JavaScript no navegador');
  }finally{await context.close();}
}
async function route(page,hash,selector){await page.evaluate(h=>location.hash=h,hash);await page.locator(selector).waitFor();}
async function newOrder(page,problem){await route(page,'ordens','#orders-list');await page.locator('#primary-action').click();await page.locator('#modal [name=solicitante]').fill('Teste E2E');await page.locator('#modal [name=local]').fill('Ambiente isolado');await page.locator('#modal [name=problema]').fill(problem);}
async function failReads(page){await page.route('**/api/**',async r=>{if(r.request().method()==='GET')await r.fulfill({status:503,contentType:'application/json',body:'{"mensagem":"Falha simulada de consulta"}'});else await r.continue();});}
async function recovery(page){await page.locator('#modal').waitFor({state:'hidden'});await page.locator('#retry-sync:not([disabled])').waitFor();await page.unrouteAll({behavior:'wait'});await page.locator('#retry-sync').click();await page.locator('#retry-sync').waitFor({state:'hidden'});}
async function data(page,url){const r=await page.request.get(base+'/api'+url);assert.equal(r.status(),200);return r.json();}

test('OS confirmada não permite reenvio quando a atualização falha',()=>session(async page=>{
  const problem='Confirmada-'+crypto.randomUUID();let posts=0;
  page.on('request',r=>{if(r.method()==='POST'&&r.url()===base+'/api/ordens')posts++;});
  await newOrder(page,problem);await failReads(page);await page.locator('#modal button[type=submit]').click();
  await recovery(page);const orders=await data(page,'/ordens');assert.equal(orders.filter(o=>o.problema===problem).length,1);assert.equal(posts,1);
  assert.match(await page.locator('#page-header h1').innerText(),/Ordem de serviço #/);
}));

test('Perda da resposta do POST permite repetir sem criar outra OS',()=>session(async page=>{
  const problem='Resposta-perdida-'+crypto.randomUUID();let first=true;
  await page.route('**/api/ordens',async r=>{if(r.request().method()==='POST'&&first){first=false;const response=await r.fetch();assert.equal(response.status(),201);await r.abort('failed');}else await r.continue();});
  await newOrder(page,problem);await page.locator('#modal button[type=submit]').click();
  await page.waitForFunction(()=>!!document.querySelector('#modal .form-error')?.textContent);
  await page.locator('#modal button[type=submit]').click();await page.locator('#modal').waitFor({state:'hidden'});
  const orders=await data(page,'/ordens');assert.equal(orders.filter(o=>o.problema===problem).length,1);
}));

async function write(page,url,body,method='POST'){
  const csrf=await page.evaluate(()=>sessionStorage.getItem('fluxoos.csrf'));
  const response=await page.request.fetch(base+'/api'+url,{method,headers:{'X-CSRF-Token':csrf,'Idempotency-Key':crypto.randomUUID()},...(body!==undefined?{data:body}:{})});
  assert.ok(response.ok(),`${method} ${url}: ${response.status()} ${await response.text()}`);
  return response.headers()['content-type']?.includes('application/json')?response.json():null;
}
async function material(page){const m=await write(page,'/materiais',{codigo:crypto.randomUUID(),nome:'Material de teste',unidade:'un',minimo:0});await write(page,`/materiais/${m.id}/movimentos`,{tipo:'ENTRADA',variacao:10,observacao:'Carga isolada'});return m;}
async function order(page,{start=false,report=false}={}){
  const o=await write(page,'/ordens',{solicitante:'E2E',local:'Teste',problema:'Atendimento '+crypto.randomUUID(),prioridade:'MEDIA'});
  const users=await data(page,'/usuarios');await write(page,`/ordens/${o.id}/atribuicao`,{tecnicoId:users.find(u=>u.perfil==='TECNICO'&&u.ativo).id});
  if(start)await write(page,`/ordens/${o.id}/inicio`);
  if(report)await write(page,`/ordens/${o.id}/relato`,{relato:'Relato prévio'},'PUT');
  return o;
}

test('Erro real de gravação mantém valores e permite corrigir/repetir',()=>session(async page=>{
  const problem='Erro-real-'+crypto.randomUUID();await newOrder(page,problem);
  await page.route('**/api/ordens',r=>r.request().method()==='POST'?r.fulfill({status:400,contentType:'application/json',body:'{"mensagem":"Confira os dados da ordem."}'}):r.continue());
  await page.locator('#modal button[type=submit]').click();await page.waitForFunction(()=>document.querySelector('#modal .form-error')?.textContent==='Confira os dados da ordem.');
  assert.equal(await page.locator('#modal [name=problema]').inputValue(),problem);assert.equal((await data(page,'/ordens')).filter(o=>o.problema===problem).length,0);
  await page.unrouteAll();await page.locator('#modal button[type=submit]').click();await page.locator('#modal').waitFor({state:'hidden'});
  assert.equal((await data(page,'/ordens')).filter(o=>o.problema===problem).length,1);
}));

test('Envios simultâneos do mesmo formulário só disparam um POST',()=>session(async page=>{
  let count=0;page.on('request',r=>{if(r.method()==='POST'&&r.url()===base+'/api/ordens')count++;});
  await newOrder(page,'Duplo-envio-'+crypto.randomUUID());
  await page.locator('#modal form').evaluate(form=>{form.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));form.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));});
  await page.locator('#modal').waitFor({state:'hidden'});assert.equal(count,1);
}));

test('Falha ao ler detalhes depois de criar OS também oferece somente atualização',()=>session(async page=>{
  await newOrder(page,'Detalhe-indisponível-'+crypto.randomUUID());
  await page.route(/\/api\/ordens\/\d+(\/historico)?$/,r=>r.fulfill({status:503,contentType:'application/json',body:'{"mensagem":"Detalhe indisponível"}'}));
  await page.locator('#modal button[type=submit]').click();await recovery(page);await page.locator('#assign').waitFor();
}));

test('Cadastro de material confirmado fecha modal apesar de falha de consulta',()=>session(async page=>{
  const code=crypto.randomUUID();await route(page,'estoque','#stock-list');await page.locator('#primary-action').click();
  await page.locator('#modal [name=codigo]').fill(code);await page.locator('#modal [name=nome]').fill('Cadastro E2E');await page.locator('#modal [name=unidade]').fill('un');
  await failReads(page);await page.locator('#modal button[type=submit]').click();await recovery(page);assert.equal((await data(page,'/materiais')).filter(m=>m.codigo===code).length,1);
}));

test('Entrada de estoque não é reenviada ao recuperar consulta',()=>session(async page=>{
  const m=await material(page);await page.locator('#refresh').click();await route(page,'estoque',`[data-move="${m.id}"]`);await page.locator(`[data-move="${m.id}"]`).click();
  await page.locator('#modal [name=variacao]').fill('3');await page.locator('#modal [name=observacao]').fill('Entrada única');
  await failReads(page);await page.locator('#modal button[type=submit]').click();await recovery(page);assert.equal((await data(page,'/materiais')).find(x=>x.id===m.id).quantidade,13);
}));

test('Status de material confirmado permanece aplicado após recuperar tela',()=>session(async page=>{
  const m=await material(page);await page.locator('#refresh').click();await route(page,'estoque',`[data-status="${m.id}"]`);await page.locator(`[data-status="${m.id}"]`).click();
  await failReads(page);await page.locator('#modal button[type=submit]').click();await recovery(page);assert.equal((await data(page,'/materiais')).find(x=>x.id===m.id).ativo,false);
}));

test('Cadastro de conta confirmado não aparece como erro de envio',()=>session(async page=>{
  const login='pessoa-'+crypto.randomUUID();await route(page,'equipe','.team-grid');await page.locator('#primary-action').click();
  await page.locator('#modal [name=nome]').fill('Pessoa E2E');await page.locator('#modal [name=login]').fill(login);await page.locator('#modal [name=senha]').fill(password);
  await failReads(page);await page.locator('#modal button[type=submit]').click();await recovery(page);assert.equal((await data(page,'/usuarios')).filter(u=>u.login===login).length,1);
}));

test('Status de conta confirmado não é repetido ao atualizar',()=>session(async page=>{
  const u=await write(page,'/usuarios',{login:'status-'+crypto.randomUUID(),nome:'Pessoa status',perfil:'TECNICO',senha:password});
  await page.locator('#refresh').click();await route(page,'equipe',`[data-user-status="${u.id}"]`);await page.locator(`[data-user-status="${u.id}"]`).click();
  await failReads(page);await page.locator('#modal button[type=submit]').click();await recovery(page);assert.equal((await data(page,'/usuarios')).find(x=>x.id===u.id).ativo,false);
}));

test('Atribuição confirmada não é reenviada quando consulta falha',()=>session(async page=>{
  const o=await write(page,'/ordens',{solicitante:'E2E',local:'Teste',problema:'Atribuição',prioridade:'MEDIA'});
  await route(page,`ordens/${o.id}`,'#assign');await page.locator('#assign').click();await failReads(page);await page.locator('#modal button[type=submit]').click();await recovery(page);
  assert.equal((await data(page,`/ordens/${o.id}/historico`)).filter(e=>e.tipo==='ATRIBUICAO').length,1);
}));

test('Início confirmado não deixa botão de iniciar novamente disponível',()=>session(async page=>{
  const o=await order(page);await route(page,`ordens/${o.id}`,'#start');await failReads(page);await page.locator('#start').click();await recovery(page);
  assert.equal((await data(page,`/ordens/${o.id}`)).situacao,'EM_ANDAMENTO');assert.equal((await data(page,`/ordens/${o.id}/historico`)).filter(e=>e.tipo==='INICIO').length,1);
}));

test('Relato salvo permanece confirmado quando atualização falha',()=>session(async page=>{
  const o=await order(page,{start:true});await route(page,`ordens/${o.id}`,'#report');await page.locator('#report').fill('Relato salvo uma vez');
  await failReads(page);await page.locator('#report-form button').click();await recovery(page);
  assert.equal((await data(page,`/ordens/${o.id}`)).relato,'Relato salvo uma vez');assert.equal((await data(page,`/ordens/${o.id}/historico`)).filter(e=>e.tipo==='RELATO').length,1);
}));

test('Consumo confirmado não baixa estoque novamente após recuperar tela',()=>session(async page=>{
  const m=await material(page),o=await order(page,{start:true});await page.locator('#refresh').click();await route(page,`ordens/${o.id}`,'#consume');await page.locator('#consume').click();
  await page.locator('#modal [name=materialId]').selectOption(String(m.id));await page.locator('#modal [name=quantidade]').fill('2');await page.locator('#modal [name=observacao]').fill('Consumo único');
  await failReads(page);await page.locator('#modal button[type=submit]').click();await recovery(page);
  assert.equal((await data(page,'/materiais')).find(x=>x.id===m.id).quantidade,8);assert.equal((await data(page,`/ordens/${o.id}`)).itens.length,1);
}));

test('Conclusão com assinatura confirmada não solicita nova assinatura após falha de consulta',()=>session(async page=>{
  const o=await order(page,{start:true,report:true});await route(page,`ordens/${o.id}`,'#complete');await page.locator('#complete').click();await page.locator('#modal [name=responsavel]').fill('Responsável E2E');
  const box=await page.locator('#signature').boundingBox();await page.mouse.move(box.x+20,box.y+80);await page.mouse.down();await page.mouse.move(box.x+180,box.y+40,{steps:12});await page.mouse.up();
  await failReads(page);await page.locator('#modal button[type=submit]').click();await recovery(page);await page.locator('.saved-signature').waitFor();
  assert.equal((await data(page,`/ordens/${o.id}`)).situacao,'CONCLUIDA');assert.equal((await data(page,`/ordens/${o.id}/historico`)).filter(e=>e.tipo==='CONCLUSAO').length,1);
}));

test('Login confirmado oferece atualização sem repetir credenciais se consulta falhar',async()=>{
  const context=await browser.newContext(),page=await context.newPage();
  try{
    let posts=0;page.on('request',r=>{if(r.method()==='POST'&&r.url().endsWith('/api/auth/login'))posts++;});
    await page.goto(base);await page.locator('#login-form').waitFor();await failReads(page);
    await page.locator('[name=usuario]').fill('admin');await page.locator('[name=senha]').fill(password);await page.locator('#login-form button').click();
    await recovery(page);assert.equal(posts,1);assert.equal(await page.locator('#page-header h1').innerText(),'Visão geral');
  }finally{await context.close();}
});

test('Expiração durante consulta posterior preserva aviso de OS criada',()=>session(async page=>{
  const problem='Sessão-expirada-'+crypto.randomUUID();await newOrder(page,problem);
  await page.route('**/api/**',async r=>{if(r.request().method()==='GET')await r.fulfill({status:401,contentType:'application/json',body:'{"mensagem":"Sessão expirada"}'});else await r.continue();});
  await page.locator('#modal button[type=submit]').click();await page.locator('#login-form').waitFor();await page.waitForLoadState('networkidle');
  assert.match(await page.locator('#toast').innerText(),/criada\..*sessão expirou/i);assert.equal((await data(page,'/ordens')).filter(o=>o.problema===problem).length,1);
}));

test('Consulta iniciada antes da gravação não apaga sua confirmação',()=>session(async page=>{
  const problem='Consulta-concorrente-'+crypto.randomUUID();await route(page,'ordens','#orders-list');
  let release;const held=new Promise(resolve=>release=resolve);let started;const intercepted=new Promise(resolve=>started=resolve);let first=true;
  await page.route('**/api/materiais',async r=>{if(first){first=false;started();await held;}await r.continue();});
  try{
    await page.locator('#refresh').click();await intercepted;
    await page.locator('#primary-action').click();await page.locator('#modal [name=solicitante]').fill('E2E');await page.locator('#modal [name=local]').fill('Teste');await page.locator('#modal [name=problema]').fill(problem);
    const saved=page.waitForResponse(r=>r.request().method()==='POST'&&r.url()===base+'/api/ordens');
    await page.locator('#modal button[type=submit]').click();assert.equal((await saved).status(),201);release();
    await page.locator('#assign').waitFor();assert.match(await page.locator('#toast').innerText(),/criada/);assert.equal((await data(page,'/ordens')).filter(o=>o.problema===problem).length,1);
  }finally{release();}
}));

test('Capacidade de sessões é informada no login e logout libera nova entrada',async()=>{
  const context=await browser.newContext(),page=await context.newPage(),acessos=[];
  try{
    let recusado=false;
    for(let i=0;i<21;i++){
      const response=await fetch(base+'/api/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({usuario:'admin',senha:password})});
      if(response.status===429){recusado=true;break;}
      assert.equal(response.status,200);const body=await response.json();acessos.push({cookie:response.headers.get('set-cookie').split(';')[0],csrf:body.csrf});
    }
    assert.ok(recusado);assert.ok(acessos.length>0);
    await page.goto(base);await page.locator('#login-form').waitFor();await page.locator('[name=usuario]').fill('admin');await page.locator('[name=senha]').fill(password);await page.locator('#login-form button').click();
    await page.waitForFunction(()=>document.querySelector('#login-form .form-error')?.textContent.includes('20 sessões'));
    const saida=await fetch(base+'/api/auth/logout',{method:'POST',headers:{Cookie:acessos[0].cookie,'X-CSRF-Token':acessos[0].csrf}});assert.equal(saida.status,200);
    await page.locator('#login-form button').click();await page.locator('#page-header h1').waitFor();assert.equal((await page.request.get(base+'/api/auth/me')).status(),200);
  }finally{
    for(const acesso of acessos.slice(1))await fetch(base+'/api/auth/logout',{method:'POST',headers:{Cookie:acesso.cookie,'X-CSRF-Token':acesso.csrf}});
    await context.close();
  }
});
