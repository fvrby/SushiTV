"""Candidates for the "Mundo" list: themed (anime, Japan/Korea, music, movies, kids, series) free
streams from iptv-org, Free-TV and TDTChannels -> candidates.json, in ../canales/deepcheck.py's format.

Usage (in a folder of its own, all downloads go there):
  curl -o channels.json  https://iptv-org.github.io/api/channels.json
  curl -o feeds.json     https://iptv-org.github.io/api/feeds.json
  curl -o streams.json   https://iptv-org.github.io/api/streams.json
  curl -o logos.json     https://iptv-org.github.io/api/logos.json
  curl -o blocklist.json https://iptv-org.github.io/api/blocklist.json
  curl -o freetv.m3u8    https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8
  curl -o tdt.json       https://www.tdtchannels.com/lists/tv.json
  python mundo_candidatos.py <folder>
  python ../canales/deepcheck.py <folder>          # -> results.json (each link down to a real segment)
  python envivo.py <folder> <names.txt>            # live? which resolution? slate markers?
  python mundo.py <folder> ../../../app/src/main/assets/mundo.m3u

The lists are only an index. What goes into SushiTV is chosen by hand in mundo.py: the channel's own
(or its official FAST) stream, never a re-stream of a pay channel, and checked on screen first.
"""
import json, os, re, sys

d = sys.argv[1]
J = lambda f: json.load(open(os.path.join(d, f), encoding='utf-8'))
IP_HOST = re.compile(r'^https?://\d{1,3}(\.\d{1,3}){3}(:\d+)?/')
THEMES = {'animation', 'music', 'movies', 'classic', 'kids', 'series'}
KEYS = re.compile(r'anime|otaku|manga|k-?pop|j-?pop|j-?rock|korea|corea|japan|jap[oó]n|nippon|nhk|arirang|kbs|'
                  r'energeek|cartoon|retro|toon|kawaii|asia', re.I)
LANGS = {'spa', 'eng', 'jpn', 'kor'}
# Per-session / per-device / app-locked platforms and third-party proxies of them (Pluto is built on the
# TV instead), and video sites the player cannot open as a channel. A label's own server stays in even
# when its path names the platform it feeds (Stingray's lotus.stingray.com/…/samsungtvplus/…).
SKIP_HOSTS = re.compile(r'pluto\.tv|jmp2\.uk|mjh\.nz|plex\.tv|roku\.com|tubi|youtube|youtu\.be|'
                        r'twitch|dailymotion|facebook|xumo|stirr|lgchannels|\bvix\b', re.I)

chans = {c['id']: c for c in J('channels.json')}
feeds, logos = {}, {}
for f in J('feeds.json'):
    feeds.setdefault(f['channel'], []).append(f)
for l in J('logos.json'):
    if l.get('format') != 'SVG':
        logos.setdefault(l['channel'], l['url'])
blocked = {b['channel'] for b in J('blocklist.json')}

out = {}
def add(key, name, url, src, **meta):
    if not url or SKIP_HOSTS.search(url) or IP_HOST.match(url):
        return
    c = out.setdefault(key, {'id': key, 'name': name, 'logo': meta.pop('logo', None), 'cat': None, 'urls': [], **meta})
    if url not in [u['url'] for u in c['urls']]:
        c['urls'].append({'url': url, 'src': src, 'headers': meta.get('headers', {}), 'ip_host': False})

for s in J('streams.json'):
    ch = chans.get(s['channel'] or '')
    if not ch or ch['is_nsfw'] or ch['closed'] or ch['id'] in blocked:
        continue
    if 'Geo-blocked' in (s.get('labels') or []):
        continue
    langs = {l for f in feeds.get(ch['id'], []) for l in f.get('languages', [])}
    themed = THEMES & set(ch['categories']) or KEYS.search(ch['name'] + ' ' + ' '.join(ch['alt_names']))
    # Music in any language (it's the music that counts); the rest in Spanish, English, Japanese or Korean.
    if not ('music' in ch['categories'] or themed and (langs & LANGS or ch['country'] in {'JP', 'KR'})):
        continue
    h = {}
    if s.get('referrer'): h['Referer'] = s['referrer']
    if s.get('user_agent'): h['User-Agent'] = s['user_agent']
    add(ch['id'], ch['name'], s['url'], 'iptvorg', logo=logos.get(ch['id']), country=ch['country'],
        langs=sorted(langs), cats=ch['categories'], headers=h, labels=s.get('labels') or [])

# Free-TV: channels its maintainers consider free and legal. Keep the themed or Asian ones.
cur = None
for ln in open(os.path.join(d, 'freetv.m3u8'), encoding='utf-8').read().splitlines():
    if ln.startswith('#EXTINF'):
        a = dict(re.findall(r'([\w-]+)="([^"]*)"', ln)); cur = (ln.rsplit(',', 1)[-1].strip(), a)
    elif ln and not ln.startswith('#') and cur:
        name, a = cur; cur = None
        g = a.get('group-title', '')
        if 'Geo-blocked' in name or not (KEYS.search(name) or g in {'日本 / Japan', 'Korea'} or
                                          re.search(r'music|m[uú]sica|movie|cine|kids|infantil|anim', name + g, re.I)):
            continue
        name = name.replace('Ⓖ', '').strip()
        add('freetv:' + name.lower(), name, ln.strip(), 'freetv', logo=a.get('tvg-logo'), country=g, langs=[],
            cats=[g], labels=[])

# TDTChannels (Spain's free list; EnerGeek hands its own links to it).
for c in J('tdt.json').get('countries', []):
    for a in c.get('ambits', []):
        for ch in a.get('channels', []):
            if not (KEYS.search(ch['name']) or re.search(r'infantil|music|anim', a['name'], re.I)):
                continue
            for o in ch.get('options', []):
                if o.get('format') in (None, 'm3u8', 'stream'):
                    add('tdt:' + ch['name'].lower(), ch['name'], o.get('url'), 'tdt', logo=ch.get('logo'),
                        country=c['name'], langs=[], cats=[a['name']], labels=[])

res = sorted(out.values(), key=lambda c: c['name'].lower())
json.dump(res, open(os.path.join(d, 'candidates.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
print('channels', len(res), 'urls', sum(len(c['urls']) for c in res))
