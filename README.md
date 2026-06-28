# 🎵 PahamLirik — AI-Powered Song Lyrics Translator & Analyzer

**PahamLirik** adalah aplikasi web publik (tanpa autentikasi) berbasis Spring Boot yang memungkinkan pengguna memasukkan lirik lagu berbahasa asing untuk diterjemahkan dan dianalisis oleh AI (Gemini / OpenAI). Hasil akhir mencakup terjemahan per baris, analisis makna lagu, rangkuman cerita lagu, dan riwayat pencarian publik.

---

## 🚀 Fitur Utama

- **Terjemahan Baris per Baris**: Lirik asli dan hasil terjemahan Bahasa Indonesia ditampilkan berdampingan dalam grid visual yang bersih.
- **Analisis Makna Lagu**: Penjelasan mendalam dari AI mengenai pesan emosional, metafora, dan makna keseluruhan lagu.
- **Rangkuman Cerita**: Narasi ringkas yang menggambarkan alur cerita di balik bait lirik lagu.
- **Riwayat Terjemahan Publik**: Halaman utama menampilkan daftar lagu terjemahan terbaru secara dinamis.
- **Local DB Caching**: Lirik lagu yang sama tidak akan memicu panggilan API AI ulang. Hasil pencarian disimpan di MySQL dan dicari berdasarkan hash SHA-256 untuk memangkas latency serta menghemat kuota token API.
- **Desain Premium**: Tampilan visual mengusung tema gelap (dark mode), efek glassmorphism, gradasi warna vibran, dan loading overlay interaktif.

---

## 🛠 Tech Stack

- **Backend**: Spring Boot 3.3.4 (Java 17)
- **Database**: MySQL 8.0 (Containerized)
- **ORM & Data Access**: Spring Data JPA / Hibernate
- **UI Template**: Thymeleaf
- **Styling**: Tailwind CSS (Play CDN v3) & Custom CSS
- **AI Integrations**: Gemini API (Primary) / OpenAI API (Fallback)
- **Containerization**: Docker & Docker Compose

---

## 📋 Prerequisites

Pastikan perangkat Anda telah memasang:
- **Docker Desktop** (direkomendasikan karena build dan eksekusi sepenuhnya dikemas dalam kontainer)
- **Maven** & **JDK 17** (jika ingin membangun dan menjalankan di luar kontainer)

---

## ⚙ Setup & Run

### 1. Salin Environment Variables
Duplikat file `.env.example` menjadi `.env` di direktori utama proyek:
```bash
cp .env.example .env
```

### 2. Konfigurasi API Key
Buka file `.env` dan masukkan API Key AI Provider Anda:
```env
# Database
MYSQL_ROOT_PASSWORD=rootpassword
MYSQL_DATABASE=paham_lirik
MYSQL_USER=pahamlirik
MYSQL_PASSWORD=pahamlirik123

# AI Provider (pilih 'gemini' atau 'openai')
AI_PROVIDER=gemini
GEMINI_API_KEY=AIzaSy...your_gemini_api_key...
OPENAI_API_KEY=sk-...your_openai_api_key...
```

### 3. Jalankan Aplikasi dengan Docker Compose
Jalankan perintah berikut untuk mengunduh database MySQL, mengompilasi kode Java di dalam build container, dan menjalankan aplikasi secara lokal:
```bash
docker-compose up --build -d
```

Setelah kontainer berhasil berjalan:
- **Aplikasi Web**: Buka [http://localhost:8080](http://localhost:8080) di peramban Anda.
- **Database MySQL**: Tersedia di port `3306` (host: `localhost`).

### 4. Menghentikan Layanan
Untuk menghentikan semua kontainer:
```bash
docker-compose down
```

---

## 📁 Struktur Proyek

```
paham-lirik/
├── docker-compose.yml              # Konfigurasi Layanan Docker (App + DB)
├── Dockerfile                      # Multi-stage Docker build file (Maven + JRE)
├── pom.xml                         # Dependensi Maven
├── src/main/java/com/pahamlirik/
│   ├── PahamLirikApplication.java  # Bootstrapper Spring Boot
│   ├── config/                     # Konfigurasi Timeout & Pemilihan AI Bean
│   ├── controller/                 # Web MVC Controllers (Home & Translation)
│   ├── dto/                        # Data Transfer Objects
│   ├── entity/                     # JPA Persisten Entity
│   ├── exception/                  # Global Exception Handling & custom exceptions
│   ├── repository/                 # Database Query Repository
│   └── service/                    # Business Logic Orchestrator & AI Provider Impls
└── src/main/resources/
    ├── application.properties      # Properties dengan default env vars
    ├── static/                     # Aset Statis (CSS custom & JS loading)
    └── templates/                  # Thymeleaf templates (Index, Result, Error)
```

---

## 🧪 Pengujian (Testing)

Untuk menjalankan pengujian unit dan integrasi secara containerized (tanpa perlu memasang Java 17 secara lokal):
```bash
docker run --rm -v "${PWD}:/app" -w /app maven:3.9-eclipse-temurin-17-alpine mvn clean test
```
