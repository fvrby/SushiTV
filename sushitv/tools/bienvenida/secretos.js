// SushiTV secret animations (long press on the rail logo), for the browser lab: the same scenes as
// SushiSecrets.kt. Floating: nothing is painted outside Sushi and her things. drawSecret(ctx, index, t).
// Stage: 1920x1080; the caller scales. SECRETS[i].d = length in seconds.
(function () {
const cl=x=>Math.max(0,Math.min(1,x)),seg=(t,a,b)=>cl((t-a)/(b-a)),lerp=(a,b,u)=>a+(b-a)*u;
const eOut=x=>1-Math.pow(1-x,3),eIO=x=>x<.5?4*x*x*x:1-Math.pow(-2*x+2,3)/2;
const eBack=x=>{const c1=1.70158,c3=c1+1,u=x-1;return 1+c3*u*u*u+c1*u*u};
const bump=(t,a,b)=>Math.sin(seg(t,a,b)*Math.PI);
const C={white:'#FFFDF8',fur:'#E6D8C8',black:'#1A1614',orange:'#D9955E',caramel:'#C98B5B',pink:'#F2B3AE',nose:'#EBA9A6',eye1:'#DDF3C4',eye2:'#93C47A',eyeLine:'#3F5C34',salmon:'#FF7A59',earLight:'#EEE6DD',cream:'#F3E6D6'};
let ctx;
function E(x,y,rx,ry,c,rot){ctx.fillStyle=c;ctx.beginPath();ctx.ellipse(x,y,rx,ry,rot||0,0,7);ctx.fill()}
function O(x,y,r,c){ctx.fillStyle=c;ctx.beginPath();ctx.arc(x,y,r,0,7);ctx.fill()}
function star(x,y,r,col){ctx.fillStyle=col;ctx.beginPath();for(let i=0;i<8;i++){const a=i*Math.PI/4,q=i%2?r*.28:r;ctx.lineTo(x+Math.sin(a)*q,y-Math.cos(a)*q)}ctx.fill()}
function heart(x,y,k,a){ctx.save();ctx.globalAlpha*=a;ctx.translate(x,y);ctx.scale(k,k);ctx.fillStyle=C.salmon;ctx.beginPath();ctx.moveTo(0,14);ctx.bezierCurveTo(-30,-8,-18,-30,0,-16);ctx.bezierCurveTo(18,-30,30,-8,0,14);ctx.fill();ctx.restore()}
const HP=(()=>{const p=new Path2D();p.moveTo(0,-110);p.bezierCurveTo(95,-110,150,-60,150,15);p.bezierCurveTo(150,85,85,122,0,122);p.bezierCurveTo(-85,122,-150,85,-150,15);p.bezierCurveTo(-150,-60,-95,-110,0,-110);p.closePath();return p})();
function ear(sign,ang,outer){ctx.save();ctx.translate(sign*92,-72);ctx.rotate(sign*ang);ctx.translate(-sign*92,72);
  const o=new Path2D();o.moveTo(sign*150,-20);o.quadraticCurveTo(sign*156,-150,sign*120,-182);o.quadraticCurveTo(sign*84,-150,sign*36,-104);o.closePath();ctx.fillStyle=outer;ctx.fill(o);
  if(outer!==C.black){ctx.lineWidth=3;ctx.strokeStyle='#CFC4B8';ctx.stroke(o)}
  const i=new Path2D();i.moveTo(sign*128,-45);i.quadraticCurveTo(sign*132,-130,sign*116,-155);i.quadraticCurveTo(sign*92,-128,sign*62,-98);i.closePath();ctx.fillStyle=C.pink;ctx.fill(i);ctx.restore()}
function markings(){ctx.fillStyle=C.black;ctx.beginPath();ctx.moveTo(-175,-135);ctx.lineTo(175,-135);ctx.lineTo(175,-66);ctx.quadraticCurveTo(120,-64,80,-50);ctx.quadraticCurveTo(40,-40,16,-58);ctx.lineTo(-16,-58);ctx.quadraticCurveTo(-46,-46,-74,-44);ctx.quadraticCurveTo(-100,-38,-104,-8);ctx.quadraticCurveTo(-108,30,-98,62);ctx.quadraticCurveTo(-135,74,-175,66);ctx.closePath();ctx.fill();
  ctx.fillStyle=C.orange;ctx.beginPath();ctx.moveTo(16,-58);ctx.quadraticCurveTo(40,-40,80,-50);ctx.quadraticCurveTo(120,-64,175,-66);ctx.lineTo(175,44);ctx.quadraticCurveTo(122,44,104,18);ctx.quadraticCurveTo(84,-14,52,-30);ctx.quadraticCurveTo(28,-38,16,-30);ctx.closePath();ctx.fill();
  ctx.fillStyle=C.white;ctx.beginPath();ctx.moveTo(-15,-96);ctx.quadraticCurveTo(0,-104,15,-96);ctx.lineTo(18,-30);ctx.quadraticCurveTo(0,-20,-18,-30);ctx.closePath();ctx.fill();E(0,-104,24,11,C.white);E(6,101,10,7,C.orange)}
function eye(x,o,pd,lx,ly){ctx.save();ctx.translate(x,-5);
  if(o<0.12){ctx.strokeStyle='#2B2320';ctx.lineWidth=6;ctx.lineCap='round';ctx.beginPath();ctx.moveTo(-26,-2);ctx.quadraticCurveTo(0,12,26,-2);ctx.stroke()}
  else{ctx.scale(1,o);const g=ctx.createRadialGradient(-6,-8,4,0,0,34);g.addColorStop(0,C.eye1);g.addColorStop(1,C.eye2);ctx.fillStyle=g;ctx.beginPath();ctx.ellipse(0,0,30,32,0,0,7);ctx.fill();ctx.lineWidth=3.5;ctx.strokeStyle=C.eyeLine;ctx.stroke();
    E(lx,ly,15*pd,18*pd,'#15110F');O(lx-9,ly-11,7,'#fff');O(lx+8,ly+9,3.5,'#fff')}
  ctx.restore()}
function face(f){
  ear(-1,0,C.earLight);ear(1,f.flick||0,C.black);ctx.fillStyle=C.white;ctx.fill(HP);ctx.save();ctx.clip(HP);markings();ctx.restore();ctx.lineWidth=3;ctx.strokeStyle=C.fur;ctx.stroke(HP);
  const lx=Math.max(-10,Math.min(10,f.lx||0)),ly=Math.max(-10,Math.min(10,f.ly||0));
  eye(-58,f.open,f.pd||1,lx,ly);eye(58,f.open,f.pd||1,lx,ly);
  ctx.fillStyle=`rgba(242,150,150,${0.25+0.45*(f.blush||0)})`;for(const s of[-1,1]){ctx.beginPath();ctx.ellipse(s*86,40,17+6*(f.puff||0),11+4*(f.puff||0),0,0,7);ctx.fill()}
  ctx.fillStyle=C.nose;ctx.beginPath();ctx.moveTo(-15,30);ctx.quadraticCurveTo(0,25,15,30);ctx.quadraticCurveTo(10,44,0,52);ctx.quadraticCurveTo(-10,44,-15,30);ctx.fill();
  ctx.fillStyle='#211816';ctx.beginPath();ctx.moveTo(-7,41);ctx.quadraticCurveTo(0,39,7,41);ctx.lineTo(0,52);ctx.closePath();ctx.fill();
  const m=f.mouth||0;
  if(m>0.05){E(0,72,17*m+3,22*m+3,'#6E2E2A');E(0,78+6*m,11*m+2,8*m+2,'#F29AA0')}
  ctx.strokeStyle='#6B4B3E';ctx.lineWidth=3;ctx.lineCap='round';ctx.beginPath();ctx.moveTo(0,52);ctx.lineTo(0,58);
  if(f.smile){ctx.moveTo(-24,58);ctx.quadraticCurveTo(0,76,24,58)}else{ctx.moveTo(-19,61);ctx.quadraticCurveTo(-9,68,0,58);ctx.quadraticCurveTo(9,68,19,61)}ctx.stroke();
  if(f.tongue){E(0,70,9,12*f.tongue,'#F29AA0')}
  ctx.strokeStyle='rgba(150,130,120,0.6)';ctx.lineWidth=2;for(const s of[-1,1])for(const d of[-14,0,14]){ctx.beginPath();ctx.moveTo(s*42,54+d*0.4);ctx.quadraticCurveTo(s*100,48+d,s*168,42+d*1.6+(f.twitch||0)*s);ctx.stroke()}}
function tailChain(p0,p1,p2,r0,r1){for(let i=0;i<=26;i++){const u=i/26,a=(1-u)*(1-u),b=2*(1-u)*u,c=u*u,x=a*p0[0]+b*p1[0]+c*p2[0],y=a*p0[1]+b*p1[1]+c*p2[1],r=lerp(r0,r1,u);O(x,y,r,u<0.38?C.black:u<0.78?C.orange:C.black);if(i%3==0)O(x+r*.5,y-r*.2,r*.42,u<0.38||u>=0.78?'#2A2421':'#E8AD7A')}}
function bodyPatches(cx,cy,rx,ry,rot){ctx.save();ctx.beginPath();ctx.ellipse(cx,cy,rx,ry,rot,0,7);ctx.clip();E(cx+rx*.5,cy-ry*.55,rx*.6,ry*.45,C.black,rot);E(cx-rx*.45,cy+ry*.2,rx*.28,ry*.22,C.orange,rot);ctx.restore()}
function paw(x,y,k){E(x,y,26*k,15*k,C.white);ctx.lineWidth=2.5;ctx.strokeStyle=C.fur;ctx.beginPath();ctx.ellipse(x,y,26*k,15*k,0,0,7);ctx.stroke()}
function shadow(x,w){ctx.fillStyle='rgba(0,0,0,0.25)';ctx.beginPath();ctx.ellipse(x,0,w,16,0,0,7);ctx.fill()}
function cat(x,y,s,o){ctx.save();ctx.translate(x,y);ctx.scale(s*(o.flip?-1:1),s*(o.squash||1));const t=o.t||0,sw=o.tailSw||0;
  let hx=0,hy=-215,hs=.62,ht=o.tilt||0;
  if(o.pose==='sit'){shadow(0,105);tailChain([55,-25],[175,-30+sw*10],[150+sw*40,-150],20,14);E(0,-95,88,100,C.white);bodyPatches(0,-95,88,100,0);paw(-32,-12,1);paw(32,-12,1);if(o.lift)paw(-30+o.lift*20,-12-o.lift*170,1)}
  else if(o.pose==='crouch'){shadow(10,135);const w=o.wig||0;tailChain([120,-80+w],[190,-120],[205+sw*25,-200],18,13);E(25+w*.3,-60+w,118,52,C.white,-0.12);bodyPatches(25+w*.3,-60+w,118,52,-0.12);paw(-115,-10,1);paw(-75,-8,1);hx=-105;hy=-100;hs=.58}
  else if(o.pose==='jump'){ctx.rotate(-0.22);tailChain([125,-70],[220,-95],[290,-80],18,12);E(0,-70,135,46,C.white);bodyPatches(0,-70,135,46,0);paw(-175,-55,1);paw(-155,-35,1);paw(120,-35,1);paw(140,-52,1);hx=-140;hy=-95;hs=.58}
  else if(o.pose==='loaf'){shadow(0,175);const b=1+0.03*Math.sin(t*2.2);E(10,-62*b,160,68*b,C.white);bodyPatches(10,-62*b,160,68*b,0);tailChain([155,-25],[130,22],[-50,-4],17,13);hx=-112;hy=-95;hs=.58}
  else if(o.pose==='stretch'){shadow(0,160);tailChain([140,-120],[200,-190],[175+sw*30,-260],17,12);E(30,-85,145,46,C.white,0.32);bodyPatches(30,-85,145,46,0.32);paw(-170,-10,1);paw(-125,-8,1);paw(130,-12,1);hx=-125;hy=-62;hs=.56}
  ctx.translate(hx,hy);ctx.rotate(ht);ctx.scale(hs,hs);face(o);ctx.restore()}
function zz(x,y,k,a){ctx.save();ctx.globalAlpha*=a;ctx.translate(x,y);ctx.scale(k,k);ctx.strokeStyle='#E8EEF6';ctx.lineWidth=7;ctx.lineJoin='round';ctx.lineCap='round';ctx.beginPath();ctx.moveTo(-14,-14);ctx.lineTo(14,-14);ctx.lineTo(-14,14);ctx.lineTo(14,14);ctx.stroke();ctx.restore()}
function nigiri(x,y,s,rot){ctx.save();ctx.translate(x,y);ctx.rotate(rot||0);ctx.fillStyle=C.white;ctx.beginPath();ctx.roundRect(-s*.8,-s*.25,s*1.6,s*.75,s*.35);ctx.fill();ctx.fillStyle='#E8824B';ctx.beginPath();ctx.roundRect(-s*.9,-s*.55,s*1.8,s*.5,s*.25);ctx.fill();ctx.fillStyle='#F7B48A';ctx.fillRect(-s*.4,-s*.5,s*.12,s*.4);ctx.fillRect(s*.15,-s*.5,s*.12,s*.4);ctx.restore()}
function maki(x,y,s){O(x,y,s*.6,'#151515');O(x,y,s*.45,C.white);O(x,y,s*.2,'#9BD27A')}
function fish(x,y,s,dir){ctx.save();ctx.translate(x,y);ctx.scale(dir||1,1);E(0,0,s*.55,s*.3,'#9BD27A');ctx.beginPath();ctx.moveTo(s*.45,0);ctx.lineTo(s*.85,-s*.3);ctx.lineTo(s*.85,s*.3);ctx.fill();O(-s*.28,-s*.05,s*.06,'#1A1614');ctx.restore()}
function food(k,x,y,s,rot){if(k==0)nigiri(x,y,s,rot);else if(k==1)maki(x,y,s);else fish(x,y,s,1)}
const SHREDS=[...Array(16)].map((_,i)=>({vx:(Math.sin(i*12.9)*0.5)*520,vy:-420-Math.abs(Math.sin(i*7.3))*380,r:Math.sin(i*3.1)*6,len:26+(i%4)*8}));
function animBox(t){const X=960,G=930;const inA=eBack(seg(t,0,0.7)),bx=lerp(1500,X,inA),wob=bump(t,2.7,3.6)*Math.sin(t*30)*0.06;
  ctx.save();ctx.translate(bx,G);ctx.rotate(wob);
  ctx.fillStyle='rgba(0,0,0,0.25)';ctx.beginPath();ctx.ellipse(0,4,230,22,0,0,7);ctx.fill();
  ctx.fillStyle='#5E4126';ctx.beginPath();ctx.moveTo(-180,-230);ctx.lineTo(180,-230);ctx.lineTo(210,-250);ctx.lineTo(-150,-250);ctx.closePath();ctx.fill();
  const peek=bump(t,0.9,2.6),eyesL=seg(t,1.4,1.7)-seg(t,1.9,2.2),jump=seg(t,3.6,4.7),landed=t>4.7;
  const headY=lerp(-110,-262,eOut(Math.min(1,peek*1.6)));
  if(t>=3.6&&!landed){const yo=-420*4*jump*(1-jump);cat(0,-40+yo,0.95,{pose:'sit',open:1,pd:1.2,mouth:0.6*bump(jump,0,0.5),t})}
  else if(landed){const s=seg(t,4.7,5.1),sl=1-seg(t,5.3,5.9);ctx.save();ctx.translate(0,lerp(-200,-268,eBack(s)));ctx.scale(.62,.62);face({open:Math.max(0,sl),pd:1,smile:t>5.4,ly:3});ctx.restore()}
  else if(t>0.8){ctx.save();ctx.translate(0,headY);ctx.scale(.62,.62);face({open:1,pd:1.1,lx:-10*eyesL+10*(seg(t,1.9,2.2)-seg(t,2.3,2.5)),ly:-2});ctx.restore()}
  ctx.fillStyle='#C9A06A';ctx.fillRect(-180,-230,360,230);ctx.fillStyle='#A97E4B';ctx.beginPath();ctx.moveTo(180,-230);ctx.lineTo(210,-250);ctx.lineTo(210,-20);ctx.lineTo(180,0);ctx.closePath();ctx.fill();
  ctx.fillStyle='#B88E58';ctx.fillRect(-180,-230,360,14);ctx.strokeStyle='#8E6A3C';ctx.lineWidth=3;ctx.beginPath();ctx.moveTo(-60,-150);ctx.lineTo(60,-150);ctx.moveTo(-40,-130);ctx.lineTo(40,-130);ctx.stroke();
  ctx.fillStyle='#D8B27A';ctx.beginPath();ctx.moveTo(-180,-230);ctx.lineTo(-250,-300+8*Math.sin(t*3));ctx.lineTo(-90,-300);ctx.lineTo(-40,-230);ctx.fill();ctx.beginPath();ctx.moveTo(180,-230);ctx.lineTo(260,-290);ctx.lineTo(120,-305+8*Math.sin(t*3+1));ctx.lineTo(60,-230);ctx.fill();
  if(landed){paw(-48,-228,1);paw(48,-228,1);const sw=Math.sin((t-4.7)*2.4);tailChain([170,-215],[250,-210],[235+sw*25,-90],18,13)}
  ctx.strokeStyle='#B38A55';ctx.lineWidth=4;ctx.lineCap='round';
  for(let i=0;i<7;i++){const x=-150+i*48;ctx.beginPath();ctx.moveTo(x,-232);ctx.quadraticCurveTo(x+12,-246-6*Math.sin(t*4+i),x+26,-232);ctx.stroke()}
  if(t>3.6&&t<5.4){const u=t-3.6;SHREDS.forEach(s=>{const x=s.vx*u,y=-240+s.vy*u+900*u*u;ctx.save();ctx.translate(x,y);ctx.rotate(s.r+u*6);ctx.beginPath();ctx.moveTo(-s.len/2,0);ctx.quadraticCurveTo(0,-10,s.len/2,0);ctx.stroke();ctx.restore()})}
  ctx.restore();
  if(t>5.6){for(let i=0;i<3;i++){const p=seg(t,5.6+i*0.45,6.9+i*0.45);if(p>0&&p<1)zz(X+90+40*p+i*18,G-330-120*p-i*20,0.6+0.5*p,Math.sin(p*Math.PI))}}}
function dotAt(t){if(t<3.5)return[960+650*Math.sin(1.3*t+0.5),540+250*Math.sin(2.1*t)];if(t<3.9){const u=eIO(seg(t,3.5,3.9)),a=[960+650*Math.sin(1.3*3.5+0.5),540+250*Math.sin(2.1*3.5)];return[lerp(a[0],1250,u),lerp(a[1],920,u)]}if(t<4.95)return[1250,920];const u=seg(t,4.95,5.25);return[lerp(1250,2050,eIO(u)),920]}
function animLaser(t){const G=930;const d=dotAt(t),da=seg(t,0.2,0.5)*(1-seg(t,5.1,5.25));
  let x=420,pose='sit',sq=1,lx=0,ly=0,tilt=0,blush=0,lift=0,tongue=0,wig=0,pd=1,flip=false;
  const dx=d[0]-x,dy=d[1]-(G-230),len=Math.hypot(dx,dy)||1;
  if(t<4.95){lx=dx/len*10;ly=dy/len*10;tilt=Math.max(-.25,Math.min(.25,dx/2500));pd=1+0.45*seg(t,0.6,1.5)}
  if(t>=3.9&&t<4.4){pose='crouch';wig=Math.sin(t*45)*5;pd=1.5;flip=true;lx=-10;ly=4;tilt=0}
  if(t>=4.4&&t<4.95){const u=seg(t,4.4,4.95);x=lerp(420,1250,u);pose='jump';flip=true;lx=-10;tilt=0}
  let yOff=0;if(t>=4.4&&t<4.95){const u=seg(t,4.4,4.95);yOff=-300*4*u*(1-u)}
  if(t>=4.95){x=1250;const u=seg(t,4.95,5.3);sq=lerp(0.8,1,eBack(u));lx=0;ly=0;blush=seg(t,5.3,5.6);lift=bump(t,5.8,6.7);tongue=lift>0.5?1:0}
  if(t>=4.95&&t<5.6){for(let i=0;i<5;i++){const p=seg(t,4.95,5.6);O(1250+(i-2)*45*(0.4+p),G-10-15*p,10+20*p,`rgba(230,220,210,${0.45*(1-p)})`)}}
  cat(x,G+yOff,1,{pose,flip,open:t>6.9?0.6:1,pd,lx,ly,tilt,squash:sq,blush,lift,tongue,wig,t,tailSw:Math.sin(t*(pose==='crouch'?18:3))});
  if(t>5.35&&t<6.1){const p=seg(t,5.35,6.1);ctx.save();ctx.globalAlpha*=1-p;ctx.fillStyle='#8FC8F2';ctx.beginPath();const sx=x+72,sy=G-290+40*p;ctx.moveTo(sx,sy-16);ctx.quadraticCurveTo(sx+12,sy,sx,sy+8);ctx.quadraticCurveTo(sx-12,sy,sx,sy-16);ctx.fill();ctx.restore()}
  if(da>0){const g=ctx.createRadialGradient(d[0],d[1],0,d[0],d[1],34);g.addColorStop(0,`rgba(255,40,40,${da})`);g.addColorStop(0.25,`rgba(255,40,40,${0.8*da})`);g.addColorStop(1,'rgba(255,40,40,0)');ctx.fillStyle=g;ctx.beginPath();ctx.arc(d[0],d[1],34,0,7);ctx.fill();O(d[0],d[1],6,`rgba(255,230,230,${da})`)}}
function cloud(x,y,w,h,a){ctx.save();ctx.globalAlpha*=a;ctx.fillStyle='rgba(255,255,255,0.95)';for(const [ox,oy,r] of[[-.32,.05,.3],[-.1,-.18,.34],[.18,-.15,.32],[.36,.06,.28],[.05,.16,.36]]){ctx.beginPath();ctx.arc(x+ox*w,y+oy*h*1.4,r*h,0,7);ctx.fill()}ctx.restore()}
function animNap(t){const X=960,G=930;
  const wake=t>5.3,stretch=t>=5.8&&t<6.7,sit=t>=6.7;
  if(!wake){cat(X,G,1.05,{pose:'loaf',open:0,smile:t>1.8&&t<5.2,twitch:Math.sin(t*9)*3*bump(t,2,5),t,tilt:-0.12})}
  else if(!stretch&&!sit){cat(X,G,1.05,{pose:'loaf',open:eOut(seg(t,5.35,5.7)),t,tilt:-0.12,pd:0.8})}
  else if(stretch){cat(X,G,1.05,{pose:'stretch',open:0.35,mouth:bump(t,5.9,6.6),t,tailSw:Math.sin(t*3)})}
  else{const p=seg(t,6.7,7.0);cat(X,G,1.05,{pose:'sit',open:1,pd:1.1,squash:lerp(0.9,1,eBack(p)),t,tailSw:Math.sin(t*2.5),blush:0.4});heart(X+70,G-420-80*seg(t,7.0,7.8),0.9+0.4*seg(t,7.0,7.3),1-seg(t,7.5,7.9))}
  if(t<5.2){for(let i=0;i<4;i++){const p=((t*0.55+i*0.25)%1);zz(X-150+45*p+i*6,G-180-150*p,0.45+0.5*p,Math.sin(p*Math.PI)*seg(t,0.3,0.8))}}
  const ba=seg(t,1.2,1.7)*(1-seg(t,5.0,5.25)),pop=1+0.2*seg(t,5.0,5.25);
  if(ba>0){O(X-20,G-260,10*ba,'rgba(255,255,255,0.9)');O(X+20,G-320,16*ba,'rgba(255,255,255,0.9)');
    const cx=X+200,cy=G-500,w=460*pop,h=260*pop;cloud(cx,cy,w,h,ba);
    ctx.save();ctx.globalAlpha*=ba;ctx.beginPath();ctx.ellipse(cx,cy,w*0.42,h*0.42,0,0,7);ctx.clip();
    const u=t*0.45;for(let i=0;i<3;i++){const px=cx-260+((u*400+i*170)%520),py=cy-20+Math.sin(t*3+i)*25;food(i,px,py,26,Math.sin(t*2+i)*.3)}
    const hx=cx-260+((u*400-110+520)%520);ctx.save();ctx.translate(hx,cy+10+Math.sin(t*6)*8);ctx.scale(.26,.26);face({open:1,pd:1.3,lx:10,mouth:0.5+0.3*Math.sin(t*8)});ctx.restore();
    ctx.restore()}}
const DROPS=[[0.3,700,0],[1.25,1180,1],[2.15,860,2],[3.05,1300,0],[3.95,620,1],[4.9,1000,0]],FALL=0.95;
function animRain(t){const G=930,CATCH=G-330;
  let x=960,yOff=0,mouth=0,caught=0,puff=0;
  for(let i=0;i<DROPS.length;i++){const [t0,dx]=DROPS[i];const prev=i?DROPS[i-1][1]:960;if(t>=t0-0.25&&t<t0+FALL){x=lerp(prev,dx,eIO(seg(t,t0-0.25,t0+0.45)))}else if(t>=t0+FALL)x=dx;if(t>=t0+FALL)caught=i+1}
  for(let i=0;i<DROPS.length;i++){const [t0]=DROPS[i];const j=seg(t,t0+FALL-0.3,t0+FALL+0.25);if(j>0&&j<1){yOff=-170*Math.sin(j*Math.PI);mouth=Math.max(mouth,bump(j,0,0.6))}}
  if(t>5.9&&t<6.9){puff=0.5+0.5*Math.sin(t*16);mouth=0}
  cat(x,G+yOff,0.95,{pose:'sit',open:1,pd:1.2,ly:t<5.9?-10:0,mouth,puff:puff*0.6,blush:t>5.9?0.7:0.1,t,tailSw:Math.sin(t*3)});
  for(let i=0;i<DROPS.length;i++){const [t0,dx,k]=DROPS[i];const p=seg(t,t0,t0+FALL);if(p>0&&p<1){food(k,dx,lerp(-60,CATCH,p*p),38,t*2+i)}
    const q=seg(t,t0+FALL,t0+FALL+0.8);if(q>0&&q<1){ctx.save();ctx.globalAlpha*=1-q;ctx.font='800 44px system-ui';ctx.fillStyle='#FFE08A';ctx.textAlign='center';ctx.fillText('+1',dx+90,CATCH-20-60*q);ctx.restore();star(dx-80,CATCH+10-30*q,18*(1-q),'#FFFFFF')}}
  const ca=seg(t,0.2,0.6)*(1-seg(t,7.2,7.8));if(ca>0){ctx.save();ctx.globalAlpha*=ca;ctx.fillStyle='rgba(20,16,14,0.78)';ctx.beginPath();ctx.roundRect(1430,150,250,92,46);ctx.fill();nigiri(1490,205,34,0);
    const pulse=caught>0?1+0.25*bump(t,DROPS[caught-1][0]+FALL,DROPS[caught-1][0]+FALL+0.3):1;
    ctx.save();ctx.translate(1585,214);ctx.scale(pulse,pulse);ctx.font='800 48px system-ui';ctx.fillStyle='#FFF6EA';ctx.textAlign='center';ctx.fillText('× '+caught,0,0);ctx.restore();ctx.restore()}
  if(t>6.0)heart(x+80,G-430-90*seg(t,6.0,7.0),0.9+0.3*seg(t,6.0,6.3),1-seg(t,6.7,7.2))}
window.SECRETS=[{name:'La caja',f:animBox,d:7.2},{name:'Puntero láser',f:animLaser,d:7.2},{name:'La siesta',f:animNap,d:8.0},{name:'Lluvia de sushi',f:animRain,d:7.8}];
window.drawSecret=(c,i,t)=>{ctx=c;const A=SECRETS[i];const a=seg(t,0,0.25)*(1-seg(t,A.d-0.5,A.d));const g=ctx.globalAlpha;ctx.globalAlpha=g*a;A.f(t);ctx.globalAlpha=g};
window.RAIN={DROPS,FALL};
})();
