# IT Company - Recruitment System

Mikroservisna arhitektura za IT Company recruitment sistem sa vektorskom pretragom kandidata.

## Opis projekta

Ovaj projekat implementira recruitment sistem za IT kompaniju koji koristi vektorsku bazu podataka (Qdrant) i Elasticsearch za naprednu pretragu i analizu kandidata. Sistem omogućava:

- Kreiranje i upravljanje oglasima za posao
- Prijavu kandidata i upravljanje njihovim podacima
- Vektorsku pretragu kandidata na osnovu sličnosti CV-ja
- Naprednu pretragu kroz Elasticsearch
- Analitiku i izveštaje o recruitment procesu

## Arhitektura

### Mikroservisi

1. **Recruitment Service** (Java Spring Boot)
   - Port: 5001
   - Baze podataka: Qdrant (vektorska), Elasticsearch
   - Funkcionalnosti: CRUD operacije, vektorska pretraga, analitika

2. **API Gateway** (.NET 8)
   - Port: 5000
   - Funkcionalnosti: Rutiranje zahteva, autentifikacija, load balancing

### Baze podataka

1. **Qdrant** - Vektorska baza podataka
   - Port: 6333 (HTTP), 6334 (gRPC)
   - Korišćena za vektorsku pretragu kandidata

2. **Elasticsearch** - Search engine
   - Port: 9200, 9300
   - Korišćena za naprednu pretragu i indeksiranje

## Pokretanje projekta

### Preduslovi

- Docker i Docker Compose
- PowerShell (za Windows)
- .NET 8 SDK (za lokalni razvoj)
- Java 17 (za lokalni razvoj)

### 🚀 Pokretanje aplikacije

```powershell
# 1. Pokretanje baza podataka
docker-compose up -d qdrant elasticsearch

# 2. Čekanje da se baze pokrenu (30-60 sekundi)
Start-Sleep -Seconds 30

# 3. Pokretanje mikroservisa
docker-compose up -d recruitment-service gateway

# 4. Proverava status
docker-compose ps
```

### 🛠️ Lokalni razvoj

```powershell
# Pokretanje baza podataka
docker-compose up -d qdrant elasticsearch

# Pokretanje Gateway servisa
cd gateway
dotnet run

# Pokretanje Recruitment servisa (u novom terminalu)
cd services/recruitment-service
./mvnw spring-boot:run
```

### 🔧 Korisne komande

```powershell
# Proverava status servisa
docker-compose ps

# Prikazuje logove
docker-compose logs -f

# Zaustavlja sve servise
docker-compose down

# Restartuje recruitment servis
docker-compose restart recruitment-service

# Briše sve podatke (volume-i)
docker-compose down -v
```

## API Endpoints

### Gateway (Port 5000)

- **Swagger UI**: http://localhost:5000/swagger
- **Health Check**: http://localhost:5000/healthz

### Recruitment Service (Port 5001)

#### Job Advertisements
- `GET /api/gateway/recruitment/job-advertisements` - Lista svih oglasa
- `POST /api/gateway/recruitment/job-advertisements` - Kreiranje oglasa
- `GET /api/gateway/recruitment/job-advertisements/{id}` - Dohvatanje oglasa
- `PUT /api/gateway/recruitment/job-advertisements/{id}` - Ažuriranje oglasa
- `DELETE /api/gateway/recruitment/job-advertisements/{id}` - Brisanje oglasa

#### Candidates
- `GET /api/gateway/recruitment/candidates` - Lista svih kandidata
- `POST /api/gateway/recruitment/candidates` - Kreiranje kandidata
- `GET /api/gateway/recruitment/candidates/{id}` - Dohvatanje kandidata
- `PUT /api/gateway/recruitment/candidates/{id}` - Ažuriranje kandidata
- `DELETE /api/gateway/recruitment/candidates/{id}` - Brisanje kandidata

#### Applications
- `GET /api/gateway/recruitment/applications` - Lista svih prijava
- `POST /api/gateway/recruitment/applications` - Kreiranje prijave
- `GET /api/gateway/recruitment/applications/{id}` - Dohvatanje prijave
- `PUT /api/gateway/recruitment/applications/{id}` - Ažuriranje prijave
- `DELETE /api/gateway/recruitment/applications/{id}` - Brisanje prijave

#### Search & Analytics
- `POST /api/gateway/recruitment/search/candidates` - Pretraga kandidata
- `POST /api/gateway/recruitment/search/jobs` - Pretraga poslova
- `POST /api/gateway/recruitment/vector-search/candidates` - Vektorska pretraga
- `GET /api/gateway/recruitment/analytics/statistics` - Statistike

## Konfiguracija

### Environment Variables

#### Gateway
- `RECRUITMENT_SERVICE_URL` - URL recruitment servisa
- `JWT_KEY` - JWT secret key
- `JWT_ISSUER` - JWT issuer
- `JWT_AUDIENCE` - JWT audience

#### Recruitment Service
- `QDRANT_URL` - URL Qdrant baze
- `ELASTICSEARCH_URL` - URL Elasticsearch baze
- `SERVER_PORT` - Port servisa

## Razvoj

### Struktura projekta

```
NAIS_ITCompany/
├── gateway/                 # API Gateway (.NET 8)
│   ├── Controllers/
│   ├── Services/
│   ├── Models/
│   └── Configuration/
├── services/
│   └── recruitment-service/ # Recruitment Service (Java Spring Boot)
│       ├── src/main/java/
│       └── pom.xml
├── docker-compose.yml
└── README.md
```

### Dodavanje novih endpoint-a

1. Dodaj endpoint u `GatewayController.cs`
2. Implementiraj logiku u `RecruitmentService`
3. Ažuriraj Swagger dokumentaciju

## Testiranje

```bash
# Pokretanje testova
cd services/recruitment-service
./mvnw test

# Pokretanje Gateway testova
cd gateway
dotnet test
```

## Monitoring

- **Gateway Health**: http://localhost:5000/healthz
- **Recruitment Service Health**: http://localhost:5001/actuator/health
- **Qdrant Dashboard**: http://localhost:6333/dashboard
- **Elasticsearch**: http://localhost:9200

## Tehnologije

- **Backend**: .NET 8, Java 17, Spring Boot
- **Baze podataka**: Qdrant, Elasticsearch
- **Containerization**: Docker, Docker Compose
- **API**: REST, OpenAPI/Swagger
- **Authentication**: JWT

## Licenca

Ovaj projekat je deo predmetnog rada za kurs "Napredne arhitekture informacionih sistema" na Fakultetu tehničkih nauka, Univerzitet u Novom Sadu.
