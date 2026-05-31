CREATE TABLE product_request (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,
     name VARCHAR(255) NOT NULL,
     hsn VARCHAR(50),
     mrp DOUBLE,
     cgst DOUBLE,
     sgst DOUBLE,
     packing VARCHAR(100),
     notes TEXT,
     status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
     requested_by BIGINT NOT NULL,
     created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);