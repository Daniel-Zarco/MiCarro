import { Product } from '../models/product';
import { ShoppingMode } from '../models/shopping-mode';
import { ShoppingPlanItemResponse } from '../models/shopping-plan-item-response';
import { ShoppingPlanRequest } from '../models/shopping-plan-request';
import { ShoppingPlanResponse } from '../models/shopping-plan-response';

/*
 * Motor del planificador portado desde Spring Boot. Conserva las mismas
 * constantes de relevancia, los pesos de las estrategias, la selección del
 * mejor candidato y los optimizadores de presupuesto.
 */

const MAX_CANDIDATES_PER_ITEM = 10;
const RELEVANCE_TOLERANCE = 0.15;
const EPSILON = 1e-9;

const RELEVANCE_EXACT = 1.0;
const RELEVANCE_PREFIX_AND_CATEGORY = 0.95;
const RELEVANCE_CATEGORY = 0.85;
const RELEVANCE_PREFIX = 0.70;
const RELEVANCE_CONTAINS = 0.50;
const RELEVANCE_FALLBACK = 0.30;
const RELEVANCE_NONE = 0.0;

interface Strategy {
  relevance: number;
  price: number;
  favorite: number;
}

const STRATEGIES: Record<ShoppingMode, Strategy> = {
  CHEAP: { relevance: 0.35, price: 0.55, favorite: 0.10 },
  BALANCED: { relevance: 0.60, price: 0.25, favorite: 0.15 },
  QUALITY: { relevance: 0.80, price: 0.05, favorite: 0.15 }
};

interface ProductScore {
  product: Product;
  relevanceScore: number;
  priceScore: number;
  favoriteScore: number;
  finalScore: number;
}

export function buildShoppingPlan(
  request: ShoppingPlanRequest,
  catalog: Product[],
  favoriteIds: Set<string>
): ShoppingPlanResponse {

  const candidatesByTerm = new Map<string, Product[]>();

  for (const item of request.items) {

    const term = item.trim();

    if (!term) {
      continue;
    }

    if (!candidatesByTerm.has(term)) {
      candidatesByTerm.set(term, findCandidates(term, catalog));
    }
  }

  const winners = new Map<string, ProductScore>();

  for (const [term, candidates] of candidatesByTerm) {

    const scores = scoreCandidates(term, candidates, request.mode, favoriteIds);
    const best = selectBest(scores);

    if (best) {
      winners.set(term, best);
    }
  }

  const optimized = optimizeBudget(request.budget, winners);

  const quantities = request.preferences?.maximizeBudget
    ? maximizeBudget(request.budget, optimized)
    : defaultQuantities(optimized);

  const items: ShoppingPlanItemResponse[] = [];
  let estimatedTotal = 0;

  for (const [term, score] of optimized) {

    const quantity = quantities.get(term) ?? 1;
    const product = score.product;
    const unitPrice = product.price;
    const subtotal = unitPrice == null ? null : round2(unitPrice * quantity);

    items.push({
      productId: product.id,
      name: product.name,
      brand: product.brand,
      format: product.format,
      imageUrl: product.imageUrl,
      quantity,
      unitPrice,
      subtotal
    });

    if (subtotal != null) {
      estimatedTotal += subtotal;
    }
  }

  estimatedTotal = round2(estimatedTotal);

  return {
    budget: request.budget,
    mode: request.mode,
    preferences: request.preferences,
    items,
    estimatedTotal,
    remainingBudget: round2(request.budget - estimatedTotal)
  };
}

function findCandidates(term: string, catalog: Product[]): Product[] {

  const normalized = term.trim();
  const variants = termVariants(normalized);

  const result: Product[] = [];
  const seen = new Set<string>();

  const addTier = (query: (variant: string) => Product[]): void => {
    for (const variant of variants) {
      for (const product of query(variant)) {
        if (result.length >= MAX_CANDIDATES_PER_ITEM) {
          return;
        }
        if (!seen.has(product.id)) {
          seen.add(product.id);
          result.push(product);
        }
      }
    }
  };

  addTier(variant => catalog.filter(product =>
    (product.name ?? '').toLowerCase() === variant.toLowerCase()));

  if (result.length < MAX_CANDIDATES_PER_ITEM) {
    addTier(variant => catalog.filter(product =>
      (product.name ?? '').toLowerCase().startsWith(variant.toLowerCase())));
  }

  if (result.length < MAX_CANDIDATES_PER_ITEM) {
    addTier(variant => catalog.filter(product =>
      (product.category ?? '').toLowerCase().includes(variant.toLowerCase())));
  }

  if (result.length < MAX_CANDIDATES_PER_ITEM) {
    addTier(variant => catalog.filter(product =>
      (product.name ?? '').toLowerCase().includes(variant.toLowerCase())));
  }

  if (result.length < MAX_CANDIDATES_PER_ITEM) {
    addTier(variant => catalog.filter(product =>
      (product.brand ?? '').toLowerCase().includes(variant.toLowerCase())
      || (product.category ?? '').toLowerCase().includes(variant.toLowerCase())));
  }

  return result;
}

function termVariants(term: string): string[] {

  const variants = new Set<string>([term]);

  if (term.endsWith('es') && term.length > 2) {
    variants.add(term.slice(0, -2));
  } else if (term.endsWith('s') && term.length > 1) {
    variants.add(term.slice(0, -1));
  }

  return [...variants];
}

function scoreCandidates(
  term: string,
  candidates: Product[],
  mode: ShoppingMode,
  favoriteIds: Set<string>
): ProductScore[] {

  const strategy = STRATEGIES[mode] ?? STRATEGIES.BALANCED;

  const prices = candidates
    .map(candidate => candidate.price)
    .filter((price): price is number => price != null);

  const minPrice = prices.length ? Math.min(...prices) : null;
  const maxPrice = prices.length ? Math.max(...prices) : null;

  return candidates.map(candidate => {

    const relevanceScore = relevance(term, candidate);
    const priceScore = priceScoreOf(candidate, minPrice, maxPrice);
    const favoriteScore = favoriteIds.has(candidate.id) ? 1.0 : 0.0;

    const finalScore =
      relevanceScore * strategy.relevance
      + priceScore * strategy.price
      + favoriteScore * strategy.favorite;

    return {
      product: candidate,
      relevanceScore,
      priceScore,
      favoriteScore,
      finalScore
    };
  });
}

function relevance(term: string, product: Product): number {

  if (!term || term.trim().length === 0) {
    return RELEVANCE_NONE;
  }

  const normalized = term.trim().toLowerCase();
  const name = (product.name ?? '').toLowerCase();
  const category = (product.category ?? '').toLowerCase();
  const brand = (product.brand ?? '').toLowerCase();

  const prefixMatch = name.startsWith(normalized);
  const categoryMatch = category.includes(normalized);

  if (name === normalized) {
    return RELEVANCE_EXACT;
  }

  if (prefixMatch && categoryMatch) {
    return RELEVANCE_PREFIX_AND_CATEGORY;
  }

  if (categoryMatch) {
    return RELEVANCE_CATEGORY;
  }

  if (prefixMatch) {
    return RELEVANCE_PREFIX;
  }

  if (name.includes(normalized)) {
    return RELEVANCE_CONTAINS;
  }

  if (brand.includes(normalized)) {
    return RELEVANCE_FALLBACK;
  }

  return RELEVANCE_NONE;
}

function priceScoreOf(
  product: Product,
  minPrice: number | null,
  maxPrice: number | null
): number {

  const price = product.price;

  if (price == null || minPrice == null || maxPrice == null) {
    return 0.0;
  }

  if (maxPrice === minPrice) {
    return 1.0;
  }

  return 1.0 - ((price - minPrice) / (maxPrice - minPrice));
}

function selectBest(scores: ProductScore[]): ProductScore | null {

  if (scores.length === 0) {
    return null;
  }

  const maxRelevance = Math.max(...scores.map(score => score.relevanceScore));
  const threshold = maxRelevance - RELEVANCE_TOLERANCE - EPSILON;

  const eligible = scores.filter(score => score.relevanceScore >= threshold);

  eligible.sort((a, b) =>
    b.finalScore - a.finalScore
    || b.relevanceScore - a.relevanceScore
    || b.priceScore - a.priceScore
    || compareId(a.product.id, b.product.id));

  return eligible[0];
}

function optimizeBudget(
  budget: number,
  selected: Map<string, ProductScore>
): Map<string, ProductScore> {

  const result = new Map<string, ProductScore>();

  if (selected.size === 0) {
    return result;
  }

  const total = totalPrice(selected);

  if (budget == null || total <= budget) {
    selected.forEach((value, key) => result.set(key, value));
    return result;
  }

  const removable = [...selected.entries()].sort((a, b) =>
    a[1].finalScore - b[1].finalScore
    || (priceOf(b[1]) ?? 0) - (priceOf(a[1]) ?? 0));

  const removed = new Set<string>();
  let currentTotal = total;

  for (const [term, score] of removable) {

    if (currentTotal <= budget) {
      break;
    }

    const price = score.product.price;

    if (price == null) {
      continue;
    }

    removed.add(term);
    currentTotal -= price;
  }

  for (const [term, score] of selected) {
    if (!removed.has(term)) {
      result.set(term, score);
    }
  }

  return result;
}

function totalPrice(selected: Map<string, ProductScore>): number {

  let total = 0;

  for (const score of selected.values()) {
    const price = priceOf(score);
    if (price != null) {
      total += price;
    }
  }

  return total;
}

function priceOf(score: ProductScore): number | null {
  return score.product.price;
}

function defaultQuantities(selected: Map<string, ProductScore>): Map<string, number> {

  const quantities = new Map<string, number>();

  for (const term of selected.keys()) {
    quantities.set(term, 1);
  }

  return quantities;
}

function maximizeBudget(
  budget: number,
  selected: Map<string, ProductScore>
): Map<string, number> {

  const quantities = new Map<string, number>();

  if (selected.size === 0) {
    return quantities;
  }

  const affordable: { term: string; price: number }[] = [];
  let total = 0;

  for (const [term, score] of selected) {

    quantities.set(term, 1);

    const price = validPrice(score);

    if (price != null) {
      total += price;
      affordable.push({ term, price });
    }
  }

  if (budget == null || affordable.length === 0) {
    return quantities;
  }

  let remaining = budget - total;

  if (remaining <= 0) {
    return quantities;
  }

  let advanced = true;

  while (advanced) {

    advanced = false;

    for (const entry of affordable) {

      if (entry.price <= remaining) {
        quantities.set(entry.term, (quantities.get(entry.term) ?? 0) + 1);
        remaining -= entry.price;
        advanced = true;
      }
    }
  }

  return quantities;
}

function validPrice(score: ProductScore): number | null {

  const price = score.product.price;

  if (price == null || price <= 0) {
    return null;
  }

  return price;
}

function compareId(a: string, b: string): number {

  const na = Number(a);
  const nb = Number(b);

  if (Number.isFinite(na) && Number.isFinite(nb)) {
    return na - nb;
  }

  return a < b ? -1 : a > b ? 1 : 0;
}

function round2(value: number): number {
  return Math.round(value * 100) / 100;
}
