import json
import sqlite3
import csv

# DB oluştur ve değişiklik yapılacağında kullan.
conn = sqlite3.connect("weather.db")
cursor = conn.cursor()





# Tablo oluştur, daha sonra başka tablo eklenmek istendiğinde bu prototip kullanılabilir.
# cursor.execute("""
# CREATE TABLE IF NOT EXISTS cities (
#     id INTEGER NOT NULL,
#     name TEXT NOT NULL,
#     country TEXT,
#     PRIMARY KEY(id)
# )
# """)




#Veri kaynağı olan JSON yüklendi ve veritabanına uygun şekilde aktarıldı.
# cursor.execute("CREATE INDEX IF NOT EXISTS index_cities_name ON cities(name)")
#
# conn.commit()
#
# # JSON yükle
# with open("city.list.json", encoding="utf-8") as f:
#     data = json.load(f)
#
# batch = []
#
#
# for city in data:
#     batch.append((
#         city["id"],
#         city["name"],
#         city["country"]
#     ))
#
#     if len(batch) == 1000:
#         cursor.executemany(
#             "INSERT INTO cities VALUES (?, ?, ?)",
#             batch
#         )
#         conn.commit()
#         batch.clear()
#
# if batch:
#     cursor.executemany(
#         "INSERT INTO cities VALUES (?, ?, ?)",
#         batch)
#     conn.commit()
#
# conn.close()


#cursor.execute("delete from cities where country = 'TR'")

# #Tablo yapısında Türkiye şehirleri için Şehir adlarına yönelik yeni kolon oluşturuldu.
#cursor.execute("ALTER TABLE cities ADD COLUMN province TEXT")
#
# #Eski bilgilerle yeni eklenecek veriler birbirine karışmasın diye eski veritabanından Türkiye' ye ait bilgiler kaldırıldı.
# cursor.execute("DELETE FROM cities WHERE country='TR'")




#Türkiye'ye ait şehirler ve ilçeler için csv dosyası eklendi ve veritabanına entegre edildi.
# with open("iller_ve_ilceler.csv", encoding="utf-8-sig") as file:
#       reader = csv.DictReader(file)
#
#       for row in reader:
#           il = row["il_adi"]
#           ilce = row["ilce_adi"]
#           kod = row["ilce_kodu"].strip()
#
#  #CSV de ilce_kodu kolonuna ait bazı satırlar boş olduğu için hata vermemesi adına o satırlar atlandı.
#           if kod == "":
#               continue
#
#           ilce_kodu = int(kod)
#           cursor.execute("""
#           INSERT INTO cities(id, name, country, province)
#               VALUES (?, ?, ?, ?)"""
#          , (ilce_kodu, ilce, "TR", il))
cursor.execute("SELECT count(*) FROM cities where country='TR'")
print(cursor.fetchone())
conn.commit()
conn.close()