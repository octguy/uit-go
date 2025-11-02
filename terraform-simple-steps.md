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

resource "docker_image" "postgres" {
  name = "postgres:15"
}

resource "docker_container" "user_db" {
  name  = "user-db-terraform"
  image = docker_image.postgres.image_id
  env = [
    "POSTGRES_DB=user_service_db",
    "POSTGRES_USER=user_service_user", 
    "POSTGRES_PASSWORD=password123"
  ]
  ports {
    internal = 5432
    external = 5436  # No conflict with your 5433-5435
  }
}