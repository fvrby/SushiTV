"""Build the curated SushiTV playlist from results.json (verified streams).

Usage: curate.py <dir with results.json> <output .m3u> [--report]
"""
import collections, json, os, re, sys, unicodedata

d, out_path = sys.argv[1], sys.argv[2]
REPORT = '--report' in sys.argv
cands = json.load(open(os.path.join(d, 'results.json'), encoding='utf-8'))

GROUPS = ['Nacionales', 'Noticias', 'Deportes', 'Cultura e infantil', 'Geek y anime', 'Entretención', 'Música',
          'Radios con video', 'Universitarios', 'Regionales', 'Institucionales', 'Religiosos', 'Paisajes en vivo']

def ascii_low(s):
    return unicodedata.normalize('NFKD', s).encode('ascii', 'ignore').decode().lower()

def clean_name(s):
    s = re.sub(r'\s*\((?:\d+[pi]|Chile)\)|\s*\[[^\]]*\]', '', s).strip()
    return re.sub(r'\s{2,}', ' ', s)

# Pay-TV restreams, non-Chilean channels and test signals: never include.
EXCLUDE = re.compile(r'tcm latin|nick jr|china channel|pruebas|internacional')

# (regex on the ascii-lowercased name, display name, group). First match wins; order inside a
# group follows this list. Several regexes may point at one display name: they are merged.
RULES = [
    (r'^mega$', 'Mega', 'Nacionales'),
    (r'^chilevision$', 'Chilevisión', 'Nacionales'),
    (r'^canal 13$', 'Canal 13', 'Nacionales'),
    (r'^tv\s*\+|^tvmas$|^tv plus', 'TV+', 'Nacionales'),
    (r'^la red$', 'La Red', 'Nacionales'),
    (r'^24 horas$', '24 Horas', 'Noticias'),
    (r'^t13', 'T13 en vivo', 'Noticias'),
    (r'^(chv|chilevision) noticias', 'CHV Noticias', 'Noticias'),
    (r'^meganoticias$', 'Meganoticias', 'Noticias'),
    (r'^24 play', '24 Play', 'Noticias'),
    (r'^bio ?bio tv$', 'Bío-Bío TV', 'Noticias'),
    (r'^adn tv', 'ADN TV', 'Noticias'),
    (r'^puranoticia', 'Puranoticia TV', 'Noticias'),
    (r'pinguino', 'El Pingüino TV', 'Noticias'),
    (r'^(chv|chilevision) deportes', 'CHV Deportes', 'Deportes'),
    (r'^turf', 'Turf Móvil', 'Deportes'),
    (r'^ntv$', 'NTV', 'Cultura e infantil'),
    (r'^tvn ?3$', 'TVN 3', 'Cultura e infantil'),
    (r'^tv chile$', 'TV Chile', 'Cultura e infantil'),
    (r'^13 ?c(ultura)?$', '13 Cultura', 'Cultura e infantil'),
    (r'^13 kids', '13 Kids', 'Cultura e infantil'),
    # EnerGeek (Santiago): links as EnerGeek publishes them in the TDTChannels list (tdtchannels.com/lists/tv.json).
    # EnerGeek's own catalogue (neotv.energeek.cl/assets/iptv.json) calls the egretro feed just "EnerGeek".
    (r'^energeek( retro)?$', 'EnerGeek', 'Geek y anime'),
    (r'^energeek fan', 'EnerGeek Fan', 'Geek y anime'),
    (r'^energeek radio', 'EnerGeek Radio', 'Geek y anime'),
    (r'^13 realities', '13 Realities', 'Entretención'),
    (r'^13 ?t(eleseries)?$', '13 Teleseries', 'Entretención'),
    (r'^13 humor', '13 Humor', 'Entretención'),
    (r'^13 festival', '13 Festival', 'Entretención'),
    (r'^(uchile|u de chile)', 'UChile TV', 'Universitarios'),
    (r'^ucv tv eventos', 'UCV TV Eventos', 'Universitarios'),
    (r'^ucv', 'UCV Televisión', 'Universitarios'),
    (r'^tv usach', 'TV Usach', 'Universitarios'),
    (r'^utalca', 'UTalca TV', 'Universitarios'),
    (r'^umag', 'UMAG TV', 'Universitarios'),
    (r'^u ?los lagos|^ulagos', 'ULagos TV', 'Universitarios'),
    (r'^ufromedios', 'UfroMedios', 'Universitarios'),
    (r'^ua ?tv', 'UATV', 'Universitarios'),
    (r'^campus ?tv', 'Campus TV', 'Universitarios'),
    (r'^tv senado', 'TV Senado', 'Institucionales'),
    (r'^tevex', 'Tevex', 'Institucionales'),
    # Same channel listed under several names.
    (r'^canal 5 (pto\.?|puerto) montt', 'Canal 5 Puerto Montt', 'Regionales'),
    (r'^canal 9', 'Canal 9 Bío-Bío', 'Regionales'),
    (r'^caracola', 'Caracola TV', 'Regionales'),
    (r'^decima', 'Décima TV', 'Regionales'),
    (r'^solo ?tv', 'Solo TV', 'Regionales'),
    (r'^t-vinet', 'T-Vinet', 'Regionales'),
    (r'^tvr', 'TVR Televisión Regional', 'Regionales'),
    (r'^vision plus', 'Vision Plus TV', 'Regionales'),
    (r'^vtv', 'VTV', 'Regionales'),
    (r'^canal isb', 'Canal ISB', 'Religiosos'),
    (r'^nctv', 'NCTV', 'Religiosos'),
    (r'^(la ?metro|radio la metro)', 'Radio La Metro', 'Radios con video'),
    (r'^(radio )?cooperativa', 'Radio Cooperativa', 'Radios con video'),
    (r'^(radio )?la clave', 'Radio La Clave', 'Radios con video'),
    (r'^agricultura', 'Radio Agricultura', 'Radios con video'),
    (r'^pauta$', 'Radio Pauta', 'Radios con video'),
    (r'^la nuestra', 'Radio La Nuestra', 'Radios con video'),
    (r'^(radio )?biobio tv$|^radio bio ?bio', 'Radio Bío-Bío', 'Radios con video'),
]
# Program guide (XMLTV). OwnTV stores url-tvg as the playlist's guide on sync and splits a
# comma-separated value into one guide per feed. open-epg.com publishes real schedules, refreshed
# daily (~1.5 days ahead). Do NOT use iptv-epg.org: its Chilean channels are "No Data" placeholders.
# chile2 has the main channels; chile1 adds La Red, UCV, TVU and Radio Agricultura.
GUIDE_URL = 'https://www.open-epg.com/files/chile2.xml,https://www.open-epg.com/files/chile1.xml'
# Our channel -> the guide's channel id, exactly as open-epg writes it.
EPG_IDS = {
    'Mega': 'Mega.cl', 'Chilevisión': 'CHV.cl', 'Canal 13': 'Canal 13.cl', 'TV+': 'TV+.cl',
    '24 Horas': '24 Horas HD.cl', 'T13 en vivo': 'T13 En Vivo.cl', 'CHV Noticias': 'CHV Noticias.cl',
    'NTV': 'NTV.cl', '13 Cultura': '13 C HD.cl', 'TV Senado': 'TV SENADO.cl',
    'La Red': 'La Red.cl', 'UCV Televisión': 'UCV TV.cl', 'TVU': 'TVU.cl', 'Radio Agricultura': 'Radio Agricultura.cl',
    # Community lists give CHV Deportes Chilevisión's id, which would show Chilevisión's schedule on it.
    'CHV Deportes': 'CHVDeportes.cl',
}

RULE_ORDER = {}
for i, (_, name, _) in enumerate(RULES):
    RULE_ORDER.setdefault(name, i)

def classify(raw, cat):
    n = ascii_low(clean_name(raw))
    for rx, name, group in RULES:
        if re.search(rx, n):
            return name, group
    cat = (cat or '').lower()
    name = clean_name(raw)
    if cat == 'religious': g = 'Religiosos'
    elif cat in ('outdoor', 'travel'): g = 'Paisajes en vivo'
    elif cat == 'legislative': g = 'Institucionales'
    elif cat == 'news': g = 'Noticias'
    elif cat in ('kids', 'animation', 'culture', 'education'): g = 'Cultura e infantil'
    elif cat == 'sports': g = 'Deportes'
    elif re.search(r'\bradio\b|\bfm\b|\d{2,3}\.\d', n): g = 'Radios con video'
    elif cat == 'music': g = 'Música'
    elif cat in ('entertainment', 'comedy', 'series'): g = 'Entretención'
    else: g = 'Regionales'
    return name, g

def png_logo(urls):
    urls = [u for u in urls if u]
    for u in urls:
        if not u.lower().split('?')[0].endswith('.svg'):
            return u
    for u in urls:  # Wikimedia SVG -> its PNG thumbnail
        m = re.match(r'https://upload\.wikimedia\.org/wikipedia/commons/(\w/\w\w)/([^/]+\.svg)$', u)
        if m:
            return f'https://upload.wikimedia.org/wikipedia/commons/thumb/{m.group(1)}/{m.group(2)}/256px-{m.group(2)}.png'
    return ''

entries, by_url = {}, {}
for c in cands:
    ok = [u for u in c['urls'] if u.get('status', '').startswith('ok') and not u['ip_host']]
    if not ok or EXCLUDE.search(ascii_low(c['name'])):
        continue
    name, group = classify(c['name'], c['cat'])
    # A stream already owned by another entry means it is the same channel under another name.
    owner = next((by_url[u['url']] for u in ok if u['url'] in by_url), None)
    e = entries.get(owner) if owner and name not in RULE_ORDER else None
    if e is None:
        e = entries.setdefault(name, {'name': name, 'group': group, 'urls': [], 'logos': [], 'tvg': None,
                                      'headers': {}, 'prio': c.get('prio', 0)})
    e['prio'] = max(e['prio'], c.get('prio', 0))
    for u in sorted(ok, key=lambda u: -u.get('prio', 0)):
        if u['url'] not in e['urls']:
            e['urls'].append(u['url'])
            by_url[u['url']] = e['name']
            e['headers'] = e['headers'] or u['headers']
    if c.get('prio'):
        e['logos'].insert(0, c['logo'])
    else:
        e['logos'].append(c['logo'])
    e['tvg'] = e['tvg'] or c.get('tvg')

rows = sorted(entries.values(), key=lambda e: (GROUPS.index(e['group']), RULE_ORDER.get(e['name'], 999), ascii_low(e['name'])))
lines = [f'#EXTM3U url-tvg="{GUIDE_URL}"']
for n, e in enumerate(rows, start=1):
    attrs = f'tvg-id="{EPG_IDS.get(e["name"], e["tvg"] or "")}" tvg-chno="{n}" tvg-logo="{png_logo(e["logos"])}" group-title="{e["group"]}"'
    if e['headers'].get('Referer'): attrs += f' http-referrer="{e["headers"]["Referer"]}"'
    if e['headers'].get('User-Agent'): attrs += f' http-user-agent="{e["headers"]["User-Agent"]}"'
    lines.append(f'#EXTINF:-1 {attrs},{e["name"]}')
    lines.append(e['urls'][0])
open(out_path, 'w', encoding='utf-8', newline='\n').write('\n'.join(lines) + '\n')

if REPORT:
    g = collections.defaultdict(list)
    for e in rows: g[e['group']].append(e['name'])
    for grp in GROUPS:
        if g[grp]: print(f'[{grp}] ({len(g[grp])}): ' + ' | '.join(g[grp]))
    print('no logo:', [e['name'] for e in rows if not png_logo(e['logos'])])
print('channels:', len(rows))
