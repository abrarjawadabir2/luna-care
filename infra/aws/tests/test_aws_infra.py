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
        self.assertIn("security_groups = [aws_security_group.app.id]", sg_file)
        self.assertNotIn('0.0.0.0/0', sg_file.split('aws_security_group" "db"')[1],
                         "Database security group must not contain public 0.0.0.0/0 CIDR")

    def test_app_security_group_restricted(self):
        """Backend application security group must accept port 3000 strictly from ALB."""
        sg_file = (TF_DIR / "security_groups.tf").read_text(encoding="utf-8")
        self.assertIn("from_port       = 3000", sg_file)
        self.assertIn("security_groups = [aws_security_group.alb.id]", sg_file)

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

if __name__ == "__main__":
    unittest.main()
