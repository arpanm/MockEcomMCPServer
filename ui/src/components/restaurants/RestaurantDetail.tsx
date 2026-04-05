import { useQuery } from '@tanstack/react-query';
import { X, Clock, IndianRupee, Star, MapPin, Leaf, ChevronDown, ChevronUp } from 'lucide-react';
import { useState } from 'react';
import { fetchRestaurantMenu } from '../../api/restaurants';
import type { Restaurant, MenuCategory } from '../../types';
import { LoadingCenter } from '../common/Spinner';

interface Props {
  restaurant: Restaurant;
  onClose: () => void;
}

function MenuCategorySection({ cat }: { cat: MenuCategory }) {
  const [open, setOpen] = useState(true);
  return (
    <div className="border border-gray-100 rounded-xl overflow-hidden mb-3">
      <button
        onClick={() => setOpen(o => !o)}
        className="w-full flex items-center justify-between px-4 py-3 bg-gray-50 hover:bg-orange-50 transition-colors"
      >
        <div className="flex items-center gap-2">
          <span className="font-semibold text-gray-800">{cat.category}</span>
          <span className="text-xs text-gray-500 bg-white border border-gray-200 px-2 py-0.5 rounded-full">
            {cat.itemCount} items
          </span>
        </div>
        {open ? <ChevronUp size={16} className="text-gray-400" /> : <ChevronDown size={16} className="text-gray-400" />}
      </button>

      {open && (
        <div className="divide-y divide-gray-50">
          {cat.items.map(item => (
            <div key={item.id} className="flex items-start gap-3 px-4 py-3 hover:bg-gray-50 transition-colors">
              {/* Veg/NonVeg indicator */}
              <div className="mt-1 shrink-0">
                <div className={`w-4 h-4 border-2 rounded-sm flex items-center justify-center ${
                  item.isVeg ? 'border-green-600' : 'border-red-600'
                }`}>
                  <div className={`w-2 h-2 rounded-full ${item.isVeg ? 'bg-green-600' : 'bg-red-600'}`} />
                </div>
              </div>

              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="font-medium text-gray-900 text-sm">{item.name}</span>
                  {item.isBestSeller && (
                    <span className="text-xs bg-amber-100 text-amber-700 border border-amber-200 px-1.5 py-0.5 rounded-full font-medium">
                      ⭐ Bestseller
                    </span>
                  )}
                  {!item.inStock && (
                    <span className="text-xs bg-gray-100 text-gray-500 px-2 py-0.5 rounded-full">Out of stock</span>
                  )}
                </div>
                {item.description && (
                  <p className="text-xs text-gray-500 mt-0.5 line-clamp-2">{item.description}</p>
                )}
              </div>

              <div className="shrink-0 text-right">
                {item.price ? (
                  <span className="font-semibold text-gray-900 text-sm">₹{item.price}</span>
                ) : (
                  <span className="text-xs text-gray-400">—</span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default function RestaurantDetail({ restaurant: r, onClose }: Props) {
  const [activeTab, setActiveTab] = useState<string | null>(null);

  const { data: menu, isLoading, isError } = useQuery({
    queryKey: ['restaurant-menu', r.swiggyId],
    queryFn: () => fetchRestaurantMenu(r.swiggyId),
    enabled: !!r.swiggyId && r.menuScraped,
  });

  const categories = menu?.menu ?? [];
  const displayCats = activeTab
    ? categories.filter(c => c.category === activeTab)
    : categories;

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="relative">
        <div className="h-48 bg-gradient-to-br from-orange-200 to-amber-100 overflow-hidden">
          {r.imageUrl ? (
            <img src={r.imageUrl} alt={r.name} className="w-full h-full object-cover" onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }} />
          ) : (
            <div className="h-full flex items-center justify-center text-7xl">🍽️</div>
          )}
        </div>
        <button
          onClick={onClose}
          className="absolute top-3 right-3 bg-white/90 hover:bg-white rounded-full p-2 shadow-md transition-colors"
        >
          <X size={18} />
        </button>
        {r.isPureVeg && (
          <div className="absolute top-3 left-3 bg-green-500 text-white text-xs font-bold px-2 py-1 rounded-full flex items-center gap-1">
            <Leaf size={10} /> Pure Veg
          </div>
        )}
      </div>

      {/* Info */}
      <div className="px-5 pt-4 pb-3 border-b border-gray-100">
        <h2 className="text-xl font-bold text-gray-900">{r.name}</h2>
        <p className="text-sm text-gray-500 mt-0.5">
          {[r.locality, r.areaName, r.city].filter(Boolean).join(' · ')}
        </p>

        <div className="flex flex-wrap gap-3 mt-3">
          {r.avgRating && (
            <div className="flex items-center gap-1 bg-green-600 text-white text-sm font-bold px-2.5 py-1 rounded-lg">
              <Star size={14} className="fill-white" />
              {r.avgRating.toFixed(1)}
              {r.totalRatings && <span className="text-green-200 font-normal text-xs">({r.totalRatings})</span>}
            </div>
          )}
          {r.deliveryTime && (
            <div className="flex items-center gap-1.5 text-sm text-gray-600 bg-gray-50 px-3 py-1 rounded-lg border border-gray-100">
              <Clock size={14} />
              {r.deliveryTime} min
            </div>
          )}
          {r.costForTwo && (
            <div className="flex items-center gap-1.5 text-sm text-gray-600 bg-gray-50 px-3 py-1 rounded-lg border border-gray-100">
              <IndianRupee size={14} />
              ₹{r.costForTwo} for two
            </div>
          )}
          {r.city && (
            <div className="flex items-center gap-1.5 text-sm text-gray-600 bg-gray-50 px-3 py-1 rounded-lg border border-gray-100">
              <MapPin size={14} />
              {r.city}
            </div>
          )}
        </div>

        {r.cuisines && (
          <div className="flex flex-wrap gap-1.5 mt-2">
            {r.cuisines.split(',').map(c => (
              <span key={c.trim()} className="text-xs text-orange-600 bg-orange-50 border border-orange-100 px-2 py-0.5 rounded-full">
                {c.trim()}
              </span>
            ))}
          </div>
        )}

        {r.discount && (
          <div className="mt-2 bg-green-50 border border-green-100 text-green-700 text-xs font-semibold px-3 py-1.5 rounded-lg inline-block">
            🎉 {r.discount}
          </div>
        )}
      </div>

      {/* Menu */}
      <div className="flex-1 overflow-y-auto">
        {!r.menuScraped ? (
          <div className="text-center py-12 text-gray-400">
            <div className="text-4xl mb-2">📋</div>
            <p className="text-sm">Menu not yet scraped</p>
            <p className="text-xs mt-1">Use the Scraper to fetch menu data</p>
          </div>
        ) : isLoading ? (
          <LoadingCenter />
        ) : isError ? (
          <div className="text-center py-12 text-red-400">Failed to load menu</div>
        ) : categories.length === 0 ? (
          <div className="text-center py-12 text-gray-400">No menu items found</div>
        ) : (
          <div>
            {/* Category tabs */}
            {categories.length > 1 && (
              <div className="px-4 py-3 bg-white border-b border-gray-100 sticky top-0 z-10">
                <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-none">
                  <button
                    onClick={() => setActiveTab(null)}
                    className={`shrink-0 filter-chip ${!activeTab ? 'filter-chip-active' : 'filter-chip-inactive'}`}
                  >
                    All ({menu?.totalItems ?? 0})
                  </button>
                  {categories.map(cat => (
                    <button
                      key={cat.category}
                      onClick={() => setActiveTab(activeTab === cat.category ? null : cat.category)}
                      className={`shrink-0 filter-chip ${activeTab === cat.category ? 'filter-chip-active' : 'filter-chip-inactive'}`}
                    >
                      {cat.category}
                    </button>
                  ))}
                </div>
              </div>
            )}

            <div className="p-4">
              {displayCats.map(cat => (
                <MenuCategorySection key={cat.category} cat={cat} />
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
