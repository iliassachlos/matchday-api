-- Add buyer_email column to orders table

ALTER TABLE orders ADD COLUMN buyer_email VARCHAR(255);
