# WebPoster — TaskFlow (JWT Auth & Task Management System)

WebPoster — zamonaviy, to'liq stekli (**Full-Stack**) vazifalarni boshqarish platformasi. Loyiha **Spring Boot 4 (Java 21)** backend, **React 19 (TypeScript, Tailwind CSS)** frontend va **Neon Serverless PostgreSQL** ma'lumotlar bazasiga asoslangan bo'lib, **Docker & Docker Compose** orqali to'liq production muhitiga tayyorlangan.

---

## 🚀 Texnologiyalar Steki

### Backend
* **Til & Freymvork**: Java 21, Spring Boot 4.x
* **Xavfsizlik**: Spring Security, JWT (jjwt `0.12.6`), BCrypt Password Encoder
* **Ma'lumotlar bazasi**: PostgreSQL (**Neon Serverless**), Spring Data JPA, Hibernate ORM
* **Ulanishlar hovuzi**: HikariCP (Serverless aloqa uzilishlariga qarshi sozlangan)
* **Konteynerlashtirish**: Multi-stage Dockerfile (Eclipse Temurin JRE 21 Alpine, Non-root user)
* **Avtomatlashtirish & Test**: Gradle, JUnit 5, Mockito, MockMvc, H2 in-memory DB (**85 ta test, 100% muvaffaqiyatli**)

### Frontend
* **Kutubxona & Til**: React 19, TypeScript
* **Yig'uvchi (Bundler)**: Vite 8
* **Styling**: Tailwind CSS v4 (Dark Minimalist Aesthetic)
* **Tarmoq (HTTP Client)**: Axios (Silent refresh & 401 retry queue interceptors)
* **Marshrutlash**: React Router DOM v7
* **Ikonkalar**: Lucide React
* **Veb-server**: Nginx Alpine (SPA routing, Gzip compression, Reverse proxy, Asset caching)

---

## ✨ Asosiy Imkoniyatlar

### 1. Xavfsiz JWT Autentifikatsiya Tizimi
* **Access Token**: Faqat xotirada (`in-memory`) saqlanadi — XSS hujumlaridan to'liq himoyalangan.
* **Refresh Token**: Brauzerning `HttpOnly`, `SameSite=Lax` cookie'larida saqlanadi va ma'lumotlar bazasida ro'yxatga olinadi.
* **Token Rotatsiyasi (Rotation)**: Har safar sessiya yangilanganda yangi refresh token beriladi va eskisi bekor qilinadi.
* **Silent Automatic Refresh**: Foydalanuvchi sahifani yangilaganda yoki access token muddati tugaganda, Axios interceptor fonda cookie orqali sessiyani avtomatik yangilaydi (foydalanuvchi qayta login qilishi shart emas).
* **Logout**: Bazadagi tokenni o'chiradi va brauzerdagi cookie'ni tozalaydi.

### 2. Task CRUD Boshqaruvi
* **Ustunlar (Columns)**:
  * `id`: Task unikal identifikatori.
  * `title`: Sarlavha (majburiy).
  * `content`: Batafsil tavsif (TEXT).
  * `published_at`: Nashr etilgan/yaratilgan vaqti (`LocalDateTime`).
  * `status`: `complete` (yoki `complate`) va `incomplete` (yoki `incomplate`).
* **Boshqaruv & Filtrlash**:
  * Real-vaqt qidiruv (Search).
  * Status bo'yicha filtrlash (Barchasi, Kutilmoqda, Bajarildi).
  * 1-bosish orqali holatni o'zgartirish (`complete` $\leftrightarrow$ `incomplete`).
  * Tahrirlash va o'chirish.
  * **Ma'lumotlar izolyatsiyasi**: Har bir foydalanuvchi faqat o'ziga tegishli vazifalarni ko'radi va boshqaradi.

### 3. Neon Serverless PostgreSQL Moslashuvi
* `sslmode=require` shifrlangan ulanishi.
* Neon serverless ulanishlarini uzib qo'ymasligi uchun HikariCP `keepalive-time: 30000`, `max-lifetime: 600000` va `idle-timeout: 300000` sozlamalari joriy qilingan.

---

## 📁 Loyiha Strukturasi

```
webposter/
├── docker-compose.yaml          # Production Docker Compose konfiguratsiyasi
├── .env.example                 # Muhit o'zgaruvchilari namunasi
├── README.md                    # Loyiha hujjati
│
├── server/                      # Spring Boot Backend
│   ├── Dockerfile               # Multi-stage Dockerfile (Temurin 21)
│   ├── .dockerignore
│   ├── build.gradle             # Bog'liqliklar va sozlamalar
│   └── src/
│       ├── main/
│       │   ├── java/com/post/server/
│       │   │   ├── config/      # SecurityConfig, JwtAuthFilter, CorsConfig
│       │   │   ├── controller/  # AuthController, TaskController
│       │   │   ├── dto/         # AuthDto, TaskDto, ErrorResponse
│       │   │   ├── entity/      # UserEntity, TokenEntity, TaskEntity, Enums
│       │   │   ├── exception/   # GlobalExceptionHandler, CustomExceptions
│       │   │   ├── repository/  # UserRepository, TokenRepository, TaskRepository
│       │   │   ├── service/     # AuthService, TokenService, TaskService
│       │   │   └── util/        # JwtUtil, CookieUtil
│       │   └── resources/
│       │       └── application.yaml
│       └── test/                # Unit & Integration testlar (85 ta)
│
└── client/                      # React Frontend
    ├── Dockerfile               # Multi-stage Dockerfile (Node 22 + Nginx)
    ├── .dockerignore
    ├── nginx.conf               # Nginx SPA & Reverse Proxy konfiguratsiyasi
    ├── package.json
    ├── vite.config.ts           # Vite + Tailwind v4 + Dev Proxy
    └── src/
        ├── api/                 # Axios instance & interceptors
        ├── context/             # AuthContext, AuthProvider, useAuth hook
        ├── components/          # Navbar, TaskModal, ProtectedRoute
        ├── pages/               # LoginPage, RegisterPage, DashboardPage
        ├── types/               # TypeScript interfeyslari (auth, task)
        ├── App.tsx              # Router
        └── main.tsx
```

---

## 📡 API Endpointlari

Barcha endpointlar `/api/v1` prefiksi ostida ishlaydi:

### Autentifikatsiya (`/api/v1/auth`)

| Metod | Endpoint | Tavsif | Ruxsat |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/register` | Yangi foydalanuvchini ro'yxatdan o'tkazish | Ochiq |
| `POST` | `/auth/login` | Tizimga kirish (Access token va Cookie qaytaradi) | Ochiq |
| `POST` | `/auth/refresh` | Access tokenni yangilash (Cookie orqali) | Ochiq |
| `POST` | `/auth/logout` | Sessiyani tugatish va cookie'ni tozalash | Ochiq |

### Vazifalar (`/api/v1/tasks`)

| Metod | Endpoint | Tavsif | Ruxsat |
| :--- | :--- | :--- | :--- |
| `GET` | `/tasks` | Userning barcha vazifalarini olish | Bearer Token |
| `GET` | `/tasks?status=complete` | Status bo'yicha filterlash (`complete`/`incomplete`) | Bearer Token |
| `GET` | `/tasks/{id}` | Bitta vazifani ID bo'yicha olish | Bearer Token |
| `POST` | `/tasks` | Yangi vazifa yaratish | Bearer Token |
| `PUT` | `/tasks/{id}` | Vazifani tahrirlash/yangilash | Bearer Token |
| `DELETE` | `/tasks/{id}` | Vazifani o'chirish | Bearer Token |

---

## ⚙️ Ishga Tushirish Qo'llanmasi

### 1. Muhit O'zgaruvchilarini Sozlash (`.env`)

Loyiha ildizida `.env.example` faylidan nusxa olib `.env` faylini yarating:

```bash
cp .env.example .env
```

`.env` fayliga o'zingizning **Neon PostgreSQL** va **JWT** ma'lumotlaringizni kiriting:

```env
# Server
PORT=8080

# Neon PostgreSQL
DB_URL=jdbc:postgresql://ep-your-project-id.us-east-2.aws.neon.tech/neondb?sslmode=require
DB_USER=your_neon_username
DB_PASS=your_neon_password

# JWT
JWT_SECRET=super_secret_jwt_key_at_least_32_characters_long_123456
JWT_REFRESH_TIME=604800000
JWT_ACCESS_TIME=900000

# Xavfsizlik & CORS
COOKIE_SECURE=false
CORS_ALLOWED_ORIGINS=http://localhost,http://localhost:80,http://localhost:5173
```

---

### 2. Docker orqali Ishga Tushirish (Tavsiya etiladi)

Loyiha bitta buyruq bilan to'liq konteynerlarda yig'iladi va ishga tushadi:

```bash
docker compose up --build -d
```

* **Frontend (Veb-interfeys)**: `http://localhost`
* **Backend API**: `http://localhost/api/v1` (yoki to'g'ridan-to'g'ri `http://localhost:8080/api/v1`)

Konteynerlar holatini tekshirish:
```bash
docker compose ps
docker compose logs -f
```

To'xtatish:
```bash
docker compose down
```

---

### 3. Lokal Ishlab Chiqish (Local Development)

#### Backend'ni ishga tushirish:
```bash
cd server
./gradlew bootRun
```
*Backend `http://localhost:8080` portida ishga tushadi.*

#### Frontend'ni ishga tushirish:
```bash
cd client
npm install
npm run dev
```
*Frontend `http://localhost:5173` portida ishga tushadi va backend bilan avtomatik integratsiyalashadi.*

---

### 4. Render orqali Deploy qilish (Render Blueprint)

Loyiha ildizida Render platformasi uchun to'liq `render.yaml` (Infrastructure as Code) tayyorlangan:

1. **GitHub**ga o'zgarishlarni commit va push qiling:
   ```bash
   git add .
   git commit -m "Add Render blueprint and cross-origin cookie support"
   git push origin main
   ```
2. **Render Dashboard**ga kiring: [dashboard.render.com](https://dashboard.render.com/)
3. **New +** tugmasini bosing va **Blueprint**ni tanlang.
4. **IncridableAcuman/webposter** omborini (`main` branch) ulang.
5. Render `render.yaml` faylini avtomatik tahlil qiladi va quyidagi muhit o'zgaruvchilarini so'raydi (`sync: false`):
   - `DB_URL`: Neon PostgreSQL JDBC URL (masalan: `jdbc:postgresql://ep-xyz.us-east-2.aws.neon.tech/neondb?sslmode=require`)
   - `DB_USER`: Neon ma'lumotlar bazasi foydalanuvchi nomi
   - `DB_PASS`: Neon ma'lumotlar bazasi paroli
6. **Apply** tugmasini bosing. Backend (Docker) va Frontend (Static Site) avtomatik tarzda build bo'lib, ishga tushadi!

---

## 🧪 Testlarni Tekshirish

Loyiha sifatini ta'minlash maqsadida backend uchun to'liq Unit va Integration testlar yozilgan:

```bash
# Backend testlarini ishga tushirish:
cd server
./gradlew test
```

Natija:
* `JwtUtilTest`: 6 ta test
* `CookieUtilTest`: 2 ta test
* `TokenServiceTest`: 8 ta test
* `AuthServiceTest`: 15 ta test
* `AuthControllerTest`: 9 ta test
* `JwtAuthenticationFilterTest`: 7 ta test
* `AuthIntegrationTest`: 9 ta test
* `TaskServiceTest`: 11 ta test
* `TaskControllerTest`: 8 ta test
* `TaskIntegrationTest`: 9 ta test
* `ServerApplicationTests`: 1 ta test
**Jami: 85 ta test, 0 ta xatolik (100% PASS).**

Frontend linter va build tekshiruvi:
```bash
cd client
npm run lint    # 0 ta xatolik
npm run build   # Muvaffaqiyatli production build
```

---

## 👨‍💻 Muallif
**Izzatbek** — Full Stack / Senior React & Spring Boot Developer.
Loyiha erkin foydalanish va rivojlantirish uchun ochiq.
