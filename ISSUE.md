# 🎵 PahamLirik — AI-Powered Song Lyrics Translator & Analyzer

## 📋 Project Overview

**PahamLirik** adalah aplikasi web publik (tanpa autentikasi) yang memungkinkan pengguna memasukkan lirik lagu berbahasa asing untuk diterjemahkan dan dianalisis oleh AI (Gemini/OpenAI). Hasil akhir mencakup **terjemahan per baris**, **analisis makna**, **rangkuman cerita lagu**, dan **riwayat pencarian publik**.

### Tujuan
Menyediakan antarmuka sederhana dan user-friendly untuk menerjemahkan & menganalisis lirik lagu menggunakan kemampuan LLM.

### Metrik Keberhasilan
- ✅ Aplikasi dapat memproses input lirik dan menampilkan hasil secara terstruktur (baris per baris)
- ✅ Menyimpan riwayat ke database dengan aman tanpa timeout
- ✅ Respons AI ditampilkan dalam format yang rapi dan mudah dibaca
- ✅ Halaman memiliki loading state yang informatif saat menunggu respons AI

---

## 🛠 Tech Stack

| Layer | Teknologi |
|-------|-----------|
| **Backend** | Spring Boot 3.x (Java 17+) |
| **Web Framework** | Spring MVC |
| **Template Engine** | Thymeleaf |
| **Database** | MySQL 8.x |
| **ORM** | Spring Data JPA / Hibernate |
| **Utility** | Lombok |
| **Styling** | Tailwind CSS (via CDN) |
| **AI Provider** | Gemini API (primary) / OpenAI API (fallback) |
| **Build Tool** | Maven |
| **Migration** | Flyway (opsional, recommended) |

---

## 🎨 UI Design Prototype

### Halaman Utama (Input Lirik + Riwayat)

![PahamLirik Homepage](C:\Users\User\.gemini\antigravity\brain\24db3d16-223e-401b-abb5-04d170285c84\pahamlirik_homepage_1782630928726.png)

### Halaman Hasil Terjemahan & Analisis

![PahamLirik Result Page](C:\Users\User\.gemini\antigravity\brain\24db3d16-223e-401b-abb5-04d170285c84\pahamlirik_result_page_1782630940031.png)

---

## 🏗 Arsitektur Aplikasi

```
┌─────────────────────────────────────────────────────┐
│                    Browser (User)                    │
│                  Thymeleaf + Tailwind                │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP (Form Submit / AJAX)
                       ▼
┌─────────────────────────────────────────────────────┐
│              Spring MVC Controller                   │
│         LyricController / HistoryController          │
└──────────┬──────────────────────┬───────────────────┘
           │                      │
           ▼                      ▼
┌──────────────────┐   ┌──────────────────────────────┐
│   LyricService   │   │      HistoryService          │
│ (Business Logic)  │   │  (Riwayat CRUD)             │
└────────┬─────────┘   └──────────┬───────────────────┘
         │                         │
         ▼                         ▼
┌──────────────────┐   ┌──────────────────────────────┐
│  AI Service      │   │   TranslationRepository      │
│ (Gemini/OpenAI)  │   │   (Spring Data JPA)          │
└──────────────────┘   └──────────┬───────────────────┘
                                  │
                                  ▼
                       ┌──────────────────────┐
                       │       MySQL DB        │
                       │  (translations table) │
                       └──────────────────────┘
```

---

## 🗄 Database Schema

### Tabel: `translations`

```sql
CREATE TABLE translations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    song_title      VARCHAR(255),
    artist          VARCHAR(255),
    original_lyrics TEXT NOT NULL,
    translated_text TEXT NOT NULL COMMENT 'JSON: array of {original, translated} per line',
    meaning_analysis TEXT COMMENT 'Analisis makna lagu secara keseluruhan',
    story_summary   TEXT COMMENT 'Rangkuman cerita/narasi lagu',
    source_language VARCHAR(50) COMMENT 'Bahasa asal lirik (auto-detected)',
    target_language VARCHAR(50) DEFAULT 'id' COMMENT 'Bahasa target terjemahan',
    ai_provider     VARCHAR(50) COMMENT 'gemini atau openai',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_created_at (created_at DESC),
    INDEX idx_song_title (song_title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

> **Catatan**: `translated_text` disimpan sebagai JSON string berisi array terjemahan per baris. Format:
> ```json
> [
>   {"original": "When you try your best but you don't succeed", "translated": "Saat kamu berusaha sebaik mungkin tapi tidak berhasil"},
>   {"original": "When you get what you want but not what you need", "translated": "Saat kamu mendapatkan apa yang kamu mau tapi bukan yang kamu butuhkan"}
> ]
> ```

---

## 📁 Project Structure

```
paham-lirik/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/pahamlirik/
│   │   │   ├── PahamLirikApplication.java            # Main Spring Boot App
│   │   │   ├── config/
│   │   │   │   ├── AiConfig.java                     # AI API key & client config
│   │   │   │   └── WebConfig.java                    # (opsional) CORS, dll.
│   │   │   ├── controller/
│   │   │   │   ├── HomeController.java               # GET / → halaman utama
│   │   │   │   └── TranslationController.java        # POST /translate, GET /result/{id}
│   │   │   ├── dto/
│   │   │   │   ├── TranslationRequest.java           # Form input DTO
│   │   │   │   ├── TranslationResponse.java          # Response DTO untuk view
│   │   │   │   ├── LyricLine.java                    # {original, translated} per baris
│   │   │   │   └── AiAnalysisResult.java             # Parsed AI response
│   │   │   ├── entity/
│   │   │   │   └── Translation.java                  # JPA Entity
│   │   │   ├── repository/
│   │   │   │   └── TranslationRepository.java        # Spring Data JPA Repo
│   │   │   ├── service/
│   │   │   │   ├── TranslationService.java           # Business logic orchestrator
│   │   │   │   ├── AiService.java                    # Interface for AI providers
│   │   │   │   ├── GeminiService.java                # Gemini API implementation
│   │   │   │   └── OpenAiService.java                # OpenAI API implementation
│   │   │   └── exception/
│   │   │       ├── AiServiceException.java           # Custom exception
│   │   │       └── GlobalExceptionHandler.java       # @ControllerAdvice
│   │   └── resources/
│   │       ├── application.properties                # Config & env variables
│   │       ├── templates/
│   │       │   ├── index.html                        # Halaman utama (input + riwayat)
│   │       │   ├── result.html                       # Halaman hasil terjemahan
│   │       │   ├── error.html                        # Halaman error
│   │       │   └── fragments/
│   │       │       ├── header.html                   # Navbar fragment
│   │       │       └── footer.html                   # Footer fragment
│   │       └── static/
│   │           ├── css/
│   │           │   └── custom.css                    # CSS tambahan (jika perlu)
│   │           └── js/
│   │               └── app.js                        # Client-side JS (loading state, dll)
│   └── test/
│       └── java/com/pahamlirik/
│           ├── service/
│           │   └── TranslationServiceTest.java       # Unit tests
│           └── controller/
│               └── TranslationControllerTest.java    # Integration tests
├── .env.example                                      # Template environment variables
├── README.md
└── ISSUE.md                                          # ← File ini
```

---

## 🤖 AI Prompt Engineering

### Prompt Template untuk Gemini/OpenAI

```
Kamu adalah seorang penerjemah dan analis lirik lagu profesional.

Diberikan lirik lagu berikut:
---
{lyrics_input}
---

Tugas kamu:
1. **Deteksi bahasa** lirik tersebut.
2. **Identifikasi** judul lagu dan artis (jika bisa dikenali dari liriknya). Jika tidak bisa, tulis "Tidak Diketahui".
3. **Terjemahkan setiap baris** lirik ke Bahasa Indonesia. Pertahankan urutan baris. Baris kosong tetap jadi baris kosong.
4. **Analisis makna** lagu secara keseluruhan (2-4 paragraf).
5. **Rangkum cerita** lagu dalam bentuk narasi singkat (1-2 paragraf).

Berikan respons dalam format JSON berikut (TANPA markdown code block, langsung JSON):
{
  "song_title": "...",
  "artist": "...",
  "source_language": "...",
  "line_translations": [
    {"original": "baris asli 1", "translated": "terjemahan 1"},
    {"original": "baris asli 2", "translated": "terjemahan 2"}
  ],
  "meaning_analysis": "...",
  "story_summary": "..."
}
```

> **Tips untuk implementor:**
> - Set `temperature: 0.3` untuk hasil yang konsisten
> - Set `max_tokens: 4096` agar tidak terpotong pada lirik panjang
> - Parse JSON response dengan `ObjectMapper` (Jackson) 
> - Jika parsing gagal, coba extract JSON dari markdown code block (```json ... ```)

---

## 📌 Implementation Tasks (Issues)

Tugas-tugas di bawah ini diurutkan berdasarkan **dependency** dan harus dikerjakan secara berurutan. Setiap task berdiri sendiri dan bisa di-assign sebagai issue terpisah.

---

### Issue #1: 🏗 Project Setup & Configuration
**Priority:** 🔴 Critical | **Estimate:** 1-2 jam

**Deskripsi:**
Inisialisasi project Spring Boot dengan semua dependencies yang diperlukan.

**Acceptance Criteria:**
- [ ] Project Spring Boot 3.x dengan Java 17+ berhasil dibuat via [Spring Initializr](https://start.spring.io/)
- [ ] Dependencies di `pom.xml`:
  - `spring-boot-starter-web`
  - `spring-boot-starter-thymeleaf`
  - `spring-boot-starter-data-jpa`
  - `mysql-connector-j`
  - `lombok`
  - `spring-boot-starter-validation`
  - `spring-boot-starter-test`
  - `com.google.code.gson:gson` atau Jackson (sudah included)
- [ ] File `application.properties` terkonfigurasi:
  ```properties
  # Server
  server.port=8080
  
  # Database
  spring.datasource.url=jdbc:mysql://localhost:3306/paham_lirik?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jakarta
  spring.datasource.username=${DB_USERNAME:root}
  spring.datasource.password=${DB_PASSWORD:}
  spring.jpa.hibernate.ddl-auto=update
  spring.jpa.show-sql=true
  spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
  
  # AI Config
  ai.provider=${AI_PROVIDER:gemini}
  ai.gemini.api-key=${GEMINI_API_KEY:}
  ai.gemini.model=gemini-2.0-flash
  ai.openai.api-key=${OPENAI_API_KEY:}
  ai.openai.model=gpt-4o-mini
  
  # Timeout
  ai.request.timeout=60000
  ```
- [ ] File `.env.example` dibuat sebagai template
- [ ] File `.gitignore` mencakup `.env`, `target/`, IDE files
- [ ] Database `paham_lirik` dibuat di MySQL
- [ ] Aplikasi berhasil dijalankan tanpa error (`mvn spring-boot:run`)

---

### Issue #2: 🗄 Entity, Repository & DTO
**Priority:** 🔴 Critical | **Estimate:** 1-2 jam

**Deskripsi:**
Buat JPA Entity, Repository, dan DTO classes sesuai database schema.

**Acceptance Criteria:**
- [ ] `Translation.java` entity:
  ```java
  @Entity
  @Table(name = "translations")
  @Data @NoArgsConstructor @AllArgsConstructor @Builder
  public class Translation {
      @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;
      
      private String songTitle;
      private String artist;
      
      @Column(columnDefinition = "TEXT", nullable = false)
      private String originalLyrics;
      
      @Column(columnDefinition = "TEXT", nullable = false)
      private String translatedText;  // JSON string
      
      @Column(columnDefinition = "TEXT")
      private String meaningAnalysis;
      
      @Column(columnDefinition = "TEXT")
      private String storySummary;
      
      private String sourceLanguage;
      private String targetLanguage;
      private String aiProvider;
      
      @Column(updatable = false)
      @CreationTimestamp
      private LocalDateTime createdAt;
  }
  ```
- [ ] `TranslationRepository.java`:
  ```java
  public interface TranslationRepository extends JpaRepository<Translation, Long> {
      List<Translation> findTop20ByOrderByCreatedAtDesc();
      Optional<Translation> findById(Long id);
  }
  ```
- [ ] DTO classes:
  - `TranslationRequest.java` — fields: `lyrics` (required, @NotBlank)
  - `LyricLine.java` — fields: `original`, `translated`
  - `AiAnalysisResult.java` — fields: `songTitle`, `artist`, `sourceLanguage`, `lineTranslations` (List\<LyricLine\>), `meaningAnalysis`, `storySummary`
  - `TranslationResponse.java` — mengandung semua data untuk view layer

---

### Issue #3: 🤖 AI Service Layer (Gemini + OpenAI)
**Priority:** 🔴 Critical | **Estimate:** 2-3 jam

**Deskripsi:**
Implementasi service layer untuk berkomunikasi dengan AI API (Gemini sebagai primary, OpenAI sebagai fallback).

**Acceptance Criteria:**
- [ ] `AiService.java` interface:
  ```java
  public interface AiService {
      AiAnalysisResult analyzeLyrics(String lyrics) throws AiServiceException;
  }
  ```
- [ ] `GeminiService.java`:
  - Menggunakan `RestTemplate` atau `WebClient` untuk call Gemini API
  - Endpoint: `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`
  - Request body sesuai Gemini API format
  - Parse JSON response dari AI menjadi `AiAnalysisResult`
  - Handle timeout (60 detik)
  - Handle rate limiting (retry 1x setelah delay 2 detik)
  - Logging request/response untuk debugging
- [ ] `OpenAiService.java`:
  - Menggunakan `RestTemplate` untuk call OpenAI Chat Completions API
  - Endpoint: `https://api.openai.com/v1/chat/completions`
  - Sama structure dengan GeminiService
- [ ] `AiConfig.java`:
  - Bean configuration untuk memilih provider berdasarkan `ai.provider` property
  - `RestTemplate` bean dengan timeout configuration
- [ ] `AiServiceException.java` custom exception
- [ ] Prompt template disimpan sebagai constant atau di `application.properties`
- [ ] JSON parsing dengan fallback (handle case di mana AI mengembalikan markdown code block)

**Detail Teknis — Gemini API Call:**
```java
// POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key={API_KEY}
// Headers: Content-Type: application/json
// Body:
{
  "contents": [{
    "parts": [{"text": "{prompt_with_lyrics}"}]
  }],
  "generationConfig": {
    "temperature": 0.3,
    "maxOutputTokens": 4096,
    "responseMimeType": "application/json"
  }
}
```

**Detail Teknis — OpenAI API Call:**
```java
// POST https://api.openai.com/v1/chat/completions
// Headers: Authorization: Bearer {API_KEY}, Content-Type: application/json
// Body:
{
  "model": "gpt-4o-mini",
  "messages": [
    {"role": "system", "content": "Kamu adalah penerjemah lirik lagu profesional."},
    {"role": "user", "content": "{prompt_with_lyrics}"}
  ],
  "temperature": 0.3,
  "max_tokens": 4096,
  "response_format": {"type": "json_object"}
}
```

---

### Issue #4: 📊 Translation Service (Business Logic)
**Priority:** 🔴 Critical | **Estimate:** 1-2 jam

**Deskripsi:**
Buat service layer yang mengorkestrasikan proses terjemahan: menerima input → memanggil AI → parsing → simpan ke DB → return response.

**Acceptance Criteria:**
- [ ] `TranslationService.java`:
  ```java
  @Service
  public class TranslationService {
      
      // Menerima lyrics string, memanggil AiService, parsing hasilnya,
      // menyimpan ke database, dan mengembalikan TranslationResponse
      public TranslationResponse translateAndAnalyze(String lyrics);
      
      // Mengambil riwayat terjemahan terbaru (max 20)
      public List<TranslationResponse> getRecentTranslations();
      
      // Mengambil detail satu terjemahan berdasarkan ID
      public TranslationResponse getTranslationById(Long id);
  }
  ```
- [ ] Method `translateAndAnalyze`:
  1. Validasi input (tidak kosong, max 10.000 karakter)
  2. Call `AiService.analyzeLyrics(lyrics)`
  3. Convert `AiAnalysisResult` ke `Translation` entity
  4. Serialize `lineTranslations` ke JSON string untuk disimpan di `translated_text`
  5. Save ke database via `TranslationRepository`
  6. Return `TranslationResponse` untuk view
- [ ] Error handling: jika AI gagal, throw exception dengan pesan yang user-friendly
- [ ] Logging di setiap step penting

---

### Issue #5: 🎮 Controller Layer
**Priority:** 🔴 Critical | **Estimate:** 1-2 jam

**Deskripsi:**
Implementasi Spring MVC controllers untuk menangani HTTP requests.

**Acceptance Criteria:**
- [ ] `HomeController.java`:
  ```java
  @Controller
  public class HomeController {
      
      @GetMapping("/")
      public String home(Model model) {
          // Load recent translations untuk riwayat
          // Add ke model
          // Return "index" template
      }
  }
  ```
- [ ] `TranslationController.java`:
  ```java
  @Controller
  @RequestMapping("/translate")
  public class TranslationController {
      
      @PostMapping
      public String translate(@Valid TranslationRequest request,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
          // Validasi input
          // Call TranslationService
          // Redirect ke /translate/result/{id}
      }
      
      @GetMapping("/result/{id}")
      public String result(@PathVariable Long id, Model model) {
          // Load translation by ID
          // Deserialize JSON translations
          // Add ke model
          // Return "result" template
      }
  }
  ```
- [ ] `GlobalExceptionHandler.java`:
  - Handle `AiServiceException` → tampilkan error page dengan pesan
  - Handle `MethodArgumentNotValidException` → redirect back dengan error message
  - Handle generic `Exception` → error page generik

---

### Issue #6: 🎨 Frontend — Halaman Utama (index.html)
**Priority:** 🔴 Critical | **Estimate:** 2-3 jam

**Deskripsi:**
Buat halaman utama menggunakan Thymeleaf + Tailwind CSS (via CDN). Lihat prototype di atas.

**Acceptance Criteria:**
- [ ] **Navbar**: Logo PahamLirik (icon musik + teks) di kiri, warna indigo/purple
- [ ] **Hero Section**:
  - Heading besar: "Apa lagu yang sedang kamu dengarkan?"
  - Subheading: "Paste liriknya di bawah, biar AI kami yang cari tahu dan jelaskan artinya."
  - Textarea besar dengan placeholder "Paste lirik lagu di sini..."
  - Tombol "Terjemahkan & Analisis" full-width, warna indigo/purple
  - Textarea minimum 8 baris tinggi
- [ ] **Riwayat Section**:
  - Heading: "Riwayat Terjemahan"
  - List card riwayat (song title + artist + tanggal)
  - Setiap card clickable → navigasi ke `/translate/result/{id}`
  - Jika belum ada riwayat, tampilkan pesan "Belum ada riwayat terjemahan"
- [ ] **Loading State**:
  - Saat form disubmit, tampilkan overlay loading spinner
  - Tombol berubah menjadi disabled dengan teks "Sedang menerjemahkan..."
  - Implementasi via JavaScript (`app.js`)
- [ ] **Validasi Client-side**:
  - Textarea tidak boleh kosong
  - Minimal 10 karakter
  - Tampilkan pesan error di bawah textarea
- [ ] **Responsive**: Tampilan baik di desktop dan mobile
- [ ] **Tailwind CDN** di `<head>`:
  ```html
  <script src="https://cdn.tailwindcss.com"></script>
  ```
- [ ] **Fragment** `header.html` dan `footer.html` digunakan via `th:replace`

**Referensi Warna:**
| Element | Tailwind Class |
|---------|---------------|
| Primary Button | `bg-indigo-600 hover:bg-indigo-700` |
| Heading | `text-gray-900` |
| Subheading | `text-gray-500` |
| Card Background | `bg-white` |
| Page Background | `bg-gray-50` |
| Accent | `text-indigo-600` |

---

### Issue #7: 🎨 Frontend — Halaman Hasil (result.html)
**Priority:** 🔴 Critical | **Estimate:** 2-3 jam

**Deskripsi:**
Buat halaman hasil terjemahan dan analisis menggunakan Thymeleaf + Tailwind CSS.

**Acceptance Criteria:**
- [ ] **Header Lagu**:
  - Card dengan border kiri indigo
  - Judul lagu (besar, bold)
  - Nama artis (gray, lebih kecil)
  - Bahasa asal (badge/pill)
  - Tanggal terjemahan
- [ ] **Terjemahan Per Baris**:
  - Tabel atau card list dengan 2 kolom: Lirik Asli | Terjemahan
  - Baris genap/ganjil beda warna (zebra striping)
  - Baris kosong (separator antar verse) tetap ditampilkan
  - Scroll horizontal di mobile jika terlalu lebar
- [ ] **Analisis Makna**:
  - Card terpisah dengan heading "📖 Analisis Makna"
  - Paragraf teks dari `meaningAnalysis`
  - Tampilkan line breaks dengan benar
- [ ] **Rangkuman Cerita**:
  - Card terpisah dengan heading "📝 Rangkuman Cerita"
  - Paragraf teks dari `storySummary`
- [ ] **Tombol Aksi**:
  - "← Kembali" → navigasi ke halaman utama
  - "Terjemahkan Lirik Lain" → navigasi ke halaman utama
- [ ] **Responsive**: Layout card stack di mobile, side-by-side di desktop jika appropriate
- [ ] Gunakan Thymeleaf `th:each` untuk iterasi baris lirik
- [ ] Gunakan `th:text` dan `th:utext` dengan benar (escape HTML untuk keamanan)

---

### Issue #8: ⚠️ Error Handling & Edge Cases
**Priority:** 🟡 Medium | **Estimate:** 1-2 jam

**Deskripsi:**
Implementasi error handling yang komprehensif di semua layer.

**Acceptance Criteria:**
- [ ] **Error Page** (`error.html`):
  - Tampilan yang user-friendly saat terjadi error
  - Pesan error yang informatif tapi tidak expose technical detail
  - Tombol "Kembali ke Beranda"
- [ ] **AI Timeout Handling**:
  - Timeout set ke 60 detik
  - Jika timeout, tampilkan pesan: "AI sedang sibuk, silakan coba lagi dalam beberapa saat."
- [ ] **AI Response Parsing Error**:
  - Jika JSON dari AI tidak valid, coba extract dari markdown code block
  - Jika masih gagal, retry sekali
  - Jika tetap gagal, tampilkan pesan error yang jelas
- [ ] **Input Validation**:
  - Max 10.000 karakter (cegah abuse/token waste)
  - Reject input kosong atau hanya whitespace
  - Sanitize input (prevent XSS via Thymeleaf auto-escaping)
- [ ] **Rate Limiting** (sederhana):
  - Minimal: delay 2 detik antar request dari IP yang sama (via simple in-memory map)
  - Atau cukup disable tombol selama proses berjalan
- [ ] **Database Error**:
  - Jika save gagal, tetap tampilkan hasil ke user (degraded mode)
  - Log error untuk investigasi

---

### Issue #9: 📝 README & Documentation
**Priority:** 🟡 Medium | **Estimate:** 1 jam

**Deskripsi:**
Buat README.md yang lengkap agar kontributor atau developer lain bisa setup project dengan mudah.

**Acceptance Criteria:**
- [ ] **README.md** mencakup:
  - Deskripsi project + screenshot
  - Prerequisites (Java 17+, Maven, MySQL 8)
  - Langkah setup:
    1. Clone repo
    2. Create database
    3. Copy `.env.example` ke `.env` dan isi
    4. `mvn spring-boot:run`
  - Konfigurasi AI API key
  - Struktur project
  - Kontribusi guidelines
- [ ] **`.env.example`**:
  ```env
  DB_USERNAME=root
  DB_PASSWORD=your_password
  AI_PROVIDER=gemini
  GEMINI_API_KEY=your_gemini_api_key
  OPENAI_API_KEY=your_openai_api_key
  ```

---

### Issue #10: 🧪 Testing
**Priority:** 🟡 Medium | **Estimate:** 2-3 jam

**Deskripsi:**
Unit test dan integration test untuk memastikan kualitas kode.

**Acceptance Criteria:**
- [ ] **Unit Tests:**
  - [ ] `TranslationServiceTest` — test `translateAndAnalyze()` dengan mock AiService
  - [ ] `GeminiServiceTest` — test JSON parsing dari sample Gemini response
  - [ ] Test DTO serialization/deserialization
- [ ] **Integration Tests:**
  - [ ] `TranslationControllerTest` — test POST `/translate` dan GET `/translate/result/{id}`
  - [ ] Test dengan H2 in-memory database
  - [ ] Test error handling (invalid input, AI error)
- [ ] **Test Coverage**: Minimal 60% line coverage pada service layer

---

### Issue #11: 🚀 Polish & Production Readiness (Opsional)
**Priority:** 🟢 Low | **Estimate:** 2-3 jam

**Deskripsi:**
Penyempurnaan untuk production readiness.

**Acceptance Criteria:**
- [ ] **Caching**: Cache hasil terjemahan yang identik (hindari re-call AI untuk lirik yang sama)
  - Cek apakah `original_lyrics` hash sudah ada di DB, jika ya return dari DB
- [ ] **SEO & Meta Tags**:
  - Title tag dinamis: "PahamLirik — Terjemahan {Song Title}"
  - Meta description
  - Open Graph tags
- [ ] **Favicon**: Icon musik warna indigo
- [ ] **Footer**: Informasi credit, powered by Gemini/OpenAI
- [ ] **Analytics** (opsional): Simple hit counter
- [ ] **Docker** (opsional):
  - `Dockerfile` untuk aplikasi
  - `docker-compose.yml` dengan MySQL
- [ ] **Pagination** untuk riwayat terjemahan (jika sudah banyak)

---

## ⏰ Estimasi Timeline

| Fase | Tasks | Estimasi |
|------|-------|----------|
| **Setup** | Issue #1 | 1-2 jam |
| **Backend Core** | Issue #2, #3, #4 | 4-7 jam |
| **Controller** | Issue #5 | 1-2 jam |
| **Frontend** | Issue #6, #7 | 4-6 jam |
| **Error Handling** | Issue #8 | 1-2 jam |
| **Documentation** | Issue #9 | 1 jam |
| **Testing** | Issue #10 | 2-3 jam |
| **Polish** | Issue #11 | 2-3 jam |
| **Total** | | **~16-26 jam** |

---

## ⚡ Quick Start untuk Implementor

### Urutan Pengerjaan yang Direkomendasikan

```
Issue #1 (Setup)
    ↓
Issue #2 (Entity/DTO)
    ↓
Issue #3 (AI Service)  ← Ini yang paling complex, mulai dari sini setelah setup
    ↓
Issue #4 (Translation Service)
    ↓
Issue #5 (Controller)
    ↓
Issue #6 (Halaman Utama)  ←  Bisa dikerjakan paralel dengan #7
    ↓
Issue #7 (Halaman Hasil)
    ↓
Issue #8 (Error Handling)
    ↓
Issue #9 (README)
    ↓
Issue #10 (Testing)
    ↓
Issue #11 (Polish) ← Opsional
```

### Tips untuk Programmer:
1. **Mulai dengan AI Service** — ini komponen paling krusial. Pastikan bisa call API dan parse response sebelum lanjut ke bagian lain.
2. **Test manual dulu** — sebelum nulis unit test, pastikan flow end-to-end berjalan via browser.
3. **Gunakan `@Slf4j`** dari Lombok untuk logging di mana-mana.
4. **Jangan hardcode API key** — selalu gunakan environment variables.
5. **Commit per issue** — setiap issue selesai, commit dengan message yang jelas (e.g., `feat: implement Gemini AI service (#3)`).

---

## 🔗 Referensi API

| Resource | URL |
|----------|-----|
| Gemini API Docs | https://ai.google.dev/gemini-api/docs |
| OpenAI API Docs | https://platform.openai.com/docs/api-reference |
| Spring Boot Reference | https://docs.spring.io/spring-boot/reference/ |
| Thymeleaf Docs | https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html |
| Tailwind CSS | https://tailwindcss.com/docs |

---

> **Last Updated:** 2026-06-28  
> **Repository:** git@github.com:charlybutar21/paham-lirik.git
