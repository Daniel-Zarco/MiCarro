-- =============================================================
-- MiCarro - esquema Supabase
-- Ejecutar una vez en el SQL Editor de Supabase (dashboard).
-- Los usuarios viven en auth.users (Supabase Auth); aquí solo
-- tablas públicas con Row Level Security por usuario.
-- =============================================================

create extension if not exists pgcrypto;

-- ---------------------------------------------------------
-- Favoritos (product_id = externalId de Mercadona + snapshot)
-- ---------------------------------------------------------
create table if not exists public.favorites (
  user_id     uuid not null references auth.users (id) on delete cascade,
  product_id  text not null,
  product     jsonb not null,
  created_at  timestamptz not null default now(),
  primary key (user_id, product_id)
);

-- ---------------------------------------------------------
-- Planes guardados (desde el carrito)
-- ---------------------------------------------------------
create table if not exists public.saved_plans (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null references auth.users (id) on delete cascade,
  name       text not null,
  created_at timestamptz not null default now()
);

create table if not exists public.saved_plan_items (
  id           uuid primary key default gen_random_uuid(),
  plan_id      uuid not null references public.saved_plans (id) on delete cascade,
  product_id   text,
  product_name text,
  brand        text,
  format       text,
  image_url    text,
  quantity     int  not null,
  unit_price   numeric(12,2),
  subtotal     numeric(12,2)
);

-- ---------------------------------------------------------
-- Historial de compras
-- ---------------------------------------------------------
create table if not exists public.shopping_history (
  id              uuid primary key default gen_random_uuid(),
  user_id         uuid not null references auth.users (id) on delete cascade,
  budget          numeric(12,2) not null,
  estimated_total numeric(12,2) not null,
  mode            text not null check (mode in ('CHEAP','BALANCED','QUALITY')),
  created_at      timestamptz not null default now()
);

create table if not exists public.shopping_history_items (
  id           uuid primary key default gen_random_uuid(),
  history_id   uuid not null references public.shopping_history (id) on delete cascade,
  product_id   text,
  product_name text,
  brand        text,
  format       text,
  image_url    text,
  quantity     int,
  unit_price   numeric(12,2),
  subtotal     numeric(12,2)
);

-- ---------------------------------------------------------
-- Índices
-- ---------------------------------------------------------
create index if not exists ix_favorites_user
  on public.favorites (user_id);

create index if not exists ix_saved_plans_user
  on public.saved_plans (user_id, created_at desc);

create index if not exists ix_saved_plan_items_plan
  on public.saved_plan_items (plan_id);

create index if not exists ix_shopping_history_user
  on public.shopping_history (user_id, created_at desc);

create index if not exists ix_shopping_history_items_history
  on public.shopping_history_items (history_id);

-- ---------------------------------------------------------
-- Row Level Security
-- ---------------------------------------------------------
alter table public.favorites enable row level security;
alter table public.saved_plans enable row level security;
alter table public.saved_plan_items enable row level security;
alter table public.shopping_history enable row level security;
alter table public.shopping_history_items enable row level security;

drop policy if exists "favorites_own" on public.favorites;
create policy "favorites_own" on public.favorites
  for all using (user_id = auth.uid()) with check (user_id = auth.uid());

drop policy if exists "saved_plans_own" on public.saved_plans;
create policy "saved_plans_own" on public.saved_plans
  for all using (user_id = auth.uid()) with check (user_id = auth.uid());

drop policy if exists "saved_plan_items_own" on public.saved_plan_items;
create policy "saved_plan_items_own" on public.saved_plan_items
  for all
  using (
    exists (select 1 from public.saved_plans p
            where p.id = plan_id and p.user_id = auth.uid())
  )
  with check (
    exists (select 1 from public.saved_plans p
            where p.id = plan_id and p.user_id = auth.uid())
  );

drop policy if exists "shopping_history_own" on public.shopping_history;
create policy "shopping_history_own" on public.shopping_history
  for all using (user_id = auth.uid()) with check (user_id = auth.uid());

drop policy if exists "shopping_history_items_own" on public.shopping_history_items;
create policy "shopping_history_items_own" on public.shopping_history_items
  for all
  using (
    exists (select 1 from public.shopping_history h
            where h.id = history_id and h.user_id = auth.uid())
  )
  with check (
    exists (select 1 from public.shopping_history h
            where h.id = history_id and h.user_id = auth.uid())
  );
