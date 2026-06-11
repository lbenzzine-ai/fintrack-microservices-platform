variable "project" {
  type = string
}

variable "jwt_secret" {
  type      = string
  sensitive = true
}

variable "db_password" {
  type      = string
  sensitive = true
}

variable "actuator_password" {
  type      = string
  sensitive = true
  default   = "actuator123!"
}

variable "services" {
  type    = list(string)
  default = [
    "user-service",
    "account-service",
    "transaction-service",
    "notification-service"
  ]
}
