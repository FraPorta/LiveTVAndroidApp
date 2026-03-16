#!/usr/bin/env python3
"""
Download football logos from luukhopman/football-logos (current season: logos/ dir),
resize each to 64x64, save into app/src/main/assets/logos/, and generate team_db.json.

Run once from repo root:
    python3 scripts/download_logos.py

Requires: pip install pillow requests
"""

import json
import os
import re
import sys
import time
import unicodedata
from pathlib import Path

import requests
from PIL import Image
from io import BytesIO

GITHUB_API_TREE = (
    "https://api.github.com/repos/luukhopman/football-logos/git/trees/master?recursive=1"
)
RAW_BASE = "https://raw.githubusercontent.com/luukhopman/football-logos/master/"
ASSETS_DIR = Path(__file__).parent.parent / "app/src/main/assets"
LOGOS_DIR = ASSETS_DIR / "logos"
DB_PATH = ASSETS_DIR / "team_db.json"
LOGO_SIZE = (64, 64)

# ── Common aliases for name-matching between scraped data and DB canonical names ──
ALIAS_MAP: dict[str, list[str]] = {
    # England - Premier League
    "Arsenal FC": ["Arsenal"],
    "Aston Villa": [],
    "Chelsea FC": ["Chelsea"],
    "Crystal Palace": [],
    "Everton FC": ["Everton"],
    "Fulham FC": ["Fulham"],
    "Liverpool FC": ["Liverpool"],
    "Manchester City": ["Man City"],
    "Manchester United": ["Man United", "Man Utd"],
    "Newcastle United": ["Newcastle"],
    "Tottenham Hotspur": ["Tottenham", "Spurs"],
    "West Ham United": ["West Ham"],
    "Wolverhampton Wanderers": ["Wolves"],
    "Brighton & Hove Albion": ["Brighton"],
    "Bournemouth": ["AFC Bournemouth"],
    "Brentford": [],
    "Leeds United": ["Leeds"],
    "Leicester City": ["Leicester"],
    "Nottingham Forest": ["Notts Forest", "Forest"],
    "Southampton": [],
    "Ipswich Town": ["Ipswich"],
    # Spain - LaLiga
    "FC Barcelona": ["Barcelona", "Barca"],
    "Real Madrid": [],
    "Atletico Madrid": ["Atlético Madrid", "Atletico de Madrid"],
    "Athletic Bilbao": ["Athletic Club"],
    "Real Sociedad": [],
    "Real Betis Balompié": ["Real Betis", "Betis"],
    "Sevilla FC": ["Sevilla"],
    "Valencia CF": ["Valencia"],
    "Villarreal CF": ["Villarreal"],
    "Getafe CF": ["Getafe"],
    "Rayo Vallecano": [],
    "CA Osasuna": ["Osasuna"],
    "Celta de Vigo": ["Celta Vigo", "Celta"],
    "RCD Mallorca": ["Mallorca"],
    "RCD Espanyol": ["Espanyol"],
    "Girona FC": ["Girona"],
    # Germany - Bundesliga
    "Bayern Munich": ["FC Bayern", "FC Bayern München", "Bayern München"],
    "Borussia Dortmund": ["BVB", "Dortmund"],
    "Bayer 04 Leverkusen": ["Bayer Leverkusen", "Leverkusen"],
    "RB Leipzig": ["Leipzig"],
    "Borussia Mönchengladbach": ["Borussia Gladbach", "Mönchengladbach"],
    "1. FC Union Berlin": ["Union Berlin"],
    "SC Freiburg": ["Freiburg"],
    "Eintracht Frankfurt": ["Frankfurt"],
    "VfB Stuttgart": ["Stuttgart"],
    "VfL Wolfsburg": ["Wolfsburg"],
    "1. FC Köln": ["Köln", "Cologne"],
    "Werder Bremen": ["Bremen"],
    "Hamburger SV": ["Hamburg"],
    # Italy - Serie A
    "Inter Milan": ["Inter", "Internazionale", "FC Internazionale", "FC Inter"],
    "AC Milan": ["Milan"],
    "Juventus": ["Juve"],
    "AS Roma": ["Roma"],
    "SS Lazio": ["Lazio"],
    "Napoli": ["SSC Napoli", "S.S.C. Napoli"],
    "Atalanta BC": ["Atalanta"],
    "Fiorentina": ["ACF Fiorentina"],
    "Torino FC": ["Torino"],
    "Udinese": [],
    "Sampdoria": [],
    "Bologna FC": ["Bologna"],
    # France - Ligue 1
    "Paris Saint-Germain": ["PSG", "Paris Saint Germain"],
    "Olympique de Marseille": ["Marseille", "OM"],
    "Olympique Lyon": ["Lyon", "OL"],
    "AS Monaco": ["Monaco"],
    "LOSC Lille": ["Lille"],
    "OGC Nice": ["Nice"],
    "Stade Rennais FC": ["Rennes"],
    "Stade Brestois 29": ["Brest"],
    "RC Lens": ["Lens"],
    "Montpellier HSC": ["Montpellier"],
    # Portugal - Liga Portugal
    "SL Benfica": ["Benfica"],
    "FC Porto": ["Porto"],
    "Sporting CP": ["Sporting"],
    "SC Braga": ["Braga"],
    # Netherlands - Eredivisie
    "Ajax Amsterdam": ["Ajax"],
    "PSV Eindhoven": ["PSV", "PSV Eindhoven"],
    "Feyenoord": [],
    "AZ Alkmaar": ["AZ"],
    # Turkey - Süper Lig
    "Galatasaray": ["Galatasaray A.S."],
    "Fenerbahce": ["Fenerbahçe"],
    "Besiktas JK": ["Beşiktaş", "Besiktas"],
    # Scotland
    "Celtic FC": ["Celtic"],
    "Rangers FC": ["Rangers"],
    # Russia
    "Spartak Moscow": [],
    "CSKA Moscow": [],
    # Belgium
    "RSC Anderlecht": ["Anderlecht"],
    "Club Brugge KV": ["Club Brugge"],
    # Romania
    "FCSB": [],
    # Serbia
    "FK Partizan Belgrade": ["Partizan"],
    "Red Star Belgrade": ["Red Star", "Crvena Zvezda"],
    # Greece
    "Olympiacos Piraeus": ["Olympiakos", "Olympiacos"],
    "PAOK Thessaloniki": ["PAOK"],
    "Panathinaikos FC": ["Panathinaikos"],
    "AEK Athens": ["AEK"],
}


def normalize(name: str) -> str:
    """Lowercase, strip diacritics, collapse whitespace."""
    nfkd = unicodedata.normalize("NFKD", name)
    ascii_only = nfkd.encode("ascii", "ignore").decode("ascii")
    return re.sub(r"\s+", " ", ascii_only.lower()).strip()


def download_and_resize(url: str, dest: Path) -> bool:
    try:
        resp = requests.get(url, timeout=15)
        resp.raise_for_status()
        img = Image.open(BytesIO(resp.content)).convert("RGBA")
        img = img.resize(LOGO_SIZE, Image.LANCZOS)
        dest.parent.mkdir(parents=True, exist_ok=True)
        img.save(dest, "PNG", optimize=True)
        return True
    except Exception as e:
        print(f"  WARN: failed to download {url}: {e}", file=sys.stderr)
        return False


def main() -> None:
    print("Fetching repo tree from GitHub API…")
    resp = requests.get(GITHUB_API_TREE, timeout=30)
    resp.raise_for_status()
    tree = resp.json().get("tree", [])

    # Filter to only logos/ blobs (current season directory)
    logo_blobs = [
        entry for entry in tree
        if entry["type"] == "blob"
        and entry["path"].startswith("logos/")
        and entry["path"].endswith(".png")
    ]
    print(f"Found {len(logo_blobs)} logo files in logos/")

    team_entries: list[dict] = []
    failed: int = 0

    for i, blob in enumerate(logo_blobs):
        path = blob["path"]  # e.g. "logos/England - Premier League/Arsenal FC.png"
        parts = path.split("/")
        if len(parts) != 3:
            continue

        league_dir = parts[1]    # "England - Premier League"
        filename   = parts[2]    # "Arsenal FC.png"
        team_name  = filename[:-4]  # strip .png

        raw_url  = RAW_BASE + requests.utils.quote(path, safe="/")
        dest_rel = f"logos/{league_dir}/{filename}"
        dest_abs = ASSETS_DIR / dest_rel

        print(f"  [{i+1}/{len(logo_blobs)}] {league_dir} / {team_name}", end=" … ", flush=True)

        if dest_abs.exists():
            print("(cached)")
        else:
            ok = download_and_resize(raw_url, dest_abs)
            if not ok:
                failed += 1
            else:
                print("OK")
            time.sleep(0.05)  # polite delay

        aliases = ALIAS_MAP.get(team_name, [])
        team_entries.append({
            "name": team_name,
            "aliases": aliases,
            "league": league_dir,
            "logoAssetPath": dest_rel,
        })

    # Sort entries by league then name for readability
    team_entries.sort(key=lambda e: (e["league"], e["name"]))

    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    with open(DB_PATH, "w", encoding="utf-8") as f:
        json.dump(team_entries, f, ensure_ascii=False, indent=2)

    print(f"\nDone. {len(team_entries)} entries written to {DB_PATH}")
    print(f"Failed downloads: {failed}")


if __name__ == "__main__":
    main()
