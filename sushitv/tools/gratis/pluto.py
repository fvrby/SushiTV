"""Genera la PLANTILLA de "Películas y series" con los canales de Pluto TV (servicio gratuito y legal,
con publicidad) disponibles en Chile: nombre, sección, logo e id de cada canal que hoy entrega programación.

Uso (desde la raíz del repo, conectado desde Chile: la API de Pluto responde según el país de la IP):
    python -I sushitv/tools/gratis/pluto.py app/src/main/assets/peliculas-series-gratis.m3u

Desde nov. 2026 Pluto solo sirve programación con una sesión que vence a las 24 h (boot.pluto.tv); las URLs
fijas antiguas reciben un video de "Pluto TV is no longer available on this device". Por eso la plantilla NO
trae URLs reproducibles: la app (features/setup/SushiPluto.kt) pide la sesión en la tele, arma la lista ahí y
la renueva sola. Así la sesión, que incluye la ubicación aproximada de quien la pide, nunca llega a GitHub.
"""
import json, re, sys, uuid, urllib.parse, urllib.request

OUT = sys.argv[1]
API = 'https://api.pluto.tv/v2/channels'
BOOT = 'https://boot.pluto.tv/v4/start'
UA = 'Mozilla/5.0 (Linux; Android 12; TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36'
# Placeholder written to the template: the app replaces it with a session URL built from the channel id.
STREAM = 'https://cfd-v4-service-channel-stitcher-use1-1.prd.pluto.tv/v2/stitch/hls/channel/{id}/master.m3u8'

# Pluto's category -> our group, in display order.
GROUPS = [
    ('Películas', ['películas', 'peliculas']),
    ('Series', ['series', 'south park']),
    ('Novelas', ['novelas']),
    ('Comedia', ['comedia']),
    ('Infantil', ['kids']),
    ('Juvenil', ['teen']),
    ('Anime y gaming', ['anim']),
    ('Documentales y curiosidad', ['curiosidad']),
    ('Crimen e investigación', ['investigación', 'investigacion']),
    ('Reality', ['reality']),
    ('Competencias', ['competencia']),
    ('Clásicos y retro', ['retro']),
    ('Estilo de vida', ['estilo de vida']),
    ('Música', ['música', 'musica']),
    ('Deportes', ['deportes']),
    ('Noticias', ['noticias']),
    ('Entretenimiento', ['entretenimiento']),
]

def group_of(category):
    c = (category or '').lower()
    for name, keys in GROUPS:
        if any(c.startswith(k) for k in keys):
            return name
    return 'Entretenimiento'

def get(url, limit=None):
    with urllib.request.urlopen(urllib.request.Request(url, headers={'User-Agent': UA}), timeout=15) as r:
        return r.geturl(), (r.read(limit) if limit else r.read())

def boot_session():
    """A fresh anonymous Pluto session (like the web player): stitcher base, params and JWT."""
    q = dict(appName='web', appVersion='9.10.0', deviceVersion='12', deviceModel='web', deviceMake='chrome',
             deviceType='web', clientID=str(uuid.uuid4()), clientModelNumber='1.0.0', serverSideAds='false',
             drmCapabilities='', blockingMode='')
    b = json.loads(get(BOOT + '?' + urllib.parse.urlencode(q))[1])
    return b['servers']['stitcher'], b['stitcherParams'], b['sessionToken']

def plays(channel_id, session):
    """Master -> variant -> first segment with a real session: True for real programming, False for the
    takedown slate ("no longer available"), errors or empty segments."""
    base, params, jwt = session
    url = f'{base}/v2/stitch/hls/channel/{channel_id}/master.m3u8?{params}&jwt={jwt}&masterJWTPassthrough=true'
    try:
        for _ in range(4):
            final, body = get(url, 300_000)
            text = body.decode('utf-8', 'ignore')
            lines = [l.strip() for l in text.splitlines() if l.strip() and not l.startswith('#')]
            if not lines:
                return False
            nxt = urllib.parse.urljoin(final, lines[0])
            if '.m3u8' in lines[0].split('?')[0]:
                url = nxt
                continue
            if 'takedown' in nxt:
                return False
            return len(get(nxt, 200_000)[1]) > 4096
    except Exception:
        return False
    return False

_, raw = get(API)
channels = [c for c in json.loads(raw) if c.get('isStitched') and c.get('_id')]
rows = []
for c in channels:
    logo = (c.get('colorLogoPNG') or {}).get('path') or (c.get('logo') or {}).get('path') or ''
    rows.append({
        'name': re.sub(r'\s{2,}', ' ', c['name']).strip(),
        'group': group_of(c.get('category')),
        'number': int(c.get('number') or 0),
        'logo': logo.split('?')[0],
        'id': c['_id'],
        'url': STREAM.format(id=c['_id']),
    })

# One channel at a time: a Pluto session hands out empty playlists to parallel requests.
session = boot_session()
ok = [plays(r['id'], session) for r in rows]
working = [r for r, good in zip(rows, ok) if good]
# Pluto lists a few channels twice ("Pluto Tv Cine Acción" / "Pluto TV Cine Acción"): keep one per
# name, preferring the one with a real channel number.
best = {}
for r in working:
    k = r['name'].lower()
    cur = best.get(k)
    if cur is None or (cur['number'] == 0 and r['number'] > 0) or (0 < r['number'] < cur['number']):
        best[k] = r
working = list(best.values())
order = [g for g, _ in GROUPS]
working.sort(key=lambda r: (order.index(r['group']), r['number'], r['name']))

lines = ['#EXTM3U']
for r in working:
    lines.append(f'#EXTINF:-1 tvg-id="pluto-{r["id"]}" tvg-chno="{r["number"]}" tvg-logo="{r["logo"]}" '
                 f'group-title="{r["group"]}",{r["name"]}')
    lines.append(r['url'])
open(OUT, 'w', encoding='utf-8', newline='\n').write('\n'.join(lines) + '\n')

by_group = {}
for r in working:
    by_group.setdefault(r['group'], []).append(r['name'])
for g in order:
    if g in by_group:
        print(f'[{g}] ({len(by_group[g])}): ' + ' | '.join(by_group[g]))
print(f'pluto channels: {len(rows)}, working: {len(working)}')
