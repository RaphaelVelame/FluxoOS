export function prepararAssinatura(canvas) {
  const ctx = canvas.getContext('2d');
  const escala = Math.min(devicePixelRatio || 1, 2);
  const largura = Math.max(200, canvas.getBoundingClientRect().width);
  canvas.width = largura * escala;
  canvas.height = 180 * escala;
  ctx.scale(escala, escala);
  ctx.lineWidth = 2.4;
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  ctx.strokeStyle = '#183f38';
  let desenhando = false;
  let tracou = false;
  const ponto = e => {
    const caixa = canvas.getBoundingClientRect();
    return [e.clientX - caixa.left, e.clientY - caixa.top];
  };
  canvas.onpointerdown = e => {
    if (e.pointerType === 'mouse' && e.button !== 0) return;
    canvas.setPointerCapture(e.pointerId);
    desenhando = true;
    ctx.beginPath();
    ctx.moveTo(...ponto(e));
  };
  canvas.onpointermove = e => {
    if (!desenhando) return;
    ctx.lineTo(...ponto(e));
    ctx.stroke();
    tracou = true;
  };
  canvas.onpointerup = canvas.onpointercancel = () => { desenhando = false; };
  return {
    limpar() { ctx.clearRect(0, 0, largura, 180); tracou = false; },
    imagem() {
      if (!tracou) throw new Error('Desenhe a assinatura do responsável antes de concluir.');
      return canvas.toDataURL('image/png');
    },
  };
}
