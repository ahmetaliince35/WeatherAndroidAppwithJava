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
        browser = p.chromium.launch(headless=True)
        try:
            page = browser.new_page(
                user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/138.0.0.0 Safari/537.36"
            )
            # URL'de il ve ilce parametreleri arasına & koyuldu ve karakterler duzeltildi
            page.goto(
                f"https://www.mgm.gov.tr/tahmin/il-ve-ilceler.aspx?il={clean_city}&ilce={clean_town}",
                wait_until="domcontentloaded",
                timeout=60000
            )
            page.wait_for_timeout(3000)

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