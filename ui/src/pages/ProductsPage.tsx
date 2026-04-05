import { useState, useEffect, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Search, X, SlidersHorizontal, ShoppingBag, ChevronDown, ChevronUp } from 'lucide-react';
import { searchProducts, fetchProductFilters } from '../api/products';
import ProductCard from '../components/products/ProductCard';
import ProductDetail from '../components/products/ProductDetail';
import { LoadingCenter } from '../components/common/Spinner';
import Pagination from '../components/common/Pagination';
import type { Product } from '../types';

function useDebounce<T>(value: T, delay: number): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return debounced;
}

const CATEGORY_EMOJIS: Record<string, string> = {
  Electronics: '🖥️', Fashion: '👗', Grocery: '🛒', Beauty: '💄', Home: '🏠',
};

export default function ProductsPage() {
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('');
  const [subCategory, setSubCategory] = useState('');
  const [brand, setBrand] = useState('');
  const [page, setPage] = useState(0);
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [showFilters, setShowFilters] = useState(true);
  const [brandOpen, setBrandOpen] = useState(false);
  const [subCatOpen, setSubCatOpen] = useState(true);
  const debouncedQuery = useDebounce(query, 350);

  const prevFilters = useRef({ category, subCategory, brand, debouncedQuery });
  useEffect(() => {
    const prev = prevFilters.current;
    if (prev.category !== category || prev.subCategory !== subCategory ||
        prev.brand !== brand || prev.debouncedQuery !== debouncedQuery) {
      setPage(0);
      prevFilters.current = { category, subCategory, brand, debouncedQuery };
    }
  }, [category, subCategory, brand, debouncedQuery]);

  // Reset subcat when category changes
  useEffect(() => { setSubCategory(''); }, [category]);

  const { data: filters } = useQuery({
    queryKey: ['product-filters', debouncedQuery, category],
    queryFn: () => fetchProductFilters(debouncedQuery || category),
    staleTime: 30000,
  });

  const { data: productPage, isLoading } = useQuery({
    queryKey: ['products', debouncedQuery, category, subCategory, brand, page],
    queryFn: () => searchProducts({
      q: debouncedQuery || category || 'top',
      category: category || undefined,
      subCategory: subCategory || undefined,
      brand: brand || undefined,
      page,
      size: 12,
    }),
    staleTime: 10000,
  });

  const availableSubCats = filters?.subCategories ?? [];
  const availableBrands = filters?.brands ?? [];
  const hasFilters = category || subCategory || brand || query;

  const clearFilters = () => {
    setCategory('');
    setSubCategory('');
    setBrand('');
    setQuery('');
    setPage(0);
  };

  return (
    <div className="flex h-[calc(100vh-60px)] overflow-hidden">
      {/* Sidebar */}
      <aside className={`${showFilters ? 'w-60' : 'w-0'} shrink-0 overflow-y-auto bg-white border-r border-gray-100 transition-all duration-200 overflow-x-hidden`}>
        <div className="w-60 p-4 space-y-5">
          {/* Categories */}
          <div>
            <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Category</h3>
            <div className="space-y-1">
              {(['Electronics', 'Fashion', 'Grocery', 'Beauty', 'Home'] as const).map(cat => (
                <button
                  key={cat}
                  onClick={() => setCategory(prev => prev === cat ? '' : cat)}
                  className={`w-full flex items-center gap-2 px-2.5 py-2 rounded-xl text-sm transition-colors ${
                    category === cat
                      ? 'bg-blue-50 text-blue-700 font-semibold'
                      : 'text-gray-600 hover:bg-gray-50'
                  }`}
                >
                  <span>{CATEGORY_EMOJIS[cat]}</span>
                  <span>{cat}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Sub-categories */}
          {availableSubCats.length > 0 && (
            <div>
              <button
                onClick={() => setSubCatOpen(o => !o)}
                className="flex items-center justify-between w-full text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2"
              >
                <span>Sub-category</span>
                {subCatOpen ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
              </button>
              {subCatOpen && (
                <div className="space-y-1 max-h-48 overflow-y-auto">
                  {availableSubCats.slice(0, 15).map((sc: { name: string; count: number }) => (
                    <button
                      key={sc.name}
                      onClick={() => setSubCategory(prev => prev === sc.name ? '' : sc.name)}
                      className={`w-full flex items-center justify-between px-2.5 py-1.5 rounded-lg text-sm transition-colors ${
                        subCategory === sc.name
                          ? 'bg-blue-50 text-blue-700 font-medium'
                          : 'text-gray-600 hover:bg-gray-50'
                      }`}
                    >
                      <span className="truncate">{sc.name}</span>
                      <span className="text-xs text-gray-400 shrink-0">{sc.count}</span>
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Brands */}
          {availableBrands.length > 0 && (
            <div>
              <button
                onClick={() => setBrandOpen(o => !o)}
                className="flex items-center justify-between w-full text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2"
              >
                <span>Brand</span>
                {brandOpen ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
              </button>
              {brandOpen && (
                <div className="space-y-1 max-h-48 overflow-y-auto">
                  {availableBrands.slice(0, 15).map((b: { name: string; count: number }) => (
                    <button
                      key={b.name}
                      onClick={() => setBrand(prev => prev === b.name ? '' : b.name)}
                      className={`w-full flex items-center justify-between px-2.5 py-1.5 rounded-lg text-sm transition-colors ${
                        brand === b.name
                          ? 'bg-blue-50 text-blue-700 font-medium'
                          : 'text-gray-600 hover:bg-gray-50'
                      }`}
                    >
                      <span className="truncate">{b.name}</span>
                      <span className="text-xs text-gray-400 shrink-0">{b.count}</span>
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}

          {hasFilters && (
            <button onClick={clearFilters} className="w-full text-xs text-gray-500 hover:text-red-500 transition-colors flex items-center gap-1.5 px-2">
              <X size={13} /> Clear all filters
            </button>
          )}
        </div>
      </aside>

      {/* Main */}
      <main className="flex-1 overflow-y-auto">
        <div className="sticky top-0 z-10 bg-white border-b border-gray-100 px-5 py-3">
          <div className="flex items-center gap-3">
            <button
              onClick={() => setShowFilters(v => !v)}
              className="p-2 rounded-lg border border-gray-200 hover:border-blue-300 transition-colors shrink-0"
            >
              <SlidersHorizontal size={16} className={showFilters ? 'text-blue-500' : 'text-gray-500'} />
            </button>

            <div className="relative flex-1">
              <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                value={query}
                onChange={e => setQuery(e.target.value)}
                placeholder="Search products by name, description..."
                className="w-full pl-9 pr-9 py-2.5 rounded-xl border border-gray-200 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 text-sm"
              />
              {query && (
                <button onClick={() => setQuery('')} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                  <X size={14} />
                </button>
              )}
            </div>
          </div>

          {/* Active chips */}
          {hasFilters && (
            <div className="flex flex-wrap gap-2 mt-2">
              {category && (
                <span className="inline-flex items-center gap-1 bg-blue-100 text-blue-700 text-xs font-medium px-2.5 py-1 rounded-full">
                  {CATEGORY_EMOJIS[category]} {category}
                  <button onClick={() => setCategory('')}><X size={11} /></button>
                </span>
              )}
              {subCategory && (
                <span className="inline-flex items-center gap-1 bg-blue-100 text-blue-700 text-xs font-medium px-2.5 py-1 rounded-full">
                  {subCategory}
                  <button onClick={() => setSubCategory('')}><X size={11} /></button>
                </span>
              )}
              {brand && (
                <span className="inline-flex items-center gap-1 bg-blue-100 text-blue-700 text-xs font-medium px-2.5 py-1 rounded-full">
                  {brand}
                  <button onClick={() => setBrand('')}><X size={11} /></button>
                </span>
              )}
            </div>
          )}
        </div>

        <div className="p-5">
          {isLoading ? (
            <LoadingCenter />
          ) : productPage && productPage.totalElements > 0 ? (
            <>
              <p className="text-sm text-gray-500 mb-4">
                {productPage.totalElements.toLocaleString()} products
                {category && ` in ${category}`}
                {subCategory && ` › ${subCategory}`}
              </p>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
                {productPage.products.map(p => (
                  <ProductCard
                    key={p.id}
                    product={p}
                    onClick={() => setSelectedProduct(p)}
                  />
                ))}
              </div>
              <Pagination
                page={page}
                totalPages={productPage.totalPages}
                onPageChange={setPage}
              />
            </>
          ) : (
            <div className="text-center py-20 text-gray-400">
              <ShoppingBag size={48} className="mx-auto mb-3 opacity-30" />
              <p className="font-medium">No products found</p>
              <p className="text-sm mt-1">Try a different search or select a category</p>
            </div>
          )}
        </div>
      </main>

      {/* Product detail panel */}
      {selectedProduct && (
        <>
          <div className="fixed inset-0 bg-black/30 z-20" onClick={() => setSelectedProduct(null)} />
          <div className="fixed inset-y-0 right-0 w-full max-w-lg bg-white shadow-2xl z-30 overflow-hidden flex flex-col">
            <ProductDetail product={selectedProduct} onClose={() => setSelectedProduct(null)} />
          </div>
        </>
      )}
    </div>
  );
}
