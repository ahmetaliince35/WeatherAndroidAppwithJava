import unicodedata
from fastapi import FastAPI
from playwright.sync_api import sync_playwright

app = FastAPI()

def tr_slugify(text: str) -> str:
    if not text:
        return ""
    text = text.replace("İ", "i").replace("I", "i").replace("ı", "i")
    text = text.lower()
    text = unicodedata.normalize('NFD', text)
    text = ''.join(c for c in text if unicodedata.category(c) != 'Mn')
    return text.strip()

def get_weather(city, town):
    clean_city = tr_slugify(city)
    clean_town = tr_slugify(town)

    with sync_playwright() as p:
        browser = playwright.chromium.launch(headless=True)

        context = browser.new_context(
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            viewport={'width': 1920, 'height': 1080},
            locale="tr-TR"
        )

        page = context.new_page()

        # Steer clear of automation detection (Olası ek bot korumalarını aşmak için)
        page.add_init_script("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})")

        # Yönlendirmeyi denerken timeout ve wait_until değerini esnetin:
        page.goto(
            f"https://www.mgm.gov.tr/tahmin/il-ve-ilceler.aspx?il={city}&ilce={town}",
            wait_until="commit", # "domcontentloaded" yerine "commit" verinin ilk geldiği anı yakalar
            timeout=30000
        )

            return {
                "city": city + "/" + town,
                "temperature": page.locator(".anlik-sicaklik-deger").first.inner_text().strip() if page.locator(".anlik-sicaklik-deger").count() > 0 else "0",
                "humidity": page.locator(".anlik-nem-deger-kac").first.inner_text().strip() if page.locator(".anlik-nem-deger-kac").count() > 0 else "0",
                "pressure": page.locator(".anlik-dibasinc-deger-kac").first.inner_text().strip() if page.locator(".anlik-dibasinc-deger-kac").count() > 0 else "0",
                "precipitation": page.locator(".anlik-yagis-deger-kac").first.inner_text().strip() if page.locator(".anlik-yagis-deger-kac").count() > 0 else "0",
                "windDirection": page.locator(".anlik-ruzgar-ikon").first.get_attribute("title") if page.locator(".anlik-ruzgar-ikon").count() > 0 else "0",
                "windSpeed": page.locator(".anlik-ruzgar-deger-kac").first.inner_text().strip() if page.locator(".anlik-ruzgar-deger-kac").count() > 0 else "0",
                "weatherStatus": page.locator(".imgAD").first.get_attribute("title") if page.locator(".imgAD").count() > 0 else "0"
            }
        finally:
            browser.close()

@app.get("/weather")
def weather(city: str, town: str):
    return get_weather(city, town)