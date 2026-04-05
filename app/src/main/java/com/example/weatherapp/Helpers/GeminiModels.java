package com.example.weatherapp.Helpers;

import java.util.Collections;
import java.util.List;

public class GeminiModels {

    // İSTEK MODELİ (AI'ya ne göndereceğiz?)
    public static class Request {
        public List<Content> contents;
        public Request(String text) {
            this.contents = Collections.singletonList(new Content(text));
        }
    }

    public static class Content {
        public List<Part> parts;
        public Content(String text) {
            this.parts = Collections.singletonList(new Part(text));
        }
    }

    public static class Part {
        public String text;
        public Part(String text) { this.text = text; }
    }

    // CEVAP MODELİ (AI'dan ne gelecek?)
    public static class Response {
        public List<Candidate> candidates;

        public static class Candidate {
            public ContentResponse content;
        }

        public static class ContentResponse {
            public List<Part> parts;
        }

        public String getAiText() {
            try {
                if (candidates != null && !candidates.isEmpty() &&
                        candidates.get(0).content != null &&
                        candidates.get(0).content.parts != null &&
                        !candidates.get(0).content.parts.isEmpty()) {

                    String res = candidates.get(0).content.parts.get(0).text;
                    return (res != null) ? res : "Boş cevap geldi.";
                }
            } catch (Exception e) {
                return "Parse hatası: " + e.getMessage();
            }
            return "AI şu an cevap veremiyor.";
        }
    }
}

