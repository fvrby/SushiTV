"""Genera la lista "Películas y series gratis" con los canales de Pluto TV (servicio gratuito y legal,
con publicidad) disponibles en Chile, y deja solo los que entregan video.

Uso (desde la raíz del repo, conectado desde Chile: la API de Pluto responde según el país de la IP):
    python -I sushitv/tools/gratis/pluto.py app/src/main/assets/peliculas-series-gratis.m3u

Privacidad: la API trae la ubicación aproximada de quien consulta (deviceLat/deviceLon) en sus URLs.
Esas URLs NO se usan: cada señal se arma desde cero con un identificador genérico y fijo, sin datos de
nadie, igual que el reproductor web de Pluto.
"""
import json, re, sys, urllib.parse, urllib.request
from concurrent.futures import ThreadPoolExecutor

OUT = sys.argv[1]
API = 'https://api.pluto.tv/v2/channels'
UA = 'Mozilla/5.0 (Linux; Android 12; TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36'
# Generic identifiers shared by every SushiTV install (random, not tied to any person or device).
DEVICE_ID = '7d4e2a51-3c9b-4f0e-9a61-5b2f8c3d1e07'
SESSION_ID = 'c1f0b6e4-8a2d-4b7c-93e5-0d6a4f2b9c18'
STREAM = ('https://service-stitcher.clusters.pluto.tv/stitch/hls/channel/{id}/master.m3u8?advertisingId=&appName=web'
          '&appVersion=unknown&appStoreUrl=&architecture=&buildVersion=&clientTime=0&deviceDNT=0'
          f'&deviceId={DEVICE_ID}&deviceMake=Chrome&deviceModel=web&deviceType=web&deviceVersion=unknown'
          f'&includeExtendedEvents=false&sid={SESSION_ID}&userId=&serverSideAds=true')

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

def plays(url):
    """Follow master -> variant -> first segment; True when a real media segment comes back."""
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

with ThreadPoolExecutor(16) as ex:
    ok = list(ex.map(lambda r: plays(r['url']), rows))
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
