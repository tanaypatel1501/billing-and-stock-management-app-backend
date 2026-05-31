ALTER TABLE product_request
ADD COLUMN admin_notes VARCHAR(255) NULL AFTER sgst,
ADD COLUMN reviewed_at DATETIME(6) NULL AFTER packing;