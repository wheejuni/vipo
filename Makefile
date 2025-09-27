# AI Chat Platform Docker Operations
# Usage: make <target>

.PHONY: help build up down logs clean test health

# Default target
help:
	@echo "AI Chat Platform Docker Commands"
	@echo "================================"
	@echo ""
	@echo "Development:"
	@echo "  build     - Build the application Docker image"
	@echo "  up        - Start all services"
	@echo "  up-full   - Start all services including optional ones"
	@echo "  down      - Stop all services"
	@echo "  restart   - Restart the application service"
	@echo "  logs      - Show application logs"
	@echo "  logs-all  - Show all services logs"
	@echo ""
	@echo "Database:"
	@echo "  db-shell  - Access PostgreSQL shell"
	@echo "  db-reset  - Reset database (removes all data)"
	@echo ""
	@echo "Maintenance:"
	@echo "  clean     - Remove all containers, networks, and volumes"
	@echo "  health    - Run health check"
	@echo "  test      - Run application tests"
	@echo ""
	@echo "Production:"
	@echo "  prod-up   - Start production environment"
	@echo "  prod-down - Stop production environment"

# Development commands
build:
	@echo "Building AI Chat Platform..."
	docker-compose build ai-chat-app

up:
	@echo "Starting development environment..."
	docker-compose up -d
	@echo "Services started. Application available at http://localhost:8080"
	@echo "Run 'make health' to check service health"

up-full:
	@echo "Starting development environment with all services..."
	docker-compose --profile with-redis --profile with-pgadmin up -d
	@echo "Services started:"
	@echo "  - Application: http://localhost:8080"
	@echo "  - pgAdmin: http://localhost:5050 (admin@example.com/admin)"
	@echo "  - Redis: localhost:6379"

down:
	@echo "Stopping all services..."
	docker-compose down

restart:
	@echo "Restarting application..."
	docker-compose restart ai-chat-app

logs:
	@echo "Showing application logs (Ctrl+C to exit)..."
	docker-compose logs -f ai-chat-app

logs-all:
	@echo "Showing all service logs (Ctrl+C to exit)..."
	docker-compose logs -f

# Database commands
db-shell:
	@echo "Connecting to PostgreSQL..."
	docker-compose exec postgres psql -U postgres -d ai_chat_dev

db-reset:
	@echo "Resetting database (this will remove all data)..."
	@read -p "Are you sure? [y/N] " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		docker-compose down -v; \
		docker-compose up -d; \
		echo "Database reset complete"; \
	else \
		echo "Database reset cancelled"; \
	fi

# Maintenance commands
clean:
	@echo "Cleaning up Docker resources..."
	docker-compose down -v --remove-orphans
	docker system prune -f
	@echo "Cleanup complete"

health:
	@echo "Running health check..."
	@if [ -f docker/scripts/health-check.sh ]; then \
		./docker/scripts/health-check.sh; \
	else \
		curl -f http://localhost:8080/actuator/health || echo "Health check failed"; \
	fi

test:
	@echo "Running application tests..."
	docker-compose exec ai-chat-app ./gradlew test

# Production commands
prod-up:
	@echo "Starting production environment..."
	@if [ -z "$$OPENAI_API_KEY" ]; then \
		echo "Error: OPENAI_API_KEY environment variable is required"; \
		exit 1; \
	fi
	@if [ -z "$$JWT_SECRET" ]; then \
		echo "Error: JWT_SECRET environment variable is required"; \
		exit 1; \
	fi
	docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
	@echo "Production environment started"

prod-down:
	@echo "Stopping production environment..."
	docker-compose -f docker-compose.yml -f docker-compose.prod.yml down

# Development workflow
dev-rebuild:
	@echo "Rebuilding and restarting application..."
	docker-compose build ai-chat-app
	docker-compose up -d ai-chat-app
	@echo "Application rebuilt and restarted"

# Quick status check
status:
	@echo "Service Status:"
	@echo "==============="
	docker-compose ps
	@echo ""
	@echo "Quick Health Check:"
	@curl -s http://localhost:8080/actuator/health | grep -o '"status":"[^"]*"' | head -1 || echo "Application not responding"

# Show environment info
env-info:
	@echo "Environment Information:"
	@echo "========================"
	@echo "Docker version: $$(docker --version)"
	@echo "Docker Compose version: $$(docker-compose --version)"
	@echo "Current directory: $$(pwd)"
	@echo "Environment file: $$(if [ -f .env ]; then echo 'Present'; else echo 'Missing (.env.example available)'; fi)"
	@echo ""
	@echo "Required environment variables:"
	@echo "  OPENAI_API_KEY: $$(if [ -n "$$OPENAI_API_KEY" ]; then echo 'Set'; else echo 'Not set'; fi)"