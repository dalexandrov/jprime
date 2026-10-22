-- The legacy Spring Boot application relied on "hibernate.ddl-auto=update" to create the tables that hold the
-- invoice number counters. The Quarkus rewrite does not let Hibernate touch the schema, so the tables are created
-- here. On an existing (production) database they already exist and these statements are no-ops.
CREATE TABLE IF NOT EXISTS EpayInvoiceNumberGenerator (
    id      INT    NOT NULL,
    counter BIGINT NOT NULL,
    PRIMARY KEY (id)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS RealInvoiceNumberGenerator (
    id      INT    NOT NULL,
    counter BIGINT NOT NULL,
    PRIMARY KEY (id)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS ProformaInvoiceNumberGenerator (
    id      INT    NOT NULL,
    counter BIGINT NOT NULL,
    PRIMARY KEY (id)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
