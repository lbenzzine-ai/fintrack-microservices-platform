output "db_endpoint" {
  value = aws_db_instance.fintrack.endpoint
}

output "db_host" {
  value = aws_db_instance.fintrack.address
}

output "db_port" {
  value = aws_db_instance.fintrack.port
}

output "db_name" {
  value = aws_db_instance.fintrack.db_name
}
