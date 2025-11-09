-- Create a new database
CREATE DATABASE prismnetai;

-- Create a new user with password
CREATE USER 'prismnetai_admin'@'%' IDENTIFIED BY 'p@ss_T1gerL32_';

-- Grant all privileges on the new database to the new user
GRANT ALL PRIVILEGES ON prismnetai.* TO 'prismnetai_admin'@'%';

-- Apply the changes
FLUSH PRIVILEGES;