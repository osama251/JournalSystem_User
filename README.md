# JournalSystem_User

User management microservice for the larger **JournalSystem** project.  
Responsible for **registering and querying users** via **Keycloak**, including:

- Patients
- Doctors
- Employees

This service acts as the system’s **identity and user profile layer**, integrating directly with Keycloak for authentication, authorization, roles, and custom user attributes.

---

## Features

- Register users (patients, doctors, employees) via Keycloak
- Query users by ID or username
- Assign and resolve realm roles (patient / doctor / employee)
- Store and read custom user attributes (organization, address, age, gender, etc.)
- Doctor–patient relationships via Keycloak attributes
- Secure REST API
- Unit-tested service logic (Mockito/JUnit)
- Containerized with Docker and deployable with Kubernetes

---

## Tech Stack

- **Java 17**
- **Spring Boot** (REST API)
- **Keycloak Admin Client**
- **Keycloak (OIDC / OAuth2)**
- **Docker**
- **Kubernetes (k3s)**
- **JUnit 5 + Mockito** (unit tests)

---

## Architecture (high level)

- `service/`
  - `UserService` – generic user queries and authentication
  - `DoctorService` – doctor registration and lookup
  - `EmployeeService` – employee registration and lookup
  - `PatientService` – patient registration and doctor–patient mapping
- `model/`
  - Includes models for patients, doctors and employees
- `controller`
  - Exposes the REST api.   
- Uses **Keycloak as the source of truth** for:
  - Users
  - Credentials
  - Roles
  - Custom attributes
- No local database — all identity data is managed by Keycloak

This service encapsulates all Keycloak interaction so other services do not depend on Keycloak directly.

---

## Keycloak Integration

- Uses **Keycloak Admin API** for:
  - Creating users
  - Assigning realm roles
  - Managing custom attributes
- Uses **OAuth2 Password Grant** for credential verification
- Supports:
  - Role-based access (patient / doctor / employee)
  - Custom attributes (organization, address, age, gender, patient lists)

---

## Kubernetes

- Deployed as part of the project’s Kubernetes (k3s) cluster
- Communicates with the Keycloak service inside the cluster
- Deployment manifests are maintained in **JournalSystem_Q8SFILES**

---

## CI/CD

- On push to `main`, GitHub Actions:
  - runs unit tests
  - builds the JAR
  - builds and pushes a Docker image
  - deploys the updated image to a VM (k3s) via SSH

---

## Running locally

### 1) Start Keycloak (example)

```bash
docker run --name keycloak \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -p 8080:8080 \
  quay.io/keycloak/keycloak:latest start-dev
