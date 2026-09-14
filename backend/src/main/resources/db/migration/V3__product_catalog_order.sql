-- Orden natural del catálogo según el proveedor externo.
-- Permite mantener la secuencia original de sincronización en lugar del orden de inserción.

alter table products add column catalog_order integer;

-- Los productos existentes conservan un orden neutro al final hasta la próxima sincronización.
update products set catalog_order = 0 where catalog_order is null;
