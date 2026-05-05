# SmartDocs AWS Deployment Guide

This guide walks you through deploying SmartDocs on AWS using ECS with EC2 instances, self-hosted PostgreSQL with pgvector, and an Application Load Balancer.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    AWS Cloud                                │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │        Application Load Balancer (ALB)               │   │
│  │              (Port 80/443)                           │   │
│  └────────────┬─────────────────────┬──────────────────┘   │
│               │                     │                       │
│  ┌────────────▼──────┐  ┌──────────▼────────────┐          │
│  │  ECS Cluster      │  │  ECS Cluster          │          │
│  │  (EC2 Instance 1) │  │  (EC2 Instance 2)     │          │
│  │                   │  │                       │          │
│  │ ┌─────────────┐   │  │ ┌─────────────┐      │          │
│  │ │  Frontend   │   │  │ │  Frontend   │      │          │
│  │ │  (Nginx)    │   │  │ │  (Nginx)    │      │          │
│  │ └─────────────┘   │  │ └─────────────┘      │          │
│  │ ┌─────────────┐   │  │ ┌─────────────┐      │          │
│  │ │  Backend    │   │  │ │  Backend    │      │          │
│  │ │ (Spring)    │   │  │ │ (Spring)    │      │          │
│  │ └──────┬──────┘   │  │ └──────┬──────┘      │          │
│  └────────┼──────────┘  └─────────┼────────────┘          │
│           │                       │                       │
│           └───────────┬───────────┘                       │
│                       │                                   │
│  ┌────────────────────▼──────────────────┐              │
│  │  PostgreSQL + pgvector                 │              │
│  │  (Self-hosted on EC2)                 │              │
│  │  Port: 5432                            │              │
│  └────────────────────────────────────────┘              │
│                                                           │
└───────────────────────────────────────────────────────────┘
```

## Prerequisites

1. **AWS Account** - with appropriate permissions
2. **AWS CLI** - installed and configured with credentials
3. **Docker** - for building and testing images locally
4. **Docker Hub Account** or **Amazon ECR** - for pushing images
5. **EC2 Key Pair** - created in your AWS region

## Step 1: Prepare AWS Secrets

Store sensitive configuration in AWS Secrets Manager:

```bash
# Set your values
$DB_PASSWORD = "your-secure-db-password"
$OPENAI_KEY = "your-openai-api-key"
$JWT_SECRET = "your-jwt-secret-key-min-32-chars"
$AWS_REGION = "us-east-1"  # Change to your region

# Create secrets
aws secretsmanager create-secret `
  --name smartdocs/db-password `
  --secret-string $DB_PASSWORD `
  --region $AWS_REGION

aws secretsmanager create-secret `
  --name smartdocs/openai-key `
  --secret-string $OPENAI_KEY `
  --region $AWS_REGION

aws secretsmanager create-secret `
  --name smartdocs/jwt-secret `
  --secret-string $JWT_SECRET `
  --region $AWS_REGION
```

## Step 2: Build and Push Docker Images to ECR

### Create ECR Repositories

```bash
$AWS_REGION = "us-east-1"
$AWS_ACCOUNT_ID = "YOUR_ACCOUNT_ID"

aws ecr create-repository `
  --repository-name smartdocs-backend `
  --region $AWS_REGION

aws ecr create-repository `
  --repository-name smartdocs-frontend `
  --region $AWS_REGION
```

### Authenticate Docker with ECR

```bash
aws ecr get-login-password --region $AWS_REGION | `
  docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com
```

### Build and Push Backend

```bash
# Build backend image
docker build -f Dockerfile.backend -t smartdocs-backend:latest .

# Tag for ECR
docker tag smartdocs-backend:latest `
  $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/smartdocs-backend:latest

# Push to ECR
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/smartdocs-backend:latest
```

### Build and Push Frontend

```bash
# Build frontend image
docker build -f Dockerfile.frontend -t smartdocs-frontend:latest .

# Tag for ECR
docker tag smartdocs-frontend:latest `
  $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/smartdocs-frontend:latest

# Push to ECR
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/smartdocs-frontend:latest
```

## Step 3: Create EC2 Key Pair

```bash
# PowerShell
aws ec2 create-key-pair `
  --key-name smartdocs-key `
  --region $AWS_REGION `
  --query 'KeyMaterial' `
  --output text | Out-File -Encoding ascii SmartDocsKey.pem

# Set proper permissions (Linux/Mac)
# chmod 400 SmartDocsKey.pem
```

## Step 4: Deploy CloudFormation Stack

Update the CloudFormation template with your values:

```bash
$KEY_NAME = "smartdocs-key"  # Your EC2 key pair name
$INSTANCE_TYPE = "t3.medium"
$AWS_REGION = "us-east-1"

aws cloudformation create-stack `
  --stack-name smartdocs-stack `
  --template-body file://cloudformation-ecs-ec2.yaml `
  --parameters `
    ParameterKey=KeyName,ParameterValue=$KEY_NAME `
    ParameterKey=InstanceType,ParameterValue=$INSTANCE_TYPE `
  --capabilities CAPABILITY_NAMED_IAM `
  --region $AWS_REGION

# Monitor stack creation
aws cloudformation describe-stacks `
  --stack-name smartdocs-stack `
  --query 'Stacks[0].StackStatus' `
  --region $AWS_REGION
```

## Step 5: Verify Deployment

```bash
# Get load balancer URL
aws cloudformation describe-stacks `
  --stack-name smartdocs-stack `
  --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerURL`].OutputValue' `
  --output text `
  --region $AWS_REGION

# Should output: http://smartdocs-alb-XXXX.region.elb.amazonaws.com
```

Access the application at the load balancer URL.

## Step 6: Monitor and Debug

### View ECS Cluster Status

```bash
aws ecs describe-clusters `
  --clusters smartdocs-cluster `
  --query 'clusters[0].runningCount' `
  --region $AWS_REGION

aws ecs describe-services `
  --cluster smartdocs-cluster `
  --services smartdocs-service `
  --region $AWS_REGION
```

### View Logs

```bash
# Backend logs
aws logs tail /ecs/smartdocs --follow --log-stream-name-pattern backend `
  --region $AWS_REGION

# Frontend logs
aws logs tail /ecs/smartdocs --follow --log-stream-name-pattern frontend `
  --region $AWS_REGION

# Database logs
aws logs tail /ecs/smartdocs --follow --log-stream-name-pattern database `
  --region $AWS_REGION
```

### SSH to EC2 Instance

```bash
# Find instance IP
$INSTANCE_IP = aws ec2 describe-instances `
  --filters "Name=tag:Name,Values=SmartDocs-ECS-Instance" `
  --query 'Reservations[0].Instances[0].PublicIpAddress' `
  --output text `
  --region $AWS_REGION

# SSH into instance
ssh -i SmartDocsKey.pem ec2-user@$INSTANCE_IP

# Check Docker containers
docker ps

# Check database
docker exec smartdocs-db psql -U postgres -d aidocs -c "\dt"
```

## Production Considerations

### 1. Enable HTTPS
- Create ACM certificate for your domain
- Update ALB to use HTTPS listener
- Add redirect from HTTP to HTTPS

### 2. Database Backups
- Create automated EBS snapshots
- Configure AWS Backup for PostgreSQL
- Test restore procedures regularly

### 3. Scaling
- Set up auto-scaling policies
- Configure target tracking for CPU/Memory
- Monitor and adjust desired capacity

### 4. Monitoring & Alerts
- Enable CloudWatch Container Insights
- Create alarms for high CPU/Memory
- Set up SNS notifications for failures

### 5. Security
- Enable VPC Flow Logs
- Use AWS WAF on ALB
- Rotate secrets regularly
- Enable AWS Systems Manager Session Manager

### 6. Cost Optimization
- Use Reserved Instances for predictable workloads
- Set up CloudWatch alarms for cost anomalies
- Review and optimize resource usage

## Cleanup

To delete all resources:

```bash
# Delete CloudFormation stack
aws cloudformation delete-stack `
  --stack-name smartdocs-stack `
  --region $AWS_REGION

# Delete ECR repositories
aws ecr delete-repository `
  --repository-name smartdocs-backend `
  --force `
  --region $AWS_REGION

aws ecr delete-repository `
  --repository-name smartdocs-frontend `
  --force `
  --region $AWS_REGION

# Delete secrets
aws secretsmanager delete-secret `
  --secret-id smartdocs/db-password `
  --region $AWS_REGION

aws secretsmanager delete-secret `
  --secret-id smartdocs/openai-key `
  --region $AWS_REGION

aws secretsmanager delete-secret `
  --secret-id smartdocs/jwt-secret `
  --region $AWS_REGION
```

## Troubleshooting

### ECS Tasks Not Starting
```bash
aws ecs describe-tasks `
  --cluster smartdocs-cluster `
  --tasks <task-arn> `
  --region $AWS_REGION
```

### Load Balancer Shows Targets as Unhealthy
- Check security group rules
- Verify health check endpoint
- Review container logs

### Database Connection Issues
- Verify security group allows port 5432
- Check database is running in container
- Verify credentials in Secrets Manager

## Support & Documentation

- AWS ECS Documentation: https://docs.aws.amazon.com/ecs/
- CloudFormation Reference: https://docs.aws.amazon.com/cloudformation/
- PostgreSQL pgvector: https://github.com/pgvector/pgvector
