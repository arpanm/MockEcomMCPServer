import { Star } from 'lucide-react';

export default function StarRating({
  rating,
  max = 5,
  size = 'sm'
}: {
  rating: number;
  max?: number;
  size?: 'sm' | 'md';
}) {
  const px = size === 'sm' ? 12 : 16;
  return (
    <div className="flex items-center gap-0.5">
      {Array.from({ length: max }, (_, i) => (
        <Star
          key={i}
          size={px}
          className={i < Math.round(rating) ? 'fill-amber-400 text-amber-400' : 'fill-gray-200 text-gray-200'}
        />
      ))}
      <span className="ml-1 text-xs text-gray-500">{rating?.toFixed(1)}</span>
    </div>
  );
}
