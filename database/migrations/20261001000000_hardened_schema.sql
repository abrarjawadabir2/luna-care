-- LunaCare Hardened Database Schema & Row Level Security (RLS)
-- Version: 20261001000000_hardened_schema.sql
-- Classification:
--   PUBLIC: Reference articles, nearby facilities, version info
--   AUTHENTICATED: General user notifications, bookmarks
--   PRIVATE USER DATA: profiles, health_profiles, period_logs, behaviour_logs, medical_journal_entries, medical_reminders, cup_care_logs, ai_usage
--   INTERNAL: rate_limits, otp_stores, security_events
--   ADMIN ONLY: audit_logs (append-only)

create extension if not exists "uuid-ossp";

-------------------------------------------------------------------------------
-- 1. PROFILES TABLE (Private User Data)
-------------------------------------------------------------------------------
create table if not exists public.profiles (
    id uuid primary key references auth.users on delete cascade,
    display_name text not null,
    user_mode text not null check (user_mode in ('SELF_TRACKING', 'SUPPORT_MODE', 'EDUCATION_ONLY')),
    gender_mode text not null check (gender_mode in ('FEMALE', 'MALE', 'OTHER', 'PREFER_NOT_TO_SAY')),
    pronoun text not null check (pronoun in ('SHE_HER', 'HE_HIM', 'THEY_THEM', 'CUSTOM', 'PREFER_NOT_TO_SAY')),
    custom_pronoun text,
    body_relevant_mode text not null check (body_relevant_mode in ('MENSTRUATES', 'DOES_NOT_MENSTRUATE', 'NOT_SURE', 'PREFER_NOT_TO_SAY')),
    support_relationship text check (support_relationship in ('WIFE', 'MOTHER', 'DAUGHTER', 'GIRLFRIEND', 'FEMALE_PARTNER', 'SISTER', 'FRIEND', 'OTHER')),
    religion text check (religion in ('ISLAM', 'HINDU', 'CHRISTIAN', 'BUDDHIST', 'OTHER', 'PREFER_NOT_TO_SAY')),
    country text,
    region text,
    city text,
    location_privacy_mode text not null default 'OFF' check (location_privacy_mode in ('OFF', 'ON_DEVICE_ONLY', 'APPROXIMATE_REGION', 'TEMPORARY_EXACT')),
    last_location_permission_status text,
    consent_confirmed boolean not null default false,
    shared_tracking_consent boolean not null default false,
    behaviour_focuses text[] not null default '{}',
    average_cycle_length integer not null default 28 check (average_cycle_length between 15 and 60),
    average_period_length integer not null default 5 check (average_period_length between 1 and 20),
    visible_status boolean not null default true,
    dashboard_layout_version text not null default 'new',
    role text not null default 'USER' check (role in ('USER', 'PREMIUM_USER', 'MODERATOR', 'MEDICAL_CONTENT_REVIEWER', 'SUPPORT_AGENT', 'ADMIN', 'SUPER_ADMIN')),
    is_premium boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-------------------------------------------------------------------------------
-- 2. PERIOD LOGS (Private User Data)
-------------------------------------------------------------------------------
create table if not exists public.period_logs (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles(id) on delete cascade,
    start_date date not null,
    end_date date,
    flow_level text check (flow_level in ('Spotting', 'Light', 'Medium', 'Heavy')),
    symptoms text[] not null default '{}',
    notes_encrypted text, -- AES-256 client-encrypted only
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-------------------------------------------------------------------------------
-- 3. BEHAVIOUR LOGS (Private User Data)
-------------------------------------------------------------------------------
create table if not exists public.behaviour_logs (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles(id) on delete cascade,
    log_date date not null,
    mood text not null,
    stress_level integer check (stress_level >= 0 and stress_level <= 10),
    anxiety_level integer check (anxiety_level >= 0 and anxiety_level <= 10),
    sleep_hours numeric(4,2) check (sleep_hours >= 0 and sleep_hours <= 24),
    sleep_quality integer check (sleep_quality >= 0 and sleep_quality <= 10),
    pain_level integer check (pain_level >= 0 and pain_level <= 10),
    energy_level integer check (energy_level >= 0 and energy_level <= 10),
    hydration_level text check (hydration_level in ('Low', 'Okay', 'Good', 'Great')),
    flow_level text,
    symptoms text[] not null default '{}',
    notes_encrypted text,
    crisis_flag boolean not null default false,
    flags text[] not null default '{}',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique(user_id, log_date)
);

-------------------------------------------------------------------------------
-- 4. MEDICAL JOURNAL ENTRIES (Private User Data)
-------------------------------------------------------------------------------
create table if not exists public.medical_journal_entries (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles(id) on delete cascade,
    entry_date date not null,
    category text not null,
    title text not null,
    symptoms text[] not null default '{}',
    pain_level integer check (pain_level >= 0 and pain_level <= 10),
    mood text,
    flow_level text,
    medicines_taken text,
    doctor_visit boolean not null default false,
    next_appointment date,
    notes_encrypted text,
    attachment_path text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-------------------------------------------------------------------------------
-- 5. AUDIT LOGS (Internal / Admin Only - Append-Only)
-------------------------------------------------------------------------------
create table if not exists public.audit_logs (
    id uuid primary key default gen_random_uuid(),
    actor_id uuid,
    actor_role text not null default 'USER',
    action text not null,
    resource_type text not null,
    resource_id text,
    ip_hash text,
    user_agent_hash text,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now()
);

-------------------------------------------------------------------------------
-- ROW LEVEL SECURITY (RLS) POLICIES
-- Default: Deny all access unless explicitly granted
-------------------------------------------------------------------------------

alter table public.profiles enable row level security;
alter table public.period_logs enable row level security;
alter table public.behaviour_logs enable row level security;
alter table public.medical_journal_entries enable row level security;
alter table public.audit_logs enable row level security;

-- Function: get current user role safely
create or replace function public.get_current_user_role()
returns text as $$
    select coalesce(
        (select role from public.profiles where id = auth.uid()),
        'USER'
    );
$$ language sql security definer;

-- Trigger: Prevent privilege escalation on profiles
-- Users CANNOT change their own role or is_premium fields
create or replace function public.prevent_profile_privilege_escalation()
returns trigger as $$
begin
    if (new.role is distinct from old.role or new.is_premium is distinct from old.is_premium) then
        if (public.get_current_user_role() not in ('ADMIN', 'SUPER_ADMIN')) then
            raise exception 'Forbidden: Cannot alter administrative role or subscription entitlement.';
        end if;
    end if;
    return new;
end;
$$ language plpgsql security definer;

drop trigger if exists trg_prevent_profile_privilege_escalation on public.profiles;
create trigger trg_prevent_profile_privilege_escalation
    before update on public.profiles
    for each row execute function public.prevent_profile_privilege_escalation();

-- Trigger: Audit logs are append-only. UPDATE and DELETE are strictly forbidden.
create or replace function public.enforce_audit_immutability()
returns trigger as $$
begin
    raise exception 'Audit log records are immutable and cannot be updated or deleted.';
end;
$$ language plpgsql;

drop trigger if exists trg_enforce_audit_immutability on public.audit_logs;
create trigger trg_enforce_audit_immutability
    before update or delete on public.audit_logs
    for each row execute function public.enforce_audit_immutability();

-- 1. Profiles RLS
create policy "profiles_select_own"
    on public.profiles for select
    using (auth.uid() = id or public.get_current_user_role() in ('ADMIN', 'SUPER_ADMIN'));

create policy "profiles_update_own"
    on public.profiles for update
    using (auth.uid() = id)
    with check (auth.uid() = id);

-- 2. Period Logs RLS (Strict user isolation: no admin bypass on medical records)
create policy "period_logs_select_own"
    on public.period_logs for select
    using (auth.uid() = user_id);

create policy "period_logs_insert_own"
    on public.period_logs for insert
    with check (auth.uid() = user_id);

create policy "period_logs_update_own"
    on public.period_logs for update
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

create policy "period_logs_delete_own"
    on public.period_logs for delete
    using (auth.uid() = user_id);

-- 3. Behaviour Logs RLS
create policy "behaviour_logs_select_own"
    on public.behaviour_logs for select
    using (auth.uid() = user_id);

create policy "behaviour_logs_insert_own"
    on public.behaviour_logs for insert
    with check (auth.uid() = user_id);

create policy "behaviour_logs_update_own"
    on public.behaviour_logs for update
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

create policy "behaviour_logs_delete_own"
    on public.behaviour_logs for delete
    using (auth.uid() = user_id);

-- 4. Medical Journal Entries RLS
create policy "medical_journal_select_own"
    on public.medical_journal_entries for select
    using (auth.uid() = user_id);

create policy "medical_journal_insert_own"
    on public.medical_journal_entries for insert
    with check (auth.uid() = user_id);

create policy "medical_journal_update_own"
    on public.medical_journal_entries for update
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

create policy "medical_journal_delete_own"
    on public.medical_journal_entries for delete
    using (auth.uid() = user_id);

-- 5. Audit Logs RLS (Insert allowed from authenticated service; Select restricted to ADMIN only)
create policy "audit_logs_select_admin_only"
    on public.audit_logs for select
    using (public.get_current_user_role() in ('ADMIN', 'SUPER_ADMIN'));

create policy "audit_logs_insert_service"
    on public.audit_logs for insert
    with check (true);
