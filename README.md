# 🌬️ AirGuard — Personalized Air Pollution Exposure Engine

> **Not just an AQI widget.** AirGuard calculates your *individual* pollution risk based on where you are, what you're doing, and how your body responds to pollution — built entirely in Java.

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2-green?style=flat-square&logo=spring"/>
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square"/>
  <img src="https://img.shields.io/badge/APIs-Free%20%2F%20No%20Key-brightgreen?style=flat-square"/>
</p>

---

## ✨ What Makes AirGuard Different

| Standard AQI App | AirGuard |
|---|---|
| Shows city-level AQI | Calculates **your personal dose** |
| Ignores what you're doing | Accounts for **activity type** (running vs sitting) |
| One-size-fits-all | Adjusts for **health profile** (asthmatic, child, elderly) |
| Static number | **Exposure score** = AQI × Activity × Duration × Vulnerability |
| No history | **Full activity log** with gamification & achievements |

---

## 🏗️ Architecture

```
┌─────────────────────┐     HTTP / REST       ┌──────────────────────────────┐
│                     │ ◄───────────────────► │                              │
│   Frontend          │                        │   Spring Boot 3.2 (Java 21)  │
│   Vanilla JS        │                        │                              │
│   3 pages:          │                        │   ┌──────────────────────┐   │
│   - index.html      │                        │   │  Exposure Engine     │   │
│   - dashboard.html  │                        │   │  Multi-factor scoring│   │
│                     │                        │   │  Physiological model │   │
└─────────────────────┘                        │   └──────────────────────┘   │
                                               │                              │
         ┌─────────────────────────────────────│   ┌──────────────────────┐   │
         │                                     │   │  H2 (dev) /          │   │
         │  Open-Meteo Air Quality API  ────── │   │  PostgreSQL (prod)    │   │
         │  WAQI API (fallback)         ────── │   │  Spring Data JPA     │   │
         │  Nominatim Reverse Geocoding ────── │   └──────────────────────┘   │
         │  Open-Meteo Geocoding        ────── │                              │
         └─────────────────────────────────────┘   JWT Auth + BCrypt          │
                                               └──────────────────────────────┘
```

### Stack

| Layer | Technology |
|---|---|
| **Language** | Java 21 |
| **Backend** | Spring Boot 3.2, Spring Security, Spring Data JPA |
| **Auth** | JWT (access + refresh tokens), BCrypt password hashing |
| **Database** | H2 (dev, embedded) / PostgreSQL (production) |
| **Frontend** | Vanilla JS (ES modules), Chart.js, CSS custom properties |
| **APIs** | Open-Meteo Air Quality, WAQI (fallback), Open-Meteo Geocoding, Nominatim |
| **Deployment** | Docker, Docker Compose, Nginx |

---

## 🧬 Exposure Formula

```
Exposure = AQI × ActivityMultiplier × DurationHours × VulnerabilityMultiplier
```

### Activity Multipliers

| Activity | Multiplier | Rationale |
|---|---|---|
| Running | ×2.5 | Respiratory rate elevated 2–3× at vigorous exercise |
| Hiking | ×2.2 | Sustained aerobic effort |
| Cycling | ×2.0 | Moderate-to-high exertion |
| Walking | ×1.5 | Light physical activity |
| Yoga | ×1.3 | Controlled breathing, mild effort |
| Sitting | ×1.0 | Resting baseline |

### Health Profile Multipliers

| Profile | Multiplier | Rationale |
|---|---|---|
| Asthmatic | ×2.0 | Heightened bronchial sensitivity to PM and ozone |
| Child | ×1.6 | Developing respiratory system, higher lung surface:body ratio |
| Elderly | ×1.5 | Reduced mucociliary clearance, cardiovascular comorbidities |
| Normal Adult | ×1.0 | Reference baseline |

### Risk Levels & XP

| Score | Risk Level | XP Reward |
|---|---|---|
| 0–100 | 🟢 LOW | +50 XP |
| 101–300 | 🟡 MODERATE | +30 XP |
| 301–600 | 🔴 HIGH | +15 XP |
| 601+ | 🟣 VERY HIGH | +5 XP |

---

## 🚀 Quick Start

### Prerequisites

- **Java 21+** — [Download Adoptium](https://adoptium.net)
- **Maven 3.9+** — [Download Maven](https://maven.apache.org) (or use the Maven wrapper)
- A modern browser (Chrome, Firefox, Edge)

### Option 1 — One-Command (Recommended)

**Linux / macOS:**
```bash
chmod +x scripts/dev.sh
./scripts/dev.sh
```

**Windows:**
```cmd
scripts\dev.bat
```

Then open `frontend/index.html` in your browser.

> **VS Code tip:** Install the *Live Server* extension → right-click `frontend/index.html` → *Open with Live Server*. This avoids CORS issues on some browsers.

---

### Option 2 — Manual

```bash
# 1. Build & start backend
cd backend
mvn -B package -DskipTests
java -jar target/airguard-backend-1.0.0.jar

# 2. Open frontend
# Simply open frontend/index.html in your browser
# Backend runs on http://localhost:8080/api
```

---

### Option 3 — Docker (Production-Ready)

```bash
# Configure secrets (required)
cp .env.example .env
# Edit .env: set JWT_SECRET and DB_PASSWORD

# Build & start entire stack
docker compose up --build

# App: http://localhost
# API: http://localhost:8080/api
```

---

## 🔌 API Reference

All endpoints prefixed with `/api`.

### Auth Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/auth/register` | ❌ | Create account |
| `POST` | `/auth/login` | ❌ | Get JWT tokens |
| `POST` | `/auth/refresh` | ❌ | Rotate access token |
| `GET` | `/auth/me` | ✅ | Get current user profile |

**Register / Login response:**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "user": {
    "id": 1,
    "username": "john",
    "email": "john@example.com",
    "xp": 0,
    "level": 1,
    "rank": "Air Rookie"
  }
}
```

### AQI Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/aqi/current?latitude=&longitude=` | ✅ | Live AQI + 24h forecast |
| `GET` | `/aqi/public/current?latitude=&longitude=` | ❌ | Public AQI check |
| `GET` | `/aqi/search?q=` | ✅ | City name → coordinates |
| `GET` | `/aqi/reverse?latitude=&longitude=` | ✅ | Coordinates → city name |

### Exposure Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/exposure/calculate` | ✅ | Compute exposure score, save, earn XP |
| `GET` | `/exposure/dashboard` | ✅ | Dashboard stats + charts data |
| `GET` | `/exposure/history` | ✅ | Full activity history |

**Calculate request:**
```json
{
  "latitude": 18.5204,
  "longitude": 73.8567,
  "activityType": "RUNNING",
  "healthProfile": "NORMAL",
  "durationHours": 1.0
}
```

**Calculate response:**
```json
{
  "id": 42,
  "aqi": 87.5,
  "pm25": 15.7,
  "pm10": 24.3,
  "exposureScore": 131.25,
  "riskLevel": "MODERATE",
  "activityMultiplier": 2.5,
  "vulnerabilityMultiplier": 1.0,
  "durationHours": 1.0,
  "xpEarned": 30,
  "cityName": "Pune",
  "recommendations": [
    "Acceptable air quality, but unusually sensitive people should consider reducing prolonged exertion.",
    "Consider reducing intensity or shortening your session."
  ]
}
```

---

## 🌐 Free APIs Used

| API | Purpose | Auth |
|---|---|---|
| [Open-Meteo Air Quality](https://open-meteo.com/en/docs/air-quality-api) | Live US EPA AQI by coordinates + 24h forecast | **None** |
| [WAQI](https://waqi.info/) | Fallback AQI (uses `demo` token for basic access) | **None** (demo token) |
| [Open-Meteo Geocoding](https://geocoding-api.open-meteo.com) | City name → lat/lon | **None** |
| [Nominatim / OpenStreetMap](https://nominatim.openstreetmap.org) | Reverse geocoding | **None** |

**Fallback strategy:** Open-Meteo → WAQI → estimated value (deterministic from coordinates). The app never fails silently.

---

## ⚙️ Configuration

All config in `backend/src/main/resources/application.properties`:

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | Backend port |
| `airguard.jwt.secret` | *(dev default)* | **Change in production** — 32+ char string |
| `airguard.jwt.access-token-expiry` | `3600000` | Access token TTL (ms) — 1 hour |
| `airguard.jwt.refresh-token-expiry` | `2592000000` | Refresh TTL — 30 days |
| `airguard.cors.allowed-origins` | `http://localhost:*` | Comma-separated origins |
| `spring.datasource.url` | H2 embedded | Switch to PostgreSQL URL for prod |

For production, set environment variables (Docker or system):
```bash
JWT_SECRET=your-random-256bit-secret
DATABASE_URL=jdbc:postgresql://host:5432/airguard
DB_USER=airguard
DB_PASSWORD=secure-password
ALLOWED_ORIGINS=https://yourdomain.com
```

---

## 🗂️ Project Structure

```
airguard/
├── backend/
│   ├── src/main/java/com/airguard/
│   │   ├── AirGuardApplication.java        ← Spring Boot entry point
│   │   ├── config/
│   │   │   ├── AppProperties.java          ← Typed config from application.properties
│   │   │   ├── SecurityConfig.java         ← Spring Security + CORS + JWT
│   │   │   └── WebConfig.java              ← RestTemplate + CacheManager
│   │   ├── controller/
│   │   │   ├── AuthController.java         ← /auth/* endpoints
│   │   │   ├── AqiController.java          ← /aqi/* endpoints
│   │   │   ├── ExposureController.java     ← /exposure/* endpoints
│   │   │   └── GlobalExceptionHandler.java ← Unified error responses
│   │   ├── model/
│   │   │   ├── User.java                   ← JPA entity: users table
│   │   │   ├── Activity.java               ← JPA entity: activities table (enums inside)
│   │   │   └── Dto.java                    ← All request/response DTOs
│   │   ├── repository/
│   │   │   ├── UserRepository.java         ← Spring Data JPA for users
│   │   │   └── ActivityRepository.java     ← Custom queries for stats
│   │   ├── security/
│   │   │   ├── JwtUtils.java               ← JWT generation & validation
│   │   │   ├── JwtAuthFilter.java          ← Per-request JWT extraction
│   │   │   └── UserDetailsServiceImpl.java ← Loads user from DB for auth
│   │   └── service/
│   │       ├── AuthService.java            ← Register, login, refresh, profile
│   │       ├── AqiService.java             ← Multi-source AQI with fallback + cache
│   │       └── ExposureService.java        ← Exposure formula, gamification, dashboard
│   ├── src/main/resources/
│   │   ├── application.properties          ← Dev config (H2)
│   │   └── application-prod.properties     ← Prod overrides (PostgreSQL)
│   ├── Dockerfile
│   ├── pom.xml
│   └── .env.example
│
├── frontend/
│   ├── index.html                          ← Landing + Login + Register
│   ├── dashboard.html                      ← Full SPA dashboard (5 panels)
│   ├── nginx.conf                          ← Nginx proxy config
│   └── Dockerfile
│
├── scripts/
│   ├── dev.sh                              ← Linux/macOS one-command dev start
│   └── dev.bat                             ← Windows one-command dev start
│
├── docker-compose.yml                      ← Full stack: backend + frontend + PostgreSQL
└── README.md
```

---

## 🏆 Gamification System

AirGuard uses XP and leveling to encourage consistent air quality monitoring:

- **XP per check**: 5–50 XP depending on risk level encountered
- **Level formula**: `level = floor(sqrt(xp / 50)) + 1`
- **Streak tracking**: Daily activity streaks
- **Ranks**: Air Rookie → Breath Watcher → Clean Air Scout → Pollution Tracker → Air Quality Analyst → Environmental Guardian → Air Master
- **Achievements**: 6 milestone achievements tracked automatically

---

## 🚀 Production Deployment

### Checklist

- [ ] Generate `JWT_SECRET`: `openssl rand -hex 32`
- [ ] Set `SPRING_PROFILES_ACTIVE=prod`
- [ ] Configure PostgreSQL `DATABASE_URL`, `DB_USER`, `DB_PASSWORD`
- [ ] Set `ALLOWED_ORIGINS` to your actual domain
- [ ] Place SSL certificate in nginx (or use a reverse proxy like Traefik)
- [ ] Set `spring.jpa.hibernate.ddl-auto=validate` after first run

### Cloud Platforms

| Platform | Method |
|---|---|
| **Render.com** | Backend: Web Service (Java/Docker), Frontend: Static Site |
| **Railway** | `railway up` from project root |
| **DigitalOcean App Platform** | Connect GitHub repo, set env vars in dashboard |
| **Fly.io** | `fly launch` with provided Dockerfile |
| **AWS ECS** | Use provided Dockerfiles with ECR + ECS task definitions |

---

## 🧪 Running Tests

```bash
cd backend
mvn test
```

---

## 📜 License

MIT — see [LICENSE](LICENSE) file.

---

## 🙏 Credits

- [Open-Meteo](https://open-meteo.com) — free, no-registration AQI & weather API
- [WAQI](https://waqi.info) — World Air Quality Index project
- [OpenStreetMap / Nominatim](https://nominatim.openstreetmap.org) — reverse geocoding
- [Chart.js](https://chartjs.org) — charts in the dashboard
- [Spring Boot](https://spring.io/projects/spring-boot) — Java backend framework
