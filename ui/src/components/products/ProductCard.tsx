import { Star } from 'lucide-react';
import type { Product } from '../../types';

interface Props {
  product: Product;
  onClick: () => void;
}

const CATEGORY_EMOJIS: Record<string, string> = {
  Electronics: '🖥️',
  Fashion: '👗',
  Grocery: '🛒',
  Beauty: '💄',
  Home: '🏠',
};

export default function ProductCard({ product: p, onClick }: Props) {
  const emoji = CATEGORY_EMOJIS[p.category] ?? '📦';
  const discount = p.mrp > p.price
    ? Math.round(((p.mrp - p.price) / p.mrp) * 100)
    : null;

  return (
    <div
      onClick={onClick}
      className="card cursor-pointer overflow-hidden group"
    >
      {/* Image */}
      <div className="relative h-44 bg-gradient-to-br from-blue-50 to-indigo-50 overflow-hidden">
        {p.imageUrl ? (
          <img
            src={p.imageUrl}
            alt={p.title}
            className="w-full h-full object-contain group-hover:scale-105 transition-transform duration-300 p-2"
            onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }}
          />
        ) : (
          <div className="h-full flex items-center justify-center text-5xl">{emoji}</div>
        )}
        {discount && (
          <div className="absolute top-2 left-2 bg-red-500 text-white text-xs font-bold px-2 py-0.5 rounded-full">
            -{discount}%
          </div>
        )}
        <div className="absolute top-2 right-2 bg-white/90 text-xs text-gray-500 px-2 py-0.5 rounded-full border border-gray-200">
          {p.category}
        </div>
      </div>

      {/* Content */}
      <div className="p-3">
        <p className="text-xs text-gray-400 font-medium uppercase tracking-wide">{p.brand}</p>
        <h3 className="font-semibold text-gray-900 text-sm mt-0.5 line-clamp-2 group-hover:text-blue-600 transition-colors leading-snug">
          {p.title}
        </h3>

        {p.averageRating > 0 && (
          <div className="flex items-center gap-1 mt-1.5">
            <div className="flex items-center gap-0.5">
              {[1,2,3,4,5].map(i => (
                <Star key={i} size={11} className={i <= Math.round(p.averageRating) ? 'fill-amber-400 text-amber-400' : 'fill-gray-200 text-gray-200'} />
              ))}
            </div>
            <span className="text-xs text-gray-500">({p.reviewCount})</span>
          </div>
        )}

        <div className="flex items-baseline gap-2 mt-2">
          <span className="text-base font-bold text-gray-900">₹{p.price.toLocaleString()}</span>
          {p.mrp > p.price && (
            <span className="text-xs text-gray-400 line-through">₹{p.mrp.toLocaleString()}</span>
          )}
        </div>

        {p.subCategory && (
          <p className="text-xs text-gray-400 mt-1">{p.subCategory}</p>
        )}
      </div>
    </div>
  );
}
