#!/usr/bin/env python3
"""Catches the mistakes that only show up in-game: bad JSON, dangling model/texture/feature references."""
import glob, json, os, re, sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else "."
RES = os.path.join(ROOT, "src", "main", "resources")
props = {}
with open(os.path.join(ROOT, "gradle.properties")) as f:
    for line in f:
        if "=" in line and not line.startswith("#"):
            k, v = line.strip().split("=", 1)
            props[k] = v
NS = props.get("mod_id", "sorrowed")
errors = []

def err(msg):
    errors.append(msg)

def load(path):
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:
        err(f"INVALID JSON {os.path.relpath(path, ROOT)}: {e}")
        return None

def mine(ref):
    return ref.startswith(NS + ":")

def local(ref):
    return ref.split(":", 1)[1]

def exists(rel):
    return os.path.exists(os.path.join(RES, rel))

jsons = [p for p in glob.glob(RES + "/**/*", recursive=True) if p.endswith((".json", ".mcmeta"))]
docs = {p: load(p) for p in jsons}

for p, d in docs.items():
    if d is None:
        continue
    rel = os.path.relpath(p, RES).replace(os.sep, "/")
    if rel.startswith(f"assets/{NS}/blockstates/"):
        def walk(o):
            if isinstance(o, dict):
                if "model" in o and isinstance(o["model"], str) and mine(o["model"]):
                    if not exists(f"assets/{NS}/models/{local(o['model'])}.json"):
                        err(f"{rel}: missing model {o['model']}")
                for v in o.values():
                    walk(v)
            elif isinstance(o, list):
                for v in o:
                    walk(v)
        walk(d)
    if rel.startswith(f"assets/{NS}/models/"):
        par = d.get("parent")
        if isinstance(par, str) and mine(par) and not exists(f"assets/{NS}/models/{local(par)}.json"):
            err(f"{rel}: missing parent {par}")
        for k, t in (d.get("textures") or {}).items():
            if isinstance(t, str) and mine(t) and not exists(f"assets/{NS}/textures/{local(t)}.png"):
                err(f"{rel}: missing texture {t}")
    if rel.startswith(f"data/{NS}/dimension/"):
        t = d.get("type", "")
        if mine(t) and not exists(f"data/{NS}/dimension_type/{local(t)}.json"):
            err(f"{rel}: missing dimension_type {t}")
    if rel.startswith(f"data/{NS}/dimension/"):
        b = ((d.get("generator") or {}).get("biome_source") or {}).get("biome", "")
        if isinstance(b, str) and mine(b) and not exists(f"data/{NS}/worldgen/biome/{local(b)}.json"):
            err(f"{rel}: missing biome {b}")
    if rel.startswith(f"data/{NS}/worldgen/placed_feature/"):
        t = d.get("feature", "")
        if mine(t) and not exists(f"data/{NS}/worldgen/configured_feature/{local(t)}.json"):
            err(f"{rel}: missing configured_feature {t}")
    if rel.startswith(f"data/{NS}/forge/biome_modifier/") and d.get("type") == "forge:add_features":
        t = d.get("features", "")
        if mine(t) and not exists(f"data/{NS}/worldgen/placed_feature/{local(t)}.json"):
            err(f"{rel}: missing placed_feature {t}")

mods_toml = os.path.join(RES, "META-INF", "mods.toml")
if not os.path.exists(mods_toml):
    err("src/main/resources/META-INF/mods.toml is missing")
if not os.path.exists(os.path.join(RES, "pack.mcmeta")):
    err("src/main/resources/pack.mcmeta is missing")

main_src = glob.glob(ROOT + "/src/main/java/**/*.java", recursive=True)
ids = set()
for p in main_src:
    with open(p, encoding="utf-8") as f:
        ids.update(re.findall(r'@Mod\("([^"]+)"\)', f.read()))
        f.seek(0)
        ids.update(re.findall(r'@Mod\(\s*\w+\.MODID\s*\)', f.read()) and [NS])
if NS not in ids:
    err(f"No @Mod class found for mod_id '{NS}' (gradle.properties mod_id must match the @Mod id)")

if errors:
    print("Resource validation FAILED:")
    for e in errors:
        print("  -", e)
    sys.exit(1)
print(f"Resource validation OK ({len(jsons)} json files checked)")
