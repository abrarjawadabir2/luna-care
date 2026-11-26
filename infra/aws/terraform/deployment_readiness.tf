# Fail closed: this repository has no verified production artifact deployment, and
# backend/src/app.ts deliberately refuses production startup with in-memory data.
# These Terraform resources are templates, not a ready-to-apply release stack.
resource "terraform_data" "deployment_readiness_gate" {
  input = {
    approved   = var.deployment_authorized
    persistent = var.backend_persistence_verified
    bootstrapped = var.backend_runtime_verified
  }

  lifecycle {
    precondition {
      condition     = var.deployment_authorized && var.backend_persistence_verified && var.backend_runtime_verified
      error_message = "AWS deployment is blocked until owner approval, real persistent backend storage, and a verified EC2 runtime artifact/boot process are complete."
    }
  }
}
