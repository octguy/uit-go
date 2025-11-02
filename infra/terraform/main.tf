terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 3.0"
    }
  }
}

provider "docker" {
  host = "npipe:////.//pipe//docker_engine"
}

resource "docker_network" "app_net" {
  name = "app_net"
}

resource "docker_image" "postgres" {
  name = "postgres:15"
}

# User Service Database
resource "docker_container" "user_db" {
  name  = "user-service-db-tf"
  image = docker_image.postgres.image_id
  env = [
    "POSTGRES_DB=user_service_db",
    "POSTGRES_USER=user_service_user", 
    "POSTGRES_PASSWORD=password123"
  ]
  ports {
    internal = 5432
    external = 5433
  }
  networks_advanced {
    name = docker_network.app_net.name
  }
}

# User Service
resource "docker_container" "user_service" {
  name  = "user-service-tf"
  image = "uit-go/user-service:latest"
  depends_on = [docker_container.user_db]
  env = [
    "DB_HOST=user-service-db-tf",
    "DB_USERNAME=user_service_user",
    "DB_PASSWORD=password123"
  ]
  ports {
    internal = 8081
    external = 8081
  }
  networks_advanced {
    name = docker_network.app_net.name
  }
}

# Trip Service Database
resource "docker_container" "trip_db" {
  name  = "trip-service-db-tf"
  image = docker_image.postgres.image_id
  env = [
    "POSTGRES_DB=trip_service_db",
    "POSTGRES_USER=trip_service_user", 
    "POSTGRES_PASSWORD=password123"
  ]
  ports {
    internal = 5432
    external = 5434
  }
  networks_advanced {
    name = docker_network.app_net.name
  }
}

# Trip Service
resource "docker_container" "trip_service" {
  name  = "trip-service-tf"
  image = "uit-go/trip-service:latest"
  depends_on = [docker_container.trip_db]
  env = [
    "DB_HOST=trip-service-db-tf",
    "DB_USERNAME=trip_service_user",
    "DB_PASSWORD=password123"
  ]
  ports {
    internal = 8082
    external = 8082
  }
  networks_advanced {
    name = docker_network.app_net.name
  }
}

# Driver Service Database
resource "docker_container" "driver_db" {
  name  = "driver-service-db-tf"
  image = docker_image.postgres.image_id
  env = [
    "POSTGRES_DB=driver_service_db",
    "POSTGRES_USER=driver_service_user", 
    "POSTGRES_PASSWORD=password123"
  ]
  ports {
    internal = 5432
    external = 5435
  }
  networks_advanced {
    name = docker_network.app_net.name
  }
}

# Driver Service
resource "docker_container" "driver_service" {
  name  = "driver-service-tf"
  image = "uit-go/driver-service:latest"
  depends_on = [docker_container.driver_db]
  env = [
    "DB_HOST=driver-service-db-tf",
    "DB_USERNAME=driver_service_user",
    "DB_PASSWORD=password123"
  ]
  ports {
    internal = 8083
    external = 8083
  }
  networks_advanced {
    name = docker_network.app_net.name
  }
}