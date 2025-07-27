#!/bin/bash

# Kubernetes Deployment Script for AI Fundamentals
# This script deploys the AI Fundamentals application to Kubernetes

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
NAMESPACE="${NAMESPACE:-ai-fundamentals}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
ENVIRONMENT="${ENVIRONMENT:-production}"
DRY_RUN="${DRY_RUN:-false}"
WAIT_TIMEOUT="${WAIT_TIMEOUT:-300s}"

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${BLUE}[DEPLOY]${NC} $1"
}

# Function to check if kubectl is available
check_kubectl() {
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl is not installed or not in PATH"
        exit 1
    fi
    
    # Check if we can connect to the cluster
    if ! kubectl cluster-info &> /dev/null; then
        print_error "Cannot connect to Kubernetes cluster"
        exit 1
    fi
    
    print_status "kubectl is available and connected to cluster"
}

# Function to check if required secrets exist
check_secrets() {
    print_status "Checking for required secrets..."
    
    if [ -z "${OPEN_AI_API_KEY}" ]; then
        print_warning "OPEN_AI_API_KEY environment variable is not set"
        print_warning "You will need to create the secret manually or set this variable"
    fi
    
    # Check if secret already exists
    if kubectl get secret ai-fundamentals-secrets -n "${NAMESPACE}" &> /dev/null; then
        print_status "Secret ai-fundamentals-secrets already exists"
    else
        print_warning "Secret ai-fundamentals-secrets does not exist"
        if [ -n "${OPEN_AI_API_KEY}" ]; then
            print_status "Will create secret from environment variables"
        else
            print_error "Cannot create secret without OPEN_AI_API_KEY"
            print_error "Please set OPEN_AI_API_KEY environment variable or create the secret manually"
            exit 1
        fi
    fi
}

# Function to create namespace
create_namespace() {
    print_header "Creating namespace: ${NAMESPACE}"
    
    if [ "${DRY_RUN}" = "true" ]; then
        kubectl apply -f k8s/namespace.yaml --dry-run=client -o yaml
    else
        kubectl apply -f k8s/namespace.yaml
        print_status "Namespace ${NAMESPACE} created/updated"
    fi
}

# Function to create secrets
create_secrets() {
    print_header "Creating secrets"
    
    if kubectl get secret ai-fundamentals-secrets -n "${NAMESPACE}" &> /dev/null; then
        print_status "Secret already exists, skipping creation"
        return
    fi
    
    if [ -n "${OPEN_AI_API_KEY}" ]; then
        if [ "${DRY_RUN}" = "true" ]; then
            echo "Would create secret with provided environment variables"
        else
            kubectl create secret generic ai-fundamentals-secrets \
                --from-literal=OPEN_AI_API_KEY="${OPEN_AI_API_KEY}" \
                --from-literal=JWT_SECRET="${JWT_SECRET:-default-jwt-secret}" \
                --from-literal=DATABASE_PASSWORD="${DATABASE_PASSWORD:-default-db-password}" \
                --from-literal=REDIS_PASSWORD="${REDIS_PASSWORD:-default-redis-password}" \
                --from-literal=ENCRYPTION_KEY="${ENCRYPTION_KEY:-default-encryption-key}" \
                --namespace="${NAMESPACE}"
            print_status "Secrets created successfully"
        fi
    else
        print_warning "Applying secret template (you need to update with actual values)"
        if [ "${DRY_RUN}" = "true" ]; then
            kubectl apply -f k8s/secret.yaml --dry-run=client -o yaml
        else
            kubectl apply -f k8s/secret.yaml
        fi
    fi
}

# Function to create configmaps
create_configmaps() {
    print_header "Creating configmaps"
    
    if [ "${DRY_RUN}" = "true" ]; then
        kubectl apply -f k8s/configmap.yaml --dry-run=client -o yaml
    else
        kubectl apply -f k8s/configmap.yaml
        print_status "ConfigMaps created/updated"
    fi
}

# Function to create RBAC resources
create_rbac() {
    print_header "Creating RBAC resources"
    
    if [ "${DRY_RUN}" = "true" ]; then
        kubectl apply -f k8s/rbac.yaml --dry-run=client -o yaml
    else
        kubectl apply -f k8s/rbac.yaml
        print_status "RBAC resources created/updated"
    fi
}

# Function to deploy application
deploy_application() {
    print_header "Deploying application"
    
    # Update image tag in deployment if specified
    if [ "${IMAGE_TAG}" != "latest" ]; then
        print_status "Using image tag: ${IMAGE_TAG}"
        # Create a temporary deployment file with the correct image tag
        sed "s|image: ai-fundamentals:latest|image: ai-fundamentals:${IMAGE_TAG}|g" k8s/deployment.yaml > /tmp/deployment-${IMAGE_TAG}.yaml
        DEPLOYMENT_FILE="/tmp/deployment-${IMAGE_TAG}.yaml"
    else
        DEPLOYMENT_FILE="k8s/deployment.yaml"
    fi
    
    if [ "${DRY_RUN}" = "true" ]; then
        kubectl apply -f "${DEPLOYMENT_FILE}" --dry-run=client -o yaml
    else
        kubectl apply -f "${DEPLOYMENT_FILE}"
        print_status "Deployment created/updated"
        
        # Wait for deployment to be ready
        print_status "Waiting for deployment to be ready..."
        kubectl rollout status deployment/ai-fundamentals -n "${NAMESPACE}" --timeout="${WAIT_TIMEOUT}"
        print_status "Deployment is ready"
    fi
    
    # Clean up temporary file
    if [ "${IMAGE_TAG}" != "latest" ] && [ -f "/tmp/deployment-${IMAGE_TAG}.yaml" ]; then
        rm "/tmp/deployment-${IMAGE_TAG}.yaml"
    fi
}

# Function to create services
create_services() {
    print_header "Creating services"
    
    if [ "${DRY_RUN}" = "true" ]; then
        kubectl apply -f k8s/service.yaml --dry-run=client -o yaml
    else
        kubectl apply -f k8s/service.yaml
        print_status "Services created/updated"
    fi
}

# Function to create HPA and PDB
create_scaling_resources() {
    print_header "Creating scaling resources (HPA and PDB)"
    
    if [ "${DRY_RUN}" = "true" ]; then
        kubectl apply -f k8s/hpa.yaml --dry-run=client -o yaml
        kubectl apply -f k8s/pdb.yaml --dry-run=client -o yaml
    else
        kubectl apply -f k8s/hpa.yaml
        kubectl apply -f k8s/pdb.yaml
        print_status "HPA and PDB created/updated"
    fi
}

# Function to verify deployment
verify_deployment() {
    if [ "${DRY_RUN}" = "true" ]; then
        print_status "Dry run mode - skipping verification"
        return
    fi
    
    print_header "Verifying deployment"
    
    # Check pods
    print_status "Checking pod status..."
    kubectl get pods -n "${NAMESPACE}" -l app.kubernetes.io/name=ai-fundamentals
    
    # Check services
    print_status "Checking service status..."
    kubectl get services -n "${NAMESPACE}"
    
    # Check HPA
    print_status "Checking HPA status..."
    kubectl get hpa -n "${NAMESPACE}"
    
    # Check if pods are ready
    READY_PODS=$(kubectl get pods -n "${NAMESPACE}" -l app.kubernetes.io/name=ai-fundamentals -o jsonpath='{.items[*].status.conditions[?(@.type=="Ready")].status}' | grep -o "True" | wc -l)
    TOTAL_PODS=$(kubectl get pods -n "${NAMESPACE}" -l app.kubernetes.io/name=ai-fundamentals --no-headers | wc -l)
    
    print_status "Ready pods: ${READY_PODS}/${TOTAL_PODS}"
    
    if [ "${READY_PODS}" -eq "${TOTAL_PODS}" ] && [ "${TOTAL_PODS}" -gt 0 ]; then
        print_status "All pods are ready!"
    else
        print_warning "Not all pods are ready. Check pod logs for issues."
    fi
    
    # Show service endpoints
    print_status "Service endpoints:"
    kubectl get endpoints -n "${NAMESPACE}"
}

# Function to show access information
show_access_info() {
    if [ "${DRY_RUN}" = "true" ]; then
        return
    fi
    
    print_header "Access Information"
    
    # Get service information
    CLUSTER_IP=$(kubectl get service ai-fundamentals -n "${NAMESPACE}" -o jsonpath='{.spec.clusterIP}')
    print_status "Cluster IP: ${CLUSTER_IP}"
    
    # Check if LoadBalancer service exists and get external IP
    if kubectl get service ai-fundamentals-lb -n "${NAMESPACE}" &> /dev/null; then
        EXTERNAL_IP=$(kubectl get service ai-fundamentals-lb -n "${NAMESPACE}" -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
        if [ -n "${EXTERNAL_IP}" ]; then
            print_status "External IP: ${EXTERNAL_IP}"
            print_status "Application URL: http://${EXTERNAL_IP}"
        else
            print_status "LoadBalancer service created, waiting for external IP..."
        fi
    fi
    
    # Port forwarding instructions
    print_status "To access via port forwarding:"
    print_status "kubectl port-forward -n ${NAMESPACE} service/ai-fundamentals 8080:80"
    print_status "Then access: http://localhost:8080"
}

# Function to show logs
show_logs() {
    if [ "${DRY_RUN}" = "true" ]; then
        return
    fi
    
    print_header "Recent application logs"
    kubectl logs -n "${NAMESPACE}" -l app.kubernetes.io/name=ai-fundamentals --tail=20
}

# Main deployment function
main() {
    print_header "Starting Kubernetes deployment for AI Fundamentals"
    print_status "Environment: ${ENVIRONMENT}"
    print_status "Namespace: ${NAMESPACE}"
    print_status "Image Tag: ${IMAGE_TAG}"
    print_status "Dry Run: ${DRY_RUN}"
    
    # Pre-flight checks
    check_kubectl
    check_secrets
    
    # Deploy resources in order
    create_namespace
    create_secrets
    create_configmaps
    create_rbac
    deploy_application
    create_services
    create_scaling_resources
    
    # Verify deployment
    verify_deployment
    show_access_info
    
    if [ "${DRY_RUN}" = "false" ]; then
        show_logs
    fi
    
    print_status "Deployment completed successfully!"
}

# Function to show usage
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -n, --namespace NAMESPACE    Kubernetes namespace (default: ai-fundamentals)"
    echo "  -t, --tag TAG               Docker image tag (default: latest)"
    echo "  -e, --environment ENV       Environment (default: production)"
    echo "  -d, --dry-run              Perform dry run without applying changes"
    echo "  -w, --wait-timeout TIMEOUT  Timeout for waiting operations (default: 300s)"
    echo "  -h, --help                 Show this help message"
    echo ""
    echo "Environment Variables:"
    echo "  OPEN_AI_API_KEY            OpenAI API key (required)"
    echo "  JWT_SECRET                 JWT secret for authentication"
    echo "  DATABASE_PASSWORD          Database password"
    echo "  REDIS_PASSWORD             Redis password"
    echo "  ENCRYPTION_KEY             Encryption key for sensitive data"
    echo ""
    echo "Examples:"
    echo "  $0                         # Deploy with defaults"
    echo "  $0 --dry-run              # Dry run deployment"
    echo "  $0 --tag v1.2.3           # Deploy specific version"
    echo "  $0 --namespace staging    # Deploy to staging namespace"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -n|--namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        -t|--tag)
            IMAGE_TAG="$2"
            shift 2
            ;;
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -d|--dry-run)
            DRY_RUN="true"
            shift
            ;;
        -w|--wait-timeout)
            WAIT_TIMEOUT="$2"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

# Run main function
main "$@"