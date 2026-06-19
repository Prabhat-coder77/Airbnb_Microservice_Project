<div align="center">

# 🏠 Airbnb Microservices Platform

### A Production-Grade Distributed System Built with Spring Boot, Spring Cloud & AWS

[![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2023.0.1-6DB33F?style=for-the-badge&logo=spring)](https://spring.io/projects/spring-cloud)
[![MySQL](https://img.shields.io/badge/MySQL-8.x-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-AWS_EC2-231F20?style=for-the-badge&logo=apachekafka)](https://kafka.apache.org/)
[![AWS S3](https://img.shields.io/badge/AWS_S3-Storage-FF9900?style=for-the-badge&logo=amazons3&logoColor=white)](https://aws.amazon.com/s3/)

</div>

---

## 📌 Table of Contents

- [Overview](#-overview)
- [Architecture](#-system-architecture)
- [Microservices Breakdown](#-microservices-breakdown)
- [Security Architecture](#-security-architecture)
- [Tech Stack](#-tech-stack)
- [API Reference](#-api-reference)
- [Database Schema](#-database-schema)
- [Setup & Installation](#-setup--installation)
- [Service Ports](#-service-ports)
- [Key Design Concepts](#-key-design-concepts)

---

## 🎯 Overview

This project is a **full-scale Airbnb-inspired property booking platform** built using a **microservices architecture**. It demonstrates real-world enterprise patterns including service discovery, JWT-based API gateway security, cloud file storage, event-driven notifications, and inter-service communication via OpenFeign.

The platform was converted from a monolithic `AuthService` into a fully distributed system with independently deployable services.

**Core capabilities at a glance:**

- 🔐 Centralized JWT authentication at the API Gateway layer
- 🏘️ Property listing with multi-file photo uploads to **AWS S3**
- 📅 Room availability search with date-based filtering
- 🛒 Booking management with real-time room count updates
- 📧 Asynchronous email notifications via **Apache Kafka**
- ⚖️ Client-side and server-side **load balancing** via Spring Cloud LoadBalancer + Eureka

---

## 🏗 System Architecture

```
                            ┌─────────────────────┐
                            │       CLIENT         │
                            │  (Postman / Browser) │
                            └──────────┬──────────┘
                                       │
                                       ▼
                    ┌──────────────────────────────────┐
                    │        API GATEWAY               │
                    │    (Port: 5555)                  │
                    │  JwtAuthenticationFilter         │
                    │  Role-Based Route Protection     │
                    └───────┬──────────┬──────────┬───┘
                            │          │          │
              ┌─────────────┘    ┌─────┘    ┌────┘
              ▼                  ▼          ▼
   ┌──────────────────┐  ┌──────────┐  ┌────────────────────┐
   │  AUTH SERVICE    │  │PROPERTY  │  │  BOOKING SERVICE   │
   │  (Port: 8083)    │  │SERVICE   │  │  (Port: 8085/9092) │
   │  JWT Generation  │  │(Port:    │  │  OpenFeign Client  │
   │  User Register   │  │ 9091)    │  │  Cart & Booking    │
   │  User Login      │  │AWS S3    │  └────────────────────┘
   └──────────────────┘  │Upload    │
                         │Kafka     │  ┌────────────────────┐
                         │Producer  │  │NOTIFICATION SERVICE│
                         └──────────┘  │  (Port: 9093/9094) │
                                       │  Kafka Consumer    │
                         ┌──────────┐  │  JavaMailSender    │
                         │ EUREKA   │  └────────────────────┘
                         │ SERVER   │
                         │(Port:    │
                         │  8761)   │
                         └──────────┘
                         ┌──────────────────────┐
                         │   APACHE KAFKA        │
                         │   (AWS EC2 Instance)  │
                         │   Topic: send_email   │
                         └──────────────────────┘
```

---

## 🔬 Microservices Breakdown

### 1. 🛡️ Eureka Server — Service Registry

The backbone of service discovery. All microservices register here on startup.

- **Port:** `8761`
- **Role:** Maintains a real-time registry of all active service instances
- Every other service is an **Eureka Discovery Client** (`@EnableDiscoveryClient`)

---

### 2. 🔑 Auth Service — Authentication & JWT Issuance

Handles user identity management. Originally a monolith, migrated to a Eureka-registered microservice.

**Key Responsibilities:**
- User Registration with BCrypt password encryption
- JWT token generation (signed with HMAC256)
- Exposes `/api/v1/auth/get-user` for inter-service user lookup

**Security Config:** JWT validation is **removed** from this service and delegated entirely to the API Gateway. The Auth Service simply validates requests as permitted or authenticated.

**Dependencies:**
- `spring-cloud-starter-netflix-eureka-client`
- `com.auth0:java-jwt:4.4.0`
- Spring Security, Spring Data JPA, MySQL

---

### 3. 🚪 API Gateway — Centralized Security & Routing

The single entry point for all client requests. Runs on **Spring WebFlux** (reactive) and implements a `GlobalFilter`.

**Port:** `5555`

**`JwtAuthenticationFilter`** (GlobalFilter):
| Endpoint | Access |
|---|---|
| `/auth/api/v1/auth/login` | 🌐 Public |
| `/auth/api/v1/auth/register` | 🌐 Public |
| `/auth/api/v1/welcome/message` | 🔐 `ROLE_ADMIN` only |
| `/micro2/message` | 🔐 `ROLE_ADMIN` only |
| `/property/api/v1/property/search-property` | 🌐 Public |

**Gateway Routes (application.yml):**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: authservice-api-1
          uri: lb://AUTH-SERVICE
          predicates:
            - Path=/auth/**
          filters:
            - RewritePath=/auth/(?<segment>.*), /${segment}

        - id: propertyservice-api
          uri: lb://property-microservice-3
          predicates:
            - Path=/property/**
          filters:
            - RewritePath=/property/(?<segment>.*), /${segment}
```

The `lb://` prefix enables **server-side load balancing** via Spring Cloud LoadBalancer.

---

### 4. 🏘️ Property Microservice — Property & Room Management

The core business service. Manages property listings, room types, availability, and photo uploads.

**Port:** `9091`  
**Database:** `propertyservicedb` (MySQL)

**Entities & Relationships:**

```
Property ──── ManyToOne ──→ City
           ── ManyToOne ──→ Area
           ── ManyToOne ──→ State
           ── OneToMany ──→ Rooms
           ── OneToMany ──→ PropertyPhotos

Rooms ──────── OneToMany ──→ RoomAvailability
```

**Key Features:**
- **Multipart file upload** (property JSON + images in same request)
- **AWS S3 integration** via `aws-java-sdk-s3` — photos stored with UUID-prefixed keys
- **Kafka producer** — publishes `EmailRequest` to `send_email` topic on property creation
- **JPQL search** — cross-entity query across `Property`, `City`, `Area`, `State`, `RoomAvailability`
- **Room count update** endpoint used by Booking Service via Feign

---

### 5. 📅 Booking Service — Reservation Engine

Handles property booking with real-time availability validation.

**Port:** `9092`  
**Database:** `bookingdb1` (MySQL)

**Booking Flow:**
```
1. Client POSTs booking request with propertyId, roomId, dates
2. BookingController calls PropertyService via Feign:
   - GET property details by ID
   - GET room type & base price
   - GET room availability by roomId
3. For each requested date → verify availableCount > 0
4. If room unavailable → return 500 "Sold Out"
5. If available → save Bookings entity (status: "pending")
6. Save BookingDate entries for each date
7. Call updateRoomCount on PropertyService (decrement count)
```

**Inter-service communication via Feign:**
```java
@FeignClient(name = "PROPERTY-MICROSERVICE-3")
public interface PropertyClient {
    @GetMapping("/api/v1/property/property-id")
    APIResponse<PropertyDto> getPropertyById(@RequestParam long id);

    @PutMapping("/api/v1/property/updateRoomCount")
    APIResponse<Boolean> updateRoomCount(@RequestParam long id, @RequestParam LocalDate date);
}
```

---

### 6. 📧 Notification Service — Email via Kafka

A dedicated consumer service that listens on the Kafka topic and sends transactional emails.

**Port:** `9093`  
**Kafka Topic:** `send_email`  
**Group ID:** `group_email`

**Event flow:**
```
Property Service  ──Kafka──▶  Notification Service  ──SMTP──▶  Gmail
   (Producer)                    (Consumer)
  publishes EmailRequest         reads message           sends email
```

**Kafka Consumer processes:**
- `to`, `subject`, `body` deserialized from JSON
- Sent via `JavaMailSender` using Gmail SMTP with App Password

---

## 🔐 Security Architecture

### Spring Security Internal Login Flow

```
Request
  │
  ▼
Security Filter Chain
  │
  ▼
UsernamePasswordAuthenticationFilter
  │  extracts username + password
  ▼
AuthenticationManager (ProviderManager)
  │
  ▼
DaoAuthenticationProvider
  │
  ├──▶ UserDetailsService.loadUserByUsername()
  │         └── fetches user from DB
  │
  └──▶ PasswordEncoder.matches(raw, encoded)
              └── BCrypt comparison
  │
  ▼
SecurityContextHolder.setAuthentication(token)
  │
  ▼
JWT Token generated & returned
```

### User Registration Flow

```
POST /register
  │
  ▼
Controller receives RegisterRequest
  │
  ▼
Validate input (username, email, password)
  │
  ▼
PasswordEncoder.encode(rawPassword)  ◀── BCrypt hash
  │
  ▼
Save User entity to DB
  │
  ▼
Return success response (+ optional JWT)
```

> **Defense-in-Depth:** JWT is verified at **API Gateway** (first layer) AND optionally re-verified at each **downstream microservice** (second layer).

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Service Discovery | Netflix Eureka |
| API Gateway | Spring Cloud Gateway (WebFlux) |
| Authentication | Spring Security + Auth0 JWT (HMAC256) |
| Inter-service Communication | OpenFeign |
| Load Balancing | Spring Cloud LoadBalancer |
| ORM | Spring Data JPA / Hibernate |
| Database | MySQL 8 |
| Cloud Storage | AWS S3 (`aws-java-sdk-s3`) |
| Messaging | Apache Kafka (hosted on AWS EC2) |
| Email | JavaMailSender (Gmail SMTP) |
| Build Tool | Maven |
| Testing | Postman |

---

## 📡 API Reference

### Auth Service (via Gateway: `localhost:5555`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/auth/api/v1/auth/register` | 🌐 Public | Register new user |
| `POST` | `/auth/api/v1/auth/login` | 🌐 Public | Login & receive JWT |
| `GET` | `/auth/api/v1/welcome/message` | 🔐 ROLE_ADMIN | Protected admin endpoint |

**Register Request:**
```json
{
  "username": "amresh",
  "name": "amresh",
  "email": "amresh@gmail.com",
  "password": "Ajay@123"
}
```

---

### Property Service (via Gateway: `localhost:5555`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/property/api/v1/property/add-property` | 🔐 JWT | Add property with photos (multipart) |
| `GET` | `/property/api/v1/property/search-property` | 🌐 Public | Search by city/area/state + date |
| `GET` | `/property/api/v1/property/property-id` | 🔐 JWT | Get property by ID |
| `GET` | `/property/api/v1/property/room-id` | 🔐 JWT | Get room details by ID |
| `GET` | `/property/api/v1/property/room-available-room-id` | 🔐 JWT | Get room availability |
| `PUT` | `/property/api/v1/property/updateRoomCount` | 🔐 JWT | Decrement room count |

**Add Property (form-data):**
```
Key: property (Text)
Value:
{
  "name": "Hotel Sunshine",
  "numberOfBeds": 4,
  "numberOfRooms": 2,
  "numberOfBathrooms": 2,
  "numberOfGuestAllowed": 6,
  "city": "bengaluru",
  "area": "BTM",
  "state": "karnatka",
  "rooms": [
    { "roomType": "Deluxe", "basePrice": 2500 },
    { "roomType": "Suite",  "basePrice": 4500 }
  ]
}

Key: files (File)
Value: [select image files]
```

**Search Property:**
```
GET /property/api/v1/property/search-property?name=chennai&date=2025-12-14
```

---

### Booking Service (`localhost:8085`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/booking/add-to-cart` | 🔐 JWT | Book a room |

**Booking Request:**
```json
{
  "propertyId": 1,
  "roomId": 1,
  "name": "Rahul Sharma",
  "email": "rahul.sharma@gmail.com",
  "mobile": "9876543210",
  "totalNigths": 3,
  "date": ["2025-12-17"]
}
```

---

## 🗄 Database Schema

### `propertyservicedb`
```
state        → id, name
city         → id, name
area         → id, name
property     → id, name, numberOfBeds, numberOfRooms, numberOfBathrooms,
               numberOfGuestAllowed, city_id, area_id, state_id
rooms        → id, roomType, basePrice, property_id
room_availability → id, availableDate, availableCount, price, room_id
property_photos   → id, url, property_id
```

### `bookingdb1`
```
bookings     → id, name, email, mobile, propertyName, totalPrice, status
booking_date → id, date, booking_id
```

### Required Seed Data (for testing search)
```sql
INSERT INTO state (name) VALUES ('Tamil Nadu');
INSERT INTO city (name) VALUES ('Chennai');
INSERT INTO area (name) VALUES ('Adyar');
INSERT INTO property (name, number_of_beds, number_of_rooms, number_of_bathrooms,
  number_of_guests_allowed, city_id, area_id, state_id)
VALUES ('Seaside Villa', 3, 2, 2, 6, 1, 1, 1);
INSERT INTO rooms (room_type, base_price, property_id) VALUES ('Deluxe', 3000.0, 1);
INSERT INTO room_availability (available_date, available_count, price, room_id)
VALUES ('2025-12-14', 2, 3000.0, 1);
```

---

## ⚙️ Setup & Installation

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8+
- AWS Account (S3 bucket + IAM credentials)
- Kafka running (AWS EC2 or local)

### Step-by-Step Startup Order

> ⚠️ **Services must be started in this exact order.**

```
1. MySQL — ensure databases created (propertyservicedb, bookingdb1)
2. Eureka Server       → http://localhost:8761
3. Auth Service        → http://localhost:8083
4. API Gateway         → http://localhost:5555
5. Property Service    → http://localhost:9091
6. Booking Service     → http://localhost:8085/9092
7. Notification Service → http://localhost:9093/9094
```

### AWS S3 Configuration (`application.properties` — Property Service)
```properties
cloud.aws.region.static=ap-south-1
cloud.aws.credentials.access-key=YOUR_ACCESS_KEY
cloud.aws.credentials.secret-key=YOUR_SECRET_KEY
aws.s3.bucket.name=your-bucket-name
```

### Kafka Configuration
```properties
spring.kafka.bootstrap-servers=YOUR_EC2_DNS:9092
```

### Gmail SMTP (Notification Service)
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your@gmail.com
spring.mail.password=YOUR_APP_PASSWORD
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```
> Get an App Password from: Gmail → Security → 2-Step Verification → App passwords

---

## 🔌 Service Ports

| Service | Port |
|---|---|
| Eureka Server | `8761` |
| API Gateway | `5555` |
| Auth Service | `8083` |
| Property Service | `9091` |
| Booking Service | `8085` / `9092` |
| Notification Service | `9093` / `9094` |

---

## 🧠 Key Design Concepts

### Load Balancing
This project uses **both** types of load balancing simultaneously:

| Type | Where Used | How |
|---|---|---|
| **Server-Side** | API Gateway | `lb://SERVICE-NAME` in routes |
| **Client-Side** | Feign Clients | Spring Cloud LoadBalancer (Round Robin) |

When a service scales to multiple instances, Eureka tracks them all and the load balancer distributes traffic automatically — no configuration changes needed.

### Feign vs RestTemplate vs WebClient

| | RestTemplate | WebClient | Feign |
|---|---|---|---|
| Type | Blocking | Non-blocking | Declarative |
| LB Support | ❌ Manual | ❌ Needs extra config | ✅ Auto |
| Best For | Legacy | Reactive/High throughput | Microservices |
| Status | ⚠️ Deprecated | ✅ Modern | ✅ Recommended |

This project uses **Feign** for all inter-service communication.

### Event-Driven Notifications
Property creation triggers an async email via Kafka:
```
PropertyService ──(KafkaTemplate)──▶ [send_email topic] ──(KafkaListener)──▶ NotificationService ──▶ Gmail
```
This ensures the property creation API doesn't block on email delivery.

---

<div align="center">

**Built with ❤️ using Spring Boot Microservices**

*Service Discovery • JWT Security • AWS S3 • Apache Kafka • OpenFeign • Load Balancing*

</div>
