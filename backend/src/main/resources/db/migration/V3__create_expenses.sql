CREATE TABLE expenses (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    paid_by BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    category VARCHAR(30) NOT NULL CHECK (category IN ('FOOD', 'UTILITIES', 'RENT', 'TRANSPORT', 'ENTERTAINMENT', 'OTHER')),
    expense_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Platilac i autor moraju biti članovi upravo te grupe (provjerava baza, ne samo kod)
    FOREIGN KEY (group_id, paid_by) REFERENCES group_members (group_id, user_id),
    FOREIGN KEY (group_id, created_by) REFERENCES group_members (group_id, user_id)
);

-- Lista troškova grupe, sortirana po datumu i filtrirana po datumu
CREATE INDEX idx_expenses_group_date ON expenses (group_id, expense_date DESC, id DESC);

CREATE TABLE expense_shares (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    expense_id BIGINT NOT NULL REFERENCES expenses (id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users (id),
    share_amount NUMERIC(12, 2) NOT NULL CHECK (share_amount > 0),
    -- Isti korisnik ne može imati dva dijela istog troška
    UNIQUE (expense_id, user_id)
);

-- Balansi se računaju zbrajanjem dijelova po korisniku
CREATE INDEX idx_expense_shares_user_id ON expense_shares (user_id);

CREATE TABLE settlements (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    from_user BIGINT NOT NULL,
    to_user BIGINT NOT NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    settled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (from_user <> to_user),
    FOREIGN KEY (group_id, from_user) REFERENCES group_members (group_id, user_id),
    FOREIGN KEY (group_id, to_user) REFERENCES group_members (group_id, user_id)
);

CREATE INDEX idx_settlements_group_id ON settlements (group_id);
