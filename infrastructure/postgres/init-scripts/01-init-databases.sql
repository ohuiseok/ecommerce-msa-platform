-- 각 서비스별 데이터베이스 생성
CREATE DATABASE ecommerce_user;
CREATE DATABASE ecommerce_product;
CREATE DATABASE ecommerce_order;

-- 각 데이터베이스에 대한 권한 부여
GRANT ALL PRIVILEGES ON DATABASE ecommerce_user TO postgres;
GRANT ALL PRIVILEGES ON DATABASE ecommerce_product TO postgres;
GRANT ALL PRIVILEGES ON DATABASE ecommerce_order TO postgres;
