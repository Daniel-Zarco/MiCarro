import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';

import { Category } from '../models/category';
import { Group } from '../models/group';
import { NovedadesSection, ProductPriceChange } from '../models/product-price-change';
import { Product } from '../models/product';
import { PageResponse } from '../models/page-response';
import { CatalogService } from './catalog.service';

const FALLBACK_GROUP = 'Otros';
const NEW_WINDOW_DAYS = 30;
const PRICE_WINDOW_DAYS = 30;
const RECENT_WINDOW_DAYS = 180;

/*
 * Catálogo en memoria: replica la lógica de consulta que antes vivía en Spring
 * Boot (búsqueda por tokens con relevancia 0-10, filtros, ordenación, paginación
 * y novedades) sobre los productos cargados desde catalog.json.
 */
@Injectable({
  providedIn: 'root'
})
export class ProductService {

  private readonly catalog = inject(CatalogService);

  loadCatalog(): Observable<void> {
    return this.catalog.load();
  }

  getProducts(
    page = 0,
    size = 24,
    search = '',
    sortBy = 'catalog',
    category = '',
    group = ''
  ): Observable<PageResponse<Product>> {

    return of(this.query(page, size, search, sortBy, category, group));
  }

  getCategories(): Observable<Category[]> {
    return of(this.categories());
  }

  getCategoryGroups(mainCategory: string): Observable<Group[]> {
    return of(this.groups(mainCategory));
  }

  getRecentProducts(
    page = 0,
    size = 24
  ): Observable<PageResponse<Product>> {

    return of(this.recent(page, size));
  }

  getNovedades(
    section: NovedadesSection,
    page = 0,
    size = 24
  ): Observable<PageResponse<ProductPriceChange>> {

    return of(this.novedades(section, page, size));
  }

  private query(
    page: number,
    size: number,
    search: string,
    sortBy: string,
    category: string,
    group: string
  ): PageResponse<Product> {

    const active = this.catalog.activeProducts();

    const hasSearch = !!search && search.trim().length > 0;
    const hasCategory = !!category && category.trim().length > 0;
    const hasGroup = !!group && group.trim().length > 0;

    let result: Product[] = [];

    if (hasCategory && hasGroup) {

      const categoryNames = this.groupCategories(category, group);

      if (categoryNames.length === 0) {
        result = [];
      } else {
        const categorySet = new Set(categoryNames.map(name => name.toLowerCase()));
        const main = category.trim().toLowerCase();
        const term = search.trim().toLowerCase();

        result = active.filter(product =>
          (product.mainCategory ?? '').toLowerCase() === main &&
          categorySet.has((product.category ?? '').toLowerCase()) &&
          (!hasSearch || (product.name ?? '').toLowerCase().includes(term))
        );
      }

    } else if (hasSearch && hasCategory) {

      const main = category.trim().toLowerCase();
      const term = search.trim().toLowerCase();

      result = active.filter(product =>
        (product.mainCategory ?? '').toLowerCase() === main &&
        (product.name ?? '').toLowerCase().includes(term)
      );

    } else if (hasCategory) {

      const main = category.trim().toLowerCase();

      result = active.filter(product =>
        (product.mainCategory ?? '').toLowerCase() === main
      );

    } else if (hasSearch) {

      return paginate(
        searchProducts(search, sortBy, active),
        page,
        size
      );

    } else {

      result = [...active];
    }

    result.sort(sortComparator(sortBy ?? 'catalog'));

    return paginate(result, page, size);
  }

  private categories(): Category[] {

    const counts = new Map<string, number>();

    for (const product of this.catalog.activeProducts()) {
      const main = product.mainCategory;
      if (main) {
        counts.set(main, (counts.get(main) ?? 0) + 1);
      }
    }

    return [...counts.entries()]
      .map(([name, productCount]) => ({ name, productCount }))
      .sort(compareCategoryName);
  }

  private groups(mainCategory: string): Group[] {

    const main = mainCategory.trim().toLowerCase();
    const inMain = this.catalog.activeProducts()
      .filter(product => (product.mainCategory ?? '').toLowerCase() === main);

    const byGroup = new Map<string, { order: number; count: number }>();

    for (const product of inMain) {
      const group = product.visualGroup ?? FALLBACK_GROUP;
      const existing = byGroup.get(group);

      if (existing) {
        existing.count += 1;
        if (product.catalogOrder < existing.order) {
          existing.order = product.catalogOrder;
        }
      } else {
        byGroup.set(group, { order: product.catalogOrder, count: 1 });
      }
    }

    return [...byGroup.entries()]
      .map(([name, value]) => ({ name, productCount: value.count, order: value.order }))
      .sort((a, b) =>
        (a.name === FALLBACK_GROUP ? 1 : 0) - (b.name === FALLBACK_GROUP ? 1 : 0)
        || a.order - b.order
      )
      .map(({ name, productCount }) => ({ name, productCount }));
  }

  private groupCategories(mainCategory: string, group: string): string[] {

    const main = mainCategory.trim().toLowerCase();
    const isFallback = group.trim() === FALLBACK_GROUP;

    const categories = new Set<string>();

    for (const product of this.catalog.activeProducts()) {

      if ((product.mainCategory ?? '').toLowerCase() !== main) {
        continue;
      }

      const productGroup = product.visualGroup ?? null;

      const matchesGroup = isFallback
        ? (productGroup == null || productGroup === '')
        : productGroup === group;

      if (matchesGroup && product.category) {
        categories.add(product.category);
      }
    }

    return [...categories];
  }

  private recent(page: number, size: number): PageResponse<Product> {

    const cutoff = Date.now() - RECENT_WINDOW_DAYS * 24 * 3600 * 1000;

    const items = this.catalog.activeProducts()
      .filter(product =>
        product.firstSeenAt && new Date(product.firstSeenAt).getTime() >= cutoff)
      .sort((a, b) => firstSeenDesc(a, b));

    return paginate(items, page, size);
  }

  private novedades(
    section: NovedadesSection,
    page: number,
    size: number
  ): PageResponse<ProductPriceChange> {

    const active = this.catalog.activeProducts();
    const all = this.catalog.allProducts();
    const now = Date.now();

    if (section === 'new') {

      const cutoff = now - NEW_WINDOW_DAYS * 24 * 3600 * 1000;

      const items: ProductPriceChange[] = active
        .filter(product =>
          product.firstSeenAt && new Date(product.firstSeenAt).getTime() >= cutoff)
        .filter(product => !hasPriorEquivalent(product, all))
        .sort((a, b) => firstSeenDesc(a, b))
        .map(product => ({
          ...product,
          currentPrice: product.price,
          previousPrice: null,
          difference: null,
          differencePercent: null
        }));

      return paginate(items, page, size);
    }

    const cutoff = now - PRICE_WINDOW_DAYS * 24 * 3600 * 1000;
    const drops = section === 'price-drops';

    const changed: ProductPriceChange[] = [];

    for (const product of active) {

      if (product.price == null) {
        continue;
      }

      const previous = priceAsOf(product, cutoff);

      if (previous == null) {
        continue;
      }

      if (drops ? product.price >= previous : product.price <= previous) {
        continue;
      }

      const difference = round2(product.price - previous);
      const differencePercent = previous === 0
        ? null
        : round2((difference / previous) * 100);

      changed.push({
        ...product,
        currentPrice: product.price,
        previousPrice: previous,
        difference,
        differencePercent
      });
    }

    changed.sort((a, b) => {
      const pa = a.differencePercent ?? 0;
      const pb = b.differencePercent ?? 0;
      return drops ? pa - pb : pb - pa;
    });

    return paginate(changed, page, size);
  }
}

/*
 * Búsqueda por tokens con tolerancia singular/plural, replicando la relevancia
 * 0-10 del backend (mainCategory exacta > visualGroup exacto > category exacta >
 * tokens en visualGroup/category > tokens en name > tokens en mainCategory).
 */
function searchProducts(
  search: string,
  sortBy: string,
  products: Product[]
): Product[] {

  const normalized = normalize(search);
  const tokens = tokenize(normalized);

  if (tokens.length === 0) {
    return [];
  }

  const regexes = tokens.map(tokenRegex);
  const exact = normalized;

  const filtered = products.filter(product => {

    const main = normalize(product.mainCategory ?? '');

    return matchesAll(product.name, regexes)
      || matchesAll(product.visualGroup, regexes)
      || matchesAll(product.category, regexes)
      || main === exact;
  });

  if (sortBy && sortBy !== 'catalog') {
    return [...filtered].sort(sortComparator(sortBy));
  }

  return filtered
    .map(product => ({ product, rank: relevanceRank(product, exact, regexes) }))
    .sort((a, b) =>
      a.rank - b.rank
      || compareCatalogOrder(a.product, b.product)
      || compareId(a.product.id, b.product.id))
    .map(entry => entry.product);
}

function relevanceRank(product: Product, exact: string, regexes: RegExp[]): number {

  const main = normalize(product.mainCategory ?? '');
  const vg = normalize(product.visualGroup ?? '');
  const cat = normalize(product.category ?? '');

  const nameMatches = matchesAll(product.name, regexes);
  const vgCatMatches = matchesAll(
    `${product.visualGroup ?? ''} ${product.category ?? ''}`,
    regexes
  );
  const mainMatches = matchesAll(product.mainCategory, regexes);

  if (main === exact && nameMatches) return 0;
  if (main === exact) return 1;
  if (vg === exact && nameMatches) return 2;
  if (vg === exact) return 3;
  if (cat === exact && nameMatches) return 4;
  if (cat === exact) return 5;
  if (vgCatMatches && nameMatches) return 6;
  if (vgCatMatches) return 7;
  if (nameMatches) return 8;
  if (mainMatches) return 9;
  return 10;
}

function matchesAll(text: string | null, regexes: RegExp[]): boolean {

  if (!text) {
    return false;
  }

  const normalized = normalize(text);

  return regexes.every(regex => regex.test(normalized));
}

function normalize(value: string): string {

  return (value ?? '')
    .toLowerCase()
    .trim()
    .replace(/á/g, 'a')
    .replace(/é/g, 'e')
    .replace(/í/g, 'i')
    .replace(/ó/g, 'o')
    .replace(/ú/g, 'u')
    .replace(/ü/g, 'u')
    .replace(/ñ/g, 'n');
}

function tokenize(normalized: string): string[] {
  return normalized.split(/[^a-z0-9]+/).filter(token => token.length > 0);
}

function tokenRegex(token: string): RegExp {

  let base = token;

  if (token.endsWith('es') && token.length > 3) {
    base = token.slice(0, -2);
  } else if (token.endsWith('s') && token.length > 2) {
    base = token.slice(0, -1);
  }

  if (base === token) {
    return new RegExp(`\\b${token}(s|es)?\\b`);
  }

  return new RegExp(`\\b(?:${token}|${base})(s|es)?\\b`);
}

function sortComparator(sortBy: string): (a: Product, b: Product) => number {

  switch (sortBy) {
    case 'price-asc':
      return (a, b) =>
        comparePriceAsc(a.price, b.price)
        || compareCatalogOrder(a, b)
        || compareId(a.id, b.id);
    case 'price-desc':
      return (a, b) =>
        comparePriceDesc(a.price, b.price)
        || compareCatalogOrder(a, b)
        || compareId(a.id, b.id);
    case 'name':
      return (a, b) =>
        compareName(a, b)
        || compareCatalogOrder(a, b)
        || compareId(a.id, b.id);
    case 'name-desc':
      return (a, b) =>
        compareNameDesc(a, b)
        || compareCatalogOrder(a, b)
        || compareId(a.id, b.id);
    default:
      return (a, b) =>
        compareCatalogOrder(a, b)
        || compareId(a.id, b.id);
  }
}

function compareCatalogOrder(a: Product, b: Product): number {
  return a.catalogOrder - b.catalogOrder;
}

function comparePriceAsc(a: number | null, b: number | null): number {
  if (a == null && b == null) return 0;
  if (a == null) return 1;
  if (b == null) return -1;
  return a - b;
}

function comparePriceDesc(a: number | null, b: number | null): number {
  if (a == null && b == null) return 0;
  if (a == null) return 1;
  if (b == null) return -1;
  return b - a;
}

function compareName(a: Product, b: Product): number {
  return (a.name ?? '').toLowerCase() < (b.name ?? '').toLowerCase() ? -1 : 1;
}

function compareNameDesc(a: Product, b: Product): number {
  return (a.name ?? '').toLowerCase() > (b.name ?? '').toLowerCase() ? -1 : 1;
}

function compareId(a: string, b: string): number {
  const na = Number(a);
  const nb = Number(b);

  if (Number.isFinite(na) && Number.isFinite(nb)) {
    return na - nb;
  }

  return a < b ? -1 : a > b ? 1 : 0;
}

function compareCategoryName(a: Category, b: Category): number {
  const na = a.name.toLowerCase();
  const nb = b.name.toLowerCase();
  return na < nb ? -1 : na > nb ? 1 : 0;
}

function firstSeenDesc(a: Product, b: Product): number {
  const ta = a.firstSeenAt ? new Date(a.firstSeenAt).getTime() : 0;
  const tb = b.firstSeenAt ? new Date(b.firstSeenAt).getTime() : 0;
  return tb - ta || compareId(a.id, b.id);
}

function priceAsOf(product: Product, cutoffMs: number): number | null {

  let best: { time: number; price: number | null } | null = null;

  for (const point of product.priceHistory ?? []) {

    if (!point.recordedAt) {
      continue;
    }

    const time = new Date(point.recordedAt).getTime();

    if (time > cutoffMs) {
      continue;
    }

    if (!best || time > best.time) {
      best = { time, price: point.price };
    }
  }

  return best?.price ?? null;
}

function hasPriorEquivalent(product: Product, all: Product[]): boolean {

  const name = (product.name ?? '').toLowerCase();
  const format = (product.format ?? '').toLowerCase();
  const category = (product.category ?? '').toLowerCase();
  const brand = (product.brand ?? '').toLowerCase();

  const first = product.firstSeenAt ? new Date(product.firstSeenAt).getTime() : null;

  return all.some(prior => {

    if (prior.id === product.id) {
      return false;
    }

    if ((prior.name ?? '').toLowerCase() !== name) return false;
    if ((prior.format ?? '').toLowerCase() !== format) return false;
    if ((prior.category ?? '').toLowerCase() !== category) return false;
    if ((prior.brand ?? '').toLowerCase() !== brand) return false;

    const priorFirst = prior.firstSeenAt ? new Date(prior.firstSeenAt).getTime() : null;

    if (priorFirst != null && first != null) {
      return priorFirst < first;
    }

    if (priorFirst == null && first != null) {
      return compareId(prior.id, product.id) < 0;
    }

    return false;
  });
}

function paginate<T>(items: T[], page: number, size: number): PageResponse<T> {

  const totalElements = items.length;
  const totalPages = totalElements === 0
    ? 0
    : Math.ceil(totalElements / size);

  const current = Math.max(0, page);
  const start = current * size;

  return {
    content: items.slice(start, start + size),
    page: current,
    size,
    totalElements,
    totalPages,
    first: current === 0,
    last: current >= totalPages - 1
  };
}

function round2(value: number): number {
  return Math.round(value * 100) / 100;
}
