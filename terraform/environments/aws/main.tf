# VPC Module
module "vpc" {
  source   = "../../modules/vpc"
  project  = var.project
  vpc_cidr = var.vpc_cidr
}

# Secrets Module
module "secrets" {
  source      = "../../modules/secrets"
  project     = var.project
  jwt_secret  = var.jwt_secret
  db_password = var.db_password
}

# ECR Module
module "ecr" {
  source  = "../../modules/ecr"
  project = var.project
}
