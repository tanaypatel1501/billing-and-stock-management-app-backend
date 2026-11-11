-- Cost per unit of the batch at the moment of sale (NULL when the cost was unknown).
-- Used for profit reporting. Old bills stay NULL; they are never guessed.
ALTER TABLE bill_items ADD COLUMN snapshot_cost DECIMAL(15,4) NULL;