-- Create initial database schema for AI Chat Platform
-- Migration: V1__Create_initial_schema.sql

-- Create users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_user_role CHECK (role IN ('MEMBER', 'ADMIN'))
);

-- Create threads table
CREATE TABLE threads (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_threads_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create conversations table
CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    thread_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    model VARCHAR(255) NOT NULL,
    is_streaming BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_conversations_thread_id FOREIGN KEY (thread_id) REFERENCES threads(id) ON DELETE CASCADE
);

-- Create feedback table
CREATE TABLE feedback (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    is_positive BOOLEAN NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_feedback_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_conversation_id FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT chk_feedback_status CHECK (status IN ('PENDING', 'RESOLVED'))
);

-- Create activity_logs table
CREATE TABLE activity_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    activity_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_activity_logs_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_activity_type CHECK (activity_type IN ('USER_REGISTRATION', 'USER_LOGIN', 'CONVERSATION_CREATED'))
);

-- Create indexes for performance optimization

-- Users table indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Threads table indexes
CREATE INDEX idx_threads_user_id ON threads(user_id);
CREATE INDEX idx_threads_last_activity_at ON threads(last_activity_at);
CREATE INDEX idx_threads_created_at ON threads(created_at);
CREATE INDEX idx_threads_user_id_last_activity ON threads(user_id, last_activity_at DESC);

-- Conversations table indexes
CREATE INDEX idx_conversations_thread_id ON conversations(thread_id);
CREATE INDEX idx_conversations_created_at ON conversations(created_at);
CREATE INDEX idx_conversations_thread_id_created_at ON conversations(thread_id, created_at);

-- Feedback table indexes
CREATE INDEX idx_feedback_user_id ON feedback(user_id);
CREATE INDEX idx_feedback_conversation_id ON feedback(conversation_id);
CREATE INDEX idx_feedback_status ON feedback(status);
CREATE INDEX idx_feedback_created_at ON feedback(created_at);
CREATE INDEX idx_feedback_user_id_status ON feedback(user_id, status);

-- Activity logs table indexes
CREATE INDEX idx_activity_logs_user_id ON activity_logs(user_id);
CREATE INDEX idx_activity_logs_activity_type ON activity_logs(activity_type);
CREATE INDEX idx_activity_logs_created_at ON activity_logs(created_at);
CREATE INDEX idx_activity_logs_activity_type_created_at ON activity_logs(activity_type, created_at);

-- Create composite indexes for common query patterns
CREATE INDEX idx_threads_user_activity_composite ON threads(user_id, last_activity_at DESC, created_at DESC);
CREATE INDEX idx_conversations_thread_time_composite ON conversations(thread_id, created_at DESC);
CREATE INDEX idx_feedback_user_conversation_composite ON feedback(user_id, conversation_id);
CREATE INDEX idx_activity_logs_24h_analytics ON activity_logs(activity_type, created_at) WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '24 hours';

-- Add comments for documentation
COMMENT ON TABLE users IS 'Stores user account information with authentication details';
COMMENT ON TABLE threads IS 'Groups conversations by user with 30-minute rule for thread organization';
COMMENT ON TABLE conversations IS 'Stores individual question-answer pairs within threads';
COMMENT ON TABLE feedback IS 'Stores user feedback on conversations for system improvement';
COMMENT ON TABLE activity_logs IS 'Tracks user activities for analytics and monitoring';

COMMENT ON COLUMN users.role IS 'User role: MEMBER or ADMIN';
COMMENT ON COLUMN threads.last_activity_at IS 'Timestamp of last conversation in this thread';
COMMENT ON COLUMN conversations.is_streaming IS 'Whether the AI response was streamed';
COMMENT ON COLUMN feedback.is_positive IS 'True for positive feedback, false for negative';
COMMENT ON COLUMN feedback.status IS 'Feedback processing status: PENDING or RESOLVED';
COMMENT ON COLUMN activity_logs.activity_type IS 'Type of activity: USER_REGISTRATION, USER_LOGIN, or CONVERSATION_CREATED';