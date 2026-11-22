"""Write SushiTV's "Mundo" list (app/src/main/assets/mundo.m3u) from verified streams.

Usage: python mundo.py <folder with results.json> <output .m3u>   (see mundo_candidatos.py)

Only the channels in PICKS go in: each one the channel's own stream or its official free (FAST)
feed, checked on screen from Chile. Never a re-stream of a pay channel (Cartoon Network, Tooncast,
Disney…), never a YouTube-only channel (the player can't open those), never a Samsung/LG-only one.
Left out on purpose: Kpop Mix (showed only a "starting soon" loop), KBS World and The K-POP (YouTube
only), Trace (its links only play when pretending to be trace.plus's own site), M6 Music (a pay channel
in France). Chilean channels such as EnerGeek go in canales-chile.m3u instead.
"""
import json, os, sys

d, out_path = sys.argv[1], sys.argv[2]
results = json.load(open(os.path.join(d, 'results.json'), encoding='utf-8'))

# Other groups first, then the music genres. MUSIC_GROUPS also go to the "Música" rail list: the file's
# #SUSHITV-MUSICA line names them, and the app reads it (SushiPluto.kt).
OTHER_GROUPS = ['Anime', 'Japón y Corea', 'Cine y series']
MUSIC_GROUPS = ['K-pop', 'Rock y metal', 'Pop y éxitos', 'Latino y reggaetón', 'Rap, soul y R&B', 'Jazz',
                'Country', 'Conciertos', 'Retro 70-80-90', 'Electrónica y dance', 'Bandas sonoras y karaoke']
GROUPS = OTHER_GROUPS + MUSIC_GROUPS

# (name in results.json, name shown, group, piece of the URL to prefer when a name has several)
PICKS = [
    ('MAX Anime', 'MAX Anime', 'Anime', None),                      # Latin American anime channel, Spanish
    ('RetroCrush', 'RetroCrush', 'Anime', None),                    # classic anime (Cineverse), English
    ('FilmRise Anime', 'FilmRise Anime', 'Anime', None),
    ('Yu-Gi-Oh!', 'Yu-Gi-Oh!', 'Anime', None),
    ('NHK World Japón', 'NHK World Japan', 'Japón y Corea', 'nhkworld.jp'),
    ('Arirang TV Corea del Sur', 'Arirang TV', 'Japón y Corea', None),
    ('Rakuten Viki', 'Rakuten Viki', 'Japón y Corea', None),       # Asian dramas, English subtitles
    ('Cine Friki', 'Cine Friki', 'Cine y series', None),
    ('Cine Western', 'Cine Western', 'Cine y series', None),
    ('MyTime Movie Network', 'MyTime Movies', 'Cine y series', 'mytimespain'),
    ('TVE Star', 'TVE Star', 'Cine y series', None),
    ('Todo Novelas', 'Todo Novelas', 'Cine y series', None),
    # Music. Stingray, XITE, Vevo, Now and Totalmusic: the labels' own free (FAST) feeds.
    ("Stingray Today's K-Pop", "Stingray Today's K-Pop", 'K-pop', None),
    ('Stingray Classic Rock', 'Stingray Classic Rock', 'Rock y metal', None),
    ('Stingray Rock Alternative', 'Stingray Rock Alternative', 'Rock y metal', None),
    ('XITE Rock x Metal', 'XITE Rock x Metal', 'Rock y metal', None),
    ('NOW Rock', 'Now Rock', 'Rock y metal', None),
    ('Radiofreccia', 'Radiofreccia', 'Rock y metal', None),        # RTL's rock radio (Italy), with video
    ('Rock TV', 'Rock TV', 'Rock y metal', 'broadcasting.ro'),      # Rock FM Romania's TV
    ('XITE Hits', 'XITE Hits', 'Pop y éxitos', None),
    ('Vevo Pop', 'Vevo Pop', 'Pop y éxitos', None),
    ('Stingray Hit List', 'Stingray Hit List', 'Pop y éxitos', None),
    ('Stingray Greatest Hits', 'Stingray Greatest Hits', 'Pop y éxitos', None),
    ('Stingray Pop Adult', 'Stingray Pop Adult', 'Pop y éxitos', None),
    ('Stingray TikTok Radio', 'Stingray TikTok Radio', 'Pop y éxitos', None),
    ('TMF', 'TMF', 'Pop y éxitos', None),
    ('Deluxe Music', 'Deluxe Music', 'Pop y éxitos', '3qsdn'),
    ('XITE Just Chill', 'XITE Just Chill', 'Pop y éxitos', None),
    ('Vevo Latino', 'Vevo Latino', 'Latino y reggaetón', None),
    ('XITE Nuevo Latino', 'XITE Nuevo Latino', 'Latino y reggaetón', None),
    ('XITE Siempre Latino', 'XITE Siempre Latino', 'Latino y reggaetón', None),
    ("Stingray Today's Latin Pop", "Stingray Today's Latin Pop", 'Latino y reggaetón', None),
    ('Stingray Romance Latino', 'Stingray Romance Latino', 'Latino y reggaetón', None),
    ('Totalmusic', 'Totalmusic', 'Latino y reggaetón', None),
    ('Sol Musica', 'Sol Música', 'Latino y reggaetón', None),
    ('Deluxe Rap', 'Deluxe Rap', 'Rap, soul y R&B', '3qsdn'),
    ('Stingray Soul Storm', 'Stingray Soul Storm', 'Rap, soul y R&B', None),
    ('Stingray DJAZZ', 'Stingray DJAZZ', 'Jazz', None),              # jazz concerts
    ('Stingray Smooth Jazz', 'Stingray Smooth Jazz', 'Jazz', None),
    ('Qwest TV', 'Qwest TV Jazz', 'Jazz', None),
    ('Stingray Hot Country', 'Stingray Hot Country', 'Country', None),
    ('Qello Concerts by Stingray', 'Qello Concerts', 'Conciertos', None),
    ('Totalmusic Concerts', 'Totalmusic Concerts', 'Conciertos', None),
    ('Stingray Classica', 'Stingray Classica', 'Conciertos', None),  # classical concerts and opera
    ('Now 70s', 'Now 70s', 'Retro 70-80-90', None),
    ('Now 80s', 'Now 80s', 'Retro 70-80-90', None),
    ('Now 90s00s', 'Now 90s & 00s', 'Retro 70-80-90', None),
    ("Stingray Flashback 70's", "Stingray Flashback 70's", 'Retro 70-80-90', None),
    ("Stingray Everything '80s", "Stingray Everything '80s", 'Retro 70-80-90', None),
    ('Stingray Remember the 80s', 'Stingray Remember the 80s', 'Retro 70-80-90', None),
    ("Stingray Nothin' But 90s", "Stingray Nothin' But 90s", 'Retro 70-80-90', None),
    ("XITE 90's Throwback", "XITE 90's Throwback", 'Retro 70-80-90', None),
    ('Totalmusic 80s', 'Totalmusic 80s', 'Retro 70-80-90', None),
    ('Totalmusic 2000s', 'Totalmusic 2000s', 'Retro 70-80-90', None),
    ('Totalmusic Dance', 'Totalmusic Dance', 'Electrónica y dance', None),
    ('RTL 102.5 Disco', 'RTL 102.5 Disco', 'Electrónica y dance', None),
    ('Dance Television', 'Dance Television', 'Electrónica y dance', None),
    ('DanceTV Deep House District', 'DanceTV Deep House', 'Electrónica y dance', None),
    ('DanceTV Techno Warehouse', 'DanceTV Techno', 'Electrónica y dance', None),
    ('Movie Music by Stingray', 'Stingray Movie Music', 'Bandas sonoras y karaoke', None),  # film soundtracks
    ('Stingray Karaoke', 'Stingray Karaoke', 'Bandas sonoras y karaoke', None),
]

lines, missing = ['#EXTM3U', '#SUSHITV-MUSICA:' + '|'.join(MUSIC_GROUPS)], []
for group in GROUPS:
    for name, shown, g, hint in PICKS:
        if g != group:
            continue
        ok = [(c, u) for c in results if c['name'] == name for u in c['urls']
              if u.get('status', '').startswith('ok') and (hint is None or hint in u['url'])]
        if not ok:
            missing.append(name)
            continue
        c, u = ok[0]
        tvg = c['id'] if not c['id'].startswith(('freetv:', 'tdt:')) else ''
        attrs = f'tvg-id="{tvg}" tvg-logo="{c.get("logo") or ""}" group-title="{group}"'
        if u['headers'].get('Referer'): attrs += f' http-referrer="{u["headers"]["Referer"]}"'
        if u['headers'].get('User-Agent'): attrs += f' http-user-agent="{u["headers"]["User-Agent"]}"'
        lines += [f'#EXTINF:-1 {attrs},{shown}', u['url']]

open(out_path, 'w', encoding='utf-8', newline='\n').write('\n'.join(lines) + '\n')
print('channels:', (len(lines) - 2) // 2, '| not working now:', missing or 'none')
