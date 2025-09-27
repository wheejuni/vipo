-- Initialize AI Chat Platform database
-- This script runs when the PostgreSQL container starts for the first time

-- Create additional database for testing if needed
CREATE DATABASE ai_chat_test;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE ai_chat_dev TO postgres;
GRANT ALL PRIVILEGES ON DATABASE ai_chat_test TO postgres;

-- Create extensions that might be useful
\c ai_chat_dev;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

\c ai_chat_test;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";