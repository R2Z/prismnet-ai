-- V3__alter_routing_strategy_column.sql
-- Alter routing_strategy column to accommodate longer values
USE prismnetai;

ALTER TABLE ai_request MODIFY COLUMN routing_strategy VARCHAR(50) NOT NULL DEFAULT 'PRICE';