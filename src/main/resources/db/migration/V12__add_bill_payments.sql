-- Partial payments for sales bills. Outstanding = bill.total_amount - SUM(bill_payment.amount).
-- bill.paid stays and is kept in sync by the service (paid = fully covered),
-- so the existing paidOnly filters and the chatbot's v_bills keep working.

CREATE TABLE bill_payment (
                              id           BIGINT        NOT NULL AUTO_INCREMENT,
                              user_id      BIGINT        NOT NULL,
                              bill_id      BIGINT        NOT NULL,
                              amount       DECIMAL(15,2) NOT NULL,
                              payment_date DATE          NOT NULL,
                              mode         VARCHAR(20)   NOT NULL DEFAULT 'CASH',
                              reference    VARCHAR(100)  NULL,
                              notes        VARCHAR(255)  NULL,
                              created_at   DATETIME      NOT NULL,
                              PRIMARY KEY (id),
                              CONSTRAINT fk_bill_payment_user FOREIGN KEY (user_id) REFERENCES users(id),
                              CONSTRAINT fk_bill_payment_bill FOREIGN KEY (bill_id) REFERENCES bill(id) ON DELETE CASCADE,
                              INDEX idx_bill_payment_bill (bill_id),
                              INDEX idx_bill_payment_user_date (user_id, payment_date)
);

-- One-time backfill: bills already marked paid get a single payment for the full total.
-- The real payment date is unknown, so the invoice date is used (approximation, noted below).
INSERT INTO bill_payment (user_id, bill_id, amount, payment_date, mode, notes, created_at)
SELECT b.user_id, b.id, b.total_amount, COALESCE(b.invoice_date, CURDATE()), 'OTHER',
       'Marked paid before payment tracking', NOW()
FROM bill b
WHERE b.paid = 1
  AND b.reverted = 0
  AND b.total_amount IS NOT NULL
  AND b.total_amount > 0;