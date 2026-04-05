import { useQuery } from '@tanstack/react-query';
import { X, Star, Package, ChevronDown, ChevronUp } from 'lucide-react';
import { useState } from 'react';
import { fetchProduct, fetchProductReviews } from '../../api/products';
import type { Product } from '../../types';
import { LoadingCenter } from '../common/Spinner';

const CATEGORY_EMOJIS: Record<string, string> = {
  Electronics: '🖥️', Fashion: '👗', Grocery: '🛒', Beauty: '💄', Home: '🏠',
};

interface Props {
  product: Product;
  onClose: () => void;
}

export default function ProductDetail({ product: p, onClose }: Props) {
  const [showAllReviews, setShowAllReviews] = useState(false);
  const [reviewPage, setReviewPage] = useState(0);

  const { data: detail, isLoading } = useQuery({
    queryKey: ['product-detail', p.id],
    queryFn: () => fetchProduct(p.id),
  });

  const { data: reviewData } = useQuery({
    queryKey: ['product-reviews', p.id, reviewPage],
    queryFn: () => fetchProductReviews(p.id, reviewPage, 5),
    enabled: showAllReviews,
  });

  const discount = p.mrp > p.price
    ? Math.round(((p.mrp - p.price) / p.mrp) * 100)
    : null;

  const emoji = CATEGORY_EMOJIS[p.category] ?? '📦';

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="relative">
        <div className="h-52 bg-gradient-to-br from-blue-50 to-indigo-100 overflow-hidden flex items-center justify-center">
          {p.imageUrl ? (
            <img src={p.imageUrl} alt={p.title} className="h-full w-full object-contain p-4"
              onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }} />
          ) : (
            <span className="text-7xl">{emoji}</span>
          )}
        </div>
        <button
          onClick={onClose}
          className="absolute top-3 right-3 bg-white/90 hover:bg-white rounded-full p-2 shadow-md"
        >
          <X size={18} />
        </button>
        {discount && (
          <div className="absolute top-3 left-3 bg-red-500 text-white text-xs font-bold px-2.5 py-1 rounded-full">
            -{discount}% OFF
          </div>
        )}
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto px-5 py-4 space-y-5">
        {/* Title & Price */}
        <div>
          <p className="text-xs text-blue-600 font-semibold uppercase tracking-widest">{p.brand}</p>
          <h2 className="text-lg font-bold text-gray-900 mt-1 leading-tight">{p.title}</h2>
          {p.averageRating > 0 && (
            <div className="flex items-center gap-2 mt-1.5">
              <div className="flex items-center gap-0.5">
                {[1,2,3,4,5].map(i => (
                  <Star key={i} size={14} className={i <= Math.round(p.averageRating) ? 'fill-amber-400 text-amber-400' : 'fill-gray-200 text-gray-200'} />
                ))}
              </div>
              <span className="text-sm text-gray-600">{p.averageRating.toFixed(1)}</span>
              <span className="text-sm text-gray-400">({p.reviewCount} reviews)</span>
            </div>
          )}
          <div className="flex items-baseline gap-3 mt-3">
            <span className="text-2xl font-bold text-gray-900">₹{p.price.toLocaleString()}</span>
            {p.mrp > p.price && (
              <>
                <span className="text-gray-400 line-through text-sm">₹{p.mrp.toLocaleString()}</span>
                <span className="text-green-600 font-semibold text-sm">You save ₹{(p.mrp - p.price).toLocaleString()}</span>
              </>
            )}
          </div>
        </div>

        {/* Category breadcrumb */}
        <div className="flex items-center gap-2 text-xs text-gray-500">
          <span className="bg-gray-100 px-2 py-1 rounded-md">{p.category}</span>
          {p.subCategory && <><span>›</span><span className="bg-gray-100 px-2 py-1 rounded-md">{p.subCategory}</span></>}
        </div>

        {/* Description */}
        {p.description && (
          <div>
            <h3 className="text-sm font-semibold text-gray-900 mb-1">Description</h3>
            <p className="text-sm text-gray-600 leading-relaxed">{p.description}</p>
          </div>
        )}

        {/* Attributes */}
        {isLoading ? (
          <LoadingCenter />
        ) : detail?.attributes && detail.attributes.length > 0 && (
          <div>
            <h3 className="text-sm font-semibold text-gray-900 mb-2">Specifications</h3>
            <div className="rounded-xl border border-gray-100 overflow-hidden">
              {detail.attributes.map((attr, i) => (
                <div key={i} className={`flex text-sm ${i % 2 === 0 ? 'bg-gray-50' : 'bg-white'}`}>
                  <div className="w-2/5 px-3 py-2 text-gray-500 font-medium">{attr.name}</div>
                  <div className="flex-1 px-3 py-2 text-gray-800">{attr.value}</div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Stock */}
        <div className="flex items-center gap-2 text-sm">
          <Package size={16} className="text-gray-400" />
          <span className="text-gray-600">
            {p.stockQuantity > 0
              ? <span className="text-green-600 font-medium">In Stock ({p.stockQuantity} units)</span>
              : <span className="text-red-600 font-medium">Out of Stock</span>
            }
          </span>
        </div>

        {/* Reviews */}
        <div>
          <button
            onClick={() => setShowAllReviews(v => !v)}
            className="flex items-center gap-2 text-sm font-semibold text-gray-900 hover:text-blue-600 transition-colors"
          >
            Customer Reviews
            {showAllReviews ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
          </button>

          {showAllReviews && (
            <div className="mt-3 space-y-3">
              {reviewData?.reviews.map(review => (
                <div key={review.id} className="bg-gray-50 rounded-xl p-3 border border-gray-100">
                  <div className="flex items-center gap-2">
                    <div className="flex gap-0.5">
                      {[1,2,3,4,5].map(i => (
                        <Star key={i} size={12} className={i <= review.rating ? 'fill-amber-400 text-amber-400' : 'fill-gray-200 text-gray-200'} />
                      ))}
                    </div>
                    <span className="font-semibold text-sm text-gray-900">{review.title}</span>
                    {review.isVerifiedPurchase && (
                      <span className="text-xs text-green-600 font-medium">✓ Verified</span>
                    )}
                  </div>
                  <p className="text-xs text-gray-600 mt-1 leading-relaxed">{review.description}</p>
                  {review.helpfulCount > 0 && (
                    <p className="text-xs text-gray-400 mt-1">{review.helpfulCount} people found this helpful</p>
                  )}
                </div>
              ))}
              {reviewData?.totalPages && reviewData.totalPages > 1 && (
                <div className="flex gap-2 justify-center">
                  <button disabled={reviewPage === 0} onClick={() => setReviewPage(p => p - 1)}
                    className="text-xs px-3 py-1 border rounded-lg disabled:opacity-40 hover:border-blue-300">← Prev</button>
                  <button disabled={reviewPage >= (reviewData.totalPages - 1)} onClick={() => setReviewPage(p => p + 1)}
                    className="text-xs px-3 py-1 border rounded-lg disabled:opacity-40 hover:border-blue-300">Next →</button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
