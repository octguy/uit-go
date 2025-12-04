# ecs-services.tf - ECS Services

# API Gateway Service (public facing)
resource "aws_ecs_service" "api_gateway" {
  name            = "api-gateway"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.api_gateway.arn
  desired_count   = var.services["api-gateway"].replicas
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.api_gateway.arn
    container_name   = "api-gateway"
    container_port   = 8080
  }

  service_registries {
    registry_arn = aws_service_discovery_service.services["api-gateway"].arn
  }

  depends_on = [aws_lb_listener.http]
}

# User Service
resource "aws_ecs_service" "user_service" {
  name            = "user-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.user_service.arn
  desired_count   = var.services["user-service"].replicas
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  service_registries {
    registry_arn = aws_service_discovery_service.services["user-service"].arn
  }

  depends_on = [aws_db_instance.user_db]
}

# Trip Service
resource "aws_ecs_service" "trip_service" {
  name            = "trip-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.trip_service.arn
  desired_count   = var.services["trip-service"].replicas
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  service_registries {
    registry_arn = aws_service_discovery_service.services["trip-service"].arn
  }

  depends_on = [aws_db_instance.trip_db_vn, aws_db_instance.trip_db_th, aws_mq_broker.rabbitmq]
}

# Driver Service
resource "aws_ecs_service" "driver_service" {
  name            = "driver-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.driver_service.arn
  desired_count   = var.services["driver-service"].replicas
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  service_registries {
    registry_arn = aws_service_discovery_service.services["driver-service"].arn
  }

  depends_on = [aws_elasticache_cluster.redis, aws_mq_broker.rabbitmq]
}

# Driver Simulator Service
resource "aws_ecs_service" "driver_simulator" {
  name            = "driver-simulator"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.driver_simulator.arn
  desired_count   = var.services["driver-simulator"].replicas
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  service_registries {
    registry_arn = aws_service_discovery_service.services["driver-simulator"].arn
  }

  depends_on = [aws_ecs_service.driver_service]
}
