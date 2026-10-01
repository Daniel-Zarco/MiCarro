-- Grupo visual derivado de Product.category mediante VisualGroupMapper.
-- Se materializa como columna (igual que main_category) para poder ordenar por
-- relevancia en el buscador. VisualGroupMapper sigue siendo la única fuente de
-- verdad; la columna es solo un resultado materializado que rellena el sync.

alter table products add column visual_group varchar(255);

create index ix_products_visual_group on products (visual_group);