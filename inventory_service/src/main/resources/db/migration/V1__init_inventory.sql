CREATE TABLE IF NOT EXISTS inventory (
  id BIGSERIAL PRIMARY KEY,
  product_id BIGINT NOT NULL UNIQUE,
  quantity INT NOT NULL CHECK (quantity >= 0),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
