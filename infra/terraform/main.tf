terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 3.0"
    }
  }
}

provider "docker" {
  host = "npipe:////.//pipe//docker_engine" // Windows Docker
}

resource "docker_image" "nginx" {
  name = "nginx:latest"
}

resource "docker_container" "hello" {
  name  = "hello-terraform"
  image = docker_image.nginx.image_id
  ports {
    internal = 80
    external = 8888 # Changed from 9080
  }
}