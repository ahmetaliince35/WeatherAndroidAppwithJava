from playwright.sync_api import sync_playwright, TimeoutError as PlaywrightTimeoutError
from fastapi import FastAPI, HTTPException
import random

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

            page.wait_for_timeout(
                random.randint(2000, 4000)
            )

            print("MGM URL:", page.url)

            # Sıcaklık ana veri olduğu için gelmesini bekliyoruz.
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
            # Yardımcı fonksiyonlar
            # --------------------------------------------------

            def get_text(selector: str, default: str = "Veri Yok"):

                try:
                    value = page.locator(selector).first.inner_text().strip()

                    if not value:
                        return default

                    return value

                except Exception:
                    return default

            def get_attribute(
                selector: str,
                attribute: str,
                default: str = "Veri Yok"
            ):

                try:
                    value = page.locator(
                        selector
                    ).first.get_attribute(attribute)

                    if value is None or not value.strip():
                        return default

                    return value.strip()

                except Exception:
                    return default

            # --------------------------------------------------
            # Verileri al
            # --------------------------------------------------

            # Numeric değerler
            temperature = get_text(
                ".anlik-sicaklik-deger",
                "0"
            )

            humidity = get_text(
                ".anlik-nem-deger-kac",
                "0"
            )

            pressure = get_text(
                ".anlik-dibasinc-deger-kac",
                "0"
            )

            precipitation = get_text(
                ".anlik-yagis-deger-kac",
                "0"
            )

            wind_speed = get_text(
                ".anlik-ruzgar-deger-kac",
                "0"
            )

            # String değerler
            wind_direction = get_attribute(
                ".anlik-ruzgar-ikon",
                "title",
                "Veri Yok"
            )

            weather_status = get_attribute(
                ".imgAD",
                "title",
                "Veri Yok"
            )

            # --------------------------------------------------
            # Sonuç
            # --------------------------------------------------

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