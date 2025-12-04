# HMS BACKEND

HMS_BACKEND — Hệ thống quản lý bệnh viện (Hospital Management System) là dự án môn học CNPMCS được tổ chức theo kiến
trúc microservices. Dự án sử dụng Spring Boot / Spring Cloud, chia thành nhiều module (config server, discovery, API
gateway, dịch vụ nghiệp vụ như medicine và auth), có cấu hình để chạy bằng Docker Compose.

## Installation

Yêu cầu trước khi cài:

- Java 23 (Có thể không cần nếu chạy bằng Docker)
- Docker & Docker Compose

#### Local:

- Dựa vào .env.example bổ sung đầy đủ các trường cần thiết
- Cần bổ sung tối thiểu các field sau đây vào .env

```
# Nên bật tính năng này
DOCKER_COMPOSE_ENABLED=true

# DISCOVERY SERVICE
DISCOVERY_SERVICE_PORT=8761
DISCOVERY_SERVICE_HOST=localhost

# CONFIG SERVER
CONFIG_SERVER_PORT=8888
CONFIG_SERVER_HOST=localhost

# API GATEWAY
API_GATEWAY=8080

# MEDICINE SERVICE
MEDICINE_SERVICE_PORT=8081
MEDICINE_DB_HOST=localhost
MEDICINE_DB_PORT=3306
MEDICINE_DB_NAME=mydatabase
MEDICINE_DB_USERNAME=myuser
MEDICINE_DB_PASSWORD=secret
# docker compose file for medicine service, if not using: default ../../infrastructure/dev/medicine/compose.yml, only used if DOCKER_COMPOSE_ENABLED=true
# DOCKER_COMPOSE_FILE_MEDICINE_SERVICE=

# AUTH SERVICE
AUTH_SERVICE_PORT=8082
AUTH_DB_HOST=localhost
AUTH_DB_PORT=3307
AUTH_DB_NAME=mydatabase
AUTH_DB_USERNAME=myuser
AUTH_DB_PASSWORD=secret
JWT_PRIVATE_KEY=---private key---
JWT_PUBLIC_KEY=---public key--
JWT_ACCESS_TOKEN_EXPIRATION=
JWT_REFRESH_TOKEN_EXPIRATION=
# docker compose file for auth service, if not using: default ../../infrastructure/dev/auth-service/compose.yml, only used if DOCKER_COMPOSE_ENABLED=true
# DOCKER_COMPOSE_FILE_AUTH_SERVICE=

PATIENT_DB_HOST=localhost
PATIENT_DB_PORT=3306
PATIENT_DB_NAME=mydatabase
PATIENT_DB_USERNAME=myuser
PATIENT_DB_PASSWORD=secret
PATIENT_SERVICE_PORT=8083
```

- DOCKER_COMPOSE_ENABLED giúp tự khởi chạy compose có sẵn ở infrastructure/dev/{service}
- Nếu cần cấu hình lại Docker-Compose cho các ứng dụng chỉ cần tạo mới file Docker-Compose và thêm lại vào đường dẫn
  trên .env

Build toàn bộ project từ thư mục gốc:

```
./gradlew clean build
```

Nếu cần build và chạy module cụ thể thì bạn có thể sử dụng như sau

```
./gradlew clean :config-server:build
```

Thứ tự khuyến nghị khi chạy local:

1. config-server
2. discovery-service
3. auth-service
4. medicine-service
5. api-gateway (nên chạy api-gateway sau cùng để giảm tình trạng tất cả service đều chạy nhưng 30s sau mới hoạt động
   được)

```bash
# chạy Config Server
./gradlew :config-server:bootRun

# chạy Discovery (Eureka)
./gradlew :discovery-service:bootRun

# chạy Auth service
./gradlew :auth-service:bootRun

# chạy Medicine service
./gradlew :medicine-service:bootRun

# chạy API Gateway (cuối cùng)
./gradlew :api-gateway:bootRun
```

#### Docker Compose:

Từ .env.example bổ sung .env vào vị trí

```
/infrastructure/pro/.env
```

với cấu hình .env khuyến nghị như sau

```
# Khi chạy bằng docker compose nên tắt tính năng này
# Trong compose đã đầy đủ database nên không cần thêm
DOCKER_COMPOSE_ENABLED=false

# DISCOVERY SERVICE
DISCOVERY_SERVICE_PORT=8761

# Không nên thay biến này, tương ứng với tên service trong compose
# Biến này dùng để khai báo trong config
DISCOVERY_SERVICE_HOST=discovery-service-pro

# CONFIG SERVER
CONFIG_SERVER_PORT=8888

# Không nên thay biến này, tương ứng với tên service trong compose
# Biến này dùng để khai báo trong config
CONFIG_SERVER_HOST=config-server-pro

# API GATEWAY
API_GATEWAY_PORT=8080

# MEDICINE SERVICE
# Không nên thay biến này, tương ứng với tên service trong compose
# Biến này dùng để khai báo trong config
MEDICINE_DB_HOST=mysql-medicine-service

# Không nên thay biến này
# Biến này dùng để khai báo trong config
MEDICINE_DB_PORT=3306

MEDICINE_DB_NAME=mydatabase
MEDICINE_DB_USERNAME=myuser
MEDICINE_DB_PASSWORD=secret
MEDICINE_SERVICE_PORT=8081

# AUTH SERVICE
# Không nên thay biến này, tương ứng với tên service trong compose
# Biến này dùng để khai báo trong config
AUTH_DB_HOST=mysql-auth-service

# Không nên thay biến này
# Biến này dùng để khai báo trong config
AUTH_DB_PORT=3306

AUTH_DB_NAME=mydatabase
AUTH_DB_USERNAME=myuser
AUTH_DB_PASSWORD=secret
AUTH_SERVICE_PORT=8082

JWT_PRIVATE_KEY=---private key---
JWT_PUBLIC_KEY=---public key--

# Không nên thay biến này, tương ứng với tên service trong compose
# Biến này dùng để khai báo trong config
AUTH_DB_HOST=mysql-auth-service
PATIENT_DB_HOST=mysql-medicine-service

# Không nên thay biến này
# Biến này dùng để khai báo trong config
PATIENT_DB_PORT=3306

PATIENT_DB_NAME=mydatabase
PATIENT_DB_USERNAME=myuser
PATIENT_DB_PASSWORD=secret
PATIENT_SERVICE_PORT=8083
```

Chạy Docker Compose

```
cd /infrastructure/pro
docker compose  -f compose.pro.yaml up -d
```

## Usage

Ví dụ endpoint (một số endpoint cơ bản, tùy cấu hình thực tế của từng module):

- Health check (Spring Actuator): http://localhost:<PORT>/{service-name}/actuator/health
- Eureka dashboard: http://localhost:<EUREKA_PORT>/ (thường là discovery-service)
- API Gateway entrypoint: http://localhost:<GATEWAY_PORT>/

Chi tiết các endpoint cụ thể đã được lưu tạm trong `postman/api.json`. Có thể dùng postman để import collection.

## Tech Stack

- Java 23
- Spring Boot (multi-service)
- Spring Cloud (Eureka discovery, Config Server, Gateway)
- Spring Security / OAuth2 (Auth service)
- Spring Data JPA (kết nối DB)


