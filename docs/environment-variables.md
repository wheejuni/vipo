# Environment Variables Configuration

This document describes all environment variables used by the AI Chat Platform application.

## Required Environment Variables

### Database Configuration
- `DATABASE_URL` - PostgreSQL database connection URL
  - Example: `jdbc:postgresql://localhost:5432/ai_chat`
  - Production: `jdbc:postgresql://prod-db-host:5432/ai_chat_prod`

- `DATABASE_USERNAME` - Database username
  - Example: `postgres`

- `DATABASE_PASSWORD` - Database password
  - Example: `your-secure-password`

### AI Service Configuration
- `OPENAI_API_KEY` - OpenAI API key for AI responses
  - Example: `sk-your-openai-api-key`
  - Required for AI functionality

### Security Configuration
- `JWT_SECRET` - Secret key for JWT token signing
  - Example: `your-very-secure-jwt-secret-key-at-least-256-bits`
  - **Important**: Must be at least 256 bits (32 characters) for production

## Optional Environment Variables

### Database Pool Configuration
- `DB_POOL_SIZE` - Maximum database connection pool size (default: 20)
- `DB_MIN_IDLE` - Minimum idle connections (default: 5)
- `DB_IDLE_TIMEOUT` - Idle timeout in milliseconds (default: 300000)
- `DB_CONNECTION_TIMEOUT` - Connection timeout in milliseconds (default: 20000)
- `DB_LEAK_DETECTION` - Leak detection threshold in milliseconds (default: 60000)

### JWT Configuration
- `JWT_EXPIRATION` - JWT token expiration time in milliseconds (default: 86400000 - 24 hours)
- `JWT_ISSUER` - JWT token issuer (default: ai-chat-platform)

### AI Configuration
- `OPENAI_MODEL` - OpenAI model to use (default: gpt-3.5-turbo)
- `OPENAI_TEMPERATURE` - AI response temperature (default: 0.7)
- `OPENAI_MAX_TOKENS` - Maximum tokens per response (default: 2000)
- `OPENAI_TIMEOUT` - AI service timeout (default: 30s)

### Application Configuration
- `THREAD_TIMEOUT_MINUTES` - Thread timeout in minutes (default: 30)
- `SERVER_PORT` - Server port (default: 8080)
- `CONTEXT_PATH` - Application context path (default: /)

### CORS Configuration
- `CORS_ALLOWED_ORIGINS` - Comma-separated list of allowed origins
  - Example: `http://localhost:3000,https://yourdomain.com`
- `CORS_ALLOWED_METHODS` - Allowed HTTP methods (default: GET,POST,PUT,DELETE,OPTIONS)
- `CORS_ALLOWED_HEADERS` - Allowed headers (default: *)
- `CORS_ALLOW_CREDENTIALS` - Allow credentials (default: true)
- `CORS_MAX_AGE` - Preflight cache time in seconds (default: 3600)

### Rate Limiting
- `RATE_LIMIT_ENABLED` - Enable rate limiting (default: true)
- `RATE_LIMIT_RPM` - Requests per minute (default: 60)
- `RATE_LIMIT_BURST` - Burst capacity (default: 100)

### Analytics Configuration
- `ANALYTICS_BATCH_SIZE` - Analytics batch processing size (default: 100)
- `ANALYTICS_RETENTION_DAYS` - Data retention period in days (default: 90)

### Logging Configuration
- `LOG_LEVEL` - Application log level (default: INFO)
- `SECURITY_LOG_LEVEL` - Security log level (default: WARN)
- `SQL_LOG_LEVEL` - SQL log level (default: WARN)
- `SQL_PARAM_LOG_LEVEL` - SQL parameter log level (default: WARN)
- `WEB_LOG_LEVEL` - Web log level (default: INFO)
- `FLYWAY_LOG_LEVEL` - Flyway log level (default: INFO)
- `LOG_FILE` - Log file path (default: logs/ai-chat-platform.log)
- `LOG_FILE_MAX_SIZE` - Maximum log file size (default: 10MB)
- `LOG_FILE_MAX_HISTORY` - Number of log files to keep (default: 30)
- `LOG_FILE_TOTAL_SIZE` - Total log files size cap (default: 1GB)

### Monitoring Configuration
- `MANAGEMENT_ENDPOINTS` - Exposed management endpoints (default: health,info,metrics,prometheus)
- `MANAGEMENT_BASE_PATH` - Management endpoints base path (default: /actuator)
- `HEALTH_SHOW_DETAILS` - Health endpoint detail level (default: when-authorized)
- `PROMETHEUS_ENABLED` - Enable Prometheus metrics (default: true)

### Security Configuration
- `REQUIRE_SSL` - Require SSL connections (default: false)
- `HTTP2_ENABLED` - Enable HTTP/2 (default: true)
- `INCLUDE_STACKTRACE` - Include stack traces in error responses (default: never)

### Flyway Configuration
- `FLYWAY_CLEAN_DISABLED` - Disable Flyway clean command (default: true)

## Environment-Specific Configurations

### Development Environment
```bash
export SPRING_PROFILES_ACTIVE=dev
export DATABASE_URL=jdbc:postgresql://localhost:5432/ai_chat_dev
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=password
export OPENAI_API_KEY=your-dev-api-key
export JWT_SECRET=dev-secret-key-for-development-only
export LOG_LEVEL=DEBUG
export FLYWAY_CLEAN_DISABLED=false
```

### Production Environment
```bash
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL=jdbc:postgresql://prod-db:5432/ai_chat_prod
export DATABASE_USERNAME=ai_chat_user
export DATABASE_PASSWORD=your-secure-production-password
export OPENAI_API_KEY=your-production-api-key
export JWT_SECRET=your-very-secure-production-jwt-secret
export CORS_ALLOWED_ORIGINS=https://yourdomain.com
export LOG_LEVEL=INFO
export REQUIRE_SSL=true
```

### Test Environment
```bash
export SPRING_PROFILES_ACTIVE=test
# Test environment uses H2 in-memory database
# Most configurations use default test values
```

## Security Notes

1. **JWT_SECRET**: Must be at least 256 bits (32 characters) for production
2. **DATABASE_PASSWORD**: Use strong passwords in production
3. **OPENAI_API_KEY**: Keep API keys secure and rotate regularly
4. **CORS_ALLOWED_ORIGINS**: Restrict to specific domains in production
5. **REQUIRE_SSL**: Always set to true in production environments

## Validation

The application will validate critical environment variables on startup:
- Database connectivity
- JWT secret strength
- OpenAI API key validity
- Required configuration presence