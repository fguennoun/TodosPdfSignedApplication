-- Migration pour ajouter external_id à la table todos
-- Cela permet de synchroniser avec JSONPlaceholder sans conflits d'ID primaires

ALTER TABLE todos ADD COLUMN external_id BIGINT;
CREATE UNIQUE INDEX idx_todos_external_id ON todos(external_id);
