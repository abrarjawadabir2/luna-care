# LunaCare AWS Deployment Assessment & Infrastructure Architecture

> **SECURITY CLASSIFICATION: CONFIDENTIAL / INTERNAL INFRASTRUCTURE SPECIFICATION**  
> **Deployment Status: PROVISIONING BLOCKED — PRE-AUTHORIZATION ASSESSMENT ONLY**  
> **Hard Rule:** No AWS billable resources may be created, purchased, or launched without explicit human approval.

---

## 1. Executive Summary & Production Blockers

LunaCare is a privacy-first menstrual wellness and reproductive health application. The backend API handles sensitive authentication data, user profiles, and proxied health data interactions.

### Production Deployment Blockers (Must be resolved before live AWS launch):
1. **In-Memory Backend Storage:** The Node.js Fastify backend (`backend/src/database/client.ts`) uses an `InMemoryDatabase`. Production startup is blocked by `requirePersistentDatabaseForProduction()` in `backend/src/app.ts` and `deployment_readiness_gate` in `infra/aws/terraform/deployment_readiness.tf`. A persistent PostgreSQL adapter wiring TLS (`sslmode=verify-full`) and running migrations `20261001000000_hardened_schema.sql` and `20261023000000_unique_username_system.sql` must be completed and tested.
2. **EC2 Runtime Bootstrap & Private-Subnet Egress:** `ec2_asg.tf` defines the `lunacare.service` unit in private subnets (`associate_public_ip_address = false`), but does not yet provision a pinned Node.js 20 build artifact or VPC Interface Endpoints (`ssm`, `ssmmessages`, `ec2messages`, `secretsmanager`, `logs`) / NAT egress required for package updates and SSM Session Manager. Keep `backend_runtime_verified = false` until wired.
3. **Object Storage Persistence:** `UploadService.inspectAndStore` validates magic bytes and MIME types, but stores upload metadata only (`db.uploads`). Byte persistence to an encrypted private S3 bucket with KMS encryption and presigned URLs must be wired.
4. **External OTP & Reset Delivery:** Phone OTP and password-reset currently return `deliveryImplemented: false`. An enterprise SMS (e.g. AWS SNS / Twilio) or transactional email (AWS SES) provider must be configured.
5. **Distributed Abuse Controls:** Rate limiting and job locking use process-memory maps. Multi-instance scaling requires Redis or PostgreSQL advisory locks.

---

## 2. Architecture Comparison: Amazon EC2 vs Amazon ECS Fargate

| Evaluation Dimension | Amazon EC2 (Launch Template + ASG) | Amazon ECS Fargate (Serverless Container) |
|---|---|---|
| **Underlying Compute** | Virtual Machine (Amazon Linux 2023 / Graviton2 `t4g.small`) | Managed serverless container tasks |
| **OS Maintenance & Patching** | Customer responsibility; automated via AWS Systems Manager Patch Manager | AWS responsibility; zero host OS maintenance |
| **Runtime Isolation** | Hypervisor-level VM isolation; systemd service with unprivileged `lunacare` user | Task-level cgroup/namespace isolation; read-only container rootfs |
| **Secrets Integration** | AWS Secrets Manager read via IAM instance profile and AWS CLI / SDK on startup | Native `secrets` block in ECS Task Definition referencing Secrets Manager ARNs |
| **Administrative Access** | AWS Systems Manager (SSM) Session Manager; Port 22 SSH completely disabled | ECS Exec via AWS SSM session |
| **Scaling Characteristics** | Auto Scaling Group launch: 60–120 seconds per instance | Fast task scale-out: 15–30 seconds per task |
| **Baseline Monthly Cost** | Low: 1x `t4g.small` ~$12.26/mo (eligible for savings plans/free tier trials) | Moderate: 0.25 vCPU / 0.5 GB RAM ~$9.00/mo per task (continuous) |
| **Architectural Verdict** | **Recommended for Phase 1** (predictable cost, familiar systemd operations, co-located worker tasks) | **Recommended for Phase 2** (containerized microservices, zero OS patching overhead) |

---

## 3. Defense-in-Depth Security Controls

1. **Network Isolation (VPC):**
   - 2 Public Subnets (across 2 Availability Zones): Hosts Application Load Balancer (ALB) only.
   - 2 Private Application Subnets: Hosts EC2 instances / ECS tasks. No public IP addresses assigned.
   - 2 Private Database Subnets: Hosts RDS PostgreSQL. No internet gateway routing; strictly isolated.
2. **Minimal Security Groups (Principle of Least Privilege):**
   - `alb-sg`: Inbound TCP 443 (HTTPS) from internet (`0.0.0.0/0`). Outbound TCP 3000 to `app-sg` only.
   - `app-sg`: Inbound TCP 3000 from `alb-sg` only. Outbound TCP 5432 to `db-sg` and TCP 443 to AWS endpoints. Zero internet ingress.
   - `db-sg`: Inbound TCP 5432 from `app-sg` only. Zero outbound to internet. Zero public ingress.
3. **IAM Least Privilege & Zero Static Keys:**
   - EC2 instances use an IAM Instance Profile.
   - Permissions: `AmazonSSMManagedInstanceCore` (Session Manager), `CloudWatchAgentServerPolicy`, and a scoped `secretsmanager:GetSecretValue` on the specific LunaCare secret ARN.
   - No AWS access keys or secret keys are stored on the instance or committed to git.
4. **IMDSv2 Enforcement:**
   - Launch Template enforces Instance Metadata Service Version 2 (`HttpTokens=required`, `HttpPutResponseHopLimit=1`), mitigating SSRF-based metadata and credential exfiltration.
5. **Port 22 SSH Closure:**
   - Port 22 is completely closed in all Security Groups. All shell administration is audited via AWS Systems Manager Session Manager with AWS CloudTrail logging.
6. **EBS & RDS KMS Encryption:**
   - All EBS root and data volumes are encrypted with AWS KMS (`aws/ebs` or customer CMK).
   - RDS PostgreSQL instance storage and automated snapshots are KMS-encrypted at rest.
7. **Sanitized CloudWatch Logging:**
   - Application logs emit JSON with prohibited health/identity keys stripped by `AuditSanitizer`.
   - Log Group retention is set to 30 days with KMS encryption.
8. **Automated Patching:**
   - AWS Systems Manager Patch Manager applies critical and security OS updates weekly within an approved maintenance window.
9. **Non-Root Application Process:**
   - Systemd unit `lunacare.service` runs under unprivileged user `lunacare` (UID 1001), with `ProtectSystem=strict`, `NoNewPrivileges=true`, and `PrivateTmp=true`.

---

## 4. Verified Cost Estimates (Monthly 730-hr Projection, Region: `us-east-1`)

| Service Component | Configuration & Official On-Demand Rate (`us-east-1`) | Estimated Monthly Cost (USD) | Notes |
|---|---|---|---|
| **EC2 Compute** | 1x `t4g.small` (2 vCPU, 2 GiB RAM, Graviton2 @ \$0.0168/hr) | \$12.26 | Lower with 1-yr Compute Savings Plan |
| **EBS Root Storage** | 30 GB `gp3` (3,000 IOPS, 125 MB/s @ \$0.08/GB-mo) | \$2.40 | Encrypted at rest |
| **RDS PostgreSQL** | 1x `db.t4g.micro` Single-AZ (@ \$0.016/hr; Multi-AZ is \$0.032/hr = \$23.36) | \$11.68 | Use Multi-AZ (~\$23.36/mo) for production HA |
| **RDS Storage** | 20 GB `gp3` (@ \$0.115/GB-mo) | \$2.30 | KMS-encrypted |
| **AWS Secrets Manager** | 2 secrets (`lunacare_config` + RDS managed password @ \$0.40/secret-mo) | \$0.80 | Plus \$0.05 per 10,000 API calls |
| **CloudWatch Logs** | ~2 GB ingestion (\$0.50/GB) + 30-day retention (\$0.03/GB-mo) | \$1.06 | First 5 GB/mo covered under AWS Free Tier |
| **Ingress Option A: ALB + Public IPv4** | 1x ALB (\$0.0225/hr = \$16.43 + ~\$2.00 LCU) + 2 public IPv4s (\$0.005/IP-hr = \$7.30) | \$25.73 | Optional if NOT using Cloudflare Tunnel (`infra/cloudflare`) |
| **Ingress Option B: Cloudflare Tunnel** | `cloudflared` outbound daemon + Cloudflare Free/Pro plan + Cloudflare Pages (`web/`) | \$0.00 (Free tier) | Eliminates ALB + public IPv4 (\$25.73/mo saved) |
| **Private Egress (Required for Private Subnet)** | Single-AZ NAT Gateway (\$0.045/hr = \$32.85 + \$0.045/GB) *or* 5 VPC Interface Endpoints (`ssm`, `ssmmessages`, `ec2messages`, `secretsmanager`, `logs` @ \$0.01/AZ-hr = \$7.30/endpoint/AZ) | \$32.85–\$36.50 | Single-AZ NAT Gateway (\$32.85/mo) is cheaper than 5 endpoints across 2 AZs (\$73.00/mo) |
| **AWS Budget Alert** | 1 monthly cost budget (`budgets.tf`) | \$0.00 | Free |
| **Total Estimated Cost** | **Cloudflare Tunnel + Single-AZ NAT + Single-AZ RDS** | **~\$63.35 / month** | **~\$89.08 / month if adding ALB instead of Tunnel** |

> **Cost Alert Guardrail:** An AWS Budget alert is pre-configured in Terraform (`budgets.tf`) to trigger email notifications at 80% ($24.00) and 100% ($30.00) of the monthly threshold.

---

## 5. Deployment Procedures (When Authorized)

```bash
# 1. Review plan safely without provisioning
cd infra/aws/terraform
terraform init
terraform validate
terraform plan -out=tfplan.binary

# 2. Never run 'terraform apply' without explicit human authorization!
```
