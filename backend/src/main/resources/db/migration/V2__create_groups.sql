CREATE TABLE groups (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    join_code VARCHAR(12) NOT NULL UNIQUE,
    created_by BIGINT NOT NULL REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE group_members (
    group_id BIGINT NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users (id),
    role VARCHAR(20) NOT NULL CHECK (role IN ('OWNER', 'MEMBER')),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (group_id, user_id)
);

-- Primarni ključ pokriva upite po group_id; "moje grupe" traži po user_id, pa treba poseban indeks
CREATE INDEX idx_group_members_user_id ON group_members (user_id);
