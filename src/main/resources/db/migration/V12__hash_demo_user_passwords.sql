-- Mantem a senha demonstrativa "123456", mas nunca em texto puro no estado final do banco.
UPDATE users
SET password = '$2y$12$7rO4KYZU2fx0mZSmOLsUNOazx9oDYfycuGqHgaiEIoshQ/ETk4FY6'
WHERE password = '123456';
