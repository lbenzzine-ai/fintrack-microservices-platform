output "cluster_name" {
  value = aws_eks_cluster.fintrack.name
}

output "cluster_endpoint" {
  value = aws_eks_cluster.fintrack.endpoint
}

output "cluster_ca" {
  value = aws_eks_cluster.fintrack.certificate_authority[0].data
}

output "fargate_role_arn" {
  value = aws_iam_role.fargate.arn
}
