-- Add documents table for RAG functionality
-- Migration: V2__Add_documents_table.sql

-- Create documents table
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    content TEXT NOT NULL,
    size BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'processing',
    uploaded_by BIGINT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_documents_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_document_status CHECK (status IN ('processing', 'indexed', 'error'))
);

-- Create indexes for documents table
CREATE INDEX idx_documents_uploaded_by ON documents(uploaded_by);
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_uploaded_at ON documents(uploaded_at);
CREATE INDEX idx_documents_title ON documents(title);
CREATE INDEX idx_documents_user_status ON documents(uploaded_by, status);
CREATE INDEX idx_documents_user_uploaded_at ON documents(uploaded_by, uploaded_at DESC);

-- Add comments for documentation
COMMENT ON TABLE documents IS 'Stores uploaded documents for RAG (Retrieval-Augmented Generation) functionality';
COMMENT ON COLUMN documents.filename IS 'Original filename of the uploaded document';
COMMENT ON COLUMN documents.title IS 'Display title for the document';
COMMENT ON COLUMN documents.description IS 'Optional description of the document content';
COMMENT ON COLUMN documents.content IS 'Full text content of the document';
COMMENT ON COLUMN documents.size IS 'File size in bytes';
COMMENT ON COLUMN documents.status IS 'Processing status: processing, indexed, or error';
COMMENT ON COLUMN documents.uploaded_by IS 'User who uploaded the document';