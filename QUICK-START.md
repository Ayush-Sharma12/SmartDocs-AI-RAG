# Quick Start: Deploy SmartDocs to AWS

## 🚀 One-Command Deployment

Run this PowerShell script to deploy everything automatically:

```powershell
./deploy-aws.ps1 -Region us-east-1 -KeyName smartdocs-key -InstanceType t3.medium
```

The script will:
1. ✓ Validate AWS CLI and Docker installation
2. ✓ Create AWS Secrets Manager entries for sensitive data
3. ✓ Create ECR repositories
4. ✓ Build and push Docker images
5. ✓ Create EC2 key pair
6. ✓ Deploy CloudFormation stack with ECS cluster
7. ✓ Wait for deployment to complete
8. ✓ Output your load balancer URL

## 📋 Prerequisites

Before you start, ensure you have:

```powershell
# Check AWS CLI
aws --version

# Check Docker
docker --version

# Check AWS credentials
aws sts get-caller-identity
```

## 🔧 Manual Deployment (Step-by-Step)

### 1. Create AWS Secrets

```powershell
$Region = "us-east-1"

# Generate secure passwords
$DbPassword = -join ((33..126) | Get-Random -Count 16 | ForEach-Object { [char]$_ })
$JwtSecret = -join ((33..126) | Get-Random -Count 32 | ForEach-Object { [char]$_ })
$OpenAiKey = "sk-..."  # Your OpenAI API key

# Create secrets
aws secretsmanager create-secret --name smartdocs/db-password --secret-string $DbPassword --region $Region
aws secretsmanager create-secret --name smartdocs/openai-key --secret-string $OpenAiKey --region $Region
aws secretsmanager create-secret --name smartdocs/jwt-secret --secret-string $JwtSecret --region $Region
```

### 2. Create ECR Repositories

```powershell
aws ecr create-repository --repository-name smartdocs-backend --region $Region
aws ecr create-repository --repository-name smartdocs-frontend --region $Region
```

### 3. Build and Push Docker Images

```powershell
$AccountId = aws sts get-caller-identity --query Account --output text
$EcrRegistry = "$AccountId.dkr.ecr.$Region.amazonaws.com"

# Login to ECR
aws ecr get-login-password --region $Region | docker login --username AWS --password-stdin $EcrRegistry

# Backend
docker build -f Dockerfile.backend -t smartdocs-backend:latest .
docker tag smartdocs-backend:latest $EcrRegistry/smartdocs-backend:latest
docker push $EcrRegistry/smartdocs-backend:latest

# Frontend
docker build -f Dockerfile.frontend -t smartdocs-frontend:latest .
docker tag smartdocs-frontend:latest $EcrRegistry/smartdocs-frontend:latest
docker push $EcrRegistry/smartdocs-frontend:latest
```

### 4. Create EC2 Key Pair

```powershell
aws ec2 create-key-pair --key-name smartdocs-key --region $Region --query 'KeyMaterial' --output text | Out-File -Encoding ascii smartdocs-key.pem
```

### 5. Deploy CloudFormation Stack

```powershell
aws cloudformation create-stack `
  --stack-name smartdocs-stack `
  --template-body file://cloudformation-ecs-ec2.yaml `
  --parameters ParameterKey=KeyName,ParameterValue=smartdocs-key ParameterKey=InstanceType,ParameterValue=t3.medium `
  --capabilities CAPABILITY_NAMED_IAM `
  --region $Region
```

### 6. Monitor Deployment

```powershell
# Check stack status
aws cloudformation describe-stacks --stack-name smartdocs-stack --query 'Stacks[0].StackStatus' --region $Region

# Get load balancer URL
aws cloudformation describe-stacks --stack-name smartdocs-stack --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerURL`].OutputValue' --output text --region $Region
```

## 📊 Deployment Architecture

```
Internet
   ↓
[Application Load Balancer - Port 80]
   ↓
[VPC - 10.0.0.0/16]
   ├─ [EC2 Instance 1 - Running ECS Task]
   │  ├─ Nginx Frontend (Port 80)
   │  └─ Spring Backend (Port 8080)
   └─ [EC2 Instance 2 - Running ECS Task]
      ├─ Nginx Frontend (Port 80)
      └─ Spring Backend (Port 8080)
   ↓
[Shared PostgreSQL + pgvector on EC2]
```

## 💻 After Deployment

### Access Your Application
```
http://smartdocs-alb-XXXX.region.elb.amazonaws.com
```

### Monitor Logs
```powershell
# View all ECS logs
aws logs tail /ecs/smartdocs --follow --region $Region

# View specific service logs
aws logs tail /ecs/smartdocs --follow --log-stream-name-pattern backend --region $Region
```

### SSH to EC2 Instance
```powershell
# Get instance IP
$InstanceIP = aws ec2 describe-instances --filters "Name=tag:Name,Values=SmartDocs-ECS-Instance" --query 'Reservations[0].Instances[0].PublicIpAddress' --output text --region $Region

# Connect
ssh -i smartdocs-key.pem ec2-user@$InstanceIP

# Check containers
docker ps
docker logs smartdocs-backend
```

### Manage ECS Service
```powershell
# Update service with new image
aws ecs update-service --cluster smartdocs-cluster --service smartdocs-service --force-new-deployment --region $Region

# Scale up/down
aws ecs update-service --cluster smartdocs-cluster --service smartdocs-service --desired-count 3 --region $Region

# Check task status
aws ecs describe-tasks --cluster smartdocs-cluster --tasks <task-arn> --region $Region
```

## 🧹 Cleanup

Delete all AWS resources:

```powershell
$Region = "us-east-1"

# Delete stack
aws cloudformation delete-stack --stack-name smartdocs-stack --region $Region

# Delete ECR repos
aws ecr delete-repository --repository-name smartdocs-backend --force --region $Region
aws ecr delete-repository --repository-name smartdocs-frontend --force --region $Region

# Delete secrets
aws secretsmanager delete-secret --secret-id smartdocs/db-password --force-delete-without-recovery --region $Region
aws secretsmanager delete-secret --secret-id smartdocs/openai-key --force-delete-without-recovery --region $Region
aws secretsmanager delete-secret --secret-id smartdocs/jwt-secret --force-delete-without-recovery --region $Region

# Delete key pair
aws ec2 delete-key-pair --key-name smartdocs-key --region $Region

# Delete local key file
Remove-Item smartdocs-key.pem -ErrorAction SilentlyContinue
```

## 🐛 Troubleshooting

### Tasks won't start?
```powershell
aws ecs describe-services --cluster smartdocs-cluster --services smartdocs-service --region $Region
```

### Check container errors
```powershell
aws logs get-log-events --log-group-name /ecs/smartdocs --log-stream-name <stream-name> --region $Region
```

### Database connection failed?
```powershell
# SSH to instance and check database
ssh -i smartdocs-key.pem ec2-user@$InstanceIP
docker exec smartdocs-db psql -U postgres -d aidocs -c "\dt"
```

## 📚 Full Documentation

See [AWS-DEPLOYMENT.md](./AWS-DEPLOYMENT.md) for comprehensive deployment guide.

## ⚠️ Cost Estimates (per month)

- EC2 (t3.medium × 2): ~$30-40
- Load Balancer: ~$16-20
- Data transfer: ~$0.05-1
- CloudWatch Logs: ~$0.50-2

**Estimated Total: $50-70/month**

---

**Need help?** Check the logs, verify secrets are created, and ensure Docker images are pushed to ECR.
