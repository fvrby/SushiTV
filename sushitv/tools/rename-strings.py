"""Genera los textos de SushiTV: copia los <string> del core de OwnTV que dicen "OwnTV" y los
sobrescribe en la app con "SushiTV" (Android da prioridad a los recursos de la app sobre los de
una librería con el mismo nombre).

Uso (desde la raíz del repo, después de compilar una vez para que Gradle baje el core):
    python sushitv/tools/rename-strings.py

Vuelve a correrlo al subir de versión el core (gradle/libs.versions.toml → owntvCore).
"""
import glob
import os
import re
import sys
import zipfile

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
LOCALES = ["values", "values-es", "values-es-rUS"]
OUT_NAME = "sushitv_rename.xml"

# Textos que hablan de servicios que de verdad son de OwnTV (su servidor de metadatos, su
# proyecto de traducción): renombrarlos sería falso, así que se dejan como vienen.
KEEP = {
    "settings_metadata_clear_advanced_message",
    "settings_metadata_fair_share",
    "settings_metadata_shared_worker_description",
    "settings_open_subtitles_access_priority",
    "settings_language_contribution_description",
    "settings_language_contribution_qr_description",
    "settings_language_help_translate",
}

STRING_RE = re.compile(r'<string name="([^"]+)"([^>]*)>(.*?)</string>', re.S)


def core_aar() -> str:
    toml = open(os.path.join(REPO, "gradle", "libs.versions.toml"), encoding="utf-8").read()
    version = re.search(r'^owntvCore\s*=\s*"([^"]+)"', toml, re.M).group(1)
    home = os.path.expanduser("~")
    pattern = os.path.join(home, ".gradle", "caches", "modules-2", "files-2.1",
                           "tv.own.owntv", "core", version, "*", f"core-{version}.aar")
    hits = glob.glob(pattern)
    if not hits:
        sys.exit(f"No encuentro core-{version}.aar en la caché de Gradle; compila una vez primero.")
    return hits[0]


def main() -> None:
    aar = core_aar()
    print("core:", os.path.basename(aar))
    with zipfile.ZipFile(aar) as z:
        for loc in LOCALES:
            xml = "".join(
                z.read(n).decode("utf-8")
                for n in z.namelist()
                if n.startswith(f"res/{loc}/") and n.endswith(".xml")
            )
            rows = []
            for name, attrs, body in STRING_RE.findall(xml):
                if "OwnTV" in body and name not in KEEP:
                    rows.append(f'    <string name="{name}"{attrs}>{body.replace("OwnTV", "SushiTV")}</string>')
            out_dir = os.path.join(REPO, "app", "src", "main", "res", loc)
            os.makedirs(out_dir, exist_ok=True)
            with open(os.path.join(out_dir, OUT_NAME), "w", encoding="utf-8", newline="\n") as f:
                f.write('<?xml version="1.0" encoding="utf-8"?>\n')
                f.write("<!-- GENERADO por sushitv/tools/rename-strings.py: no editar a mano. -->\n")
                f.write(
                    '<resources xmlns:tools="http://schemas.android.com/tools"'
                    ' xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2"'
                    ' tools:ignore="MissingTranslation,ExtraTranslation">\n'
                )
                f.write("\n".join(rows) + "\n</resources>\n")
            print(f"{loc}: {len(rows)} textos")


if __name__ == "__main__":
    main()
