# Development Setup Guide

This guide explains how to set up the AI Chat Platform for development using Docker.

## Prerequisites

- Docker Desktop (version 4.0 or later)
- Docker Compose (version 2.0 or later)
- OpenAI API Key
- Git

## Quick Start

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd ai-chat-platform
   ```

2. **Set up environment variables**
   ```bash
   cp .env.example .env
   # Edit .env file with your OpenAI API key
   ```

3. **Start the development environment**
   ```bash
   docker-compose up -d
   ```

4. **Verify the setup**
   ```bash
   # Check if all services are running
   docker-compose ps
   
   # Check application health
   curl http://localhost:8080/actuator/health
   ```

## Environment Variables

Create a `.env` file in the project root with the following variables:

```bash
# Required
OPENAI_API_KEY=your-openai-api-key-here

# Optional (defaults provided)
POSTGRES_DB=ai_chat_dev
POSTGRES_USER=postgres
POSTGRES_PASSWORD=password
JWT_SECRET=dev-docker-secret-key-for-development-only
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001
```

## Docker Services

### Core Services

- **ai-chat-app**: Main application (port 8080)
- **postgres**: PostgreSQL database (port 5432)

### Optional Services

- **redis**: Redis cache (port 6379) - use `--profile with-redis`
- **pgadmin**: Database management UI (port 5050) - use `--profile with-pgadmin`

## Docker Commands

### Basic Operations

```bash
# Start all services
docker-compose up -d

# Start with optional services
docker-compose --profile with-redis --profile with-pgadmin up -d

# Stop all services
docker-compose down

# View logs
docker-compose logs -f ai-chat-app

# Rebuild application
docker-compose build ai-chat-app
docker-compose up -d ai-chat-app
```

### Database Operations

```bash
# Access PostgreSQL directly
docker-compose exec postgres psql -U postgres -d ai_chat_dev

# Run database migrations manually
docker-compose exec ai-chat-app java -jar app.jar --spring.flyway.command=migrate

# Reset database (development only)
docker-compose down -v
docker-compose up -d
```

### Development Workflow

```bash
# Make code changes
# Rebuild and restart application
docker-compose build ai-chat-app
docker-compose up -d ai-chat-app

# View application logs
docker-compose logs -f ai-chat-app

# Access application shell
docker-compose exec ai-chat-app sh
```

## Service URLs

- **Application**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **API Documentation**: http://localhost:8080/actuator/info
- **Database**: localhost:5432 (postgres/password)
- **pgAdmin** (if enabled): http://localhost:5050 (admin@example.com/admin)

## API Testing

### Authentication

```bash
# Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "name": "Test User"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

### Conversations

```bash
# Create a conversation (replace TOKEN with JWT from login)
curl -X POST http://localhost:8080/api/conversations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{
    "question": "Hello, how are you?",
    "model": "gpt-3.5-turbo",
    "isStreaming": false
  }'
```

## Troubleshooting

### Common Issues

1. **Port conflicts**
   ```bash
   # Check what's using the ports
   lsof -i :8080
   lsof -i :5432
   
   # Change ports in docker-compose.yml if needed
   ```

2. **Database connection issues**
   ```bash
   # Check database logs
   docker-compose logs postgres
   
   # Verify database is ready
   docker-compose exec postgres pg_isready -U postgres
   ```

3. **Application won't start**
   ```bash
   # Check application logs
   docker-compose logs ai-chat-app
   
   # Verify environment variables
   docker-compose exec ai-chat-app env | grep -E "(DATABASE|OPENAI|JWT)"
   ```

4. **OpenAI API issues**
   ```bash
   # Verify API key is set
   docker-compose exec ai-chat-app env | grep OPENAI_API_KEY
   
   # Check application logs for AI service errors
   docker-compose logs ai-chat-app | grep -i openai
   ```

### Reset Everything

```bash
# Stop and remove all containers, networks, and volumes
docker-compose down -v --remove-orphans

# Remove all images
docker-compose down --rmi all

# Start fresh
docker-compose up -d --build
```

## Production Deployment

For production deployment, use the production compose file:

```bash
# Set production environment variables
export POSTGRES_PASSWORD=your-secure-password
export JWT_SECRET=your-secure-jwt-secret
export OPENAI_API_KEY=your-production-api-key
export CORS_ALLOWED_ORIGINS=https://yourdomain.com

# Deploy with production configuration
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

## Monitoring

### Health Checks

```bash
# Application health
curl http://localhost:8080/actuator/health

# Database health
docker-compose exec postgres pg_isready -U postgres

# Service status
docker-compose ps
```

### Logs

```bash
# All services
docker-compose logs

# Specific service
docker-compose logs ai-chat-app

# Follow logs
docker-compose logs -f

# Last 100 lines
docker-compose logs --tail=100
```

### Metrics

```bash
# Application metrics
curl http://localhost:8080/actuator/metrics

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

## Development Tips

1. **Hot Reload**: For faster development, consider mounting the source code as a volume and using Spring Boot DevTools.

2. **Database GUI**: Use pgAdmin (with `--profile with-pgadmin`) for easier database management.

3. **API Testing**: Use tools like Postman or Insomnia with the provided API examples.

4. **Logs**: Always check logs when something isn't working as expected.

5. **Clean State**: Use `docker-compose down -v` to start with a clean database state.

## Next Steps

- Set up your frontend application to connect to http://localhost:8080
- Configure your IDE to connect to the database for development
- Set up automated testing with the Docker environment
- Configure CI/CD pipelines using the Docker setup