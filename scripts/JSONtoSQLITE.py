import json
import sqlite3

# DB oluştur
conn = sqlite3.connect("weather.db")
cursor = conn.cursor()

# Tablo oluştur
cursor.execute("""
CREATE TABLE IF NOT EXISTS cities (
    id INTEGER NOT NULL,
    name TEXT NOT NULL,
    country TEXT,
    PRIMARY KEY(id)
)
""")

cursor.execute("CREATE INDEX IF NOT EXISTS index_cities_name ON cities(name)")

conn.commit()

# JSON yükle
with open("city.list.json", encoding="utf-8") as f:
    data = json.load(f)

batch = []

for city in data:
    batch.append((
        city["id"],
        city["name"],
        city["country"]
    ))

    if len(batch) == 1000:
        cursor.executemany(
            "INSERT INTO cities VALUES (?, ?, ?)",
            batch
        )
        conn.commit()
        batch.clear()

if batch:
    cursor.executemany(
        "INSERT INTO cities VALUES (?, ?, ?)",
        batch)
    conn.commit()

conn.close()

print("Database oluşturuldu.")