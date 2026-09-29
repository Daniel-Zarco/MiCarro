-- Capa derivada de categorías principales (agrupación de subcategorías).
-- Las categorías originales (category) se mantienen intactas; main_category
-- la rellena la sincronización mediante el MainCategoryMapper.

alter table products add column main_category varchar(255);

create index ix_products_main_category on products (main_category);