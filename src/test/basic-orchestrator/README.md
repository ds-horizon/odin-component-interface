# Odin Component Interface - Test Environment

This directory contains test orchestration scripts for the Odin Component Interface. The tests can run in two modes:
1. **Local Mode** - Uses LocalStack to simulate AWS S3 (no AWS account needed)
2. **AWS Mode** - Uses real AWS S3 (requires AWS credentials)

## Prerequisites

### For Local Mode
- Docker and Docker Compose
- Groovy
- Maven

### For AWS Mode
- All of the above, plus:
- AWS CLI configured with valid credentials
- AWS_PROFILE environment variable set

## Quick Start

### Local Testing (Recommended for development)
```bash
# Set up LocalStack environment
./setup-env.sh local

# Export the environment variables shown in the output
export ODIN_TEST_MODE="local"
export ODIN_S3_BUCKET="odin-components-state"
export AWS_ACCESS_KEY_ID="test"
export AWS_SECRET_ACCESS_KEY="test"
export AWS_DEFAULT_REGION="us-east-1"

# Run tests
cd nginx/local_docker
./deploy.sh
./healthcheck.sh
./undeploy.sh
```

### AWS Testing
```bash
# Set up AWS environment
export AWS_PROFILE=your-profile-name
./setup-env.sh aws

# The script will:
# 1. Check if 'odin-components-state' bucket exists
# 2. If not, prompt you to:
#    - Provide an existing bucket name, OR
#    - Press Enter to create the default bucket (requires permissions)

# Export the environment variables shown in the output
export ODIN_TEST_MODE="aws"
export ODIN_S3_BUCKET="<bucket-name-from-setup>"
export AWS_PROFILE="your-profile-name"

# Run tests
cd nginx/local_docker
./deploy.sh
./healthcheck.sh
./undeploy.sh
```

## Directory Structure

```
basic-orchestrator/
├── docker-compose.yml       # LocalStack configuration
├── setup-env.sh            # Environment setup script
├── init-localstack.sh      # LocalStack initialization
├── README.md              # This file
└── nginx/
    └── local_docker/
        ├── common-env.sh   # Shared environment config
        ├── deploy.sh       # Deploy stage
        ├── undeploy.sh     # Undeploy stage
        ├── predeploy.sh    # Pre-deploy stage
        ├── postdeploy.sh   # Post-deploy stage
        ├── healthcheck.sh  # Health check stage
        ├── validate.sh     # Validation stage
        └── operate.sh      # Operations stage
```

## How It Works

### Local Mode with LocalStack
1. `setup-env.sh local` starts a LocalStack container that simulates AWS S3
2. The LocalStack container automatically creates the required S3 bucket
3. Test scripts use `http://localhost:4566` as the S3 endpoint
4. No real AWS credentials are needed (dummy credentials are used)
5. State files are stored in the LocalStack S3 simulator

### AWS Mode
1. `setup-env.sh aws` verifies AWS credentials are configured
2. Test scripts use the real AWS S3 service
3. State files are stored in actual S3 buckets
4. Requires valid AWS credentials and permissions

## Environment Variables

The following environment variables control the test mode:

- `ODIN_TEST_MODE`: Set to `local` or `aws` (default: `aws`)
- `ODIN_S3_BUCKET`: S3 bucket name for state storage (default: `odin-components-state`)
- `AWS_PROFILE`: Required for AWS mode
- `S3_ENDPOINT`: Automatically set based on mode
  - Local: `http://localhost:4566`
  - AWS: `https://s3.us-east-1.amazonaws.com`

## S3 Bucket Configuration

### Local Mode
- Always uses the bucket name specified in `ODIN_S3_BUCKET` (default: `odin-components-state`)
- Bucket is automatically created in LocalStack on startup

### AWS Mode
- The setup script checks if the default bucket (`odin-components-state`) exists
- If the bucket doesn't exist, you have three options:
  1. **Provide an existing bucket name** - Use any S3 bucket you have access to
  2. **Create the default bucket** - Press Enter (requires S3 bucket creation permissions)
  3. **Use a different bucket** - Set `ODIN_S3_BUCKET` environment variable before running setup

### Using a Custom Bucket
```bash
# Option 1: Set environment variable before setup
export ODIN_S3_BUCKET="my-custom-bucket"
./setup-env.sh aws

# Option 2: Provide bucket name when prompted during setup
./setup-env.sh aws
# When prompted, enter: my-custom-bucket
```

## Troubleshooting

### LocalStack not starting
```bash
# Check if LocalStack is running
docker ps | grep odin-localstack

# View LocalStack logs
docker logs odin-localstack

# Restart LocalStack
docker compose down
docker compose up -d
```

### Permission Errors in AWS Mode
Ensure your AWS profile has permissions to:
- Read/write to the S3 bucket you're using
- Create/delete objects in the bucket
- (Optional) Create S3 buckets if you want to create new buckets

### Bucket Not Found Errors
- In AWS mode, ensure the bucket exists and you have access
- Run `aws s3 ls s3://<bucket-name>` to verify access
- Check that `ODIN_S3_BUCKET` environment variable is set correctly

### Tests failing in Local Mode
1. Ensure LocalStack is running: `docker ps`
2. Check bucket exists: `aws --endpoint-url=http://localhost:4566 s3 ls`
3. Verify environment: `echo $ODIN_TEST_MODE` (should be "local")

## Cleanup

### Local Mode
```bash
# Stop and remove LocalStack container
docker compose down

# Remove LocalStack data volume
docker volume rm basic-orchestrator_localstack-data
```

### AWS Mode
State files remain in S3 unless manually deleted.

## Benefits of Local Testing

- **No AWS Account Required**: Test without AWS credentials
- **Fast Iteration**: No network latency to AWS
- **Cost-Free**: No AWS charges for S3 operations
- **Isolated Testing**: No risk of affecting production resources
- **CI/CD Friendly**: Can run in any CI environment with Docker
