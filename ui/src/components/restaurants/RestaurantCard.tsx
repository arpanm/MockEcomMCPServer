import { Clock, IndianRupee, Star, Leaf } from 'lucide-react';
import type { Restaurant } from '../../types';

interface Props {
  restaurant: Restaurant;
  onClick: () => void;
}

export default function RestaurantCard({ restaurant: r, onClick }: Props) {
  const cuisines = r.cuisines?.split(',').map(c => c.trim()).slice(0, 3) ?? [];

  return (
    <div
      onClick={onClick}
      className="card cursor-pointer overflow-hidden group"
    >
      {/* Image */}
      <div className="relative h-40 bg-gradient-to-br from-orange-100 to-amber-50 overflow-hidden">
        {r.imageUrl ? (
          <img
            src={r.imageUrl}
            alt={r.name}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
            onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }}
          />
        ) : (
          <div className="absolute inset-0 flex items-center justify-center">
            <span className="text-5xl">🍽️</span>
          </div>
        )}
        {r.discount && (
          <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/70 to-transparent px-3 py-2">
            <span className="text-white text-xs font-semibold">{r.discount}</span>
          </div>
        )}
        {r.isPureVeg && (
          <div className="absolute top-2 right-2 bg-green-500 text-white text-xs font-bold px-2 py-0.5 rounded-full flex items-center gap-1">
            <Leaf size={10} /> Pure Veg
          </div>
        )}
        {!r.isOpen && (
          <div className="absolute inset-0 bg-black/40 flex items-center justify-center">
            <span className="bg-black/70 text-white text-xs font-semibold px-3 py-1 rounded-full">Currently Closed</span>
          </div>
        )}
      </div>

      {/* Content */}
      <div className="p-3">
        <div className="flex items-start justify-between gap-2">
          <h3 className="font-semibold text-gray-900 text-sm leading-tight line-clamp-1 group-hover:text-orange-600 transition-colors">
            {r.name}
          </h3>
          {r.avgRating && (
            <div className="flex items-center gap-1 bg-green-600 text-white text-xs font-bold px-1.5 py-0.5 rounded shrink-0">
              <Star size={10} className="fill-white" />
              {r.avgRating.toFixed(1)}
            </div>
          )}
        </div>

        <p className="text-xs text-gray-500 mt-0.5 line-clamp-1">{r.locality || r.areaName || r.city}</p>

        <div className="flex flex-wrap gap-1 mt-1.5">
          {cuisines.map(c => (
            <span key={c} className="text-xs text-gray-500 bg-gray-50 px-1.5 py-0.5 rounded-md border border-gray-100">
              {c}
            </span>
          ))}
        </div>

        <div className="flex items-center gap-3 mt-2 pt-2 border-t border-gray-50">
          {r.deliveryTime && (
            <div className="flex items-center gap-1 text-xs text-gray-500">
              <Clock size={11} />
              <span>{r.deliveryTime} min</span>
            </div>
          )}
          {r.costForTwo && (
            <div className="flex items-center gap-1 text-xs text-gray-500">
              <IndianRupee size={11} />
              <span>₹{r.costForTwo} for two</span>
            </div>
          )}
          {r.totalRatings && (
            <span className="text-xs text-gray-400 ml-auto">{r.totalRatings}</span>
          )}
        </div>
      </div>
    </div>
  );
}
