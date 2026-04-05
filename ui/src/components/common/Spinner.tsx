export default function Spinner({ size = 'md' }: { size?: 'sm' | 'md' | 'lg' }) {
  const sz = { sm: 'w-4 h-4', md: 'w-8 h-8', lg: 'w-12 h-12' }[size];
  return (
    <div className={`${sz} border-4 border-orange-200 border-t-orange-500 rounded-full animate-spin`} />
  );
}

export function LoadingCenter() {
  return (
    <div className="flex justify-center items-center h-48">
      <Spinner size="lg" />
    </div>
  );
}
