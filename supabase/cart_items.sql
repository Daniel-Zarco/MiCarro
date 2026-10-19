-- =============================================================
-- MiCarro - carrito sincronizado por usuario
-- Ejecutar una vez en Supabase Dashboard > SQL Editor.
--
-- Estrategia:
--  - Una fila por usuario + producto (UNIQUE), sin duplicados.
--  - user_id siempre = auth.uid() y protegido con RLS.
--  - Las cantidades se modifican con una función atómica para
--    que dos dispositivos no se pisen al sumar/restar.
--  - Borrado logico: al llegar a 0 la fila se conserva con
--    quantity = 0 y se excluye de las consultas del carrito.
--  - Realtime publica los cambios de esta tabla para que el
--    otro dispositivo reciba el aviso y recargue el carrito.
-- =============================================================

create extension if not exists pgcrypto;

-- ---------------------------------------------------------
-- Tabla del carrito
-- ---------------------------------------------------------
create table if not exists public.cart_items (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null references auth.users (id) on delete cascade,
  product_id  text not null,
  product     jsonb not null,
  quantity    integer not null,
  version     integer not null default 1,
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now(),
  constraint cart_items_user_product_key unique (user_id, product_id),
  constraint cart_items_quantity_check check (quantity >= 0)
);

-- Normaliza instalaciones creadas con la restriccion anterior (> 0).
alter table public.cart_items
  drop constraint if exists cart_items_quantity_check;

alter table public.cart_items
  add constraint cart_items_quantity_check check (quantity >= 0);

create index if not exists ix_cart_items_user
  on public.cart_items (user_id, updated_at desc);

-- ---------------------------------------------------------
-- Versionado de cada fila. El frontend no usa version/old_record:
-- solo usa el evento como aviso para recargar el carrito.
-- ---------------------------------------------------------
create or replace function public.cart_items_touch()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  new.version := old.version + 1;
  new.updated_at := now();
  return new;
end;
$$;

drop trigger if exists trg_cart_items_touch on public.cart_items;

create trigger trg_cart_items_touch
before update on public.cart_items
for each row execute function public.cart_items_touch();

-- ---------------------------------------------------------
-- Row Level Security
-- ---------------------------------------------------------
alter table public.cart_items enable row level security;

-- Realtime solo se usa como aviso: no se leen old_record ni version.
alter table public.cart_items replica identity default;

drop policy if exists "cart_items_own" on public.cart_items;

create policy "cart_items_own" on public.cart_items
  for all
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

-- ---------------------------------------------------------
-- Permisos de tabla para PostgREST.
-- La RLS sigue limitando cada fila a su usuario.
-- ---------------------------------------------------------
grant usage on schema public to authenticated;
grant select, insert, update, delete on public.cart_items to authenticated;
revoke all on public.cart_items from anon;

-- ---------------------------------------------------------
-- Cambio atómico de cantidad.
--
-- p_quantity_delta > 0: inserta o suma (ON CONFLICT).
-- p_quantity_delta < 0: resta; si llega a 0 o menos, deja la fila
-- con quantity = 0 (borrado logico).
-- Devuelve la fila resultante.
-- ---------------------------------------------------------
create or replace function public.cart_change_quantity(
  p_product_id text,
  p_product jsonb,
  p_quantity_delta integer
)
returns jsonb
language plpgsql
security invoker
set search_path = public
as $$
declare
  v_user_id uuid := auth.uid();
  v_row public.cart_items;
  v_quantity integer;
begin
  if v_user_id is null then
    raise exception 'Not authenticated' using errcode = '28000';
  end if;

  if p_product_id is null or p_product_id = '' then
    raise exception 'product_id is required' using errcode = '22023';
  end if;

  if p_quantity_delta is null or p_quantity_delta = 0 then
    select * into v_row
      from public.cart_items
     where user_id = v_user_id
       and product_id = p_product_id;

    if v_row.id is null then
      return null;
    end if;

    return to_jsonb(v_row);
  end if;

  if p_quantity_delta > 0 then

    /*
     * El INSERT ... ON CONFLICT DO UPDATE es atomico: si dos dispositivos
     * suman a la vez, PostgreSQL bloquea la fila y aplica ambas sumas.
     */
    insert into public.cart_items (user_id, product_id, product, quantity)
    values (
      v_user_id,
      p_product_id,
      coalesce(p_product, '{}'::jsonb),
      p_quantity_delta
    )
    on conflict (user_id, product_id) do update
      set quantity = public.cart_items.quantity + excluded.quantity,
          product = case
            when p_product is null then public.cart_items.product
            else p_product
          end
    returning * into v_row;

  else

    /*
     * Las restas se serializan con FOR UPDATE para que dos dispositivos no
     * lean la misma cantidad y se pisen. Si el resultado es <= 0 se guarda
     * quantity = 0 (borrado logico), conservando la fila para reutilizarla.
     */
    select * into v_row
      from public.cart_items
     where user_id = v_user_id
       and product_id = p_product_id
     for update;

    if v_row.id is null then
      return null;
    end if;

    v_quantity := greatest(v_row.quantity + p_quantity_delta, 0);

    update public.cart_items
       set quantity = v_quantity,
           product = case
             when p_product is null then product
             else p_product
           end
     where id = v_row.id
    returning * into v_row;

  end if;

  return to_jsonb(v_row);
end;
$$;

revoke all on function public.cart_change_quantity(text, jsonb, integer) from public;
revoke all on function public.cart_change_quantity(text, jsonb, integer) from anon;
grant execute on function public.cart_change_quantity(text, jsonb, integer) to authenticated;

-- ---------------------------------------------------------
-- Borrado logico de un producto.
-- Deja la fila con quantity = 0 para que el cambio se propague
-- como UPDATE por Realtime y se pueda reutilizar al volver a
-- anadir el mismo producto.
-- ---------------------------------------------------------
create or replace function public.cart_remove_item(
  p_product_id text
)
returns jsonb
language plpgsql
security invoker
set search_path = public
as $$
declare
  v_user_id uuid := auth.uid();
  v_row public.cart_items;
begin
  if v_user_id is null then
    raise exception 'Not authenticated' using errcode = '28000';
  end if;

  if p_product_id is null or p_product_id = '' then
    raise exception 'product_id is required' using errcode = '22023';
  end if;

  update public.cart_items
     set quantity = 0
   where user_id = v_user_id
     and product_id = p_product_id
     and quantity > 0
  returning * into v_row;

  if v_row.id is null then
    return null;
  end if;

  return to_jsonb(v_row);
end;
$$;

revoke all on function public.cart_remove_item(text) from public;
revoke all on function public.cart_remove_item(text) from anon;
grant execute on function public.cart_remove_item(text) to authenticated;

-- ---------------------------------------------------------
-- Realtime: añadir la tabla a la publicación si aún no está.
-- ---------------------------------------------------------
do $$
begin
  if exists (
    select 1
      from pg_publication
     where pubname = 'supabase_realtime'
  )
  and not exists (
    select 1
      from pg_publication_tables
     where pubname = 'supabase_realtime'
       and schemaname = 'public'
       and tablename = 'cart_items'
  ) then
    alter publication supabase_realtime add table public.cart_items;
  end if;
end $$;
