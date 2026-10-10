import json
import sqlite3
import csv

# DB oluştur ve değişiklik yapılacağında kullan.
#conn = sqlite3.connect("weather.db")
#cursor = conn.cursor()





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
#cursor.execute("SELECT count(*) FROM cities where country='TR'")
#print(cursor.fetchone())
#conn.commit()
#conn.close()
"""
import json
import sqlite3

JSON_FILE = "full.json"
DB_FILE = "stations.db"

# JSON'u oku
with open(JSON_FILE, "r", encoding="utf-8") as file:
    data = json.load(file)

# SQLite bağlantısı
conn = sqlite3.connect(DB_FILE)
cursor = conn.cursor()

# Tabloyu oluştur
cursor.execute("""
    #CREATE TABLE stations (
     #   id TEXT NOT NULL PRIMARY KEY,
      #  name TEXT NOT NULL,
       # country TEXT
    #)
""")

# Index oluştur
cursor.execute("""
    #CREATE INDEX index_stations_name
    #ON stations(name)
""")

# Verileri ekle
for station in data:
    station_id = station.get("id")

    name_data = station.get("name", {})
    name = name_data.get("en")

    country = station.get("country")

    # Gerekli alanlar yoksa atla
    if not station_id or not name:
        continue

    cursor.execute("""
        #INSERT INTO stations (id, name, country)
        #VALUES (?, ?, ?)
   # """, (station_id, name, country))
"""
conn.commit()

# Kontrol
cursor.execute("SELECT COUNT(*) FROM stations")
count = cursor.fetchone()[0]

print(f"{count} istasyon veritabanına eklendi.")

conn.close()

print(f"Database oluşturuldu: {DB_FILE}")"""
#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from reportlab.lib.pagesizes import A4
from reportlab.lib.units import cm
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, PageBreak,
    Table, TableStyle, HRFlowable
)
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY, TA_LEFT, TA_RIGHT

# ── Font registration ──────────────────────────────────────────────────────────
# Standart (Normal) Times New Roman
pdfmetrics.registerFont(TTFont('DejaVuSerif', r'C:\Windows\Fonts\times.ttf'))

# Varsa Bold ve Italic versiyonları için de:
pdfmetrics.registerFont(TTFont('DejaVuSerif-Bold', r'C:\Windows\Fonts\timesbd.ttf'))
pdfmetrics.registerFont(TTFont('DejaVuSerif-Italic', r'C:\Windows\Fonts\timesi.ttf'))
pdfmetrics.registerFontFamily('DejaVuSerif',
    normal='DejaVuSerif',
    bold='DejaVuSerif-Bold',
    italic='DejaVuSerif-Italic',
    boldItalic='DejaVuSerif-BoldItalic')

# ── Page setup (DEÜ margins) ───────────────────────────────────────────────────
PAGE_W, PAGE_H = A4
LEFT   = 3.0 * cm
RIGHT  = 2.5 * cm
TOP    = 3.0 * cm
BOTTOM = 2.0 * cm
TW = PAGE_W - LEFT - RIGHT   # usable text width

# ── Styles ────────────────────────────────────────────────────────────────────
def s(name, **kw):
    base = kw.pop('parent', None)
    defaults = dict(fontName='DejaVuSerif', fontSize=12, leading=18,
                    spaceAfter=0, spaceBefore=0, alignment=TA_JUSTIFY)
    if base:
        defaults.update(base.__dict__)
    defaults.update(kw)
    return ParagraphStyle(name, **defaults)

normal   = s('Normal')
justify  = s('Justify', firstLineIndent=0.5*cm)
centered = s('Centered', alignment=TA_CENTER)
bold_c   = s('BoldC',   alignment=TA_CENTER, fontName='DejaVuSerif-Bold')
h1       = s('H1', fontSize=18, fontName='DejaVuSerif-Bold',
             alignment=TA_CENTER, spaceAfter=6, spaceBefore=12)
h2       = s('H2', fontSize=14, fontName='DejaVuSerif-Bold',
             alignment=TA_CENTER, spaceAfter=6, spaceBefore=20)
h_sec    = s('HSec', fontSize=12, fontName='DejaVuSerif-Bold',
             spaceAfter=6, spaceBefore=14, alignment=TA_LEFT)
h_sub    = s('HSub', fontSize=12, fontName='DejaVuSerif-Bold',
             spaceAfter=4, spaceBefore=10, alignment=TA_LEFT)
small_c  = s('SmallC', fontSize=10, alignment=TA_CENTER)
toc_title= s('TocTitle', fontSize=18, fontName='DejaVuSerif-Bold',
             alignment=TA_CENTER, spaceAfter=20)

# ── Helper ────────────────────────────────────────────────────────────────────
def sp(h=0.3): return Spacer(1, h*cm)
def para(text, style=None): return Paragraph(text, style or justify)
def line(): return HRFlowable(width="100%", thickness=0.5, color=colors.black)

# ── Page numbering ─────────────────────────────────────────────────────────────
class NumberedCanvas:
    """Draws centered page number at bottom of each page."""
    def __init__(self, filename, **kw):
        from reportlab.pdfgen import canvas as cv
        self._cv = cv.Canvas(filename, **kw)
        self._pages = []

    def showPage(self):
        self._pages.append(dict(self._cv.__dict__))
        self._cv.showPage()

    def save(self):
        num_pages = len(self._pages)
        for p in range(num_pages):
            self._cv.restoreState() if p else None
            page = self._pages[p]
            self._cv.__dict__.update(page)
            self._draw_page_number(p + 1)
            self._cv.showPage()
        self._cv.save()

    def _draw_page_number(self, num):
        self._cv.setFont('DejaVuSerif', 10)
        self._cv.drawCentredString(PAGE_W / 2, BOTTOM - 0.5*cm, str(num))

# ── Story builder ─────────────────────────────────────────────────────────────
story = []

# ══════════════════════════════════════════════════════════════════════════════
# KAPAK SAYFASI
# ══════════════════════════════════════════════════════════════════════════════
story += [
    sp(1.5),
    Paragraph("T.C", bold_c),
    Paragraph("DOKUZ EYLÜL ÜNİVERSİTESİ", bold_c),
    Paragraph("FEN FAKÜLTESİ", bold_c),
    sp(1.2),
    # Logo placeholder
    Paragraph("[DEÜ AMBLEMİ — 3x3 cm]", s('LogoPH', alignment=TA_CENTER,
              fontSize=10, textColor=colors.grey)),
    sp(3.0),
    Paragraph("STAJ RAPOR DOSYASI", h1),
    sp(3.5),
    Paragraph("Ad Soyad: .....................................", bold_c),
    sp(3.5),
    Paragraph("Ağustos, 2026", s('DateS', fontSize=12,
              fontName='DejaVuSerif-Bold', alignment=TA_CENTER)),
    Paragraph("İZMİR", s('CityS', fontSize=12,
              fontName='DejaVuSerif-Bold', alignment=TA_CENTER)),
    PageBreak(),
]

# ══════════════════════════════════════════════════════════════════════════════
# İÇİNDEKİLER
# ══════════════════════════════════════════════════════════════════════════════
story.append(Paragraph("İÇİNDEKİLER", toc_title))
story.append(sp(0.3))

toc_entries = [
    ("İçindekiler", "i"),
    ("BÖLÜM BİR — GİRİŞ", "1"),
    ("    1.1  Staj Yapılan Kurum Hakkında", "1"),
    ("    1.2  Stajın Amacı ve Kapsamı", "2"),
    ("BÖLÜM İKİ — YAPILAN ÇALIŞMALAR", "3"),
    ("    2.1  Platform İnceleme ve Test Çalışmaları", "3"),
    ("    2.2  Masaüstü ve Servis Uygulama Geliştirme", "5"),
    ("    2.3  Veritabanı Tasarımı ve SQL Çalışmaları", "7"),
    ("    2.4  Üretim Senaryosu Geliştirme ve Entegrasyon", "9"),
    ("    2.5  API Test ve Mobil Uygulama Testleri", "12"),
    ("    2.6  Dashboard ve Veri Görselleştirme", "14"),
    ("    2.7  Müşteri Geri Bildirim Süreci", "15"),
    ("BÖLÜM ÜÇ — SONUÇLAR", "16"),
    ("Ekler", "18"),
]

toc_st = s('TOC', alignment=TA_LEFT, fontName='DejaVuSerif', fontSize=12, leading=20)
for entry, pg in toc_entries:
    dots = '.' * max(2, 80 - len(entry) - len(pg))
    story.append(Paragraph(f"{entry} {dots} {pg}", toc_st))

story.append(PageBreak())

# ══════════════════════════════════════════════════════════════════════════════
# BÖLÜM BİR — GİRİŞ
# ══════════════════════════════════════════════════════════════════════════════
story += [
    Paragraph("BÖLÜM BİR", h2),
    Paragraph("GİRİŞ", h2),
    sp(0.4),
]

story += [
    Paragraph("1.1  Staj Yapılan Kurum Hakkında", h_sec),
    sp(0.2),
    para(
        "Bu staj çalışması, Bilgi Teknolojileri alanında faaliyet gösteren bir şirkette "
        "gerçekleştirilmiştir. Şirket, Flowias adlı no-code platform geliştirme ve kurumsal "
        "yazılım çözümleri üzerine yoğunlaşmaktadır. Flowias; işletmelerin teknik bilgiye "
        "ihtiyaç duymaksızın iş süreçlerini dijitalleştirebilmesine olanak tanıyan, "
        "web ve mobil bileşenlere sahip kapsamlı bir no-code platformdur. Şirket, bu platform "
        "üzerinden farklı sektörlere yönelik özel çözümler geliştirmekte ve müşterilerine "
        "yazılım danışmanlığı hizmetleri sunmaktadır."
    ),
    sp(0.4),
    para(
        "Staj süresi boyunca Bilgi Teknolojileri birimi bünyesinde çalışılmış; "
        "platform test süreçleri, yazılım geliştirme, veritabanı tasarımı ve iş "
        "akışı entegrasyonu gibi alanlarda deneyim kazanılmıştır."
    ),
    sp(0.6),
    Paragraph("1.2  Stajın Amacı ve Kapsamı", h_sec),
    sp(0.2),
    para(
        "Bu stajın temel amacı, üniversitede edinilen teorik bilgileri gerçek bir "
        "yazılım geliştirme ortamında uygulamak ve endüstriyel süreçlere ilişkin "
        "pratik deneyim kazanmaktır. Bu doğrultuda staj kapsamında; no-code platform "
        "kullanımı ve test süreçleri, C# .NET ile masaüstü ve Windows servis uygulama "
        "geliştirme, ilişkisel veritabanı tasarımı ve SQL sorgu yazımı, üretim "
        "senaryolarının dijital platformlara entegrasyonu, REST API testleri ve veri "
        "akışı analizi gibi konularda çalışmalar yürütülmüştür."
    ),
    sp(0.4),
    para(
        "Staj, 13 Temmuz 2026 – 26 Ağustos 2026 tarihleri arasında 32 iş günü süreyle "
        "tamamlanmıştır. Bu süre zarfında Flowias platformunun hem web hem de mobil "
        "bileşenleri üzerinde çalışılmış; platforma entegre edilebilecek üretim "
        "senaryoları tasarlanmış, veritabanı şemaları oluşturulmuş ve çeşitli yardımcı "
        "uygulamalar geliştirilmiştir."
    ),
    PageBreak(),
]

# ══════════════════════════════════════════════════════════════════════════════
# BÖLÜM İKİ — YAPILAN ÇALIŞMALAR
# ══════════════════════════════════════════════════════════════════════════════
story += [
    Paragraph("BÖLÜM İKİ", h2),
    Paragraph("YAPILAN ÇALIŞMALAR", h2),
    sp(0.4),
]

# 2.1
story += [
    Paragraph("2.1  Platform İnceleme ve Test Çalışmaları", h_sec),
    sp(0.2),
    Paragraph("2.1.1  Oryantasyon ve Geliştirme Ortamı Kurulumu", h_sub),
    sp(0.1),
    para(
        "Stajın ilk günü (13 Temmuz 2026) şirket içi oryantasyon programı ve zorunlu İş "
        "Sağlığı ve Güvenliği (İSG) bilgilendirmesi tamamlandı. Ofis ortamı ve ekip "
        "arkadaşlarıyla tanışıldı. Staj süresince kullanılacak bilgisayarın kurulumu "
        "yapılarak gerekli programlar ve geliştirme araçları yüklendi. Ekip liderinden "
        "Flowias no-code platformu ve üzerinde çalışılacak projeler hakkında ön bilgi "
        "alındı. Yapılan planlama doğrultusunda staj boyunca izlenecek adımlar "
        "netleştirildi."
    ),
    sp(0.4),
    Paragraph("2.1.2  Flowias Web ve Mobil Uygulama Testleri", h_sub),
    sp(0.1),
    para(
        "İkinci gün itibarıyla Flowias web uygulaması detaylıca incelenerek kullanıcı "
        "deneyimi ve temel güvenlik açısından manuel testlere tabi tutuldu. Uygulama "
        "içerisindeki fonksiyonel hataları ve aksaklıkları tespit etmek amacıyla form "
        "alanları ve butonlar üzerinde denemeler yapıldı. Karşılaşılan arayüz problemleri "
        "ve beklenmeyen sistem tepkileri not alınarak raporlandı."
    ),
    sp(0.3),
    para(
        "Üçüncü gün ise farklı modüller ve ekranlar üzerindeki iş akışları test edilerek "
        "olası hatalar gözlemlendi. Tespit edilen eksiklikler ilgili birimle paylaşıldı. "
        "Dördüncü gün Flowias mobil uygulaması üzerinde test incelemeleri yapılarak "
        "ekranların uyumluluğu kontrol edildi."
    ),
    sp(0.4),
    Paragraph("2.1.3  Flowias Mobil Kullanıcı Dokümantasyonu", h_sub),
    sp(0.1),
    para(
        "Beşinci gün (20 Temmuz 2026), Flowias mobil uygulamasının güncellenen ekranları "
        "ve yeni özellikleri doğrultusunda mevcut kullanım kılavuzu incelendi. "
        "Kullanıcıların uygulama içi süreçleri rahat takip edebilmesi için kılavuzdaki "
        "eski metinler ve yönlendirme adımları revize edildi. Ekran görüntüleri ile "
        "metinlerin tutarlılığı kontrol edilerek eksik kısımlar tamamlandı. Bu çalışma "
        "sayesinde son kullanıcı dokümantasyonu güncel hale getirildi."
    ),
    sp(0.5),
]

# 2.2
story += [
    Paragraph("2.2  Masaüstü ve Servis Uygulama Geliştirme", h_sec),
    sp(0.2),
    Paragraph("2.2.1  FileTransferAssistant Masaüstü Uygulaması", h_sub),
    sp(0.1),
    para(
        "Altıncı gün (21 Temmuz 2026), projelerin paketlenip yerel ağ üzerinden hedef "
        "sunucuya aktarılmasını sağlayan Windows Forms tabanlı bir masaüstü uygulaması "
        "geliştirildi. Bu uygulama, C# .NET teknolojisi kullanılarak hayata geçirildi. "
        "Dosya gönderimi sırasında ortaya çıkan ağ erişimi ve klasör yetki problemleri "
        "analiz edilerek koda gerekli düzenlemeler entegre edildi. Yapılan hata çözümleri "
        "ve iyileştirmeler sayesinde transfer işlemi otomatik ve kararlı hale getirildi. "
        "Uygulamanın farklı dosya boyutlarında sorunsuz çalıştığı test edilerek doğrulandı."
    ),
    sp(0.4),
    Paragraph("2.2.2  Windows Güvenlik Duvarı Takip ve Uyarı Servisi", h_sub),
    sp(0.1),
    para(
        "On üçüncü gün (30 Temmuz 2026), Windows Güvenlik Duvarı durumunu arka planda "
        "takip eden bir Windows servis uygulaması geliştirildi. Güvenlik duvarının "
        "kapatıldığı durumu anında tespit edip kullanıcıya otomatik e-posta gönderen "
        "bir mekanizma kuruldu. Uygulamanın tetiklenme anları test edilerek hatalı "
        "durumlarda loglama yapması sağlandı. Yapılan denemeler sonucunda servisin "
        "arka planda stabil ve sorunsuz şekilde çalıştığı onaylandı. Bu uygulama da "
        "C# .NET altyapısıyla geliştirilmiştir."
    ),
    PageBreak(),
]

# 2.3
story += [
    Paragraph("2.3  Veritabanı Tasarımı ve SQL Çalışmaları", h_sec),
    sp(0.2),
    Paragraph("2.3.1  Üretim Senaryosuna Ait Veritabanı Tasarımı", h_sub),
    sp(0.1),
    para(
        "Onuncu gün (27 Temmuz 2026), kararlaştırılan üretim senaryosunu Flowias projesine "
        "entegre etmek için kullanılacak veritabanı yapısı planlandı. Sistemde tutulması "
        "gereken veriler belirlenerek hangi tablolarda yer alacakları değerlendirildi. "
        "Tablolar arasındaki birincil ve yabancı anahtar ilişkileri kurularak veri "
        "akışının mantığı oluşturuldu. Veritabanının düzenli ve performanslı çalışması "
        "için gerekli şema yapısı tamamlandı."
    ),
    sp(0.4),
    Paragraph("2.3.2  SQL Sorgularının Hazırlanması ve Optimizasyonu", h_sub),
    sp(0.1),
    para(
        "On ikinci gün (29 Temmuz 2026), üretim senaryosu kapsamındaki satın alma ve "
        "hammadde süreçleri için SQL sorguları hazırlandı. Kayıt ekleme, listeleme ve "
        "ilişkili tabloları bağlayan sorgular oluşturulup test edildi. Hazırlanan "
        "sorgular Flowias veri kaynakları ile entegre edilerek doğru çalıştığı doğrulandı."
    ),
    sp(0.3),
    para(
        "On dördüncü ve on beşinci günlerde (31 Temmuz – 3 Ağustos 2026) veritabanı "
        "yapısı genişletildi; yeni tablolar eklendi ve tablolar arası ilişkiler "
        "güncellendi. Örnek üretim ve hammadde verileri sisteme aktarıldı. SQL sorguları "
        "optimize edilerek filtreleme ve listeleme sonuçları tek tek incelendi."
    ),
    sp(0.3),
    para(
        "Yirmi ikinci ve yirmi üçüncü günlerde (12-13 Ağustos 2026) veritabanı üzerinde "
        "çeşitli filtreleme, sıralama ve veri kontrol sorguları gerçekleştirildi. "
        "SQL içerisinde JSON formatındaki verilerin aktarımı ve işlenmesi üzerine "
        "denemeler yapıldı. JSON yapısıyla gelen dinamik parametrelerin SQL sorgularında "
        "nasıl ayrıştırılacağı test edildi ve veri tipi uyumsuzluğu kaynaklı hatalar "
        "giderildi."
    ),
    sp(0.5),
]

# 2.4
story += [
    Paragraph("2.4  Üretim Senaryosu Geliştirme ve Entegrasyon", h_sec),
    sp(0.2),
    Paragraph("2.4.1  Senaryo Tasarımı ve Araştırma", h_sub),
    sp(0.1),
    para(
        "Yedinci gün (22 Temmuz 2026), Flowias platformunun iş hayatındaki süreçlere "
        "uyarlanabilmesi amacıyla bir üretim senaryosu üzerinde çalışmalara başlandı. "
        "Hammadde girişinden ürün çıkışına kadar olan aşamalar değerlendirilerek "
        "sistemin kullanım alanını genişletecek senaryo tasarlandı. Sekizinci ve "
        "dokuzuncu günlerde (23-24 Temmuz 2026) üretim sektöründeki operasyonel "
        "işleyiş araştırıldı; sektörün temel kuralları, hammadde takibi ve istasyon "
        "süreçleri incelendi. Araştırma verileri doğrultusunda ihtiyaç duyulacak "
        "formlar ve kontroller belirlendi."
    ),
    sp(0.4),
    Paragraph("2.4.2  Flowias'a Entegrasyon ve Sunum Süreci", h_sub),
    sp(0.1),
    para(
        "On birinci gün (28 Temmuz 2026), üretim senaryosunun Flowias platformuna "
        "entegrasyonu için arayüz ve akış geliştirme çalışmalarına başlandı. "
        "Senaryoya uygun veri giriş ekranları tasarlanarak operasyonel süreçler "
        "sistem üzerinde adım adım kurgulandı."
    ),
    sp(0.3),
    para(
        "On altıncı gün (4 Ağustos 2026), üretim senaryosunun tüm akışı baştan "
        "sona test edilerek küçük arayüz ve mantık hataları giderildi; proje "
        "ekibe sunulacak formata getirildi. On yedinci gün (5 Ağustos 2026) "
        "geliştirilen senaryo ekip liderine ve çalışma arkadaşlarına sunuldu. "
        "Hammadde girişinden ürün tamamlanmasına kadar olan adımlar uygulamalı "
        "olarak gösterildi ve ekipten geri bildirimler alındı."
    ),
    sp(0.3),
    para(
        "On sekizinci gün (6 Ağustos 2026) geri bildirimler doğrultusunda revize "
        "çalışmalarına başlandı; eksiklikler giderildi ve sistem yeniden test "
        "edildi. On dokuzuncu gün (7 Ağustos 2026) revize edilen senaryo ekibe "
        "ve danışmana yeniden sunuldu; sistemin istenilen standartları karşıladığı "
        "değerlendirildi."
    ),
    PageBreak(),
]

# 2.5
story += [
    Paragraph("2.5  API Test ve Mobil Uygulama Testleri", h_sec),
    sp(0.2),
    Paragraph("2.5.1  REST API İncelemesi ve Testleri", h_sub),
    sp(0.1),
    para(
        "Yirminci gün (10 Ağustos 2026), Flowias projesinde kullanılan REST API "
        "istekleri ve parametre yapıları detaylıca incelendi. İsteklerdeki gövde "
        "verileri ve parametreler değiştirilerek sistemin farklı durumlardaki "
        "davranışları test edildi. Hatalı ya da eksik veri gönderildiğinde "
        "API'den dönen yanıtlar ve hata kodları gözlemlendi. Bu çalışmalar "
        "sayesinde API'nin çalışma mantığı ve veri alışverişi yapısı kavrandı."
    ),
    sp(0.4),
    Paragraph("2.5.2  API Verilerinin Mobil Uygulamada Doğrulanması", h_sub),
    sp(0.1),
    para(
        "Yirmi birinci gün (11 Ağustos 2026) Flowias mobil uygulaması üzerinde "
        "API bağlantı testlerine devam edildi. API üzerinden dönen üretim "
        "verilerinin mobil ekranlarda doğru ve eksiksiz listelenip listelenmediği "
        "kontrol edildi. Farklı ekranlar ve kullanıcı işlemleri denenerek veri "
        "akışında gecikme ya da aksaklık olup olmadığı incelendi. Karşılaşılan "
        "arayüz hataları not alınarak uygulamanın stabil çalışması sağlandı."
    ),
    sp(0.5),
]

# 2.6
story += [
    Paragraph("2.6  Dashboard ve Veri Görselleştirme", h_sec),
    sp(0.2),
    Paragraph("2.6.1  Flowias Dashboard Araçlarıyla Raporlama", h_sub),
    sp(0.1),
    para(
        "Yirmi yedinci gün (19 Ağustos 2026), üretim senaryosu kapsamında "
        "veritabanına kaydedilen verilerin toplu olarak izlenebilmesi amacıyla "
        "Flowias'ın kendi dashboard araçları kullanıldı. Üretim adetleri ve "
        "süreç durumlarını gösteren grafik ve gösterge panelleri oluşturuldu. "
        "Verilerin daha anlaşılır sunulması için raporlama ekranları düzenlenerek "
        "dashboard yapısı tamamlandı."
    ),
    sp(0.4),
    Paragraph("2.6.2  JSON Parser ve Kod Bloklarının Test Edilmesi", h_sub),
    sp(0.1),
    para(
        "Yirmi sekizinci gün (20 Ağustos 2026), Flowias platformunun no-code "
        "yapısını esneten kod yazma ve JSON Parser aktivitelerini test etmek "
        "amacıyla çalışmalar yapıldı. Bu özellikleri denemek için HTML, CSS "
        "ve JavaScript kullanılarak küçük bir oyun ve kişisel web arayüzü "
        "geliştirildi. Platformun dışarıdan yazılan kodları ve JSON verilerini "
        "işleme performansı gözlemlenerek tespit edilen hatalar ilgili birimlere "
        "aktarıldı."
    ),
    PageBreak(),
]

# 2.7
story += [
    Paragraph("2.7  Müşteri Geri Bildirim Süreci", h_sec),
    sp(0.2),
    Paragraph("2.7.1  Geri Bildirim İş Akışının Oluşturulması", h_sub),
    sp(0.1),
    para(
        "Yirmi dokuzuncu gün (21 Ağustos 2026), Flowias projesine müşterilerden "
        "gelen öneri, talep ve hata bildirimlerinin düzenli şekilde toplanabilmesi "
        "amacıyla yeni bir iş akışı oluşturuldu. Bildirimlerin sisteme girilmesi, "
        "kaydedilmesi ve ilgili birimlerce takip edilmesi adımları planlandı. "
        "Bu sayede kullanıcı geri bildirimlerinin geliştirme süreçlerine düzenli "
        "aktarılması hedeflendi."
    ),
    sp(0.4),
    Paragraph("2.7.2  Müşteriye Sunum ve Proje Teslimi", h_sub),
    sp(0.1),
    para(
        "Otuzuncu gün (24 Ağustos 2026), Flowias üzerinde hazırlanan üretim "
        "senaryosu ve iş akışları müşteriye sunuldu. Sistemin genel işleyişi, "
        "ekran geçişleri ve form yapıları uygulamalı olarak gösterildi. "
        "Müşteriden süreçle ilgili ilk görüşler ve geri bildirimler alındı."
    ),
    sp(0.3),
    para(
        "Otuz birinci gün (25 Ağustos 2026) teknik dokümantasyon çalışmaları "
        "tamamlandı. Veritabanı tabloları, hazırlanan SQL sorguları ve akış "
        "şemaları derlenerek staj raporuna eklenecek şekilde düzenlendi. "
        "Otuz ikinci ve son gün (26 Ağustos 2026) Flowias üzerinde geliştirilen "
        "tüm senaryolar, tasarlanan ekranlar ve yazılan uygulamalar ekip "
        "liderine eksiksiz şekilde teslim edildi. Staj süresince edinilen "
        "teknik kazanımlar üzerine yetkililerle genel bir değerlendirme "
        "görüşmesi yapıldı ve staj dönemi başarıyla tamamlandı."
    ),
    PageBreak(),
]

# ══════════════════════════════════════════════════════════════════════════════
# BÖLÜM ÜÇ — SONUÇLAR
# ══════════════════════════════════════════════════════════════════════════════
story += [
    Paragraph("BÖLÜM ÜÇ", h2),
    Paragraph("SONUÇLAR", h2),
    sp(0.4),
]

story += [
    Paragraph("3.1  Stajdan Elde Edilen Kazanımlar", h_sec),
    sp(0.2),
    para(
        "Bu staj çalışması süresince Flowias no-code platformu üzerinde kapsamlı "
        "uygulama geliştirme ve test deneyimi edinilmiştir. Platform mimarisinin "
        "anlaşılması, iş akışlarının tasarlanması ve bunların gerçek bir üretim "
        "senaryosuna uyarlanması süreçleri, yazılım geliştirme yaşam döngüsüne "
        "dair önemli kazanımlar sağlamıştır."
    ),
    sp(0.3),
    para(
        "C# .NET teknolojisiyle geliştirilen FileTransferAssistant masaüstü "
        "uygulaması ve Windows Güvenlik Duvarı Takip Servisi, nesne yönelimli "
        "programlama, ağ iletişimi ve Windows hizmet mimarisi konularında "
        "pratik beceriler kazandırmıştır."
    ),
    sp(0.3),
    para(
        "Veritabanı tasarımı sürecinde ilişkisel şema oluşturma, normalizasyon, "
        "birincil/yabancı anahtar ilişkileri ve SQL sorgu optimizasyonu "
        "konularında derinlemesine deneyim edinilmiştir. JSON verilerinin SQL "
        "içinde işlenmesi ise modern API entegrasyonlarına yönelik önemli "
        "bir teknik beceri olarak kazanılmıştır."
    ),
    sp(0.3),
    para(
        "REST API test süreçleri, parametre analizi ve hata ayıklama çalışmaları "
        "sayesinde web servisi mimarisine ilişkin kavrayış pekiştirilmiştir. "
        "Flowias'ın dashboard araçları kullanılarak gerçekleştirilen veri "
        "görselleştirme çalışmaları ise iş zekası ve raporlama konularında "
        "farkındalık kazandırmıştır."
    ),
    sp(0.5),
    Paragraph("3.2  Genel Değerlendirme", h_sec),
    sp(0.2),
    para(
        "Staj süreci boyunca bireysel teknik çalışmaların yanı sıra ekip içi "
        "iletişim, sunum hazırlama ve müşteri karşısında sistemi tanıtma "
        "deneyimleri de yaşanmıştır. Alınan geri bildirimler doğrultusunda "
        "geliştirilen revizyon süreci, gerçek bir yazılım projesinin iteratif "
        "doğasını deneyimleme açısından son derece değerli olmuştur."
    ),
    sp(0.3),
    para(
        "Sonuç olarak bu staj, akademik bilgilerin endüstriyel bir ortamda "
        "nasıl uygulamaya döküldüğünü gösteren, mesleki ve kişisel gelişim "
        "açısından verimli bir deneyim olmuştur. Staj süresince edinilen "
        "bilgi ve beceriler, ilerleyen kariyer sürecinde sağlam bir temel "
        "oluşturmaktadır."
    ),
    PageBreak(),
]

# ══════════════════════════════════════════════════════════════════════════════
# EKLER
# ══════════════════════════════════════════════════════════════════════════════
story += [
    Paragraph("EKLER", h2),
    sp(0.5),
    Paragraph("Ek 1 — Staj Günlükleri Özeti", h_sec),
    sp(0.2),
]

# Özet tablo
ek_data = [
    ["Gün", "Tarih", "Konu Başlığı"],
    ["1",  "13.07.2026", "Oryantasyon ve Geliştirme Ortamı Kurulumu"],
    ["2",  "14.07.2026", "Flowias Web Uygulaması Testi ve Hata Raporlama"],
    ["3",  "16.07.2026", "Flowias Web Uygulaması Farklı Ekran Testleri"],
    ["4",  "17.07.2026", "Flowias Mobil Testi ve Dosya Dağıtım Projesi Araştırması"],
    ["5",  "20.07.2026", "Flowias Mobil Kullanıcı Dokümantasyonunun Güncellenmesi"],
    ["6",  "21.07.2026", "FileTransferAssistant Masaüstü Uygulamasının Geliştirilmesi"],
    ["7",  "22.07.2026", "Flowias Platformuna Eklenebilecek Özelliklerin Değerlendirilmesi"],
    ["8",  "23.07.2026", "Flowias Üzerinde Üretim Senaryosu Tasarımı ve Değerlendirmesi"],
    ["9",  "24.07.2026", "Üretim Sektörüne Ait İş Akışlarının Araştırılması"],
    ["10", "27.07.2026", "Üretim Senaryosuna Ait Veritabanı Tasarımı"],
    ["11", "28.07.2026", "Üretim Senaryosunun Flowias'a Entegrasyonu"],
    ["12", "29.07.2026", "Satın Alma ve Üretim Senaryosu İçin SQL Sorgularının Hazırlanması"],
    ["13", "30.07.2026", "Windows Güvenlik Duvarı Takip ve Uyarı Servisi Geliştirilmesi"],
    ["14", "31.07.2026", "Üretim Senaryosu Veritabanının Genişletilmesi"],
    ["15", "03.08.2026", "SQL Sorgularının Düzenlenmesi ve Veri Kontrolü"],
    ["16", "04.08.2026", "Üretim Senaryosunun Optimizasyonu ve Sunuma Hazırlanması"],
    ["17", "05.08.2026", "Üretim Senaryosunun Ekibe Sunulması"],
    ["18", "06.08.2026", "Sunum Sonrası İstenen Revizyonların Yapılması"],
    ["19", "07.08.2026", "Revize Edilen Üretim Senaryosunun Yeniden Sunumu"],
    ["20", "10.08.2026", "Flowias API İsteklerinin İncelenmesi ve Test Edilmesi"],
    ["21", "11.08.2026", "API Verilerinin Mobil Uygulamada Test Edilmesi"],
    ["22", "12.08.2026", "SQL Sorgu ve Veri Filtreleme Çalışmaları"],
    ["23", "13.08.2026", "SQL Üzerinde JSON Veri Aktarımı ve İşlenmesi"],
    ["24", "14.08.2026", "Üretim Senaryosunun İlerletilmesi Üzerine Fikir Alışverişi"],
    ["25", "17.08.2026", "Üretim Senaryosunda İyileştirme Çalışmaları"],
    ["26", "18.08.2026", "Sistem Kontrolleri ve Akış Testleri"],
    ["27", "19.08.2026", "Flowias Araçlarıyla Dashboard ve Veri Görselleştirme"],
    ["28", "20.08.2026", "Flowias JSON Parser ve Kod Bloklarının Test Edilmesi"],
    ["29", "21.08.2026", "Müşteriden Öneri ve Geri Bildirim Alma Süreci Oluşturulması"],
    ["30", "24.08.2026", "Üretim Senaryosunun Müşteriye Sunulması"],
    ["31", "25.08.2026", "Yapılan Çalışmaların Notlarının ve Dokümantasyonunun Çıkarılması"],
    ["32", "26.08.2026", "Projelerin Teslimi ve Staj Kapanışı"],
]

col_w = [1.0*cm, 2.5*cm, TW - 3.5*cm]
tbl = Table(ek_data, colWidths=col_w, repeatRows=1)
tbl.setStyle(TableStyle([
    ('FONTNAME',  (0,0), (-1,-1), 'DejaVuSerif'),
    ('FONTNAME',  (0,0), (-1,0),  'DejaVuSerif-Bold'),
    ('FONTSIZE',  (0,0), (-1,-1), 10),
    ('LEADING',   (0,0), (-1,-1), 14),
    ('ALIGN',     (0,0), (1,-1),  'CENTER'),
    ('VALIGN',    (0,0), (-1,-1), 'MIDDLE'),
    ('BACKGROUND',(0,0), (-1,0),  colors.HexColor('#d0d0d0')),
    ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, colors.HexColor('#f5f5f5')]),
    ('GRID',      (0,0), (-1,-1), 0.4, colors.black),
    ('TOPPADDING',(0,0), (-1,-1), 4),
    ('BOTTOMPADDING',(0,0), (-1,-1), 4),
    ('LEFTPADDING',(0,0), (-1,-1), 4),
]))
story.append(tbl)

# ── Build PDF ─────────────────────────────────────────────────────────────────
OUT = 'DEU_Staj_Raporu.pdf'

doc = SimpleDocTemplate(
    OUT,
    pagesize=A4,
    leftMargin=LEFT, rightMargin=RIGHT,
    topMargin=TOP,   bottomMargin=BOTTOM,
)

def add_page_number(canvas, doc):
    canvas.saveState()
    canvas.setFont('DejaVuSerif', 10)
    canvas.drawCentredString(PAGE_W / 2, BOTTOM - 0.5*cm, str(doc.page))
    canvas.restoreState()

doc.build(story, onFirstPage=add_page_number, onLaterPages=add_page_number)
print(f"PDF oluşturuldu: {OUT}")