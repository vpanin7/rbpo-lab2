BEGIN;

CREATE TABLE IF NOT EXISTS public.user_accounts (
    user_id bigint PRIMARY KEY REFERENCES public.app_users(id) ON DELETE CASCADE,
    password_hash text NOT NULL,
    role text NOT NULL CHECK (role IN ('RESIDENT', 'ADMIN')),
    enabled boolean NOT NULL DEFAULT true
);

GRANT SELECT, INSERT, UPDATE (enabled) ON public.user_accounts TO utilities_app;

COMMIT;
