from playwright.sync_api import sync_playwright

def get_weather(city,town):

    with sync_playwright() as p:

        browser = p.chromium.launch(headless=True)
        try:
            page = browser.new_page(
                user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/138.0.0.0 Safari/537.36"
            )
            page.goto(f"https://www.mgm.gov.tr/tahmin/il-ve-ilceler.aspx?il={city}&ilce={town}",
                      wait_until="domcontentloaded",
                      timeout=60000)
            page.wait_for_timeout(3000)


            print(page.url)
            print(page.locator(".anlik-sicaklik-deger" ).count())

            return {
                "city": city +"/"+ town,
                "temperature": page.locator(".anlik-sicaklik-deger").first.inner_text().strip() or "0",
                "humidity": page.locator(".anlik-nem-deger-kac").first.inner_text().strip() or "0",
                "pressure": page.locator(".anlik-dibasinc-deger-kac").first.inner_text().strip() or "0",
                "precipitation": page.locator(".anlik-yagis-deger-kac").first.inner_text().strip() or "0",
                "windDirection": page.locator(".anlik-ruzgar-ikon").get_attribute("title") or "0",
                "windSpeed": page.locator(".anlik-ruzgar-deger-kac").first.inner_text().strip() or "0",
                "weatherStatus": page.locator(".imgAD").get_attribute("title") or "0"
            }
        finally:
            browser.close()
from fastapi import FastAPI

app = FastAPI()
@app.get("/weather")
def weather(city:str,town:str):

    return get_weather(city,town)
