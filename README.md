# Patient Service

Patient & Appointment management microservice for the Clinic Management System.

## Overview

This microservice handles:
- **Patient Profile Management** — Create, read, update patient profiles linked to user accounts
- **Appointment Booking** — Book appointments by reserving doctor time slots
- **Appointment Lifecycle** — Cancel, complete, and add notes to appointments

## Integration Points

| Service | Direction | Purpose |
|---------|-----------|---------|
| **user-service** | Patient → User | Token validation via `GET /api/auth/validate` |
| **doctor-service** | Patient → Doctor | Slot reservation (`POST /api/slots/{id}/reserve`), slot release (`POST /api/slots/{id}/release`), slot details (`GET /api/slots/{id}`) |

## API Endpoints

### Patient Endpoints (`/api/patients`)

| Method | Path | Auth | Role | Description |
|--------|------|------|------|-------------|
| POST | `/api/patients` | Required | Any | Create patient profile |
| GET | `/api/patients` | Required | ADMIN, RECEPTIONIST | List all patients |
| GET | `/api/patients/me` | Required | Any | Get own patient profile |
| GET | `/api/patients/{id}` | Required | Any | Get patient by ID |
| PUT | `/api/patients/{id}` | Required | Self/ADMIN/RECEPTIONIST | Update patient profile |

### Appointment Endpoints (`/api/appointments`)

| Method | Path | Auth | Role | Description |
|--------|------|------|------|-------------|
| POST | `/api/appointments` | Required | Any | Book appointment (reserves slot) |
| GET | `/api/appointments/my` | Required | Any | Get own appointments |
| GET | `/api/appointments/{id}` | Required | Any | Get appointment by ID |
| GET | `/api/appointments/patient/{patientId}` | Required | ADMIN/RECEPTIONIST/DOCTOR | Get appointments by patient |
| GET | `/api/appointments/doctor/{doctorId}` | Required | ADMIN/RECEPTIONIST/DOCTOR | Get appointments by doctor |
| PATCH | `/api/appointments/{id}/cancel` | Required | Self | Cancel appointment (releases slot) |
| PATCH | `/api/appointments/{id}/complete` | Required | ADMIN/DOCTOR | Mark as completed |
| PATCH | `/api/appointments/{id}/notes` | Required | ADMIN/DOCTOR | Add/update notes |

## Tech Stack

- Java 17, Spring Boot 3.2.3
- Spring Data JPA + PostgreSQL (Supabase)
- Docker (multi-stage build)
- Actuator for health checks

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | 8080 | Server port |
| `DB_URL` | Supabase URL | PostgreSQL connection string |
| `DB_USERNAME` | — | Database username |
| `DB_PASSWORD` | — | Database password |
| `USER_SERVICE_URL` | user-service Cloud Run URL | User service base URL |
| `DOCTOR_SERVICE_URL` | doctor-service Cloud Run URL | Doctor service base URL |

## Running Locally

```bash
# Set required env vars
export DB_PASSWORD=your_db_password

# Build and run
./mvnw clean package -DskipTests
java -jar target/patient-service-1.0.0.jar
```

## Docker

```bash
docker build -t patient-service .
docker run -p 8080:8080 \
  -e DB_PASSWORD=your_db_password \
  patient-service
```

## Deployment (Choreo)

This service is containerized and ready for deployment on Choreo using Docker. The `Dockerfile` uses a multi-stage build with:
- Maven build stage (dependencies cached)
- Minimal JRE runtime (eclipse-temurin:17-jre-alpine)
- Non-root user for security
- Health check endpoint at `/actuator/health`
