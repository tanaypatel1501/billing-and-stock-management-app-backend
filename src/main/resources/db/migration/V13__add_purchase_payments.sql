-- Payments against purchase invoices (what the shop owes suppliers).
-- Outstanding = purchase.total_amount - SUM(purchase_payment.amount). There is no paid flag on purchase.

CREATE TABLE purchase_payment (
                                  id           BIGINT        NOT NULL AUTO_INCREMENT,
                                  user_id      BIGINT        NOT NULL,
                                  purchase_id  BIGINT        NOT NULL,
                                  amount       DECIMAL(15,2) NOT NULL,
                                  payment_date DATE          NOT NULL,
                                  mode         VARCHAR(20)   NOT NULL DEFAULT 'CASH',
                                  reference    VARCHAR(100)  NULL,
                                  notes        VARCHAR(255)  NULL,
                                  created_at   DATETIME      NOT NULL,
                                  PRIMARY KEY (id),
                                  CONSTRAINT fk_purchase_payment_user     FOREIGN KEY (user_id)     REFERENCES users(id),
                                  CONSTRAINT fk_purchase_payment_purchase FOREIGN KEY (purchase_id) REFERENCES purchase(id) ON DELETE CASCADE,
                                  INDEX idx_purchase_payment_purchase (purchase_id),
                                  INDEX idx_purchase_payment_user_date (user_id, payment_date)
);

-- One-time backfill: purchases recorded before payment tracking are assumed paid in full
-- (most purchases are prepaid). The real payment date is unknown, so the invoice date is used.
-- To mark one as still owed, delete its payment in the app.
INSERT INTO purchase_payment (user_id, purchase_id, amount, payment_date, mode, notes, created_at)
SELECT p.user_id, p.id, p.total_amount, p.invoice_date, 'OTHER',
       'Recorded before payment tracking (assumed paid)', NOW()
FROM purchase p
WHERE p.reverted = 0
  AND p.total_amount IS NOT NULL
  AND p.total_amount > 0;