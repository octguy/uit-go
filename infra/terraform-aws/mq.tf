# mq.tf - Amazon MQ for RabbitMQ

resource "aws_mq_broker" "rabbitmq" {
  broker_name = "${var.project_name}-rabbitmq"

  engine_type        = "RabbitMQ"
  engine_version     = "3.13"
  host_instance_type = "mq.t3.micro"
  deployment_mode    = "SINGLE_INSTANCE"

  security_groups = [aws_security_group.rabbitmq.id]
  subnet_ids      = [aws_subnet.private[0].id]

  user {
    username = "guest"
    password = var.db_password # Reuse the same password for simplicity
  }

  publicly_accessible = false

  tags = {
    Name = "${var.project_name}-rabbitmq"
  }
}
