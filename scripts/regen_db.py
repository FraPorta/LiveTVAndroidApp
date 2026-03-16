#!/usr/bin/env python3
"""
Regenerate app/src/main/assets/team_db.json from already-downloaded logos.
Run from repo root: python3 scripts/regen_db.py
"""
import json
from pathlib import Path

ASSETS_DIR = Path(__file__).parent.parent / "app/src/main/assets"
LOGOS_DIR = ASSETS_DIR / "logos"
DB_PATH = ASSETS_DIR / "team_db.json"

ALIAS_MAP: dict[str, list[str]] = {
    # England - Premier League
    "Arsenal FC": ["Arsenal", "AFC", "The Gunners"],
    "Aston Villa": ["Villa", "AVFC", "Aston Villa FC"],
    "AFC Bournemouth": ["Bournemouth", "AFCB"],
    "Brentford": ["Brentford FC"],
    "Brighton & Hove Albion": ["Brighton", "Brighton and Hove Albion", "BHAFC"],
    "Chelsea FC": ["Chelsea", "CFC", "The Blues"],
    "Crystal Palace": ["Palace", "CPFC"],
    "Everton FC": ["Everton", "EFC", "The Toffees"],
    "Fulham FC": ["Fulham", "FFC"],
    "Ipswich Town": ["Ipswich", "ITFC", "Town"],
    "Leeds United": ["Leeds", "LUFC"],
    "Leicester City": ["Leicester", "LCFC", "The Foxes"],
    "Liverpool FC": ["Liverpool", "LFC", "The Reds"],
    "Manchester City": ["Man City", "MCFC", "City", "Man. City"],
    "Manchester United": ["Man United", "Man Utd", "MUFC", "United", "Man. United"],
    "Newcastle United": ["Newcastle", "NUFC", "The Magpies"],
    "Nottingham Forest": ["Notts Forest", "Forest", "NFFC", "Nottm Forest"],
    "Southampton": ["Saints", "SFC", "Southampton FC"],
    "Tottenham Hotspur": ["Tottenham", "Spurs", "THFC"],
    "West Ham United": ["West Ham", "WHUFC", "The Hammers", "WHU"],
    "Wolverhampton Wanderers": ["Wolves", "WWFC", "Wolverhampton"],
    "Sunderland": ["SAFC", "Sunderland AFC"],
    "Burnley": ["Burnley FC"],
    "Luton Town": ["Luton"],
    "Sheffield United": ["Sheffield Utd", "SUFC", "The Blades"],
    "Stoke City": ["Stoke"],
    "Coventry City": ["Coventry", "CCFC"],
    "Middlesbrough": ["Boro", "MFC"],
    "Norwich City": ["Norwich", "NCFC", "The Canaries"],
    "Swansea City": ["Swansea"],
    "West Bromwich Albion": ["West Brom", "WBA", "The Baggies"],
    "Blackburn Rovers": ["Blackburn"],
    "Derby County": ["Derby"],
    "Watford": ["Watford FC"],
    "Queens Park Rangers": ["QPR"],
    "Bristol City": ["Bristol"],
    "Plymouth Argyle": ["Plymouth"],
    # Spain - LaLiga
    "FC Barcelona": ["Barcelona", "Barca", "FCB"],
    "Real Madrid": ["Real Madrid CF", "RMCF", "RMA"],
    "Atletico Madrid": [
        "Atletico de Madrid",
        "Atletico",
        "ATM",
        "Atleti",
        "Atl. Madrid",
    ],
    "Athletic Bilbao": ["Athletic Club", "Athletic Club de Bilbao", "Bilbao"],
    "Real Sociedad": ["Real Sociedad de Futbol", "La Real"],
    "Real Betis Balompie": ["Real Betis", "Betis"],
    "Sevilla FC": ["Sevilla"],
    "Valencia CF": ["Valencia", "VCF"],
    "Villarreal CF": ["Villarreal"],
    "Getafe CF": ["Getafe"],
    "Rayo Vallecano": ["Rayo"],
    "CA Osasuna": ["Osasuna"],
    "Celta de Vigo": ["Celta Vigo", "Celta", "RC Celta", "RC Celta de Vigo"],
    "RCD Mallorca": ["Mallorca"],
    "RCD Espanyol": ["Espanyol"],
    "Girona FC": ["Girona"],
    "UD Las Palmas": ["Las Palmas"],
    "CD Leganes": ["Leganes"],
    "UD Almeria": ["Almeria"],
    "Levante UD": ["Levante"],
    "Elche CF": ["Elche"],
    # Germany - Bundesliga
    "Bayern Munich": [
        "FC Bayern Munchen",
        "FC Bayern",
        "Bayern",
        "Bayern Munchen",
        "FC Bayern Munich",
        "FCB",
    ],
    "Borussia Dortmund": ["BVB", "Dortmund", "BVB 09"],
    "Bayer 04 Leverkusen": ["Bayer Leverkusen", "Leverkusen", "B04"],
    "RB Leipzig": ["Leipzig", "Red Bull Leipzig"],
    "Borussia Monchengladbach": [
        "Borussia Gladbach",
        "Monchengladbach",
        "Gladbach",
        "BMG",
    ],
    "1. FC Union Berlin": ["Union Berlin", "Union", "FC Union Berlin"],
    "SC Freiburg": ["Freiburg"],
    "Eintracht Frankfurt": ["Frankfurt", "SGE", "Eintracht"],
    "VfB Stuttgart": ["Stuttgart"],
    "VfL Wolfsburg": ["Wolfsburg"],
    "1. FC Koln": ["Koln", "Cologne", "FC Koln", "Koeln"],
    "SV Werder Bremen": ["Werder Bremen", "Bremen", "Werder"],
    "Hamburger SV": ["Hamburg", "HSV"],
    "FC Augsburg": ["Augsburg"],
    "TSG 1899 Hoffenheim": ["Hoffenheim", "TSG Hoffenheim"],
    "FC St. Pauli": ["St Pauli", "St. Pauli"],
    "1.FSV Mainz 05": ["Mainz", "FSV Mainz", "Mainz 05"],
    "1.FC Heidenheim 1846": ["Heidenheim", "FC Heidenheim"],
    "Hertha BSC": ["Hertha", "Hertha Berlin"],
    "Schalke 04": ["Schalke", "FC Schalke 04"],
    "Hannover 96": ["Hannover"],
    "Holstein Kiel": ["Kiel"],
    # Italy - Serie A
    "Inter Milan": [
        "Inter",
        "Internazionale",
        "FC Internazionale",
        "FC Inter",
        "FC Internazionale Milano",
    ],
    "AC Milan": ["Milan", "Rossoneri"],
    "Juventus": ["Juve", "Juventus FC"],
    "AS Roma": ["Roma", "Giallorossi"],
    "SS Lazio": ["Lazio"],
    "Napoli": ["SSC Napoli"],
    "Atalanta BC": ["Atalanta", "La Dea"],
    "ACF Fiorentina": ["Fiorentina", "Viola"],
    "Torino FC": ["Torino", "Toro"],
    "Udinese": ["Udinese Calcio"],
    "Bologna FC": ["Bologna"],
    "Genoa CFC": ["Genoa"],
    "Cagliari": ["Cagliari Calcio"],
    "Frosinone Calcio": ["Frosinone"],
    "Lecce": ["US Lecce"],
    "Hellas Verona": ["Verona"],
    "Sassuolo": ["US Sassuolo"],
    "Salernitana": ["US Salernitana"],
    "Sampdoria": ["UC Sampdoria", "Samp"],
    "Spezia Calcio": ["Spezia"],
    "Empoli": ["Empoli FC"],
    "Como 1907": ["Como"],
    "Venezia FC": ["Venezia"],
    "Parma Calcio 1913": ["Parma"],
    "Monza": ["AC Monza"],
    # France - Ligue 1
    "Paris Saint-Germain": [
        "PSG",
        "Paris Saint Germain",
        "Paris SG",
        "PSG FC",
        "Paris",
    ],
    "Olympique de Marseille": ["Marseille", "OM", "Olympique Marseille"],
    "Olympique Lyon": ["Lyon", "OL", "Olympique Lyonnais", "Olympique de Lyon"],
    "AS Monaco": ["Monaco", "ASM"],
    "LOSC Lille": ["Lille", "LOSC"],
    "OGC Nice": ["Nice"],
    "Stade Rennais FC": ["Rennes", "Stade Rennais"],
    "Stade Brestois 29": ["Brest"],
    "RC Lens": ["Lens"],
    "Montpellier HSC": ["Montpellier"],
    "FC Nantes": ["Nantes"],
    "Toulouse FC": ["Toulouse"],
    "Stade Reims": ["Reims"],
    "RC Strasbourg Alsace": ["Strasbourg", "RC Strasbourg"],
    "Le Havre AC": ["Le Havre", "HAC"],
    "Angers SCO": ["Angers"],
    "AJ Auxerre": ["Auxerre"],
    "AS Saint-Etienne": ["Saint-Etienne", "Saint Etienne", "ASSE", "Les Verts"],
    "Clermont Foot": ["Clermont"],
    "FC Metz": ["Metz"],
    # Portugal - Liga Portugal
    "SL Benfica": ["Benfica", "Sport Lisboa e Benfica"],
    "FC Porto": ["Porto", "FCP"],
    "Sporting CP": ["Sporting", "Sporting Clube de Portugal", "Sporting Lisbon"],
    "SC Braga": ["Braga", "Sporting Braga"],
    "Vitoria SC": ["Vitoria Guimaraes"],
    "Casa Pia AC": ["Casa Pia"],
    "GD Estoril Praia": ["Estoril"],
    "FC Arouca": ["Arouca"],
    "Gil Vicente FC": ["Gil Vicente"],
    "Moreirense FC": ["Moreirense"],
    "Rio Ave FC": ["Rio Ave"],
    "Boavista FC": ["Boavista"],
    # Netherlands - Eredivisie
    "Ajax Amsterdam": ["Ajax", "AFC Ajax"],
    "PSV Eindhoven": ["PSV"],
    "Feyenoord": ["Feyenoord Rotterdam"],
    "AZ Alkmaar": ["AZ"],
    "FC Twente": ["Twente"],
    "SC Heerenveen": ["Heerenveen"],
    "Vitesse Arnhem": ["Vitesse"],
    "FC Utrecht": ["Utrecht"],
    "NEC Nijmegen": ["NEC"],
    "Sparta Rotterdam": ["Sparta"],
    "PEC Zwolle": ["Zwolle"],
    "Fortuna Sittard": ["Fortuna"],
    "RKC Waalwijk": ["RKC"],
    # Scotland
    "Celtic FC": ["Celtic"],
    "Rangers FC": ["Rangers"],
    "Heart of Midlothian": ["Hearts"],
    "Hibernian": ["Hibs"],
    "Hibernian FC": ["Hibs", "Hibernian"],
    "Aberdeen FC": ["Aberdeen"],
    "Kilmarnock FC": ["Kilmarnock"],
    "Motherwell FC": ["Motherwell"],
    "Dundee FC": ["Dundee"],
    "Dundee United": ["Dundee Utd"],
    # Belgium
    "RSC Anderlecht": ["Anderlecht", "RSCA"],
    "Club Brugge KV": ["Club Brugge", "Club Bruges", "Brugge"],
    "R. Antwerp FC": ["Antwerp", "Royal Antwerp"],
    "Royal Antwerp FC": ["Antwerp", "R. Antwerp"],
    "KRC Genk": ["Genk"],
    "KAA Gent": ["Gent", "Ghent"],
    "Union Saint-Gilloise": ["Union SG", "USG"],
    # Turkey
    "Galatasaray": ["Galatasaray A.S.", "Galatasaray SK", "Gala"],
    "Fenerbahce": ["Fenerbahce SK", "Fener", "FB"],
    "Besiktas JK": ["Besiktas", "BJK"],
    "Trabzonspor": ["Trabzon"],
    "Istanbul Basaksehir": ["Basaksehir", "Medipol Basaksehir"],
    "Basaksehir FK": ["Basaksehir", "Istanbul Basaksehir"],
    # Russia
    "Spartak Moscow": ["Spartak", "FC Spartak Moscow"],
    "CSKA Moscow": ["CSKA", "FC CSKA Moscow"],
    "Lokomotiv Moscow": ["Lokomotiv", "FC Lokomotiv"],
    "Zenit Saint Petersburg": ["Zenit", "Zenit St. Petersburg", "FC Zenit"],
    "Dynamo Moscow": ["Dynamo", "FC Dynamo", "Dinamo Moscow"],
    "FC Krasnodar": ["Krasnodar"],
    "Rubin Kazan": ["Rubin"],
    # Greece
    "Olympiacos Piraeus": ["Olympiakos", "Olympiacos", "Olympiakos Piraeus"],
    "PAOK Thessaloniki": ["PAOK"],
    "Panathinaikos FC": ["Panathinaikos", "PAO"],
    "AEK Athens": ["AEK"],
    "Aris Thessaloniki": ["Aris"],
    # Ukraine
    "Shakhtar Donetsk": ["Shakhtar", "FC Shakhtar"],
    "Dynamo Kyiv": ["Dinamo Kiev", "Dynamo Kiev", "Dinamo Kyiv", "FC Dynamo Kyiv"],
    # Austria
    "Red Bull Salzburg": ["RB Salzburg", "FC Salzburg", "Salzburg"],
    "Rapid Vienna": ["Rapid Wien", "SK Rapid Wien", "Rapid"],
    "Austria Vienna": ["FK Austria Wien", "Austria Wien"],
    "LASK": ["LASK Linz"],
    # Norway
    "Rosenborg BK": ["Rosenborg"],
    "Molde FK": ["Molde"],
    "Viking FK": ["Viking"],
    "Bodo/Glimt": ["Bodo Glimt", "FK Bodo/Glimt"],
    # Sweden
    "Malmo FF": ["Malmo", "MFF"],
    "IFK Goteborg": ["Gothenburg", "Goteborg", "IFK Gothenburg"],
    "AIK": ["AIK Solna"],
    "Hammarby IF": ["Hammarby"],
    "Djurgardens IF": ["Djurgarden"],
    # Switzerland
    "FC Basel 1893": ["Basel", "FC Basel"],
    "BSC Young Boys": ["Young Boys", "YB"],
    "FC Zurich": ["Zurich"],
    "Servette FC": ["Servette"],
    # Croatia
    "GNK Dinamo Zagreb": ["Dinamo Zagreb", "Dinamo"],
    "HNK Hajduk Split": ["Hajduk Split", "Hajduk"],
    "HNK Rijeka": ["Rijeka", "NK Rijeka"],
    # Czech Republic
    "AC Sparta Prague": ["Sparta Prague", "Sparta Praha", "Sparta"],
    "SK Slavia Prague": ["Slavia Prague", "Slavia Praha", "Slavia"],
    "FC Viktoria Plzen": ["Viktoria Plzen", "Plzen"],
    # Poland
    "Lech Poznan": ["Lech"],
    "Legia Warszawa": ["Legia Warsaw", "Legia"],
    "Gornik Zabrze": ["Gornik"],
    "Wisla Krakow": ["Wisla"],
    "Rakow Czestochowa": ["Rakow"],
    # Romania
    "FCSB": ["Steaua Bucharest", "Steaua", "FC Steaua"],
    "CFR Cluj": ["CFR 1907 Cluj", "Cluj"],
    # Serbia
    "FK Partizan Belgrade": ["Partizan", "FK Partizan"],
    "Red Star Belgrade": [
        "Red Star",
        "Crvena Zvezda",
        "FK Crvena zvezda",
        "Estrella Roja",
    ],
    # Denmark
    "FC Copenhagen": ["Copenhagen", "FCK"],
    "Brondby IF": ["Brondby"],
    "FC Midtjylland": ["Midtjylland"],
    # Israel
    "Maccabi Tel Aviv": ["Maccabi TA"],
    "Hapoel Beer Sheva": ["Beer Sheva"],
}


def main() -> None:
    if not LOGOS_DIR.is_dir():
        print(f"ERROR: logos directory not found at {LOGOS_DIR}")
        return

    entries: list[dict] = []
    seen: set[str] = set()

    for league_dir in sorted(LOGOS_DIR.iterdir()):
        if not league_dir.is_dir():
            continue
        for png in sorted(league_dir.glob("*.png")):
            path = f"logos/{league_dir.name}/{png.name}"
            if path in seen:
                continue
            seen.add(path)
            entries.append(
                {
                    "name": png.stem,
                    "aliases": ALIAS_MAP.get(png.stem, []),
                    "league": league_dir.name,
                    "logoAssetPath": path,
                }
            )

    entries.sort(key=lambda e: (e["league"], e["name"]))
    with open(DB_PATH, "w", encoding="utf-8") as f:
        json.dump(entries, f, ensure_ascii=False, indent=2)

    wa = sum(1 for e in entries if e["aliases"])
    print(
        f"Wrote {len(entries)} entries  ({wa} with aliases, {len(entries) - wa} without)"
    )


if __name__ == "__main__":
    main()
