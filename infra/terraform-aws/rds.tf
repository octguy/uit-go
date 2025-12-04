# rds.tf - RDS PostgreSQL Databases

# DB Subnet Group
resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-db-subnet"
  subnet_ids = aws_subnet.private[*].id

  tags = {
    Name = "${var.project_name}-db-subnet"
  }
}

# User Service Database
resource "aws_db_instance" "user_db" {
  identifier     = "${var.project_name}-user-db"
  engine         = "postgres"
  engine_version = "15.4"
  instance_class = "db.t3.micro"

  allocated_storage     = 20
  max_allocated_storage = 50
  storage_type          = "gp2"

  db_name  = "user_service_db"
  username = var.db_username
  password = var.db_password

  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = aws_db_subnet_group.main.name

  skip_final_snapshot = true
  publicly_accessible = false

  tags = {
    Name    = "${var.project_name}-user-db"
    Service = "user-service"
  }
}

# Trip Service Database (VN region)
resource "aws_db_instance" "trip_db_vn" {
  identifier     = "${var.project_name}-trip-db-vn"
  engine         = "postgres"
  engine_version = "15.4"
  instance_class = "db.t3.micro"

  allocated_storage     = 20
  max_allocated_storage = 50
  storage_type          = "gp2"

  db_name  = "trip_service_db"
  username = var.db_username
  password = var.db_password

  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = aws_db_subnet_group.main.name

  skip_final_snapshot = true
  publicly_accessible = false

  tags = {
    Name    = "${var.project_name}-trip-db-vn"
    Service = "trip-service"
    Region  = "VN"
  }
}

# Trip Service Database (TH region)
resource "aws_db_instance" "trip_db_th" {
  identifier     = "${var.project_name}-trip-db-th"
  engine         = "postgres"
  engine_version = "15.4"
  instance_class = "db.t3.micro"

  allocated_storage     = 20
  max_allocated_storage = 50
  storage_type          = "gp2"

  db_name  = "trip_service_db"
  username = var.db_username
  password = var.db_password

  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = aws_db_subnet_group.main.name

  skip_final_snapshot = true
  publicly_accessible = false

  tags = {
    Name    = "${var.project_name}-trip-db-th"
    Service = "trip-service"
    Region  = "TH"
  }
}
