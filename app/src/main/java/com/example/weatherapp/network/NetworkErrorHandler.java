package com.example.weatherapp.network;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class NetworkErrorHandler {

    public static String formatVolleyError(VolleyError error) {
        if (error == null) {
            return "Bilinmeyen bir hata oluştu.";
        }

        if (error instanceof TimeoutError) {
            return "İstek zaman aşımına uğradı. Lütfen internet bağlantınızı kontrol edip tekrar deneyin.";
        }

        if (error instanceof NoConnectionError || error.getCause() instanceof UnknownHostException || error.getCause() instanceof ConnectException) {
            return "İnternet bağlantısı bulunamadı. Lütfen ağınızı kontrol edin.";
        }

        if (error instanceof AuthFailureError) {
            return "Yetkilendirme hatası: API anahtarı geçersiz veya yetkiniz bulunmuyor.";
        }

        if (error instanceof ParseError) {
            return "Veri işleme hatası: Sunucudan gelen yanıt ayrıştırılamadı.";
        }

        if (error instanceof ServerError || error.networkResponse != null) {
            int statusCode = error.networkResponse != null ? error.networkResponse.statusCode : 0;
            switch (statusCode) {
                case 400:
                    return "Geçersiz istek. Lütfen şehir adını kontrol edin.";
                case 401:
                case 403:
                    return "Erişim reddedildi. API anahtarı kontrol edilmelidir.";
                case 404:
                    return "Aradığınız şehir bulunamadı. Lütfen adı doğru yazdığınızdan emin olun.";
                case 429:
                    return "İstek limiti aşıldı. Lütfen bir süre sonra tekrar deneyin.";
                case 500:
                case 502:
                case 503:
                case 504:
                    return "Hava durumu sağlayıcısının sunucularında geçici bir aksaklık var. Lütfen daha sonra tekrar deneyin.";
                default:
                    if (statusCode > 0) {
                        return "Sunucu hatası: HTTP " + statusCode;
                    }
                    return "Sunucu kaynaklı bir hata oluştu.";
            }
        }

        if (error instanceof NetworkError) {
            return "Ağ hatası oluştu. Lütfen bağlantınızı kontrol edin.";
        }

        if (error.getMessage() != null && !error.getMessage().trim().isEmpty()) {
            return error.getMessage();
        }

        return "Hava durumu bilgisi alınırken bir hata oluştu.";
    }

    public static String formatThrowable(Throwable throwable) {
        if (throwable == null) {
            return "Bilinmeyen bir hata oluştu.";
        }

        if (throwable instanceof SocketTimeoutException) {
            return "İstek zaman aşımına uğradı. Lütfen tekrar deneyin.";
        }

        if (throwable instanceof UnknownHostException || throwable instanceof ConnectException) {
            return "İnternet bağlantısı bulunamadı.";
        }

        String msg = throwable.getMessage();
        return (msg != null && !msg.trim().isEmpty()) ? msg : "Bir hata meydana geldi.";
    }
}
