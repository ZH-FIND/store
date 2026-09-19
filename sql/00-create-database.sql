CREATE DATABASE IF NOT EXISTS coffeeflow CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE coffeeflow;

CREATE USER IF NOT EXISTS 'coffeeflow'@'%' IDENTIFIED BY 'coffeeflow';
GRANT ALL PRIVILEGES ON coffeeflow.* TO 'coffeeflow'@'%';
FLUSH PRIVILEGES;
