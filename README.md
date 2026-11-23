# Mini Marketplace

A full-stack marketplace web application built with Spring Boot, Thymeleaf, and PostgreSQL.

Users can browse products, place orders, and manage activity based on role:
- Buyer: browse products, checkout, track own orders, leave reviews
- Seller: manage products, view seller orders, update order status
- Admin: manage users, products, orders, categories, and home slides

## Quick start with Docker Compose

Requires Docker Engine or Docker Desktop with Compose.

```bash
cp .env.example .env
```

Set `DB_PASSWORD` and `ADMIN_PASSWORD` in `.env` before starting the services.

```bash
docker compose up --build -d
docker compose ps
```

Open [the catalog](http://localhost:8080). Register a buyer or seller account, or
sign in as admin using the email and password from `.env`. Stop the stack with
`docker compose down`.

[Render deployment link](https://mini-marketplace-26bl.onrender.com)

## Architecture Diagram

![Architecture Diagram](images/architecture-diagram.png)

## ER Diagram

![ER Diagram](images/ER.png)

## Tech Stack

- Backend: Java 17, Spring Boot, Spring Security, Spring Data JPA
- Frontend: Thymeleaf, Bootstrap 5, custom CSS
- Database: PostgreSQL, H2 for local integration tests
- Build Tool: Maven Wrapper
- Containerization: Docker, Docker Compose
- CI: GitHub Actions
- Deployment: Render

## Project Structure

```text
src/main/java/com/hasan/marketplace
  controller/
  controller/api/
  service/
  service/impl/
  repository/
  entity/
  dto/
  config/

src/main/resources
  templates/
  static/
  application.yml
```

## API Endpoints (REST)

Base path examples below are from controllers in `controller/api`.

### Product API (`/api/products`)

- `GET /api/products` : list/search products (`keyword`, `categoryId` optional)
- `GET /api/products/{id}` : get product by id
- `POST /api/products` : create product (seller/admin)
- `PUT /api/products/{id}` : update product (seller/admin)
- `DELETE /api/products/{id}` : delete product (seller/admin)

### Order API (`/api/orders`)

- `POST /api/orders` : place order (buyer/admin context)
- `GET /api/orders` : list orders by role (buyer/seller/admin)
- `GET /api/orders/{id}` : order details by role access
- `PATCH /api/orders/{id}/status?status=...` : update status (seller)

### Category API (`/api/categories`)

- `GET /api/categories` : list categories
- `GET /api/categories/{id}` : get category by id
- `POST /api/categories` : create category (admin)
- `PUT /api/categories/{id}` : update category (admin)
- `DELETE /api/categories/{id}` : delete category (admin)

## Main Web Routes (Thymeleaf)

- `GET /` : home + catalog
- `GET /login` : login page
- `GET /register`, `POST /register` : registration
- `GET /products`, `GET /products/{id}` : product listing/details
- `POST /products/{id}/reviews` : add/update review
- `GET /checkout/{productId}` : checkout page
- `POST /orders`, `GET /orders`, `GET /orders/{id}` : buyer/admin order flow
- `GET /seller/dashboard`, `GET /seller/orders` : seller area
- `GET /admin` : admin dashboard
- `GET /admin/users` : user management view
- `GET /admin/products` : product management view
- `GET /admin/orders` : order management view
- `GET /admin/categories` : category management view
- `GET /admin/home-slides` : home carousel management view

## Run without Docker

With Java 17 and a local PostgreSQL database, export `DB_URL`, `DB_USERNAME`,
`DB_PASSWORD`, and `ADMIN_PASSWORD` to your shell. You can also set
`ADMIN_EMAIL` (defaults to `admin@marketplace.com`). Maven does not load the
Compose `.env` file. PostgreSQL must already have the database in `DB_URL`.

```bash
./mvnw spring-boot:run
```

Open [the catalog](http://localhost:8080). On Windows, use `mvnw.cmd` in place of
`./mvnw`.

## Tests and CI

```bash
./mvnw clean verify
```

The suite covers authentication, products, and order workflows at the controller,
service, and integration levels. [GitHub Actions](.github/workflows/ci.yml)
runs the Maven verification job. See the [test sources](src/test) for coverage.

The production deployment uses runtime environment variables configured in Render.
