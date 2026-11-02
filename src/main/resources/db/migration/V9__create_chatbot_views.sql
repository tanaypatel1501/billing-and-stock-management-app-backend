-- Rename to your next Flyway version, e.g. V8__chatbot_views.sql
--
-- The chatbot only ever sees these views. Each view filters itself to ONE user
-- via chatbot_uid(), which reads a session variable that only the backend sets
-- (from the JWT). The LLM-written SQL can't remove that filter.
-- If the variable is NULL, every view returns zero rows (fails closed).

CREATE FUNCTION chatbot_uid() RETURNS BIGINT DETERMINISTIC NO SQL
    RETURN @chatbot_uid;

CREATE VIEW v_bills AS
SELECT b.id            AS bill_id,
       b.invoice_date  AS invoice_date,
       b.purchaser_name AS purchaser_name,
       b.total_amount  AS total_amount,
       b.paid          AS paid
FROM bill b
WHERE b.user_id = chatbot_uid()
  AND b.reverted = 0;

CREATE VIEW v_bill_items AS
SELECT bi.bill_id               AS bill_id,
       b.invoice_date           AS invoice_date,
       b.purchaser_name         AS purchaser_name,
       b.paid                   AS paid,
       bi.snapshot_product_name AS product_name,
       bi.snapshot_packing      AS packing,
       bi.batch_no              AS batch_no,
       bi.quantity              AS quantity,
       bi.`free`                AS free_quantity,
       bi.rate                  AS rate,
       bi.amount                AS amount,
       bi.expiry_date           AS expiry_date
FROM bill_items bi
         JOIN bill b ON b.id = bi.bill_id
WHERE b.user_id = chatbot_uid()
  AND b.reverted = 0;

CREATE VIEW v_stock AS
SELECT s.id          AS stock_id,
       p.name        AS product_name,
       p.packing     AS packing,
       s.batch_no    AS batch_no,
       s.quantity    AS quantity,
       s.expiry_date AS expiry_date,
       s.mrp         AS mrp
FROM stock s
         JOIN product p ON p.id = s.product_id
WHERE s.user_id = chatbot_uid()
  AND s.quantity > 0;

CREATE VIEW v_purchasers AS
SELECT pu.id   AS purchaser_id,
       pu.name AS name
FROM purchaser pu
WHERE pu.user_id = chatbot_uid();