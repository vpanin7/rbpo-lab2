CREATE TABLE app_users (
    id bigserial PRIMARY KEY,
    name text NOT NULL CHECK (length(trim(name)) > 0),
    email text NOT NULL CHECK (email ~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$'),
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX users_email_unique ON app_users (lower(email));
CREATE TABLE premises (
    id bigserial PRIMARY KEY,
    address text NOT NULL CHECK (length(trim(address)) > 0),
    area numeric NOT NULL CHECK (area > 0),
    owner_id bigint NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE meters (
    id bigserial PRIMARY KEY,
    premise_id bigint NOT NULL REFERENCES premises(id) ON DELETE RESTRICT,
    resource_type text NOT NULL CHECK (resource_type IN ('COLD_WATER','HOT_WATER','ELECTRICITY','GAS','HEATING')),
    serial_number text NOT NULL UNIQUE CHECK (length(trim(serial_number)) > 0),
    tariff numeric NOT NULL CHECK (tariff >= 0),
    status text NOT NULL CHECK (status IN ('ACTIVE','INACTIVE')),
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (id, premise_id)
);
CREATE TABLE meter_readings (
    id bigserial PRIMARY KEY,
    meter_id bigint NOT NULL REFERENCES meters(id) ON DELETE RESTRICT,
    period char(7) NOT NULL CHECK (period ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    value numeric NOT NULL CHECK (value >= 0),
    status text NOT NULL CHECK (status IN ('PENDING','VERIFIED','REJECTED')),
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (meter_id, period)
);
CREATE TABLE utility_charges (
    id bigserial PRIMARY KEY,
    premise_id bigint NOT NULL REFERENCES premises(id) ON DELETE RESTRICT,
    meter_id bigint NOT NULL,
    period char(7) NOT NULL CHECK (period ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    volume numeric NOT NULL CHECK (volume >= 0),
    amount numeric(18,2) NOT NULL CHECK (amount >= 0),
    status text NOT NULL CHECK (status IN ('UNPAID','PAID')),
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (meter_id, period),
    FOREIGN KEY (meter_id, premise_id) REFERENCES meters(id, premise_id) ON DELETE RESTRICT,
    FOREIGN KEY (meter_id, period) REFERENCES meter_readings(meter_id, period) ON DELETE RESTRICT
);
CREATE INDEX premises_owner_idx ON premises(owner_id);
CREATE INDEX meters_premise_idx ON meters(premise_id);
CREATE INDEX charges_premise_period_idx ON utility_charges(premise_id, period);
