"""Check every candidate stream down to a real media segment. Writes results.json."""
import json, os, sys, time, urllib.request, urllib.parse
from concurrent.futures import ThreadPoolExecutor

d = sys.argv[1]
cands = json.load(open(os.path.join(d, 'candidates.json'), encoding='utf-8'))
UA = 'Mozilla/5.0 (Linux; Android 12; TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36'

def get(url, headers, limit=None, timeout=12):
    h = {'User-Agent': UA, **headers}
    req = urllib.request.Request(url, headers=h)
    with urllib.request.urlopen(req, timeout=timeout) as r:
        data = r.read(limit) if limit else r.read()
        return r.status, r.geturl(), data

def check(url, headers):
    try:
        for _ in range(4):
            st, final, body = get(url, headers, limit=400_000)
            text = body[:200_000].decode('utf-8', 'ignore')
            if '#EXTM3U' not in text:
                # Not a playlist: a direct stream (ts/aac/mp3/mp4) counts if it delivers bytes.
                return ('ok-direct', len(body)) if len(body) > 4096 else ('fail-short', len(body))
            lines = [l.strip() for l in text.splitlines() if l.strip() and not l.startswith('#')]
            if not lines:
                return ('fail-empty-playlist', 0)
            nxt = urllib.parse.urljoin(final, lines[0])
            if '.m3u8' in lines[0].split('?')[0] or '#EXT-X-STREAM-INF' in text:
                url = nxt
                continue
            st, _, seg = get(nxt, headers, limit=300_000)
            return ('ok', len(seg)) if len(seg) > 4096 else ('fail-seg-small', len(seg))
        return ('fail-depth', 0)
    except Exception as e:
        return ('fail-' + type(e).__name__ + ':' + str(getattr(e, 'code', '')), 0)

jobs = [(ci, ui, u) for ci, c in enumerate(cands) for ui, u in enumerate(c['urls']) if not u['ip_host']]
t0 = time.time()
with ThreadPoolExecutor(16) as ex:
    res = list(ex.map(lambda j: (j, check(j[2]['url'], j[2]['headers'])), jobs))
for (ci, ui, u), (status, size) in res:
    cands[ci]['urls'][ui]['status'] = status
    cands[ci]['urls'][ui]['bytes'] = size
json.dump(cands, open(os.path.join(d, 'results.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
ok = [c for c in cands if any(u.get('status', '').startswith('ok') for u in c['urls'])]
print(f'checked {len(jobs)} urls in {time.time()-t0:.0f}s; channels with a working stream: {len(ok)}')
