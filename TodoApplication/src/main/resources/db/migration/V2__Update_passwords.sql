-- Mise à jour des mots de passe vers les valeurs documentées dans le schéma initial
-- admin123 pour admin, user123 pour testuser

UPDATE users SET password = '$2a$10$sUZ2bxh4B/ZVKwcqPscF4.BKm4IFz/Gy8xf54YJtIbKGSN6m4nJ22' WHERE username = 'admin';
UPDATE users SET password = '$2a$10$rDX.Uh1ilL/p6f53WyC88.xkURaW7UaLWKOJ.v3yMBjbh2KDyeP2m' WHERE username = 'testuser';
