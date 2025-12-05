-- Flyway migration: add snapshot columns to bill_items
-- Adds snapshot_product_name, snapshot_unit_price and additional snapshot columns to preserve historical product data in bill items
-- Use IF NOT EXISTS checks for safety on MySQL-compatible databases

ALTER TABLE `bill_items`
  ADD COLUMN IF NOT EXISTS `snapshot_product_name` VARCHAR(512) NULL COMMENT 'Product name snapshot at time of billing',
  ADD COLUMN IF NOT EXISTS `snapshot_unit_price` DECIMAL(15,2) NULL COMMENT 'Unit price snapshot at time of billing',
  ADD COLUMN IF NOT EXISTS `snapshot_packing` VARCHAR(255) NULL COMMENT 'Packing snapshot at time of billing',
  ADD COLUMN IF NOT EXISTS `snapshot_hsn` VARCHAR(128) NULL COMMENT 'HSN snapshot at time of billing',
  ADD COLUMN IF NOT EXISTS `snapshot_cgst` DECIMAL(10,2) NULL COMMENT 'CGST snapshot at time of billing',
  ADD COLUMN IF NOT EXISTS `snapshot_sgst` DECIMAL(10,2) NULL COMMENT 'SGST snapshot at time of billing';

-- Optional: populate snapshot columns for existing rows from the linked product table where available
-- This is a best-effort population; it will not change rows where product_id is null

UPDATE `bill_items` bi
JOIN `product` p ON bi.product_id = p.id
SET bi.snapshot_product_name = COALESCE(bi.snapshot_product_name, p.name),
    bi.snapshot_unit_price = COALESCE(bi.snapshot_unit_price, p.MRP),
    bi.snapshot_packing = COALESCE(bi.snapshot_packing, p.packing),
    bi.snapshot_hsn = COALESCE(bi.snapshot_hsn, p.HSN),
    bi.snapshot_cgst = COALESCE(bi.snapshot_cgst, p.CGST),
    bi.snapshot_sgst = COALESCE(bi.snapshot_sgst, p.SGST)
WHERE bi.product_id IS NOT NULL;
