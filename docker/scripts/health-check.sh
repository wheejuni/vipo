#!/bin/bash

# Health check script for AI Chat Platform
# This script performs comprehensive health checks for the application

set -e

# Configuration
APP_URL="http://localhost:8080"
HEALTH_ENDPOINT="$APP_URL/actuator/health"
TIMEOUT=10

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    local status=$1
    local message=$2
    
    case $status in
        "OK")
            echo -e "${GREEN}✓${NC} $message"
            ;;
        "WARN")
            echo -e "${YELLOW}⚠${NC} $message"
            ;;
        "ERROR")
            echo -e "${RED}✗${NC} $message"
            ;;
    esac
}

# Function to check HTTP endpoint
check_endpoint() {
    local url=$1
    local expected_status=${2:-200}
    local timeout=${3:-$TIMEOUT}
    
    local response=$(curl -s -w "%{http_code}" -o /dev/null --max-time $timeout "$url" 2>/dev/null || echo "000")
    
    if [ "$response" = "$expected_status" ]; then
        return 0
    else
        return 1
    fi
}

# Function to check JSON endpoint and parse response
check_json_endpoint() {
    local url=$1
    local timeout=${2:-$TIMEOUT}
    
    local response=$(curl -s --max-time $timeout "$url" 2>/dev/null || echo "{}")
    echo "$response"
}

echo "AI Chat Platform Health Check"
echo "============================="

# Check if application is responding
print_status "INFO" "Checking application availability..."
if check_endpoint "$APP_URL"; then
    print_status "OK" "Application is responding"
else
    print_status "ERROR" "Application is not responding"
    exit 1
fi

# Check health endpoint
print_status "INFO" "Checking health endpoint..."
health_response=$(check_json_endpoint "$HEALTH_ENDPOINT")
health_status=$(echo "$health_response" | grep -o '"status":"[^"]*"' | cut -d'"' -f4 2>/dev/null || echo "UNKNOWN")

if [ "$health_status" = "UP" ]; then
    print_status "OK" "Health check passed: $health_status"
else
    print_status "ERROR" "Health check failed: $health_status"
    echo "Health response: $health_response"
    exit 1
fi

# Check individual health components
print_status "INFO" "Checking individual components..."

# Database health
db_status=$(echo "$health_response" | grep -o '"db":{"status":"[^"]*"' | cut -d'"' -f6 2>/dev/null || echo "UNKNOWN")
if [ "$db_status" = "UP" ]; then
    print_status "OK" "Database: $db_status"
else
    print_status "ERROR" "Database: $db_status"
fi

# Disk space health
disk_status=$(echo "$health_response" | grep -o '"diskSpace":{"status":"[^"]*"' | cut -d'"' -f6 2>/dev/null || echo "UNKNOWN")
if [ "$disk_status" = "UP" ]; then
    print_status "OK" "Disk space: $disk_status"
else
    print_status "WARN" "Disk space: $disk_status"
fi

# Check metrics endpoint
print_status "INFO" "Checking metrics endpoint..."
if check_endpoint "$APP_URL/actuator/metrics"; then
    print_status "OK" "Metrics endpoint is available"
else
    print_status "WARN" "Metrics endpoint is not available"
fi

# Check info endpoint
print_status "INFO" "Checking info endpoint..."
if check_endpoint "$APP_URL/actuator/info"; then
    print_status "OK" "Info endpoint is available"
    
    # Get application info
    info_response=$(check_json_endpoint "$APP_URL/actuator/info")
    app_name=$(echo "$info_response" | grep -o '"name":"[^"]*"' | cut -d'"' -f4 2>/dev/null || echo "Unknown")
    app_version=$(echo "$info_response" | grep -o '"version":"[^"]*"' | cut -d'"' -f4 2>/dev/null || echo "Unknown")
    
    if [ "$app_name" != "Unknown" ] && [ "$app_version" != "Unknown" ]; then
        print_status "OK" "Application: $app_name v$app_version"
    fi
else
    print_status "WARN" "Info endpoint is not available"
fi

# Check API endpoints
print_status "INFO" "Checking API endpoints..."

# Auth endpoints (should return 400 for empty body, not 404)
if check_endpoint "$APP_URL/api/auth/register" 400; then
    print_status "OK" "Auth register endpoint is available"
else
    print_status "ERROR" "Auth register endpoint is not available"
fi

if check_endpoint "$APP_URL/api/auth/login" 400; then
    print_status "OK" "Auth login endpoint is available"
else
    print_status "ERROR" "Auth login endpoint is not available"
fi

# Protected endpoints (should return 401 without auth)
if check_endpoint "$APP_URL/api/conversations" 401; then
    print_status "OK" "Conversations endpoint is protected"
else
    print_status "ERROR" "Conversations endpoint protection issue"
fi

echo ""
print_status "OK" "Health check completed successfully!"
exit 0