"""For the channels named in <names.txt> (one per line, as in results.json): load the best variant's
media playlist twice, 8 s apart, and print its height, segment length, whether it advances (live) and
any slate / geo-block words in it. Text only: a picture still has to be checked on screen.

Usage: python envivo.py <folder with results.json> <names.txt>
"""
import json, re, sys, time, urllib.parse, urllib.request
from concurrent.futures import ThreadPoolExecutor

sys.stdout.reconfigure(encoding='utf-8')
d, names = sys.argv[1], [n.strip() for n in open(sys.argv[2], encoding='utf-8') if n.strip()]
UA = 'Mozilla/5.0 (Linux; Android 12; TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36'
SLATE = re.compile(r'slate|takedown|geo|block|unavailable|blackout|not[-_]?available|restrict|error|filler', re.I)

def get(url, h):
    req = urllib.request.Request(url, headers={'User-Agent': UA, **h})
    with urllib.request.urlopen(req, timeout=15) as r:
        return r.geturl(), r.read(300_000).decode('utf-8', 'ignore')

def media(url, h):
    info = {}
    for _ in range(4):
        final, t = get(url, h)
        if '#EXT-X-STREAM-INF' not in t:
            return final, t, info
        best, ls = None, t.splitlines()
        for i, l in enumerate(ls):
            if l.startswith('#EXT-X-STREAM-INF'):
                m = re.search(r'RESOLUTION=\d+x(\d+)', l)
                nxt = next((x for x in ls[i + 1:] if x and not x.startswith('#')), None)
                if nxt and (best is None or (int(m.group(1)) if m else 0) > best[0]):
                    best = (int(m.group(1)) if m else 0, nxt)
        info['height'] = best[0]
        url = urllib.parse.urljoin(final, best[1])
    return url, '', info

by = {c['name']: c for c in json.load(open(d + '/results.json', encoding='utf-8'))}

def one(n):
    c = by.get(n)
    if not c:
        return f'{n}\tNOT FOUND'
    u = next(u for u in c['urls'] if u.get('status', '').startswith('ok'))
    try:
        _, t1, info = media(u['url'], u['headers'])
        time.sleep(8)
        _, t2, _ = media(u['url'], u['headers'])
        segs = lambda t: [l for l in t.splitlines() if l and not l.startswith('#')]
        dur = re.findall(r'#EXTINF:([\d.]+)', t1)
        marks = sorted({m.group(0).lower() for s in segs(t1) + [t1] for m in SLATE.finditer(s)})
        return (f"{n}\t{info.get('height', '?')}p\tseg={dur[0] if dur else '?'}s\t"
                f"{'live' if segs(t1) != segs(t2) else 'STATIC'}\t{'VOD' if '#EXT-X-ENDLIST' in t1 else ''}\t"
                f"{','.join(marks)}\t{urllib.parse.urlsplit(u['url']).netloc}")
    except Exception as e:
        return f'{n}\tERR {type(e).__name__} {getattr(e, "code", "")}'

with ThreadPoolExecutor(10) as ex:
    for line in ex.map(one, names):
        print(line, flush=True)
