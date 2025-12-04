# ecs.tf - ECS Cluster and Task Definitions

# ECS Cluster
resource "aws_ecs_cluster" "main" {
  name = "${var.project_name}-cluster"

  setting {
    name  = "containerInsights"
    value = "disabled"
  }

  tags = {
    Name = "${var.project_name}-cluster"
  }
}

# IAM Role for ECS Task Execution
resource "aws_iam_role" "ecs_execution" {
  name = "${var.project_name}-ecs-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_execution" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# IAM Role for ECS Task
resource "aws_iam_role" "ecs_task" {
  name = "${var.project_name}-ecs-task-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }
    }]
  })
}

# CloudWatch Log Group
resource "aws_cloudwatch_log_group" "ecs" {
  name              = "/ecs/${var.project_name}"
  retention_in_days = 7
}

# Service Discovery Namespace
resource "aws_service_discovery_private_dns_namespace" "main" {
  name        = "${var.project_name}.local"
  description = "Service discovery for ${var.project_name}"
  vpc         = aws_vpc.main.id
}

# Service Discovery Services
resource "aws_service_discovery_service" "services" {
  for_each = var.services

  name = each.key

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.main.id

    dns_records {
      ttl  = 10
      type = "A"
    }

    routing_policy = "MULTIVALUE"
  }

  health_check_custom_config {
    failure_threshold = 1
  }
}

# Task Definitions
resource "aws_ecs_task_definition" "api_gateway" {
  family                   = "${var.project_name}-api-gateway"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.services["api-gateway"].cpu
  memory                   = var.services["api-gateway"].memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name  = "api-gateway"
    image = "${aws_ecr_repository.services["api-gateway"].repository_url}:latest"

    portMappings = [{
      containerPort = 8080
      hostPort      = 8080
      protocol      = "tcp"
    }]

    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "aws" },
      { name = "USER_SERVICE_URL", value = "http://user-service.${var.project_name}.local:8081" },
      { name = "TRIP_SERVICE_URL", value = "http://trip-service.${var.project_name}.local:8082" },
      { name = "DRIVER_SERVICE_URL", value = "http://driver-service.${var.project_name}.local:8083" }
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.ecs.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "api-gateway"
      }
    }

    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
  }])
}

resource "aws_ecs_task_definition" "user_service" {
  family                   = "${var.project_name}-user-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.services["user-service"].cpu
  memory                   = var.services["user-service"].memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name  = "user-service"
    image = "${aws_ecr_repository.services["user-service"].repository_url}:latest"

    portMappings = [{
      containerPort = 8081
      hostPort      = 8081
      protocol      = "tcp"
    }]

    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "aws" },
      { name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://${aws_db_instance.user_db.endpoint}/user_service_db" },
      { name = "SPRING_DATASOURCE_USERNAME", value = var.db_username },
      { name = "SPRING_DATASOURCE_PASSWORD", value = var.db_password }
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.ecs.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "user-service"
      }
    }

    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:8081/actuator/health || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
  }])
}

resource "aws_ecs_task_definition" "trip_service" {
  family                   = "${var.project_name}-trip-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.services["trip-service"].cpu
  memory                   = var.services["trip-service"].memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name  = "trip-service"
    image = "${aws_ecr_repository.services["trip-service"].repository_url}:latest"

    portMappings = [{
      containerPort = 8082
      hostPort      = 8082
      protocol      = "tcp"
    }]

    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "aws" },
      { name = "SPRING_DATASOURCE_VN_URL", value = "jdbc:postgresql://${aws_db_instance.trip_db_vn.endpoint}/trip_service_db" },
      { name = "SPRING_DATASOURCE_TH_URL", value = "jdbc:postgresql://${aws_db_instance.trip_db_th.endpoint}/trip_service_db" },
      { name = "SPRING_DATASOURCE_USERNAME", value = var.db_username },
      { name = "SPRING_DATASOURCE_PASSWORD", value = var.db_password },
      { name = "SPRING_RABBITMQ_HOST", value = replace(aws_mq_broker.rabbitmq.instances[0].endpoints[0], "amqps://", "") },
      { name = "SPRING_RABBITMQ_PORT", value = "5671" },
      { name = "SPRING_RABBITMQ_USERNAME", value = "guest" },
      { name = "SPRING_RABBITMQ_PASSWORD", value = var.db_password }
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.ecs.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "trip-service"
      }
    }

    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:8082/actuator/health || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
  }])
}

resource "aws_ecs_task_definition" "driver_service" {
  family                   = "${var.project_name}-driver-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.services["driver-service"].cpu
  memory                   = var.services["driver-service"].memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name  = "driver-service"
    image = "${aws_ecr_repository.services["driver-service"].repository_url}:latest"

    portMappings = [{
      containerPort = 8083
      hostPort      = 8083
      protocol      = "tcp"
    }]

    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "aws" },
      { name = "SPRING_DATA_REDIS_HOST", value = aws_elasticache_cluster.redis.cache_nodes[0].address },
      { name = "SPRING_DATA_REDIS_PORT", value = "6379" },
      { name = "SPRING_RABBITMQ_HOST", value = replace(aws_mq_broker.rabbitmq.instances[0].endpoints[0], "amqps://", "") },
      { name = "SPRING_RABBITMQ_PORT", value = "5671" },
      { name = "SPRING_RABBITMQ_USERNAME", value = "guest" },
      { name = "SPRING_RABBITMQ_PASSWORD", value = var.db_password }
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.ecs.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "driver-service"
      }
    }

    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:8083/actuator/health || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
  }])
}

resource "aws_ecs_task_definition" "driver_simulator" {
  family                   = "${var.project_name}-driver-simulator"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.services["driver-simulator"].cpu
  memory                   = var.services["driver-simulator"].memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name  = "driver-simulator"
    image = "${aws_ecr_repository.services["driver-simulator"].repository_url}:latest"

    portMappings = [{
      containerPort = 8084
      hostPort      = 8084
      protocol      = "tcp"
    }]

    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "aws" },
      { name = "DRIVER_SERVICE_URL", value = "http://driver-service.${var.project_name}.local:8083" }
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.ecs.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "driver-simulator"
      }
    }
  }])
}
