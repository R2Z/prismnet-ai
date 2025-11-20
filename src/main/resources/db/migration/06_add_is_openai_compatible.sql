-- V6__add_is_openai_compatible.sql
-- Add is_openai_compatible column to provider table

USE prismnetai;

ALTER TABLE provider DROP KEY name;


-- Add is_openai_compatible column with default false
ALTER TABLE provider ADD COLUMN is_openai_compatible BOOLEAN NOT NULL DEFAULT FALSE;

-- Update existing OpenAI provider to have is_openai_compatible = false (already default)
-- No update needed as default is false, and official OpenAI should be false