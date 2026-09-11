-- Sincronización profesional del catálogo.
-- Añade trazabilidad de origen/estado a products sin perder datos.
-- Los productos existentes se consideran de Mercadona y activos.

alter table products add column source varchar(255);
alter table products add column last_synced_at timestamp(6) with time zone;
alter table products add column active boolean not null default true;

update products set source = 'MERCADONA' where source is null;
