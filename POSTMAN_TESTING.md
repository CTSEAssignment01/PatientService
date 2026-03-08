# PatientService — Postman Testing Guide

## Setup

### 1. Import the collection
Open Postman → **Import** → select `PatientService.postman_collection.json`

### 2. Set collection variables
Click the **Patient Service** collection → **Variables** tab and fill in:

| Variable | Description | Example |
|---|---|---|
| `baseUrl` | Local or deployed URL | `http://localhost:8080` |
| `token` | Set after login (see Step 1 below) | _(auto-filled)_ |
| `patientId` | Set automatically after Create Patient | _(auto-filled)_ |
| `appointmentId` | Set automatically after Book Appointment | _(auto-filled)_ |
| `doctorId` | A doctor's UUID from doctor-service | _(copy from doctor-service)_ |
| `slotId` | An available slot's UUID from doctor-service | _(copy from doctor-service)_ |

---

## Step 1 — Register & Login (via User Service)

> These requests are made directly to the **user-service**, not the patient-service.

### Register a PATIENT account

```
POST https://user-service-268672367192.us-central1.run.app/api/auth/register
Content-Type: application/json
```
```json
{
  "name": "Panduka Wijesinghe",
  "email": "panduka@example.com",
  "password": "12345678",
  "role": "PATIENT"
}
```

**Expected response:** `201 Created`
```json
{
  "id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "name": "Panduka Wijesinghe",
  "email": "panduka@example.com",
  "role": "PATIENT",
  "enabled": true,
  "createdAt": "2026-03-08T12:00:00"
}
```

---

### Register an ADMIN account (for restricted endpoint testing)

```
POST https://user-service-268672367192.us-central1.run.app/api/auth/register
Content-Type: application/json
```
```json
{
  "name": "Admin User",
  "email": "admin@example.com",
  "password": "admin1234",
  "role": "ADMIN"
}
```

---

### Login

```
POST https://user-service-268672367192.us-central1.run.app/api/auth/login
Content-Type: application/json
```
```json
{
  "email": "panduka@example.com",
  "password": "12345678"
}
```

**Expected response:** `200 OK`
```json
{
  "accessToken": "token_xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "refreshToken": "token_yyyyyyyy-yyyy-yyyy-yyyy-yyyyyyyyyyyy",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": { ... }
}
```

**Copy the `accessToken` value** → paste it into the `token` collection variable in Postman.

---

## Step 2 — Health Check

```
GET http://localhost:8080/actuator/health
```

**Expected response:** `200 OK`
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

---

## Step 3 — Patient Endpoints

### Create Patient Profile
> Requires: any authenticated user — `token` must be set  
> Auto-saves `patientId` to collection variable

```
POST http://localhost:8080/api/patients
Authorization: Bearer {{token}}
Content-Type: application/json
```
```json
{
  "name": "Panduka Wijesinghe",
  "email": "panduka@example.com",
  "phone": "+94771234567",
  "dateOfBirth": "1995-08-20",
  "gender": "Male",
  "address": "123 Galle Road, Colombo 03",
  "bloodGroup": "B+",
  "allergies": "None"
}
```

**Expected response:** `201 Created`
```json
{
  "id": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "userId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "name": "Panduka Wijesinghe",
  "email": "panduka@example.com",
  "phone": "+94771234567",
  "dateOfBirth": "1995-08-20",
  "gender": "Male",
  "address": "123 Galle Road, Colombo 03",
  "bloodGroup": "B+",
  "allergies": "None",
  "createdAt": "2026-03-08T12:00:00"
}
```

---

### Get My Profile
> Requires: authenticated user

```
GET http://localhost:8080/api/patients/me
Authorization: Bearer {{token}}
```

**Expected response:** `200 OK` — returns the profile linked to the token's user

---

### Get All Patients
> Requires: **ADMIN** or **RECEPTIONIST** role

```
GET http://localhost:8080/api/patients
Authorization: Bearer {{token}}
```

**Expected response:** `200 OK`
```json
[
  {
    "id": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    "name": "Panduka Wijesinghe",
    ...
  }
]
```

If called with a `PATIENT` token: `403 Forbidden`
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied: insufficient role"
}
```

---

### Get Patient by ID
> Requires: any authenticated user

```
GET http://localhost:8080/api/patients/{{patientId}}
Authorization: Bearer {{token}}
```

**Expected response:** `200 OK` — same shape as Create Patient response

---

### Update Patient Profile
> Requires: the patient themselves, ADMIN, or RECEPTIONIST  
> Only send the fields you want to update (partial update supported)

```
PUT http://localhost:8080/api/patients/{{patientId}}
Authorization: Bearer {{token}}
Content-Type: application/json
```
```json
{
  "phone": "+94779876543",
  "address": "456 Kandy Road, Kandy",
  "allergies": "Penicillin"
}
```

**Expected response:** `200 OK` — updated patient object

---

## Step 4 — Appointment Endpoints

> **Before booking**, you need a `doctorId` and an available `slotId` from the doctor-service.  
> Get available slots: `GET https://doctor-service-efc3c5f3xa-uc.a.run.app/api/slots`

### Book Appointment
> Requires: authenticated user with an existing patient profile  
> Auto-saves `appointmentId` to collection variable

```
POST http://localhost:8080/api/appointments
Authorization: Bearer {{token}}
Content-Type: application/json
```
```json
{
  "doctorId": "{{doctorId}}",
  "doctorName": "Dr. Silva",
  "slotId": "{{slotId}}",
  "reason": "Routine checkup"
}
```

**Expected response:** `201 Created`
```json
{
  "id": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
  "patientId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "patientName": "Panduka Wijesinghe",
  "doctorId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
  "doctorName": "Dr. Silva",
  "slotId": "dddddddd-dddd-dddd-dddd-dddddddddddd",
  "slotTime": "2026-03-10T09:00:00",
  "reason": "Routine checkup",
  "status": "PENDING",
  "notes": null,
  "createdAt": "2026-03-08T12:05:00"
}
```

If the slot is already taken: `409 Conflict`
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Slot is not available"
}
```

---

### Get My Appointments
> Requires: authenticated user

```
GET http://localhost:8080/api/appointments/my
Authorization: Bearer {{token}}
```

**Expected response:** `200 OK` — array of appointments

---

### Get Appointment by ID
> Requires: authenticated user

```
GET http://localhost:8080/api/appointments/{{appointmentId}}
Authorization: Bearer {{token}}
```

---

### Get Appointments by Patient
> Requires: **ADMIN**, **RECEPTIONIST**, or **DOCTOR** role

```
GET http://localhost:8080/api/appointments/patient/{{patientId}}
Authorization: Bearer {{token}}
```

---

### Get Appointments by Doctor
> Requires: **ADMIN**, **RECEPTIONIST**, or **DOCTOR** role

```
GET http://localhost:8080/api/appointments/doctor/{{doctorId}}
Authorization: Bearer {{token}}
```

---

### Cancel Appointment
> Requires: the patient who booked the appointment  
> Also releases the slot back in doctor-service

```
PATCH http://localhost:8080/api/appointments/{{appointmentId}}/cancel
Authorization: Bearer {{token}}
```

**Expected response:** `200 OK`
```json
{
  "id": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
  "status": "CANCELLED",
  ...
}
```

---

### Complete Appointment
> Requires: **ADMIN** or **DOCTOR** role

```
PATCH http://localhost:8080/api/appointments/{{appointmentId}}/complete
Authorization: Bearer {{token}}
```

**Expected response:** `200 OK` — appointment with `"status": "COMPLETED"`

---

### Add Notes to Appointment
> Requires: **ADMIN** or **DOCTOR** role  
> Body is plain text

```
PATCH http://localhost:8080/api/appointments/{{appointmentId}}/notes
Authorization: Bearer {{token}}
Content-Type: text/plain
```
```
Patient presented with mild fever. Blood pressure 120/80. Prescribed paracetamol 500mg.
```

**Expected response:** `200 OK` — appointment with `notes` field populated

---

## Common Error Responses

| Status | Meaning | Cause |
|---|---|---|
| `401 Unauthorized` | Missing or invalid token | `token` variable not set, or token expired |
| `403 Forbidden` | Insufficient role | Calling an ADMIN endpoint with a PATIENT token |
| `404 Not Found` | Resource doesn't exist | Wrong `patientId` / `appointmentId` |
| `409 Conflict` | Duplicate or conflict | Creating a second patient profile, or slot already taken |

---

## Recommended Test Order

1. Register (user-service)
2. Login (user-service) → set `token`
3. Health Check
4. Create Patient Profile → `patientId` auto-saved
5. Get My Profile
6. Book Appointment (set `doctorId` + `slotId` first) → `appointmentId` auto-saved
7. Get My Appointments
8. Add Notes (use ADMIN/DOCTOR token)
9. Complete Appointment (use ADMIN/DOCTOR token)
10. Book another appointment → Cancel it
