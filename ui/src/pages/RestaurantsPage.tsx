import { useState, useEffect, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Search, X, SlidersHorizontal, Leaf, ChevronDown, ChevronUp, UtensilsCrossed } from 'lucide-react';
import { fetchCities, fetchCuisines, searchRestaurants, searchMenuItems } from '../api/restaurants';
import RestaurantCard from '../components/restaurants/RestaurantCard';
import RestaurantDetail from '../components/restaurants/RestaurantDetail';
import { LoadingCenter } from '../components/common/Spinner';
import Pagination from '../components/common/Pagination';
import type { Restaurant, MenuItemResult } from '../types';

type SearchMode = 'restaurants' | 'menuItems';

function useDebounce<T>(value: T, delay: number): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return debounced;
}

export default function RestaurantsPage() {
  const [query, setQuery] = useState('');
  const [mode, setMode] = useState<SearchMode>('restaurants');
  const [selectedCities, setSelectedCities] = useState<string[]>([]);
  const [selectedCuisines, setSelectedCuisines] = useState<string[]>([]);
  const [isPureVeg, setIsPureVeg] = useState<boolean | undefined>(undefined);
  const [page, setPage] = useState(0);
  const [selectedRestaurant, setSelectedRestaurant] = useState<Restaurant | null>(null);
  const [showFilters, setShowFilters] = useState(true);
  const [cuisineOpen, setCuisineOpen] = useState(true);
  const debouncedQuery = useDebounce(query, 350);

  // Reset page when filters change
  const prevFilters = useRef({ selectedCities, selectedCuisines, isPureVeg, debouncedQuery });
  useEffect(() => {
    const prev = prevFilters.current;
    if (prev.selectedCities !== selectedCities || prev.selectedCuisines !== selectedCuisines ||
        prev.isPureVeg !== isPureVeg || prev.debouncedQuery !== debouncedQuery) {
      setPage(0);
      prevFilters.current = { selectedCities, selectedCuisines, isPureVeg, debouncedQuery };
    }
  }, [selectedCities, selectedCuisines, isPureVeg, debouncedQuery]);

  const { data: cityData } = useQuery({
    queryKey: ['cities'],
    queryFn: fetchCities,
    staleTime: 60000,
  });

  const primaryCity = selectedCities.length === 1 ? selectedCities[0] : undefined;
  const { data: cuisines } = useQuery({
    queryKey: ['cuisines', primaryCity],
    queryFn: () => fetchCuisines(primaryCity),
    staleTime: 60000,
  });

  // Restaurant search
  const { data: restaurantPage, isLoading: restLoading } = useQuery({
    queryKey: ['restaurants', selectedCities, selectedCuisines, isPureVeg, debouncedQuery, page],
    queryFn: () => searchRestaurants({
      cityName: selectedCities.length === 1 ? selectedCities[0] : undefined,
      cuisine: selectedCuisines.length > 0 ? selectedCuisines[0] : undefined,
      name: mode === 'restaurants' && debouncedQuery ? debouncedQuery : undefined,
      isPureVeg,
      page,
      size: 12,
    }),
    enabled: mode === 'restaurants',
  });

  // Menu item search
  const { data: menuItemPage, isLoading: menuLoading } = useQuery({
    queryKey: ['menu-items', debouncedQuery, primaryCity, page],
    queryFn: () => searchMenuItems(debouncedQuery, primaryCity, page, 20),
    enabled: mode === 'menuItems' && debouncedQuery.length >= 2,
  });

  const cities = cityData?.cities ?? [];
  const activeCities = cities.filter(c => c.restaurantCount > 0);

  const toggleCity = (name: string) => {
    setSelectedCities(prev =>
      prev.includes(name) ? prev.filter(c => c !== name) : [...prev, name]
    );
    setSelectedCuisines([]);
  };

  const toggleCuisine = (c: string) => {
    setSelectedCuisines(prev => prev.includes(c) ? prev.filter(x => x !== c) : [...prev, c]);
  };

  const clearFilters = () => {
    setSelectedCities([]);
    setSelectedCuisines([]);
    setIsPureVeg(undefined);
    setQuery('');
    setPage(0);
  };

  const hasFilters = selectedCities.length > 0 || selectedCuisines.length > 0 || isPureVeg !== undefined || query;

  return (
    <div className="flex h-[calc(100vh-60px)] overflow-hidden">
      {/* Sidebar */}
      <aside className={`${showFilters ? 'w-60' : 'w-0'} shrink-0 overflow-y-auto bg-white border-r border-gray-100 transition-all duration-200 overflow-x-hidden`}>
        <div className="w-60 p-4 space-y-5">
          {/* Veg toggle */}
          <div>
            <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Preference</h3>
            <button
              onClick={() => setIsPureVeg(v => v === true ? undefined : true)}
              className={`flex items-center gap-2 w-full px-3 py-2 rounded-xl border text-sm transition-all ${
                isPureVeg === true
                  ? 'bg-green-50 border-green-300 text-green-700 font-semibold'
                  : 'border-gray-200 text-gray-600 hover:border-green-200'
              }`}
            >
              <Leaf size={15} />
              Pure Veg Only
            </button>
          </div>

          {/* Cities */}
          <div>
            <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
              City {selectedCities.length > 0 && <span className="text-orange-500">({selectedCities.length})</span>}
            </h3>
            <div className="space-y-1">
              {activeCities.map(city => (
                <button
                  key={city.id}
                  onClick={() => toggleCity(city.name)}
                  className={`w-full flex items-center justify-between px-2.5 py-1.5 rounded-lg text-sm transition-colors ${
                    selectedCities.includes(city.name)
                      ? 'bg-orange-50 text-orange-700 font-medium'
                      : 'text-gray-600 hover:bg-gray-50'
                  }`}
                >
                  <span>{city.name}</span>
                  <span className="text-xs text-gray-400">{city.restaurantCount}</span>
                </button>
              ))}
              {activeCities.length === 0 && (
                <p className="text-xs text-gray-400 px-2">No scraped cities yet</p>
              )}
            </div>
          </div>

          {/* Cuisines */}
          {(cuisines && cuisines.length > 0) && (
            <div>
              <button
                onClick={() => setCuisineOpen(o => !o)}
                className="flex items-center justify-between w-full text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2"
              >
                <span>Cuisine {selectedCuisines.length > 0 && <span className="text-orange-500">({selectedCuisines.length})</span>}</span>
                {cuisineOpen ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
              </button>
              {cuisineOpen && (
                <div className="space-y-1 max-h-52 overflow-y-auto pr-1">
                  {cuisines.map(c => (
                    <button
                      key={c}
                      onClick={() => toggleCuisine(c)}
                      className={`w-full text-left px-2.5 py-1.5 rounded-lg text-sm transition-colors ${
                        selectedCuisines.includes(c)
                          ? 'bg-orange-50 text-orange-700 font-medium'
                          : 'text-gray-600 hover:bg-gray-50'
                      }`}
                    >
                      {c}
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
        {/* Search & controls bar */}
        <div className="sticky top-0 z-10 bg-white border-b border-gray-100 px-5 py-3">
          <div className="flex items-center gap-3">
            <button
              onClick={() => setShowFilters(v => !v)}
              className="p-2 rounded-lg border border-gray-200 hover:border-orange-300 transition-colors shrink-0"
              title="Toggle filters"
            >
              <SlidersHorizontal size={16} className={showFilters ? 'text-orange-500' : 'text-gray-500'} />
            </button>

            {/* Search */}
            <div className="relative flex-1">
              <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                value={query}
                onChange={e => setQuery(e.target.value)}
                placeholder={mode === 'menuItems' ? 'Search menu items (biryani, pizza...)' : 'Search restaurants by name...'}
                className="w-full pl-9 pr-9 py-2.5 rounded-xl border border-gray-200 focus:border-orange-400 focus:outline-none focus:ring-2 focus:ring-orange-100 text-sm"
              />
              {query && (
                <button onClick={() => setQuery('')} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                  <X size={14} />
                </button>
              )}
            </div>

            {/* Mode toggle */}
            <div className="flex bg-gray-100 rounded-lg p-0.5 shrink-0">
              <button
                onClick={() => { setMode('restaurants'); setPage(0); }}
                className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all ${mode === 'restaurants' ? 'bg-white shadow text-orange-600' : 'text-gray-500 hover:text-gray-700'}`}
              >
                Restaurants
              </button>
              <button
                onClick={() => { setMode('menuItems'); setPage(0); }}
                className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all ${mode === 'menuItems' ? 'bg-white shadow text-orange-600' : 'text-gray-500 hover:text-gray-700'}`}
              >
                Menu Items
              </button>
            </div>
          </div>

          {/* Active filter chips */}
          {hasFilters && (
            <div className="flex flex-wrap gap-2 mt-2">
              {selectedCities.map(c => (
                <span key={c} className="inline-flex items-center gap-1 bg-orange-100 text-orange-700 text-xs font-medium px-2.5 py-1 rounded-full">
                  📍 {c}
                  <button onClick={() => toggleCity(c)}><X size={11} /></button>
                </span>
              ))}
              {selectedCuisines.map(c => (
                <span key={c} className="inline-flex items-center gap-1 bg-orange-100 text-orange-700 text-xs font-medium px-2.5 py-1 rounded-full">
                  🍜 {c}
                  <button onClick={() => toggleCuisine(c)}><X size={11} /></button>
                </span>
              ))}
              {isPureVeg && (
                <span className="inline-flex items-center gap-1 bg-green-100 text-green-700 text-xs font-medium px-2.5 py-1 rounded-full">
                  🌱 Veg only
                  <button onClick={() => setIsPureVeg(undefined)}><X size={11} /></button>
                </span>
              )}
            </div>
          )}
        </div>

        {/* Results */}
        <div className="p-5">
          {/* Restaurant mode */}
          {mode === 'restaurants' && (
            <>
              {restLoading ? (
                <LoadingCenter />
              ) : restaurantPage && restaurantPage.totalElements > 0 ? (
                <>
                  <p className="text-sm text-gray-500 mb-4">
                    {restaurantPage.totalElements.toLocaleString()} restaurants
                    {selectedCities.length > 0 && ` in ${selectedCities.join(', ')}`}
                  </p>
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                    {restaurantPage.restaurants.map(r => (
                      <RestaurantCard
                        key={r.swiggyId}
                        restaurant={r}
                        onClick={() => setSelectedRestaurant(r)}
                      />
                    ))}
                  </div>
                  <Pagination
                    page={page}
                    totalPages={restaurantPage.totalPages}
                    onPageChange={setPage}
                  />
                </>
              ) : (
                <div className="text-center py-20 text-gray-400">
                  <UtensilsCrossed size={48} className="mx-auto mb-3 opacity-30" />
                  <p className="font-medium">No restaurants found</p>
                  <p className="text-sm mt-1">Try different filters or use the scraper to fetch more data</p>
                </div>
              )}
            </>
          )}

          {/* Menu item search mode */}
          {mode === 'menuItems' && (
            <>
              {debouncedQuery.length < 2 ? (
                <div className="text-center py-20 text-gray-400">
                  <Search size={48} className="mx-auto mb-3 opacity-30" />
                  <p className="font-medium">Search for a dish</p>
                  <p className="text-sm mt-1">Type at least 2 characters to search menu items</p>
                </div>
              ) : menuLoading ? (
                <LoadingCenter />
              ) : menuItemPage && menuItemPage.totalElements > 0 ? (
                <>
                  <p className="text-sm text-gray-500 mb-4">
                    {menuItemPage.totalElements} menu items matching "{debouncedQuery}"
                    {primaryCity && ` in ${primaryCity}`}
                  </p>
                  <div className="space-y-3">
                    {menuItemPage.items.map((item: MenuItemResult) => (
                      <div key={item.id} className="card p-4 flex items-start gap-4">
                        <div className={`mt-1 w-4 h-4 shrink-0 border-2 rounded-sm flex items-center justify-center ${
                          item.isVeg ? 'border-green-600' : 'border-red-600'
                        }`}>
                          <div className={`w-2 h-2 rounded-full ${item.isVeg ? 'bg-green-600' : 'bg-red-600'}`} />
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 flex-wrap">
                            <span className="font-semibold text-gray-900">{item.name}</span>
                            {item.isBestSeller && (
                              <span className="text-xs bg-amber-100 text-amber-700 border border-amber-200 px-1.5 py-0.5 rounded-full">⭐ Bestseller</span>
                            )}
                          </div>
                          {item.description && (
                            <p className="text-xs text-gray-500 mt-0.5 line-clamp-2">{item.description}</p>
                          )}
                          <div className="flex items-center gap-3 mt-2">
                            <button
                              onClick={() => {
                                const rest = restaurantPage?.restaurants.find(r => r.swiggyId === item.restaurantSwiggyId);
                                if (rest) setSelectedRestaurant(rest);
                                else setMode('restaurants');
                              }}
                              className="text-xs text-orange-600 hover:text-orange-700 font-medium"
                            >
                              🍽 {item.restaurantName}
                            </button>
                            <span className="text-xs text-gray-400">📍 {item.cityName}</span>
                          </div>
                        </div>
                        <div className="shrink-0 text-right">
                          {item.price ? <span className="font-bold text-gray-900">₹{item.price}</span> : null}
                          {!item.inStock && <p className="text-xs text-gray-400 mt-0.5">Out of stock</p>}
                        </div>
                      </div>
                    ))}
                  </div>
                  <Pagination
                    page={page}
                    totalPages={menuItemPage.totalPages}
                    onPageChange={setPage}
                  />
                </>
              ) : (
                <div className="text-center py-20 text-gray-400">
                  <p className="font-medium">No menu items found for "{debouncedQuery}"</p>
                  <p className="text-sm mt-1">Try a different dish name</p>
                </div>
              )}
            </>
          )}
        </div>
      </main>

      {/* Restaurant detail panel */}
      {selectedRestaurant && (
        <>
          <div
            className="fixed inset-0 bg-black/30 z-20"
            onClick={() => setSelectedRestaurant(null)}
          />
          <div className="fixed inset-y-0 right-0 w-full max-w-xl bg-white shadow-2xl z-30 overflow-hidden flex flex-col">
            <RestaurantDetail
              restaurant={selectedRestaurant}
              onClose={() => setSelectedRestaurant(null)}
            />
          </div>
        </>
      )}
    </div>
  );
}
