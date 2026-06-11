variable "project" {
  type = string
}

variable "services" {
  type = list(string)
  default = [
    "api-gateway",
    "user-service",
    "account-service",
    "transaction-service",
    "notification-service",
    "config-server",
    "eureka-server"
  ]
}
