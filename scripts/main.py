from playwright.sync_api import sync_playwright, TimeoutError as PlaywrightTimeoutError
from fastapi import FastAPI, HTTPException
import random
import time

app = FastAPI()


def get_weather(city: str, town: str):

    with sync_playwright() as p:

        browser = p.chromium.launch(
            headless=True,
            args=[
                "--disable-blink-features=AutomationControlled",
                "--disable-dev-shm-usage",
                "--no-sandbox",
                "--disable-gpu",
            ]
        )

        try:

            context = browser.new_context(
                user_agent=(
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                    "AppleWebKit/537.36 (KHTML, like Gecko) "
                    "Chrome/138.0.0.0 Safari/537.36"
                ),
                viewport={
                    "width": 1920,
                    "height": 1080
                },
                locale="tr-TR",
                timezone_id="Europe/Istanbul",
                extra_http_headers={
                    "Accept-Language": "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
                    "Accept": (
                        "text/html,application/xhtml+xml,"
                        "application/xml;q=0.9,image/avif,image/webp,"
                        "image/apng,*/*;q=0.8"
                    ),
                    "Upgrade-Insecure-Requests": "1",
                }
            )

            page = context.new_page()

            # webdriver bilgisini gizle
            page.add_init_script("""
                Object.defineProperty(
                    navigator,
                    'webdriver',
                    {
                        get: () => undefined
                    }
                );
            """)

            url = (
                "https://www.mgm.gov.tr/tahmin/"
                f"il-ve-ilceler.aspx?il={city}&ilce={town}"
            )

            page.goto(
                url,
                wait_until="domcontentloaded",
                timeout=60000
            )

            # Sayfanın tamamen yüklenmesi için biraz bekle
            page.wait_for_timeout(
                random.randint(2000, 4000)
            )

            print("MGM URL:", page.url)

            # Hava durumu elemanının gelmesini bekle
            try:
                page.locator(
                    ".anlik-sicaklik-deger"
                ).first.wait_for(
                    state="visible",
                    timeout=15000
                )

            except PlaywrightTimeoutError:

                print("Hava durumu elementi bulunamadı.")
                print("Sayfa başlığı:", page.title())

                raise HTTPException(
                    status_code=503,
                    detail="MGM hava durumu verisi şu anda alınamıyor."
                )

            # --------------------------------------------------
            # Verileri oku
            # --------------------------------------------------

            temperature = (
                page.locator(
                    ".anlik-sicaklik-deger"
                ).first.inner_text().strip()
                or "0"
            )

            humidity = (
                page.locator(
                    ".anlik-nem-deger-kac"
                ).first.inner_text().strip()
                or "0"
            )

            pressure = (
                page.locator(
                    ".anlik-dibasinc-deger-kac"
                ).first.inner_text().strip()
                or "0"
            )

            precipitation = (
                page.locator(
                    ".anlik-yagis-deger-kac"
                ).first.inner_text().strip()
                or "0"
            )

            wind_direction = (
                page.locator(
                    ".anlik-ruzgar-ikon"
                ).get_attribute("title")
                or "0"
            )

            wind_speed = (
                page.locator(
                    ".anlik-ruzgar-deger-kac"
                ).first.inner_text().strip()
                or "0"
            )

            weather_status = (
                page.locator(
                    ".imgAD"
                ).get_attribute("title")
                or "0"
            )

            return {
                "city": f"{city}/{town}",
                "temperature": temperature,
                "humidity": humidity,
                "pressure": pressure,
                "precipitation": precipitation,
                "windDirection": wind_direction,
                "windSpeed": wind_speed,
                "weatherStatus": weather_status
            }

        except PlaywrightTimeoutError:

            raise HTTPException(
                status_code=504,
                detail="MGM sunucusundan cevap alınamadı."
            )

        except HTTPException:
            raise

        except Exception as e:

            print("MGM Hatası:", str(e))

            raise HTTPException(
                status_code=500,
                detail=f"Hava durumu alınırken hata oluştu: {str(e)}"
            )

        finally:

            browser.close()


@app.get("/weather")
def weather(city: str, town: str):

    return get_weather(city, town)