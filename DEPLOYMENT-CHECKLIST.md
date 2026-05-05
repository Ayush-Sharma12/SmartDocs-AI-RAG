# 📋 AWS Deployment Checklist - Step by Step

Copy and paste this into a text file and check off each step as you complete it!

```
═══════════════════════════════════════════════════════════════════════════
                    SMARTDOCS AWS DEPLOYMENT CHECKLIST
═══════════════════════════════════════════════════════════════════════════

PHASE 1: PREPARATION
═══════════════════════════════════════════════════════════════════════════

□ Step 1: Create AWS Account
  □ Go to: https://aws.amazon.com/
  □ Click "Create an AWS Account"
  □ Complete the setup with email and payment info
  □ Verify your account

□ Step 2: Get OpenAI API Key
  □ Go to: https://platform.openai.com/api-keys
  □ Create account or login
  □ Click "Create new secret key"
  □ Save the key (looks like: sk-...)
  □ Keep it safe!

□ Step 3: Get AWS Access Keys
  □ Go to: https://console.aws.amazon.com/
  □ Click profile icon (top right) → "Security Credentials"
  □ Click "Create New Access Key"
  □ Save both:
    - Access Key ID (looks like: AKIA...)
    - Secret Access Key (looks like: wJal...)
  □ Remember your region (e.g., "us-east-1")


PHASE 2: INSTALL SOFTWARE
═══════════════════════════════════════════════════════════════════════════

□ Step 4: Install AWS CLI
  ✓ Windows: Download and run installer from https://aws.amazon.com/cli/
  ✓ Mac/Linux: Use curl command (see BEGINNER-GUIDE.md)
  □ Verify: In PowerShell, run "aws --version"

□ Step 5: Install Docker Desktop
  □ Download from: https://www.docker.com/products/docker-desktop
  □ Run installer
  □ Restart your computer
  □ Verify: In PowerShell, run "docker --version"
  □ Make sure Docker is running (check system tray)

□ Step 6: Install Git
  □ Download from: https://git-scm.com/download
  □ Run installer with default settings
  □ Verify: In PowerShell, run "git --version"


PHASE 3: CONFIGURATION
═══════════════════════════════════════════════════════════════════════════

□ Step 7: Configure AWS Credentials
  □ Open PowerShell
  □ Run: aws configure
  □ Enter values when prompted:
    - Access Key ID: (paste from Step 3)
    - Secret Access Key: (paste from Step 3)
    - Default region: us-east-1 (or your region)
    - Default format: json (press Enter)
  □ Verify: Run "aws sts get-caller-identity"
  □ You should see your account info

□ Step 8: Prepare Your Secrets Document
  □ Open Notepad
  □ Write down:
    Database Password: ___________________
    OpenAI API Key: sk-...________________
    JWT Secret: (Generate 32+ random chars)
    AWS Region: us-east-1________________
    EC2 Key Name: smartdocs-key__________
  □ Save to a secure location
  □ Keep this file PRIVATE!


PHASE 4: DEPLOYMENT
═══════════════════════════════════════════════════════════════════════════

□ Step 9: Open PowerShell in Project Directory
  □ Open PowerShell
  □ Navigate to: C:\Users\AYUSH SHARMA\Documents\storeHere\SmartDocs
  □ Run: cd "C:\Users\AYUSH SHARMA\Documents\storeHere\SmartDocs"

□ Step 10: Verify Docker is Running
  □ Run: docker ps
  □ Should show: CONTAINER ID, IMAGE, STATUS headers (no errors)

□ Step 11: Run Deployment Script
  □ Run: ./deploy-aws.ps1 -Region us-east-1 -KeyName smartdocs-key -InstanceType t3.medium
  □ Answer prompts with values from Step 8:
    - Database Password: (paste)
    - OpenAI API Key: (paste)
    - JWT Secret: (paste or press Enter to generate)
  
  □ Watch the output for ✓ checkmarks
  
  Steps the script is doing:
    ✓ Validating prerequisites
    ✓ Creating AWS Secrets Manager entries
    ✓ Creating ECR repositories
    ✓ Building Docker images (This takes 3-5 minutes)
    ✓ Pushing to ECR
    ✓ Creating EC2 key pair
    ✓ Deploying CloudFormation stack (Takes 5-10 minutes)
  
  □ Script will end with your Application URL:
    URL: http://smartdocs-alb-XXXX.region.elb.amazonaws.com
    ⚠️  SAVE THIS URL!


PHASE 5: VERIFICATION
═══════════════════════════════════════════════════════════════════════════

□ Step 12: Wait for Services to Start
  □ Wait 3-5 minutes after deployment completes
  □ Services need time to boot up
  □ Don't worry if first try shows "error", it's normal

□ Step 13: Access Your Application
  □ Open your browser
  □ Go to the URL from Step 11
  □ You should see SmartDocs login page

□ Step 14: Create Account & Test
  □ Click "Register"
  □ Create new account with:
    Email: your-email@example.com
    Username: testuser
    Password: SecurePassword123!
  □ Login with created credentials
  □ Try uploading a PDF file
  □ Try asking a question about the PDF
  □ Verify everything works!


PHASE 6: TROUBLESHOOTING (If Needed)
═══════════════════════════════════════════════════════════════════════════

If you see: 502 Bad Gateway or Connection Error
  □ Wait 5 more minutes
  □ Refresh the page
  □ Check logs:
    aws logs tail /ecs/smartdocs --follow --region us-east-1

If you see: Docker error
  □ Check Docker Desktop is running
  □ Restart Docker (quit and restart Docker Desktop)
  □ Run: docker ps (should work now)

If you see: AWS credentials error
  □ Run: aws configure
  □ Re-enter your Access Keys
  □ Run: aws sts get-caller-identity (should work)

If you see: Application loads but doesn't work
  □ Check OpenAI API key is correct
  □ Try logging out and in again
  □ Check browser console for errors (F12)

For more help, see: BEGINNER-GUIDE.md section "Troubleshooting"


PHASE 7: OPTIONAL - MONITOR & MAINTAIN
═══════════════════════════════════════════════════════════════════════════

□ Monitor your services (optional)
  Command: aws ecs describe-services --cluster smartdocs-cluster --services smartdocs-service --region us-east-1

□ View real-time logs (optional)
  Command: aws logs tail /ecs/smartdocs --follow --region us-east-1

□ Update application (optional)
  Steps:
    1. Make code changes locally
    2. Run deployment script again
    3. It will rebuild and deploy


PHASE 8: CLEANUP (When Done)
═══════════════════════════════════════════════════════════════════════════

When you want to DELETE everything and STOP PAYING:

□ Delete the CloudFormation stack
  Command: aws cloudformation delete-stack --stack-name smartdocs-stack --region us-east-1

□ Wait 5 minutes for stack deletion to complete

□ Delete ECR repositories
  Command: aws ecr delete-repository --repository-name smartdocs-backend --force --region us-east-1
  Command: aws ecr delete-repository --repository-name smartdocs-frontend --force --region us-east-1

□ Delete secrets
  Command: aws secretsmanager delete-secret --secret-id smartdocs/db-password --force-delete-without-recovery --region us-east-1
  Command: aws secretsmanager delete-secret --secret-id smartdocs/openai-key --force-delete-without-recovery --region us-east-1
  Command: aws secretsmanager delete-secret --secret-id smartdocs/jwt-secret --force-delete-without-recovery --region us-east-1


═══════════════════════════════════════════════════════════════════════════
                              COMPLETION SUMMARY
═══════════════════════════════════════════════════════════════════════════

✓ All steps completed?
✓ Application accessible from browser?
✓ Can create account and upload PDF?
✓ Can ask questions and get answers?

If YES to all: 🎉 CONGRATULATIONS! Your app is deployed!

If NO: Check BEGINNER-GUIDE.md troubleshooting section or re-run specific steps.


═══════════════════════════════════════════════════════════════════════════
                            ESTIMATED TIMING
═══════════════════════════════════════════════════════════════════════════

Installation & Setup:        15-20 minutes
Deployment Script Running:   15-20 minutes
Services Starting Up:        5-10 minutes
First Test:                  2-3 minutes
                            ─────────────
TOTAL:                      40-55 minutes

═══════════════════════════════════════════════════════════════════════════
```

---

## Quick Command Reference

**Check AWS connection:**
```powershell
aws sts get-caller-identity
```

**Check Docker is running:**
```powershell
docker ps
```

**Navigate to project:**
```powershell
cd "C:\Users\AYUSH SHARMA\Documents\storeHere\SmartDocs"
```

**Start deployment:**
```powershell
./deploy-aws.ps1 -Region us-east-1 -KeyName smartdocs-key -InstanceType t3.medium
```

**Check deployment status:**
```powershell
aws cloudformation describe-stacks --stack-name smartdocs-stack --query 'Stacks[0].StackStatus' --region us-east-1
```

**View logs:**
```powershell
aws logs tail /ecs/smartdocs --follow --region us-east-1
```

**Delete everything:**
```powershell
aws cloudformation delete-stack --stack-name smartdocs-stack --region us-east-1
```

---

## When to Pause and Call for Help

If you get stuck at these points, ask for help:
- ❌ "AWS CLI not found" after installation
- ❌ "Docker is not running" errors
- ❌ "Access Denied" from AWS
- ❌ Deployment script fails completely
- ❌ Application never loads (after 15 minute wait)

---

**Total Cost:** ~$50/month (or less with AWS Free Tier)

**Good luck! 🚀**
