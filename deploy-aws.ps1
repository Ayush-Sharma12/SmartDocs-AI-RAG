# SmartDocs AWS Deployment Script
# PowerShell script to automate deployment to AWS

param(
    [string]$KeyName = "smartdocs-key",
    [string]$InstanceType = "t3.medium",
    [string]$Region = "us-east-1",
    [string]$StackName = "smartdocs-stack"
)

# Colors for output
$ErrorColor = "Red"
$SuccessColor = "Green"
$InfoColor = "Cyan"
$WarningColor = "Yellow"

function Write-Info {
    param([string]$Message)
    Write-Host "ℹ️  $Message" -ForegroundColor $InfoColor
}

function Write-Success {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor $SuccessColor
}

function Write-Error-Custom {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor $ErrorColor
}

function Write-Warning-Custom {
    param([string]$Message)
    Write-Host "⚠️  $Message" -ForegroundColor $WarningColor
}

# Step 1: Validate prerequisites
Write-Info "Step 1: Validating prerequisites..."

# Check AWS CLI
try {
    $awsVersion = aws --version
    Write-Success "AWS CLI found: $awsVersion"
} catch {
    Write-Error-Custom "AWS CLI not found. Please install it first: https://aws.amazon.com/cli/"
    exit 1
}

# Check Docker
try {
    $dockerVersion = docker --version
    Write-Success "Docker found: $dockerVersion"
} catch {
    Write-Error-Custom "Docker not found. Please install it first: https://www.docker.com/"
    exit 1
}

# Get AWS Account ID
$AccountId = aws sts get-caller-identity --query Account --output text --region $Region
if (-not $AccountId) {
    Write-Error-Custom "Failed to get AWS Account ID. Check your credentials."
    exit 1
}
Write-Success "AWS Account ID: $AccountId"

# Step 2: Prompt for secrets
Write-Info ""
Write-Info "Step 2: Configuring secrets..."
Write-Warning-Custom "You will be prompted to enter sensitive information"

# Read secrets securely
$DbPassword = Read-Host "Enter database password (or press Enter to generate)" -AsSecureString
if ([string]::IsNullOrEmpty($DbPassword)) {
    $DbPassword = -join ((33..126) | Get-Random -Count 16 | ForEach-Object { [char]$_ })
    Write-Info "Generated database password"
}

$OpenAiKey = Read-Host "Enter OpenAI API key"
if ([string]::IsNullOrEmpty($OpenAiKey)) {
    Write-Error-Custom "OpenAI API key is required"
    exit 1
}

$JwtSecret = Read-Host "Enter JWT secret (or press Enter to generate)" -AsSecureString
if ([string]::IsNullOrEmpty($JwtSecret)) {
    $JwtSecret = -join ((33..126) | Get-Random -Count 32 | ForEach-Object { [char]$_ })
    Write-Info "Generated JWT secret"
}

# Convert SecureString to plain text for use
$DbPasswordPlain = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto([System.Runtime.InteropServices.Marshal]::SecureStringToCoTaskMemUnicode($DbPassword))
$JwtSecretPlain = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto([System.Runtime.InteropServices.Marshal]::SecureStringToCoTaskMemUnicode($JwtSecret))

# Step 3: Create secrets in AWS Secrets Manager
Write-Info ""
Write-Info "Step 3: Creating AWS Secrets Manager secrets..."

try {
    aws secretsmanager create-secret `
        --name smartdocs/db-password `
        --secret-string $DbPasswordPlain `
        --region $Region 2>&1 | Out-Null
    Write-Success "Created database password secret"
} catch {
    Write-Warning-Custom "Database password secret may already exist"
}

try {
    aws secretsmanager create-secret `
        --name smartdocs/openai-key `
        --secret-string $OpenAiKey `
        --region $Region 2>&1 | Out-Null
    Write-Success "Created OpenAI key secret"
} catch {
    Write-Warning-Custom "OpenAI key secret may already exist"
}

try {
    aws secretsmanager create-secret `
        --name smartdocs/jwt-secret `
        --secret-string $JwtSecretPlain `
        --region $Region 2>&1 | Out-Null
    Write-Success "Created JWT secret"
} catch {
    Write-Warning-Custom "JWT secret may already exist"
}

# Step 4: Create ECR repositories
Write-Info ""
Write-Info "Step 4: Creating ECR repositories..."

$EcrRegistry = "$AccountId.dkr.ecr.$Region.amazonaws.com"

try {
    aws ecr create-repository `
        --repository-name smartdocs-backend `
        --region $Region 2>&1 | Out-Null
    Write-Success "Created backend ECR repository"
} catch {
    Write-Warning-Custom "Backend repository may already exist"
}

try {
    aws ecr create-repository `
        --repository-name smartdocs-frontend `
        --region $Region 2>&1 | Out-Null
    Write-Success "Created frontend ECR repository"
} catch {
    Write-Warning-Custom "Frontend repository may already exist"
}

# Step 5: Build and push Docker images
Write-Info ""
Write-Info "Step 5: Building and pushing Docker images..."

# Authenticate Docker with ECR
Write-Info "Authenticating Docker with ECR..."
aws ecr get-login-password --region $Region | docker login --username AWS --password-stdin $EcrRegistry 2>&1 | Out-Null
Write-Success "Docker authenticated with ECR"

# Build and push backend
Write-Info "Building backend image..."
docker build -f Dockerfile.backend -t smartdocs-backend:latest . 2>&1 | Out-Null
Write-Success "Backend image built"

Write-Info "Tagging and pushing backend image..."
docker tag smartdocs-backend:latest "$EcrRegistry/smartdocs-backend:latest"
docker push "$EcrRegistry/smartdocs-backend:latest" 2>&1 | Out-Null
Write-Success "Backend image pushed to ECR"

# Build and push frontend
Write-Info "Building frontend image..."
docker build -f Dockerfile.frontend -t smartdocs-frontend:latest . 2>&1 | Out-Null
Write-Success "Frontend image built"

Write-Info "Tagging and pushing frontend image..."
docker tag smartdocs-frontend:latest "$EcrRegistry/smartdocs-frontend:latest"
docker push "$EcrRegistry/smartdocs-frontend:latest" 2>&1 | Out-Null
Write-Success "Frontend image pushed to ECR"

# Step 6: Create EC2 Key Pair
Write-Info ""
Write-Info "Step 6: Creating EC2 key pair..."

try {
    aws ec2 describe-key-pairs --key-names $KeyName --region $Region 2>&1 | Out-Null
    Write-Info "Using existing key pair: $KeyName"
} catch {
    Write-Info "Creating new key pair: $KeyName"
    aws ec2 create-key-pair `
        --key-name $KeyName `
        --region $Region `
        --query 'KeyMaterial' `
        --output text | Out-File -Encoding ascii "smartdocs-key.pem"
    Write-Success "Created key pair and saved to smartdocs-key.pem"
}

# Step 7: Deploy CloudFormation stack
Write-Info ""
Write-Info "Step 7: Deploying CloudFormation stack..."

$StackExists = aws cloudformation describe-stacks `
    --stack-name $StackName `
    --region $Region 2>&1 | Out-Null

if ($LASTEXITCODE -eq 0) {
    Write-Info "Stack exists. Updating..."
    aws cloudformation update-stack `
        --stack-name $StackName `
        --template-body "file://cloudformation-ecs-ec2.yaml" `
        --parameters `
            ParameterKey=KeyName,ParameterValue=$KeyName `
            ParameterKey=InstanceType,ParameterValue=$InstanceType `
        --capabilities CAPABILITY_NAMED_IAM `
        --region $Region 2>&1 | Out-Null
    Write-Info "Stack update initiated"
} else {
    Write-Info "Creating new stack..."
    aws cloudformation create-stack `
        --stack-name $StackName `
        --template-body "file://cloudformation-ecs-ec2.yaml" `
        --parameters `
            ParameterKey=KeyName,ParameterValue=$KeyName `
            ParameterKey=InstanceType,ParameterValue=$InstanceType `
        --capabilities CAPABILITY_NAMED_IAM `
        --region $Region 2>&1 | Out-Null
    Write-Info "Stack creation initiated"
}

# Step 8: Monitor stack creation
Write-Info ""
Write-Info "Step 8: Monitoring stack creation (this may take 5-10 minutes)..."

$StackStatus = ""
$MaxAttempts = 120
$Attempts = 0

while ($Attempts -lt $MaxAttempts) {
    $StackStatus = aws cloudformation describe-stacks `
        --stack-name $StackName `
        --query 'Stacks[0].StackStatus' `
        --output text `
        --region $Region

    if ($StackStatus -eq "CREATE_COMPLETE" -or $StackStatus -eq "UPDATE_COMPLETE") {
        Write-Success "Stack status: $StackStatus"
        break
    } elseif ($StackStatus -like "*FAILED*") {
        Write-Error-Custom "Stack status: $StackStatus"
        exit 1
    } else {
        Write-Info "Stack status: $StackStatus"
    }

    Start-Sleep -Seconds 5
    $Attempts++
}

if ($Attempts -ge $MaxAttempts) {
    Write-Warning-Custom "Stack creation monitoring timeout"
}

# Step 9: Get load balancer URL
Write-Info ""
Write-Info "Step 9: Retrieving load balancer URL..."

$LoadBalancerUrl = aws cloudformation describe-stacks `
    --stack-name $StackName `
    --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerURL`].OutputValue' `
    --output text `
    --region $Region

if ($LoadBalancerUrl) {
    Write-Success ""
    Write-Success "═══════════════════════════════════════════════════════"
    Write-Success "SmartDocs deployment completed successfully!"
    Write-Success "═══════════════════════════════════════════════════════"
    Write-Success ""
    Write-Success "Application URL: $LoadBalancerUrl"
    Write-Success ""
    Write-Info "Next steps:"
    Write-Info "1. Wait a few minutes for containers to start"
    Write-Info "2. Open the URL in your browser"
    Write-Info "3. Register a new account or login"
    Write-Info ""
    Write-Info "For monitoring:"
    Write-Info "  View logs: aws logs tail /ecs/smartdocs --follow --region $Region"
    Write-Info "  SSH to instance: ssh -i smartdocs-key.pem ec2-user@<instance-ip>"
    Write-Info ""
    Write-Info "To cleanup resources:"
    Write-Info "  aws cloudformation delete-stack --stack-name $StackName --region $Region"
} else {
    Write-Error-Custom "Failed to retrieve load balancer URL"
    exit 1
}
