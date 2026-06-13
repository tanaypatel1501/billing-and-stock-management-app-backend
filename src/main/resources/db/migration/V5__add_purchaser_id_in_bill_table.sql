ALTER TABLE bill ADD COLUMN purchaser_id BIGINT NULL;
ALTER TABLE bill ADD CONSTRAINT fk_bill_purchaser
    FOREIGN KEY (purchaser_id) REFERENCES purchaser(id) ON DELETE SET NULL;