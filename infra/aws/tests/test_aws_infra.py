"""
LunaCare AWS Infrastructure Static Security & Configuration Tests
Validates Terraform infrastructure specifications against production security requirements.
"""

import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent.parent
TF_DIR = REPO_ROOT / "infra" / "aws" / "terraform"

class TestAwsInfraSecurity(unittest.TestCase):

    def test_security_groups_no_open_ssh(self):
        """Port 22 SSH must never be open to the internet or defined in ingress blocks."""
        sg_file = (TF_DIR / "security_groups.tf").read_text(encoding="utf-8")
        self.assertNotIn("from_port   = 22", sg_file)
        self.assertNotIn("to_port     = 22", sg_file)
        self.assertNotIn("from_port = 22", sg_file)
        self.assertNotIn("to_port = 22", sg_file)

    def test_database_security_group_restricted(self):
        """PostgreSQL database security group must only accept ingress from app security group."""
        sg_file = (TF_DIR / "security_groups.tf").read_text(encoding="utf-8")
        self.assertIn("aws_security_group\" \"db\"", sg_file)
        self.assertIn('resource "aws_vpc_security_group_ingress_rule" "db_from_app"', sg_file)
        self.assertIn('referenced_security_group_id = aws_security_group.app.id', sg_file)
        self.assertNotIn('resource "aws_vpc_security_group_ingress_rule" "db_public"', sg_file)

    def test_app_security_group_restricted(self):
        """Backend application security group must accept port 3000 strictly from ALB."""
        sg_file = (TF_DIR / "security_groups.tf").read_text(encoding="utf-8")
        self.assertIn("from_port       = 3000", sg_file)
        self.assertIn('resource "aws_vpc_security_group_ingress_rule" "app_from_alb"', sg_file)
        self.assertIn("referenced_security_group_id = aws_security_group.alb.id", sg_file)

    def test_group_references_do_not_create_a_cycle(self):
        """Group identities must be separate from SG-to-SG ingress/egress rules."""
        sg = (TF_DIR / "security_groups.tf").read_text(encoding="utf-8")
        self.assertIn('resource "aws_vpc_security_group_egress_rule" "alb_to_app"', sg)
        self.assertIn('resource "aws_vpc_security_group_egress_rule" "app_to_db"', sg)
        self.assertIn('resource "aws_vpc_security_group_ingress_rule" "db_from_app"', sg)
        self.assertNotIn('  egress {', sg)
        self.assertNotIn('  ingress {', sg)

    def test_imds_v2_enforced(self):
        """Launch template must enforce IMDSv2 to defend against SSRF credential theft."""
        ec2_file = (TF_DIR / "ec2_asg.tf").read_text(encoding="utf-8")
        self.assertIn('http_tokens                 = "required"', ec2_file,
                      "IMDSv2 http_tokens must be set to 'required'")
        self.assertIn("http_put_response_hop_limit = 1", ec2_file,
                      "Hop limit must be 1 to prevent container-based token exfiltration")

    def test_ebs_encryption_enforced(self):
        """Root EBS volume must be KMS encrypted."""
        ec2_file = (TF_DIR / "ec2_asg.tf").read_text(encoding="utf-8")
        self.assertIn("encrypted             = true", ec2_file,
                      "EBS volume must have encrypted = true")

    def test_rds_not_publicly_accessible(self):
        """RDS PostgreSQL must not be publicly accessible and must be encrypted."""
        rds_file = (TF_DIR / "rds.tf").read_text(encoding="utf-8")
        self.assertIn("publicly_accessible         = false", rds_file,
                      "RDS must have publicly_accessible = false")
        self.assertIn("storage_encrypted           = true", rds_file,
                      "RDS must have storage_encrypted = true")

    def test_outputs_zero_secrets(self):
        """Terraform outputs must not expose sensitive keys, passwords, or tokens."""
        outputs_file = (TF_DIR / "outputs.tf").read_text(encoding="utf-8")
        forbidden = ["password", "secret_string", "jwt", "gemini_api_key", "master_user_password"]
        for term in forbidden:
            self.assertNotIn(term, outputs_file.lower(), f"Outputs must not contain forbidden term: {term}")

    def test_gitignore_terraform_state(self):
        """Gitignore must exclude terraform state, variables, and plan files."""
        gitignore = (REPO_ROOT / ".gitignore").read_text(encoding="utf-8")
        self.assertIn("*.tfstate", gitignore)
        self.assertIn("*.tfvars", gitignore)
        self.assertIn("*.tfplan", gitignore)

    def test_https_listener_has_real_certificate_input(self):
        alb = (TF_DIR / "alb.tf").read_text(encoding="utf-8")
        variables = (TF_DIR / "variables.tf").read_text(encoding="utf-8")
        self.assertIn('resource "aws_lb_listener" "https"', alb)
        self.assertIn('certificate_arn   = var.alb_certificate_arn', alb)
        self.assertIn('target_group_arn = aws_lb_target_group.app.arn', alb)
        self.assertIn('variable "alb_certificate_arn"', variables)
        self.assertNotIn('default     = "arn:aws:acm:', variables)

    def test_production_templates_are_blocked_before_real_backend_exists(self):
        gate = (TF_DIR / "deployment_readiness.tf").read_text(encoding="utf-8")
        variables = (TF_DIR / "variables.tf").read_text(encoding="utf-8")
        self.assertIn('var.deployment_authorized', gate)
        self.assertIn('var.backend_persistence_verified', gate)
        self.assertIn('var.backend_runtime_verified', gate)
        self.assertIn('default     = false', variables)

    def test_secret_resource_never_creates_placeholder_credentials(self):
        secret = (TF_DIR / "secrets.tf").read_text(encoding="utf-8")
        self.assertIn('resource "aws_secretsmanager_secret" "lunacare_config"', secret)
        self.assertNotIn('resource "aws_secretsmanager_secret_version"', secret)
        self.assertNotIn('REPLACE_WITH_MINIMUM_32_CHAR', secret)
        self.assertNotIn('postgresql://postgres:REPLACE', secret)

if __name__ == "__main__":
    unittest.main()
