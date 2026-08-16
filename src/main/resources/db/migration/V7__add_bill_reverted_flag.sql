-- Bill revert feature: marks a bill as reverted instead of deleting it,
-- preserving audit trail (StockLog bill-links) while excluding it from
-- normal bill lists, sales aggregates, and inventory calculations.
ALTER TABLE bill
    ADD COLUMN reverted TINYINT(1) NOT NULL DEFAULT 0;

-- All existing bills predate this feature — none of them are reverted
UPDATE bill SET reverted = 0;

-- Index to keep the "exclude reverted" filter cheap on the bills list,
-- sales aggregate queries, and getAllBillsByUser
CREATE INDEX idx_bill_reverted ON bill(reverted);