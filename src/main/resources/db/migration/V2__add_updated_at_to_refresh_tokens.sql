-- Add updated_at column to refresh_tokens table
-- This addresses the Hibernate validation issue where the entity extends BaseEntity 
-- which requires both created_at and updated_at columns

ALTER TABLE refresh_tokens 
ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);