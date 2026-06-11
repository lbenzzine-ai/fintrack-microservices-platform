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

# RDS Module
module "rds" {
  source             = "../../modules/rds"
  project            = var.project
  vpc_id             = module.vpc.vpc_id
  vpc_cidr           = var.vpc_cidr
  private_subnet_ids = module.vpc.private_subnet_ids
  db_password        = var.db_password
}

# EKS Module
module "eks" {
  source             = "../../modules/eks"
  project            = var.project
  aws_region         = var.aws_region
  cluster_version    = var.eks_cluster_version
  vpc_id             = module.vpc.vpc_id
  public_subnet_ids  = module.vpc.public_subnet_ids
  private_subnet_ids = module.vpc.private_subnet_ids
}
