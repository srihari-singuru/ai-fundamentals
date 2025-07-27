#!/bin/bash

# Docker Security Scanning Script
# This script performs security scanning on the Docker image

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
IMAGE_NAME="${1:-ai-fundamentals:latest}"
SCAN_RESULTS_DIR="./security-scan-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

echo -e "${GREEN}Starting Docker security scan for image: ${IMAGE_NAME}${NC}"

# Create results directory
mkdir -p "${SCAN_RESULTS_DIR}"

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to run Trivy scan
run_trivy_scan() {
    if command_exists trivy; then
        echo -e "${YELLOW}Running Trivy vulnerability scan...${NC}"
        trivy image \
            --format json \
            --output "${SCAN_RESULTS_DIR}/trivy_${TIMESTAMP}.json" \
            "${IMAGE_NAME}"
        
        trivy image \
            --format table \
            --output "${SCAN_RESULTS_DIR}/trivy_${TIMESTAMP}.txt" \
            "${IMAGE_NAME}"
        
        echo -e "${GREEN}Trivy scan completed. Results saved to ${SCAN_RESULTS_DIR}${NC}"
    else
        echo -e "${YELLOW}Trivy not found. Skipping vulnerability scan.${NC}"
        echo -e "${YELLOW}Install Trivy: https://aquasecurity.github.io/trivy/latest/getting-started/installation/${NC}"
    fi
}

# Function to run Docker Bench Security
run_docker_bench() {
    if command_exists docker-bench-security; then
        echo -e "${YELLOW}Running Docker Bench Security...${NC}"
        docker run --rm --net host --pid host --userns host --cap-add audit_control \
            -e DOCKER_CONTENT_TRUST=$DOCKER_CONTENT_TRUST \
            -v /etc:/etc:ro \
            -v /usr/bin/containerd:/usr/bin/containerd:ro \
            -v /usr/bin/runc:/usr/bin/runc:ro \
            -v /usr/lib/systemd:/usr/lib/systemd:ro \
            -v /var/lib:/var/lib:ro \
            -v /var/run/docker.sock:/var/run/docker.sock:ro \
            --label docker_bench_security \
            docker/docker-bench-security > "${SCAN_RESULTS_DIR}/docker_bench_${TIMESTAMP}.txt"
        
        echo -e "${GREEN}Docker Bench Security completed. Results saved to ${SCAN_RESULTS_DIR}${NC}"
    else
        echo -e "${YELLOW}Docker Bench Security not available. Skipping.${NC}"
    fi
}

# Function to check image configuration
check_image_config() {
    echo -e "${YELLOW}Checking image configuration...${NC}"
    
    # Check if image runs as non-root
    USER_CHECK=$(docker inspect "${IMAGE_NAME}" --format='{{.Config.User}}')
    if [ -n "$USER_CHECK" ] && [ "$USER_CHECK" != "root" ] && [ "$USER_CHECK" != "0" ]; then
        echo -e "${GREEN}✓ Image runs as non-root user: ${USER_CHECK}${NC}"
    else
        echo -e "${RED}✗ Image may be running as root user${NC}"
    fi
    
    # Check exposed ports
    EXPOSED_PORTS=$(docker inspect "${IMAGE_NAME}" --format='{{range $port, $config := .Config.ExposedPorts}}{{$port}} {{end}}')
    if [ -n "$EXPOSED_PORTS" ]; then
        echo -e "${GREEN}✓ Exposed ports: ${EXPOSED_PORTS}${NC}"
    else
        echo -e "${YELLOW}! No exposed ports found${NC}"
    fi
    
    # Check for health check
    HEALTHCHECK=$(docker inspect "${IMAGE_NAME}" --format='{{.Config.Healthcheck}}')
    if [ "$HEALTHCHECK" != "<nil>" ] && [ -n "$HEALTHCHECK" ]; then
        echo -e "${GREEN}✓ Health check configured${NC}"
    else
        echo -e "${YELLOW}! No health check configured${NC}"
    fi
    
    # Check image size
    IMAGE_SIZE=$(docker images "${IMAGE_NAME}" --format "{{.Size}}")
    echo -e "${GREEN}✓ Image size: ${IMAGE_SIZE}${NC}"
    
    # Save configuration check results
    {
        echo "Docker Image Security Configuration Check"
        echo "========================================"
        echo "Image: ${IMAGE_NAME}"
        echo "Timestamp: $(date)"
        echo ""
        echo "User: ${USER_CHECK}"
        echo "Exposed Ports: ${EXPOSED_PORTS}"
        echo "Health Check: ${HEALTHCHECK}"
        echo "Image Size: ${IMAGE_SIZE}"
    } > "${SCAN_RESULTS_DIR}/config_check_${TIMESTAMP}.txt"
}

# Function to run Hadolint (Dockerfile linter)
run_hadolint() {
    if command_exists hadolint; then
        echo -e "${YELLOW}Running Hadolint Dockerfile analysis...${NC}"
        hadolint Dockerfile > "${SCAN_RESULTS_DIR}/hadolint_${TIMESTAMP}.txt" 2>&1 || true
        echo -e "${GREEN}Hadolint analysis completed. Results saved to ${SCAN_RESULTS_DIR}${NC}"
    else
        echo -e "${YELLOW}Hadolint not found. Skipping Dockerfile analysis.${NC}"
        echo -e "${YELLOW}Install Hadolint: https://github.com/hadolint/hadolint${NC}"
    fi
}

# Function to check for secrets in image
check_secrets() {
    echo -e "${YELLOW}Checking for potential secrets in image...${NC}"
    
    # Create a temporary container to inspect filesystem
    CONTAINER_ID=$(docker create "${IMAGE_NAME}")
    
    # Check for common secret file patterns
    SECRET_PATTERNS=(
        "*.key"
        "*.pem"
        "*.p12"
        "*.jks"
        "*password*"
        "*secret*"
        "*.env"
    )
    
    SECRETS_FOUND=false
    for pattern in "${SECRET_PATTERNS[@]}"; do
        if docker export "${CONTAINER_ID}" | tar -tv | grep -i "${pattern}" > /dev/null 2>&1; then
            echo -e "${RED}✗ Potential secret files found matching pattern: ${pattern}${NC}"
            SECRETS_FOUND=true
        fi
    done
    
    if [ "$SECRETS_FOUND" = false ]; then
        echo -e "${GREEN}✓ No obvious secret files found${NC}"
    fi
    
    # Clean up
    docker rm "${CONTAINER_ID}" > /dev/null
}

# Main execution
main() {
    echo -e "${GREEN}Docker Security Scan Report${NC}"
    echo -e "${GREEN}===========================${NC}"
    echo "Image: ${IMAGE_NAME}"
    echo "Timestamp: $(date)"
    echo ""
    
    # Run all security checks
    check_image_config
    echo ""
    
    run_hadolint
    echo ""
    
    run_trivy_scan
    echo ""
    
    check_secrets
    echo ""
    
    run_docker_bench
    echo ""
    
    # Generate summary report
    {
        echo "Docker Security Scan Summary"
        echo "============================"
        echo "Image: ${IMAGE_NAME}"
        echo "Scan Date: $(date)"
        echo ""
        echo "Scan Results Location: ${SCAN_RESULTS_DIR}"
        echo ""
        echo "Files Generated:"
        ls -la "${SCAN_RESULTS_DIR}"/*"${TIMESTAMP}"* 2>/dev/null || echo "No scan files generated"
    } > "${SCAN_RESULTS_DIR}/summary_${TIMESTAMP}.txt"
    
    echo -e "${GREEN}Security scan completed!${NC}"
    echo -e "${GREEN}Results saved in: ${SCAN_RESULTS_DIR}${NC}"
    echo -e "${YELLOW}Review all generated reports for security findings.${NC}"
}

# Run main function
main "$@"