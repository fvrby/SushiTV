// SushiTV welcome animation for the browser lab: the same scene as SushiWelcome.kt. drawScene(ctx, t, canvasWidth).
const cl=x=>Math.max(0,Math.min(1,x)),seg=(t,a,b)=>cl((t-a)/(b-a));
const eOut=x=>1-Math.pow(1-x,3),eIO=x=>x<.5?4*x*x*x:1-Math.pow(-2*x+2,3)/2;
const eBack=x=>{const c1=1.70158,c3=c1+1,u=x-1;return 1+c3*u*u*u+c1*u*u};
const bump=(t,a,b)=>Math.sin(seg(t,a,b)*Math.PI);
const C={ink:'#0E0B09',body:'#1B1512',cafe:'#8B5A3C',caramel:'#C98B5B',orange:'#D9955E',white:'#FFFDF8',fur:'#E6D8C8',black:'#1A1614',pink:'#F2B3AE',nose:'#EBA9A6',eye1:'#DDF3C4',eye2:'#93C47A',eyeLine:'#3F5C34',green:'#A8DB8E',salmon:'#FF7A59',cream:'#F3E6D6',earLight:'#EEE6DD'};
let ctx;
function rr(x,y,w,h,r){ctx.beginPath();ctx.roundRect(x,y,w,h,r)}
function hash(i,n){const x=Math.sin(i*127.1+n*311.7)*43758.5453;return x-Math.floor(x)}
const F=[...Array(16)].map((_,i)=>({x:hash(i,1)*1920,y:hash(i,2)*1280,v:22+hash(i,3)*34,sz:20+hash(i,4)*24,k:i%5,ph:hash(i,5)*6.28,rot:hash(i,6)*6.28}));
function star(x,y,r,col){ctx.fillStyle=col;ctx.beginPath();for(let i=0;i<8;i++){const a=i*Math.PI/4,q=i%2?r*.28:r;ctx.lineTo(x+Math.sin(a)*q,y-Math.cos(a)*q)}ctx.fill()}
function floater(f,t){
  const y=((f.y-t*f.v)%1280+1280)%1280-100,x=f.x+26*Math.sin(t*0.8+f.ph),s=f.sz;
  ctx.save();ctx.translate(x,y);ctx.rotate(f.rot+0.25*Math.sin(t*0.6+f.ph));
  if(f.k==0){rr(-s*.8,-s*.25,s*1.6,s*.75,s*.35);ctx.fillStyle=C.white;ctx.fill();rr(-s*.9,-s*.55,s*1.8,s*.5,s*.25);ctx.fillStyle='#D9824B';ctx.fill();ctx.fillStyle='#F2B48A';ctx.fillRect(-s*.4,-s*.5,s*.12,s*.4);ctx.fillRect(s*.15,-s*.5,s*.12,s*.4)}
  else if(f.k==1){ctx.fillStyle='#151515';ctx.beginPath();ctx.arc(0,0,s*.6,0,7);ctx.fill();ctx.fillStyle=C.white;ctx.beginPath();ctx.arc(0,0,s*.45,0,7);ctx.fill();ctx.fillStyle='#9BD27A';ctx.beginPath();ctx.arc(0,0,s*.2,0,7);ctx.fill()}
  else if(f.k==2){ctx.fillStyle=C.caramel;ctx.beginPath();ctx.ellipse(0,s*.15,s*.38,s*.3,0,0,7);ctx.fill();for(const a of[-.9,-.3,.3,.9]){ctx.beginPath();ctx.arc(Math.sin(a)*s*.5,-Math.cos(a)*s*.42,s*.14,0,7);ctx.fill()}}
  else if(f.k==3){ctx.fillStyle='#9BD27A';ctx.beginPath();ctx.ellipse(0,0,s*.55,s*.3,0,0,7);ctx.fill();ctx.beginPath();ctx.moveTo(s*.45,0);ctx.lineTo(s*.85,-s*.3);ctx.lineTo(s*.85,s*.3);ctx.fill();ctx.fillStyle='#1A1614';ctx.beginPath();ctx.arc(-s*.28,-s*.05,s*.06,0,7);ctx.fill()}
  else star(0,0,s*.45,'#FFE7C7');
  ctx.restore()}
function headPath(){const p=new Path2D();p.moveTo(0,-110);p.bezierCurveTo(95,-110,150,-60,150,15);p.bezierCurveTo(150,85,85,122,0,122);p.bezierCurveTo(-85,122,-150,85,-150,15);p.bezierCurveTo(-150,-60,-95,-110,0,-110);p.closePath();return p}
function ear(sign,ang,outer){ctx.save();ctx.translate(sign*92,-72);ctx.rotate(sign*ang);ctx.translate(-sign*92,72);
  const o=new Path2D();o.moveTo(sign*150,-20);o.quadraticCurveTo(sign*156,-150,sign*120,-182);o.quadraticCurveTo(sign*84,-150,sign*36,-104);o.closePath();ctx.fillStyle=outer;ctx.fill(o);
  if(outer!==C.black){ctx.lineWidth=3;ctx.strokeStyle='#CFC4B8';ctx.stroke(o)}
  const i=new Path2D();i.moveTo(sign*128,-45);i.quadraticCurveTo(sign*132,-130,sign*116,-155);i.quadraticCurveTo(sign*92,-128,sign*62,-98);i.closePath();ctx.fillStyle=C.pink;ctx.fill(i);ctx.restore()}
function eye(x,o,pd,lx,ly){
  ctx.save();ctx.translate(x,-5);
  if(o<0.12){ctx.strokeStyle='#2B2320';ctx.lineWidth=5;ctx.lineCap='round';ctx.beginPath();ctx.moveTo(-26,-2);ctx.quadraticCurveTo(0,12,26,-2);ctx.stroke()}
  else{ctx.scale(1,o);const g=ctx.createRadialGradient(-6,-8,4,0,0,34);g.addColorStop(0,C.eye1);g.addColorStop(1,C.eye2);ctx.fillStyle=g;ctx.beginPath();ctx.ellipse(0,0,30,32,0,0,7);ctx.fill();ctx.lineWidth=3.5;ctx.strokeStyle=C.eyeLine;ctx.stroke();
    ctx.fillStyle='#15110F';ctx.beginPath();ctx.ellipse(lx,ly,15*pd,18*pd,0,0,7);ctx.fill();
    ctx.fillStyle='#fff';ctx.beginPath();ctx.arc(lx-9,ly-11,7,0,7);ctx.fill();ctx.beginPath();ctx.arc(lx+8,ly+9,3.5,0,7);ctx.fill()}
  ctx.restore()}
function blinkAt(t,a,c,h,o){if(t<a||t>a+c+h+o)return 1;if(t<a+c)return 1-eIO(seg(t,a,a+c));if(t<a+c+h)return 0;return eIO(seg(t,a+c+h,a+c+h+o))}
function markings(){
  ctx.fillStyle=C.black;ctx.beginPath();ctx.moveTo(-175,-135);ctx.lineTo(175,-135);ctx.lineTo(175,-66);
  ctx.quadraticCurveTo(120,-64,80,-50);ctx.quadraticCurveTo(40,-40,16,-58);ctx.lineTo(-16,-58);
  ctx.quadraticCurveTo(-46,-46,-74,-44);ctx.quadraticCurveTo(-100,-38,-104,-8);ctx.quadraticCurveTo(-108,30,-98,62);ctx.quadraticCurveTo(-135,74,-175,66);ctx.closePath();ctx.fill();
  ctx.fillStyle=C.orange;ctx.beginPath();ctx.moveTo(16,-58);ctx.quadraticCurveTo(40,-40,80,-50);ctx.quadraticCurveTo(120,-64,175,-66);ctx.lineTo(175,44);
  ctx.quadraticCurveTo(122,44,104,18);ctx.quadraticCurveTo(84,-14,52,-30);ctx.quadraticCurveTo(28,-38,16,-30);ctx.closePath();ctx.fill();
  ctx.fillStyle=C.white;ctx.beginPath();ctx.moveTo(-15,-96);ctx.quadraticCurveTo(0,-104,15,-96);ctx.lineTo(18,-30);ctx.quadraticCurveTo(0,-20,-18,-30);ctx.closePath();ctx.fill();
  ctx.beginPath();ctx.ellipse(0,-104,24,11,0,0,7);ctx.fill();
  ctx.fillStyle=C.orange;ctx.beginPath();ctx.ellipse(6,101,10,7,0,0,7);ctx.fill()}
function sushi(t){
  const rise=eBack(seg(t,1.6,2.6)),hy=455+(1-rise)*380,br=1+0.012*Math.sin(t*2.4),tilt=0.12*bump(t,4.4,5.6);
  ctx.save();ctx.translate(960,hy);ctx.rotate(tilt);ctx.scale(br,br);
  ctx.fillStyle=C.white;ctx.beginPath();ctx.ellipse(0,195,178,118,0,0,7);ctx.fill();
  ctx.fillStyle=C.fur;for(const [x,y] of[[-40,140],[0,150],[40,140],[-75,128],[75,128]]){ctx.beginPath();ctx.moveTo(x-14,y);ctx.lineTo(x,y+18);ctx.lineTo(x+14,y);ctx.fill()}
  ear(-1,0,C.earLight);ear(1,-0.28*bump(t,3.9,4.12)+0.1*bump(t,4.12,4.32),C.black);
  const hp=headPath();ctx.fillStyle=C.white;ctx.fill(hp);
  ctx.save();ctx.clip(hp);markings();ctx.restore();
  ctx.lineWidth=3;ctx.strokeStyle=C.fur;ctx.stroke(hp);
  let o=t<3.2?0:eOut(seg(t,3.2,3.5));o*=blinkAt(t,6.3,0.09,0.04,0.1);o*=blinkAt(t,7.9,0.32,0.22,0.36);
  const pd=0.78+0.32*eOut(seg(t,3.35,3.9)),w=seg(t,5.0,5.3)*(1-seg(t,6.6,7.0)),lx=w*(-8+16*seg(t,5.2,6.3)),ly=w*7;
  eye(-58,o,pd,lx,ly);eye(58,o,pd,lx,ly);
  ctx.fillStyle='rgba(242,150,150,0.25)';for(const s of[-1,1]){ctx.beginPath();ctx.ellipse(s*86,40,17,11,0,0,7);ctx.fill()}
  ctx.fillStyle=C.nose;ctx.beginPath();ctx.moveTo(-15,30);ctx.quadraticCurveTo(0,25,15,30);ctx.quadraticCurveTo(10,44,0,52);ctx.quadraticCurveTo(-10,44,-15,30);ctx.fill();
  ctx.fillStyle='#211816';ctx.beginPath();ctx.moveTo(-7,41);ctx.quadraticCurveTo(0,39,7,41);ctx.lineTo(0,52);ctx.closePath();ctx.fill();
  ctx.strokeStyle='#6B4B3E';ctx.lineWidth=3;ctx.lineCap='round';ctx.beginPath();ctx.moveTo(0,52);ctx.lineTo(0,58);ctx.moveTo(-19,61);ctx.quadraticCurveTo(-9,68,0,58);ctx.quadraticCurveTo(9,68,19,61);ctx.stroke();
  ctx.strokeStyle='rgba(150,130,120,0.6)';ctx.lineWidth=2;for(const s of[-1,1])for(const d of[-14,0,14]){ctx.beginPath();ctx.moveTo(s*42,54+d*0.4);ctx.quadraticCurveTo(s*100,48+d,s*168,42+d*1.6);ctx.stroke()}
  for(const [x,y,d] of[[-118,-40,0],[122,-30,0.15]]){const s=bump(t,3.45+d,4.25+d);if(s>0)star(x,y,20*s,'#FFFFFF')}
  const hz=seg(t,4.7,6.2);if(hz>0&&hz<1){ctx.save();ctx.globalAlpha*=1-seg(hz,0.6,1);ctx.translate(70+18*Math.sin(hz*6),-205-110*eOut(hz));const k=0.6+0.5*eBack(Math.min(1,hz*3));ctx.scale(k,k);ctx.fillStyle=C.salmon;ctx.beginPath();ctx.moveTo(0,14);ctx.bezierCurveTo(-30,-8,-18,-30,0,-16);ctx.bezierCurveTo(18,-30,30,-8,0,14);ctx.fill();ctx.restore()}
  ctx.restore()}
function tail(t){
  const up=eOut(seg(t,5.4,6.3));if(up<=0)return;
  const sw=Math.sin((t-5.4)*2.3)*48,bx=1262,by=610,tx=1400+sw,ty=640-280*up,cx=1440+sw*0.4,cy=600-60*up;
  for(let i=0;i<=34;i++){const u=i/34,a=(1-u)*(1-u),b=2*(1-u)*u,c=u*u,x=a*bx+b*cx+c*tx,y=a*by+b*cy+c*ty,r=27-6*u;
    ctx.fillStyle=u<0.38?C.black:u<0.78?C.orange:C.black;ctx.beginPath();ctx.arc(x,y,r,0,7);ctx.fill();
    if(i%3==0){ctx.fillStyle=u<0.38||u>=0.78?'#2A2421':'#E8AD7A';ctx.beginPath();ctx.arc(x+r*0.55,y-r*0.2,r*0.42,0,7);ctx.fill()}}}
function tv(t){
  const dot=seg(t,0.2,0.5),line=seg(t,0.5,0.9),on=seg(t,0.9,1.4),ba=seg(t,0,0.6);
  if(on>0){const g=ctx.createRadialGradient(960,430,60,960,430,620);g.addColorStop(0,`rgba(255,196,140,${0.22*on})`);g.addColorStop(1,'rgba(255,196,140,0)');ctx.fillStyle=g;ctx.fillRect(0,0,1920,1080)}
  const ga=ctx.globalAlpha;ctx.globalAlpha=ga*ba;
  tail(t);
  const wig=t>2.5?0.2*Math.sin((t-2.5)*14)*Math.exp(-(t-2.5)*2.6):0;
  ctx.strokeStyle=C.cafe;ctx.lineWidth=8;ctx.lineCap='round';
  for(const [bx,a,l] of[[900,-0.55+wig,130],[1020,0.5-wig,142]]){const ex=bx+Math.sin(a)*l,ey=192-Math.cos(a)*l;ctx.beginPath();ctx.moveTo(bx,192);ctx.lineTo(ex,ey);ctx.stroke();ctx.fillStyle=C.caramel;ctx.beginPath();ctx.arc(ex,ey,12,0,7);ctx.fill()}
  ctx.fillStyle=C.cafe;rr(830,664,60,24,8);ctx.fill();rr(1030,664,60,24,8);ctx.fill();
  rr(620,190,680,480,72);ctx.fillStyle=C.body;ctx.fill();ctx.lineWidth=6;ctx.strokeStyle=C.cafe;ctx.stroke();
  const sx=650,sy=220,sw=620,sh=420;
  if(t<0.9){rr(sx,sy,sw,sh,56);ctx.fillStyle='#0A0807';ctx.fill();
    if(line>0){const w=sw*eOut(line);ctx.fillStyle='#FFF6EA';ctx.fillRect(960-w/2,428,w,4)}
    else if(dot>0){ctx.fillStyle='#FFF6EA';ctx.beginPath();ctx.arc(960,430,7*eOut(dot),0,7);ctx.fill()}}
  else{const h=Math.max(4,sh*eOut(on));ctx.save();rr(sx,430-h/2,sw,h,Math.min(56,h/2));ctx.clip();
    const g=ctx.createRadialGradient(960,400,40,960,430,420);g.addColorStop(0,'#E9CBA7');g.addColorStop(1,'#B98558');ctx.fillStyle=g;ctx.fillRect(sx,sy,sw,sh);
    sushi(t);
    ctx.fillStyle='rgba(0,0,0,0.04)';for(let y=sy;y<sy+sh;y+=6)ctx.fillRect(sx,y,sw,2);
    if(on<1){ctx.fillStyle=`rgba(255,246,234,${0.85*(1-on)})`;ctx.fillRect(sx,sy,sw,sh)}
    ctx.restore();rr(sx,sy,sw,sh,56);ctx.lineWidth=5;ctx.strokeStyle='rgba(0,0,0,0.45)';ctx.stroke()}
  const p=eBack(seg(t,2.35,2.85));if(p>0){for(const dx of[-82,82]){const y=636+(1-p)*30;ctx.fillStyle=C.white;ctx.beginPath();ctx.ellipse(960+dx,y,40,25,0,0,7);ctx.fill();ctx.lineWidth=3;ctx.strokeStyle=C.fur;ctx.stroke();ctx.strokeStyle='#CDBDAE';ctx.lineWidth=2.5;for(const k of[-13,0,13]){ctx.beginPath();ctx.moveTo(960+dx+k,y+8);ctx.lineTo(960+dx+k,y+22);ctx.stroke()}}}
  ctx.globalAlpha=ga}
function title(t){
  const txt='SushiTV';ctx.font='900 128px system-ui, sans-serif';ctx.textBaseline='alphabetic';
  const ws=[...txt].map(c=>ctx.measureText(c).width),tot=ws.reduce((a,b)=>a+b,0);let x=960-tot/2;
  [...txt].forEach((c,i)=>{const a=5.2+i*0.09,p=seg(t,a,a+0.55),cw=ws[i];if(p>0){
    const sc=eBack(p),wv=Math.sin((t-6.6)*7-i*0.7)*10*bump(t,6.6,7.6);
    ctx.save();ctx.translate(x+cw/2,835+(1-eOut(p))*60-wv);ctx.rotate((1-eOut(p))*-0.35);ctx.scale(sc,sc);
    const tvc=i>=5;ctx.fillStyle=tvc?'#2F5A24':'#5C3A21';ctx.fillText(c,-cw/2,8);ctx.fillStyle=tvc?C.green:C.white;ctx.fillText(c,-cw/2,0);ctx.restore()}
    x+=cw});
  const s=seg(t,7.0,7.8);if(s>0){ctx.save();ctx.globalAlpha*=eOut(s);ctx.font='600 44px system-ui, sans-serif';ctx.textAlign='center';ctx.fillStyle=C.cream;ctx.fillText('¡Te damos la bienvenida!',960,912+(1-eOut(s))*20);ctx.restore()}}
function drawScene(c,t,W){ctx=c;
  const s=W/1920;ctx.setTransform(s,0,0,s,0,0);ctx.clearRect(0,0,1920,1080);
  const out=eIO(seg(t,9.3,10));ctx.globalAlpha=1-out;
  const g=ctx.createRadialGradient(960,430,50,960,540,1150);g.addColorStop(0,'#3A271B');g.addColorStop(1,C.ink);ctx.fillStyle=g;ctx.fillRect(0,0,1920,1080);
  ctx.save();const z=1+0.04*out;ctx.translate(960,540);ctx.scale(z,z);ctx.translate(-960,-540);
  const fa=seg(t,1.3,2.4);if(fa>0){const ga=ctx.globalAlpha;ctx.globalAlpha=ga*0.3*fa;F.forEach(f=>floater(f,t));ctx.globalAlpha=ga}
  tv(t);title(t);ctx.restore();
  const h=seg(t,1.5,2.2);if(h>0){ctx.globalAlpha=(1-out)*0.4*h;ctx.font='500 26px system-ui, sans-serif';ctx.textAlign='right';ctx.fillStyle=C.cream;ctx.fillText('Presiona OK para saltar',1880,1040);ctx.textAlign='left'}
  ctx.globalAlpha=1}
