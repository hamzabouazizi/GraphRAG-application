## User Management Service

### Secure Authentication & Authorization with Neo4j and Spring Boot

## Overview
This project is a Java 21/Spring Boot 3.5.3–based User Management microservice designed to provide a secure, scalable, and production-ready authentication layer for modern applications. It handles the entire user lifecycle, from signup and email verification to login, profile management, and password recovery. While maintaining strong security guarantees and robust persistence. Built with Spring Data Neo4j as the graph database backend, this service leverages Redis caching for efficient temporary data storage (pending verification tokens), and enforces strict rate-limiting, JWT-based stateless authentication, and HTTPS protection.
Follows modern Spring conventions with clear separation of concerns across layers: Controller → Service → Repository → Model.

## Core Technologies
| Component | Description |
|------------|-------------|
| **Language & Framework** | Java 21 with Spring Boot 3.5.3 |
| **Database** | Neo4j (Graph + Vector Database) |
| **Cache Store** | Redis (for pending user verification) |
| **Build Tool** | Apache Maven |
| **Email Service** | Brevo |
| **Containerization** | Docker (multi-stage builds for Dev & Prod) |
| **Security** | JWT (HttpOnly cookie), HTTPS (mkcert), Rate Limiting |
| **Testing** | unit + integration in CI/CD setup |
| **Deployment** | Docker Compose/Kubernetes manifests |
| **Version Control** | GitHub |

## Security Architecture
Security is a first-class concern in this application. The following mechanisms are in place to protect both users and infrastructure:
### JWT-Based Authentication: 
Upon successful login, the server issues a **JWT** token stored in a **HttpOnly cookie**, making it inaccessible to client-side scripts and therefore resilient against XSS attacks.
### Environment Variables
All environment variables are injected dynamically at runtime.
### HTTPS Enforcement
The application is served exclusively via **HTTPS**, backed by a self-signed TLS certificate (.p12) generated using mkcert.
### Rate Limiting
Rate limiting is enforced globally and per endpoint to mitigate brute-force and abuse attempts.
### One-Time Verification Tokens
User verification tokens are consumed once and immediately invalidated after use.
### Secure Logout
Logout endpoint are protected by JWT verification and ensure that only authenticated users can terminate active sessions.
### Redis Caching for Pending Users
Newly registered (unverified) users are temporarily stored in **Redis** to reduce database load and ensure fast access. Upon successful email verification, their data is migrated to the persistent Neo4j store.

## Database Integration
The service communicates with **Neo4j** through **Neo4jClient** and the native **Neo4j Java Driver**, providing flexibility for complex graph queries and relationships between users. Local development uses the official Neo4j Docker image (hub.docker.com/_/neo4j).

## Email Verification
User verification and password reset flows rely on **Brevo’s SMTP** email service.
Note: Certain antivirus software may interfere with TLS certificate signatures used by Brevo; disabling email “core shield” during local testing is recommended.

## Development vs Production Containers
| Stage           | Command                       | Purpose                                            |
| --------------- | ----------------------------- | -------------------------------------------------- |
| **Development** | `mvn compile spring-boot:run` | Enables hot-reload and debugging                   |
| **Production**  | `java -jar app.jar`           | Executes the packaged JAR in a lightweight runtime |

## Testing and CI/CD
Automated **JUnit** and **integration** tests validate the core logic, email flows, and authentication lifecycle.
Continuous Integration is configured to:
- Build the application image
- Execute test suites
- Push validated images to the container registry (GitHub Container Registry)

Continuous Deployment is handled through a **GitOps workflow** managed by **ArgoCD**.

## API Endpoints
| Endpoint                    | Description                                    |
| --------------------------- | ---------------------------------------------- |
| `POST /api/signup`          | Registers a new user (with email verification) |
| `POST /api/verifyAndLogin`  | Verifies user token and issues JWT             |
| `POST /api/login`           | Authenticates existing users                   |
| `POST /api/forgot-password` | Sends password reset email                     |
| `POST /api/reset-password`  | Resets password using secure token             |
| `GET /profile`              | Retrieves authenticated user profile           |
| `POST /api/logout`          | Invalidates JWT and clears HttpOnly cookie     |

## License
This project is released under the MIT License.


