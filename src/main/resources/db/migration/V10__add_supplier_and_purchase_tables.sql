-- Purchase entry: suppliers, purchase invoices, purchase line items,
-- and a per-batch cost on stock (used later for profit reporting).

CREATE TABLE supplier (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    name         VARCHAR(255) NOT NULL,
    dl1          VARCHAR(255) NULL,
    dl2          VARCHAR(255) NULL,
    gstin        VARCHAR(255) NULL,
    state        VARCHAR(100) NULL,
    phone_number VARCHAR(50)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_supplier_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_supplier_user (user_id)
);

CREATE TABLE purchase (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    user_id             BIGINT        NOT NULL,
    supplier_id         BIGINT        NULL,
    supplier_name       VARCHAR(255)  NOT NULL,
    supplier_gstin      VARCHAR(255)  NULL,
    supplier_invoice_no VARCHAR(100)  NOT NULL,
    invoice_date        DATE          NOT NULL,
    tax_mode            VARCHAR(20)   NOT NULL DEFAULT 'CGST_SGST',
    taxable_amount      DECIMAL(15,2) NOT NULL DEFAULT 0,
    cgst_amount         DECIMAL(15,2) NOT NULL DEFAULT 0,
    sgst_amount         DECIMAL(15,2) NOT NULL DEFAULT 0,
    igst_amount         DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_amount        DECIMAL(15,2) NOT NULL DEFAULT 0,
    reverted            TINYINT(1)    NOT NULL DEFAULT 0,
    created_at          DATETIME      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_purchase_user     FOREIGN KEY (user_id)     REFERENCES users(id),
    CONSTRAINT fk_purchase_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(id) ON DELETE SET NULL,
    INDEX idx_purchase_user_date (user_id, invoice_date),
    INDEX idx_purchase_user_invoice (user_id, supplier_invoice_no)
);

CREATE TABLE purchase_items (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    purchase_id           BIGINT        NOT NULL,
    product_id            BIGINT        NULL,
    stock_id              BIGINT        NULL,
    snapshot_product_name VARCHAR(512)  NULL,
    snapshot_packing      VARCHAR(255)  NULL,
    snapshot_hsn          VARCHAR(128)  NULL,
    snapshot_cgst         DECIMAL(10,2) NULL,
    snapshot_sgst         DECIMAL(10,2) NULL,
    batch_no              VARCHAR(100)  NOT NULL,
    expiry_date           DATE          NOT NULL,
    mrp                   DECIMAL(15,2) NULL,
    quantity              INT           NOT NULL,
    free_quantity         INT           NOT NULL DEFAULT 0,
    purchase_rate         DECIMAL(15,2) NOT NULL,
    discount_percent      DECIMAL(5,2)  NOT NULL DEFAULT 0,
    amount                DECIMAL(15,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_pitems_purchase FOREIGN KEY (purchase_id) REFERENCES purchase(id) ON DELETE CASCADE,
    CONSTRAINT fk_pitems_product  FOREIGN KEY (product_id)  REFERENCES product(id)  ON DELETE SET NULL,
    CONSTRAINT fk_pitems_stock    FOREIGN KEY (stock_id)    REFERENCES stock(id)    ON DELETE SET NULL,
    INDEX idx_pitems_purchase (purchase_id)
);

-- Effective cost per unit of a stock batch (amount / (qty + free)).
-- NULL for stock that was added manually before purchase entry existed.
ALTER TABLE stock ADD COLUMN cost_rate DECIMAL(15,4) NULL;