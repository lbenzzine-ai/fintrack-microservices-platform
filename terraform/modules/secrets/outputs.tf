output "jwt_secret_arn" {
  value = aws_secretsmanager_secret.jwt_secret.arn
}

output "db_password_arns" {
  value = {
    for k, v in aws_secretsmanager_secret.db_passwords : k => v.arn
  }
}

output "actuator_secret_arn" {
  value = aws_secretsmanager_secret.actuator.arn
}
