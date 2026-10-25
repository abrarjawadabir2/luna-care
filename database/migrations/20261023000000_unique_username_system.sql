-- LunaCare Unique Username System Migration
-- Version: 20261023000000_unique_username_system.sql
-- Classification:
--   PUBLIC: Public profile username lookup (/u/{username})
--   AUTHENTICATED: Profile username claim & update
--   PRIVATE USER DATA: profiles table extension

-------------------------------------------------------------------------------
-- 1. ADD USERNAME FIELDS TO PROFILES TABLE
-------------------------------------------------------------------------------
-- Fields:
--   username: Preserves user's approved casing (e.g. "AbrarJawad")
--   username_normalized: Lowercase NFKC-normalized form for uniqueness (e.g. "abrarjawad")
--   username_updated_at: Timestamp of last username change (for cooldown enforcement)
--   avatar: Optional public avatar URL
--   bio: Optional public biographical summary

alter table public.profiles
    add column if not exists username text,
    add column if not exists username_normalized text,
    add column if not exists username_updated_at timestamptz,
    add column if not exists avatar text,
    add column if not exists bio text;

-------------------------------------------------------------------------------
-- 2. UNIQUE CONSTRAINT & INDEX ON USERNAME_NORMALIZED
-- Database is the final authority. Enforces global uniqueness case-insensitively.
-- Does not create redundant second indexes.
-------------------------------------------------------------------------------
create unique index if not exists profiles_username_normalized_idx
    on public.profiles(username_normalized)
    where username_normalized is not null;

-------------------------------------------------------------------------------
-- 3. REASSIGNMENT PROTECTION TABLE (30-day anti-impersonation reservation)
-- Option B: reserve old username temporarily for original account to prevent impersonation.
-------------------------------------------------------------------------------
create table if not exists public.username_reservations (
    username_normalized text primary key,
    original_user_id uuid not null references public.profiles(id) on delete cascade,
    reserved_until timestamptz not null,
    created_at timestamptz not null default now()
);

alter table public.username_reservations enable row level security;

-- Only service role and original owner can view their reservation
create policy "username_reservations_select_own"
    on public.username_reservations for select
    using (auth.uid() = original_user_id or public.get_current_user_role() in ('ADMIN', 'SUPER_ADMIN'));

-------------------------------------------------------------------------------
-- 4. PUBLIC PROFILE VIEW WITH STRICT FIELD ALLOWLIST
-- Never expose private health data, internal IDs, email, or credentials
-------------------------------------------------------------------------------
create or replace view public.public_user_profiles as
    select
        username,
        display_name,
        avatar,
        bio as public_bio
    from public.profiles
    where username_normalized is not null;
