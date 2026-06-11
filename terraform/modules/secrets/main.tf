# JWT Secret
resource "aws_secretsmanager_secret" "jwt_secret" {
  name                    = "${var.project}/jwt-secret"
  description             = "JWT signing secret for FinTrack"
  recovery_window_in_days = 0  # instant delete for dev

  tags = { Name = "${var.project}-jwt-secret" }
}

resource "aws_secretsmanager_secret_version" "jwt_secret" {
  secret_id     = aws_secretsmanager_secret.jwt_secret.id
  secret_string = var.jwt_secret
}

# DB Passwords — one per service
resource "aws_secretsmanager_secret" "db_passwords" {
  for_each                = toset(var.services)
  name                    = "${var.project}/db/${each.key}/password"
  description             = "DB password for ${each.key}"
  recovery_window_in_days = 0

  tags = { Name = "${var.project}-db-${each.key}-password" }
}

resource "aws_secretsmanager_secret_version" "db_passwords" {
  for_each      = toset(var.services)
  secret_id     = aws_secretsmanager_secret.db_passwords[each.key].id
  secret_string = var.db_password
}

# Actuator credentials
resource "aws_secretsmanager_secret" "actuator" {
  name                    = "${var.project}/actuator"
  description             = "Actuator credentials"
  recovery_window_in_days = 0

  tags = { Name = "${var.project}-actuator" }
}

resource "aws_secretsmanager_secret_version" "actuator" {
  secret_id = aws_secretsmanager_secret.actuator.id
  secret_string = jsonencode({
    username = "actuator"
    password = var.actuator_password
  })
}
