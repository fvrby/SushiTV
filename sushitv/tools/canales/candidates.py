"""Merge Chilean channel sources into candidates.json: one entry per channel, all stream URLs."""
import json, re, sys, unicodedata, os

d = sys.argv[1]  # folder with teles.json, iptvorg.m3u, freetv.md
IP_HOST = re.compile(r'^https?://\d{1,3}(\.\d{1,3}){3}(:\d+)?/')

def norm(s):
    s = unicodedata.normalize('NFKD', s).encode('ascii', 'ignore').decode().lower()
    s = re.sub(r'\((?:\d+p|chile|geo-blocked|not 24/7)\)|\[[^\]]*\]', '', s)
    s = re.sub(r'\b(tv|television|canal|hd|en vivo|chile|cl)\b', ' ', s)
    return re.sub(r'[^a-z0-9]+', '', s)

chans = {}
def add(name, url, src, logo=None, cat=None, headers=None, yt=None, tvg=None, prio=0):
    k = norm(name) or name
    c = chans.setdefault(k, {'name': name, 'logo': None, 'cat': None, 'urls': [], 'yt': None, 'tvg': None, 'prio': 0})
    if prio: c['prio'] = prio; c['name'] = name; c['logo'] = logo or c['logo']
    c['logo'] = c['logo'] or logo; c['cat'] = c['cat'] or cat; c['yt'] = c['yt'] or yt; c['tvg'] = c['tvg'] or tvg
    if url and url not in [u['url'] for u in c['urls']]:
        c['urls'].append({'url': url, 'src': src, 'headers': headers or {}, 'ip_host': bool(IP_HOST.match(url)), 'prio': prio})

for blk in open(os.path.join(d, 'current.m3u'), encoding='utf-8').read().split('#EXTINF')[1:]:
    head, url = blk.strip().splitlines()[0], blk.strip().splitlines()[-1]
    a = dict(re.findall(r'([\w-]+)="([^"]*)"', head))
    add(head.rsplit(',', 1)[-1].strip(), url.strip(), 'sushitv', a.get('tvg-logo'), None, tvg=a.get('tvg-id') or None, prio=1)

for ch in json.load(open(os.path.join(d, 'teles.json'), encoding='utf-8'))['channels']:
    m3u8 = [s['url'] for s in ch.get('signals', []) if s.get('type') == 'm3u8']
    if not m3u8:
        add(ch['name'], None, 'teles', ch.get('logo'), ch.get('category'), yt=ch.get('youtube'))
    for u in m3u8:
        add(ch['name'], u, 'teles', ch.get('logo'), ch.get('category'), yt=ch.get('youtube'))

lines = open(os.path.join(d, 'iptvorg.m3u'), encoding='utf-8').read().splitlines()
cur, hdr = None, {}
for ln in lines:
    if ln.startswith('#EXTINF'):
        attrs = dict(re.findall(r'([\w-]+)="([^"]*)"', ln))
        cur = {'name': ln.rsplit(',', 1)[-1].strip(), **attrs}; hdr = {}
        if attrs.get('http-referrer'): hdr['Referer'] = attrs['http-referrer']
        if attrs.get('http-user-agent'): hdr['User-Agent'] = attrs['http-user-agent']
    elif ln.startswith('#EXTVLCOPT:http-referrer='): hdr['Referer'] = ln.split('=', 1)[1]
    elif ln.startswith('#EXTVLCOPT:http-user-agent='): hdr['User-Agent'] = ln.split('=', 1)[1]
    elif ln and not ln.startswith('#') and cur:
        add(cur['name'], ln.strip(), 'iptvorg', cur.get('tvg-logo'), cur.get('group-title'), hdr, tvg=cur.get('tvg-id'))
        cur = None

for row in open(os.path.join(d, 'freetv.md'), encoding='utf-8').read().splitlines():
    m = re.match(r'\|\s*\d+\s*\|\s*([^|]+?)\s*\|\s*\[[^\]]*\]\((http[^)]+)\)\s*\|\s*(?:<img[^>]*src="([^"]+)"|\[[^\]]*\]\(([^)]+)\))?[^|]*\|\s*([^|]*)\|', row)
    if m:
        add(m.group(1).replace('Ⓖ', '').strip(), m.group(2), 'freetv', m.group(3) or m.group(4), tvg=(m.group(5) or '').strip() or None)

out = sorted(chans.values(), key=lambda c: c['name'].lower())
json.dump(out, open(os.path.join(d, 'candidates.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
n_urls = sum(len(c['urls']) for c in out)
print('channels', len(out), 'with urls', sum(1 for c in out if c['urls']), 'urls', n_urls,
      'ip-host urls', sum(u['ip_host'] for c in out for u in c['urls']),
      'youtube-only', sum(1 for c in out if not c['urls'] and c['yt']))
