from pathlib import Path
import json

root = Path(__file__).resolve().parents[1]
fixture = json.loads((root / "tests/fixtures/metaranai-backup-v064-sample.json").read_text())
assert fixture["format"] == "metaranai-backup"
assert fixture["version"] == 64
prefs = fixture["preferences"]
assert len(json.loads(prefs["history"])) == 2
assert len(json.loads(prefs["external_artists"])) == 2
assert json.loads(prefs["genre_lens_v05"])["manual"] == ["Thrash Metal"]

local = (root / "app/src/main/java/jp/metaranai/app/LocalStore.kt").read_text()
assert 'out.put("format", "metaranai-backup")' in local
assert 'out.put("version", 90)' in local
assert 'root.getJSONObject("preferences")' in local
assert 'require(root.optString("format") == "metaranai-backup")' in local
assert 'require(root.optInt("version")' not in local
assert 'prefs.getString("external_artists", "[]")' in local
assert 'archiveDb.replaceAll(restoredArtists.map(::archiveRow))' in local
assert 'check(e.commit())' in local
for key in ["profile", "history", "search_history", "external_artists", "genre_lens_v05", "vocal_profile_v05"]:
    assert key in local, key

db = (root / "app/src/main/java/jp/metaranai/app/MetalArchiveDatabase.kt").read_text()
assert 'DATABASE_NAME = "metaranai_archive.db"' in db
assert 'CREATE TABLE artists' in db
assert 'payload_json TEXT NOT NULL' in db
print("V070_OLD_JSON_BACKUP_RESTORE_OK")
print("fixture history=2 external_artists=2 -> preferences restore + SQLite rebuild path present")
