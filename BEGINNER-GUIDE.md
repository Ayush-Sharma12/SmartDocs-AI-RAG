# 🚀 SmartDocs AWS Deployment - Complete Beginner's Guide

This guide walks you through deploying your SmartDocs application to AWS from scratch.

---

## 📚 Table of Contents
1. [What You Need Before Starting](#what-you-need)
2. [Setup AWS Account](#setup-aws)
3. [Install Required Tools](#install-tools)
4. [Prepare Your Secrets](#prepare-secrets)
5. [Deploy to AWS](#deploy)
6. [Verify Deployment](#verify)
7. [Access Your Application](#access)
8. [Troubleshooting](#troubleshoot)

---

## <a name="what-you-need"></a>📋 What You Need Before Starting

### 1. **AWS Account**
   - Go to: https://aws.amazon.com/
   - Click "Create an AWS Account"
   - Add payment method (you won't be charged much for testing)
   - Verify your email and phone

### 2. **OpenAI API Key**
   - Go to: https://platform.openai.com/api-keys
   - Sign in or create account
   - Click "Create new secret key"
   - Copy and save it somewhere safe (you'll need this later)

### 3. **A Computer with:**
   - Windows, Mac, or Linux
   - At least 5GB free disk space
   - Internet connection

---

## <a name="setup-aws"></a>🔑 Step 1: Setup AWS Account & Get Access Keys

### What is an AWS Access Key?
An access key is like a password that lets your computer talk to AWS services. You need this to deploy.

### How to Create Access Keys:

**1. Go to AWS Console**
- Open: https://console.aws.amazon.com/
- Sign in with your email and password

**2. Create Access Key**
- Click your profile icon (top right)
- Click "Security Credentials"
- Scroll down to "Access Keys"
- Click "Create New Access Key"
- You'll see:
  - **Access Key ID** (looks like: `AKIA...`)
  - **Secret Access Key** (looks like: `wJal...`)
- ⚠️ **IMPORTANT**: Save these somewhere safe! You won't see the secret key again.

**3. Choose Your Region**
- Go to: https://console.aws.amazon.com/
- Look at top right corner
- You'll see "Region" dropdown (e.g., "us-east-1")
- Remember this region name - you'll need it later

---

## <a name="install-tools"></a>🛠️ Step 2: Install Required Tools

You need 4 tools to deploy:

### Tool 1: AWS CLI (Amazon's Command Tool)

**What is it?** Software that lets you control AWS from your computer.

**How to install:**

**On Windows:**
1. Download from: https://awscli.amazonaws.com/AWSCLIV2.msi
2. Double-click the downloaded file
3. Click "Next" and "Install"

**On Mac:**
```bash
curl "https://awscli.amazonaws.com/AWSCLIV2.pkg" -o "AWSCLIV2.pkg"
sudo installer -pkg AWSCLIV2.pkg -target /
```

**On Linux:**
```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
```

**Verify it installed:**
```powershell
aws --version
```
Should show: `aws-cli/2.x.x`

---

### Tool 2: Docker (Container Software)

**What is it?** Software that packages your application into a container (like a box) that can run anywhere.

**How to install:**

**On Windows & Mac:**
1. Download from: https://www.docker.com/products/docker-desktop
2. Double-click the installer
3. Follow the installation steps
4. Restart your computer when done

**On Linux:**
```bash
sudo apt-get update
sudo apt-get install docker.io docker-compose -y
sudo usermod -aG docker $USER
```

**Verify it installed:**
```powershell
docker --version
```
Should show: `Docker version 20.x.x`

---

### Tool 3: Git (Version Control)

**What is it?** Software to manage code versions.

**How to install:**

1. Download from: https://git-scm.com/download
2. Install with default settings

**Verify it installed:**
```powershell
git --version
```

---

### Tool 4: Maven (Building Tool)

**What is it?** Tool to build/compile your Java code.

Already included in your project! (The file `mvnw` and `mvnw.cmd`)

---

## <a name="prepare-secrets"></a>🔐 Step 3: Prepare Your Secrets

"Secrets" are sensitive information like API keys and passwords.

### What secrets do we need?

1. **Database Password** - Password for PostgreSQL database
2. **OpenAI API Key** - Key from OpenAI (you got this earlier)
3. **JWT Secret** - Secret key for user authentication

### Create a File with Your Secrets

**On Windows:**

1. Open Notepad
2. Copy this content and replace the values:

```
DB_USER=postgres
DB_PASSWORD=MySecure123!Password@
OPENAI_API_KEY=sk-your-actual-openai-key-here
JWT_SECRET=MyJWTSecretKeyMin32CharactersLong!
AWS_REGION=us-east-1
EC2_KEY_NAME=smartdocs-key
```

3. Save as `deployment-secrets.txt` on your Desktop
4. Keep this file safe and secret!

---

## <a name="deploy"></a>🚀 Step 4: Configure AWS Credentials

Now let's tell your computer your AWS access keys.

### On Windows (PowerShell):

1. Press `Windows + R`
2. Type `powershell` and press Enter
3. Copy and paste this command:

```powershell
aws configure
```

You'll be asked for 4 things:

```
AWS Access Key ID [None]: AKIA... (paste your Access Key ID)
AWS Secret Access Key [None]: wJal... (paste your Secret Access Key)
Default region name [None]: us-east-1 (or your region)
Default output format [None]: json (just press Enter)
```

**Example:**
```powershell
AWS Access Key ID [None]: AKIAIOSFODNN7EXAMPLE
AWS Secret Access Key [None]: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
Default region name [None]: us-east-1
Default output format [None]: json
```

### Verify it worked:
```powershell
aws sts get-caller-identity
```

Should show JSON with your account info.

---

## <a name="deploy"></a>⚡ Step 5: Run the Automated Deployment

We have a script that does everything for you!

### What the script does:
1. ✓ Creates password/key storage in AWS
2. ✓ Creates storage for Docker images
3. ✓ Builds and uploads your application
4. ✓ Sets up servers and networking
5. ✓ Starts everything
6. ✓ Gives you a URL to access your app

### How to run it:

**1. Open PowerShell**
- Press `Windows + R`
- Type `powershell`
- Press Enter

**2. Navigate to Your Project**
```powershell
cd "C:\Users\AYUSH SHARMA\Documents\storeHere\SmartDocs"
```

**3. Check Docker is running**
```powershell
docker ps
```

Should show "CONTAINER ID" header (not an error).

**4. Run the deployment script**

```powershell
./deploy-aws.ps1 -Region us-east-1 -KeyName smartdocs-key -InstanceType t3.medium
```

**What the parameters mean:**
- `-Region us-east-1` = AWS region (change if needed)
- `-KeyName smartdocs-key` = Name for your security key
- `-InstanceType t3.medium` = Size of server (t3.medium is good for starting)

**5. Enter Your Information**

The script will ask you:

```
Enter database password (or press Enter to generate): MySecure123!Password@
Enter OpenAI API key: sk-your-actual-openai-key-here
Enter JWT secret (or press Enter to generate):
```

Just paste your values from your secrets file.

---

## <a name="verify"></a>✅ Step 6: Verify Deployment

The script will show progress. Watch for messages like:

```
✓ AWS CLI found
✓ Docker found
✓ AWS Account ID: 123456789012
✓ Created database password secret
✓ Created backend ECR repository
✓ Building backend image...
✓ Backend image pushed to ECR
✓ CloudFormation stack initiated
```

The deployment takes **5-15 minutes**. Be patient!

### What's happening in the background:

1. **AWS is creating servers** ☁️
2. **It's installing docker containers** 📦
3. **It's setting up networking** 🌐
4. **It's starting your application** 🚀

### When it's done, you'll see:

```
✓ SmartDocs deployment completed successfully!
✓ Application URL: http://smartdocs-alb-XXXX.region.elb.amazonaws.com
```

**Save this URL!** This is your application.

---

## <a name="access"></a>🌐 Step 7: Access Your Application

### Wait 2-3 Minutes
The containers take time to start. In the meantime, you can:

1. Check the status (see next section)
2. Get a coffee ☕
3. Celebrate! 🎉

### Open Your Application

1. In your browser, go to the URL from step 6
2. You should see the SmartDocs login page
3. Click "Register" to create an account
4. Upload a PDF and ask questions!

### First Time Setup Checklist:
- [ ] Account created
- [ ] Can login
- [ ] Can upload a PDF
- [ ] Can ask questions about the PDF

---

## <a name="troubleshoot"></a>🔧 Troubleshooting

### Problem 1: "AWS CLI not found"

**Solution:**
- Install AWS CLI from: https://aws.amazon.com/cli/
- Restart PowerShell
- Run: `aws --version`

### Problem 2: "Docker not running"

**Solution:**
- Start Docker Desktop (find it in your Applications)
- Wait 1-2 minutes for it to start
- In PowerShell, run: `docker ps`

### Problem 3: "Access Denied" error

**Solution:**
- Check your AWS Access Keys are correct
- Run: `aws configure` again
- Make sure you copied the full keys

### Problem 4: "Application URL shows 502 Bad Gateway"

**Solution 1:** Wait longer (containers take 5-10 minutes to start)

**Solution 2:** Check logs:
```powershell
$Region = "us-east-1"
aws logs tail /ecs/smartdocs --follow --region $Region
```

**Solution 3:** Restart containers:
```powershell
aws ecs update-service `
  --cluster smartdocs-cluster `
  --service smartdocs-service `
  --force-new-deployment `
  --region us-east-1
```

### Problem 5: "Application loads but nothing works"

**Solution:**
1. Check OpenAI API key is correct
2. Check database is running: Check logs (see above)
3. Try logging out and back in

### Problem 6: "How do I see what's running?"

**Check services status:**
```powershell
aws ecs describe-services `
  --cluster smartdocs-cluster `
  --services smartdocs-service `
  --region us-east-1
```

**View logs:**
```powershell
aws logs tail /ecs/smartdocs --follow --region us-east-1
```

---

## 📊 Costs

Your deployment will cost approximately:

| Component | Cost/Month |
|-----------|-----------|
| Servers (2x t3.medium) | $30 |
| Load Balancer | $18 |
| Data Transfer | $1 |
| Logs & Monitoring | $1 |
| **TOTAL** | **~$50/month** |

**Free Tier Note:** If you're on AWS free tier, some of this might be free!

---

## 🧹 Cleaning Up (When You're Done)

To delete everything and stop charges:

```powershell
$Region = "us-east-1"

# Delete the main infrastructure
aws cloudformation delete-stack --stack-name smartdocs-stack --region $Region

# Wait 5 minutes, then delete databases/storage
aws ecr delete-repository --repository-name smartdocs-backend --force --region $Region
aws ecr delete-repository --repository-name smartdocs-frontend --force --region $Region

# Delete secrets
aws secretsmanager delete-secret --secret-id smartdocs/db-password --force-delete-without-recovery --region $Region
aws secretsmanager delete-secret --secret-id smartdocs/openai-key --force-delete-without-recovery --region $Region
aws secretsmanager delete-secret --secret-id smartdocs/jwt-secret --force-delete-without-recovery --region $Region
```

---

## ❓ Common Questions

**Q: Will I be charged if I leave it running?**
A: Yes. Turn it off when not using it (see cleanup above) or update the script to auto-stop instances.

**Q: How do I update my application?**
A: Make changes to code locally, then run the deployment script again.

**Q: Can I add more servers?**
A: Yes. In the CloudFormation template, change `DesiredCapacity` from 2 to 3+ and update the stack.

**Q: What if I need a custom domain?**
A: After deployment works, we can add Route53 and SSL certificate separately.

**Q: How do I SSH into the server?**
A: See [AWS-DEPLOYMENT.md](./AWS-DEPLOYMENT.md) for SSH instructions.

---

## ✨ You're Ready!

Just follow these steps:

1. ✓ Create AWS account
2. ✓ Get OpenAI API key
3. ✓ Install AWS CLI and Docker
4. ✓ Run deployment script
5. ✓ Open your app URL
6. ✓ Start using SmartDocs!

**Happy deploying! 🚀**

---

## 📞 Need More Help?

Check these files:
- [QUICK-START.md](./QUICK-START.md) - Quick command reference
- [AWS-DEPLOYMENT.md](./AWS-DEPLOYMENT.md) - Detailed technical guide

Or visit:
- AWS Support: https://aws.amazon.com/premiumsupport/
- Docker Docs: https://docs.docker.com/
- Spring Boot: https://spring.io/projects/spring-boot
