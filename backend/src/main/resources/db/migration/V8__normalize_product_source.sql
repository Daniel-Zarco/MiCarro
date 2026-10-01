-- Normalización segura de source para productos del antiguo catálogo Mercadona.
--
-- Solo se normalizan productos con source IS NULL y active=true que sean el
-- MISMO producto que una contraparte MERCADONA activa (identidad estricta
-- name + format + category + brand, normalizada) y que la fila NULL sea la
-- histórica/anterior (id menor). Debe existir EXACTAMENTE 1 contraparte.
--
-- Solo cambia source a 'MERCADONA'. NO toca externalId, NO toca firstSeenAt,
-- NO fusiona, NO borra filas y NO reasigna ProductPriceHistory/SavedPlan/favoritos.
-- Tras el siguiente sync, el externalId antiguo de estas filas quedará
-- inactive de forma normal (no está en el set entrante del proveedor).

update products p
set source = 'MERCADONA'
from products m
where p.source is null
  and p.active = true
  and m.source = 'MERCADONA'
  and m.active = true
  and lower(m.name) = lower(p.name)
  and coalesce(lower(m.format), '') = coalesce(lower(p.format), '')
  and coalesce(lower(m.category), '') = coalesce(lower(p.category), '')
  and coalesce(lower(m.brand), '') = coalesce(lower(p.brand), '')
  and m.id > p.id
  and (
        select count(*)
        from products x
        where x.source = 'MERCADONA'
          and x.active = true
          and lower(x.name) = lower(p.name)
          and coalesce(lower(x.format), '') = coalesce(lower(p.format), '')
          and coalesce(lower(x.category), '') = coalesce(lower(p.category), '')
          and coalesce(lower(x.brand), '') = coalesce(lower(p.brand), '')
          and x.id > p.id
  ) = 1;