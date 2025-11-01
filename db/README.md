# Database Management

This directory contains database schemas and scripts for all UIT-Go microservices.

## Structure

- `user-service-db/`: User service database schema and scripts
- `trip-service-db/`: Trip service database schema and scripts  
- `driver-service-db/`: Driver service database schema and scripts
- `shared/`: Common database utilities and Docker Compose configurations

## Database Setup

### Using Docker Compose (Recommended for Development)

1. Start all databases:
```bash
cd db/shared
docker-compose -f docker-compose-db.yml up -d
```

2. Database connections:
- **User Service DB**: `localhost:5432/user_service_db`
- **Trip Service DB**: `localhost:5433/trip_service_db`
- **Driver Service DB**: `localhost:5434/driver_service_db`
- **RabbitMQ**: `localhost:5672` (Management UI: `localhost:15672`)

### Manual Setup

1. Create databases using the init script:
```bash
psql -U postgres -f shared/init-all-dbs.sql
```

2. Run individual schemas:
```bash
psql -U user_service_user -d user_service_db -f user-service-db/schema.sql
psql -U trip_service_user -d trip_service_db -f trip-service-db/schema.sql
psql -U driver_service_user -d driver_service_db -f driver-service-db/schema.sql
```

## Database Credentials

### Development Environment
- **User Service**: `user_service_user` / `user_service_pass`
- **Trip Service**: `trip_service_user` / `trip_service_pass`
- **Driver Service**: `driver_service_user` / `driver_service_pass`

### RabbitMQ
- **Username**: `admin`
- **Password**: `admin123`

## Schema Overview

### User Service Database
- `users`: User accounts (passengers and drivers)
- `user_profiles`: Extended user information
- `user_sessions`: Authentication sessions

### Trip Service Database
- `trips`: Trip requests and details
- `trip_status_history`: Status change tracking
- `trip_ratings`: Trip ratings and reviews

### Driver Service Database
- `drivers`: Driver-specific information
- `driver_locations`: Real-time location tracking
- `driver_availability`: Availability windows
- `driver_documents`: Document verification
- `driver_earnings`: Earnings and payouts

## Migration Strategy

Each service manages its own database migrations independently:
- Use Flyway or Liquibase for version control
- Environment-specific configurations
- Rollback capabilities for safe deployments