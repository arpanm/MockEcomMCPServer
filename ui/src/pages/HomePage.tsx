import { useQuery } from '@tanstack/react-query';
import { Building2, UtensilsCrossed, BookOpen, Activity, RefreshCw } from 'lucide-react';
import { fetchStats, fetchScraperStatus } from '../api/restaurants';
import { LoadingCenter } from '../components/common/Spinner';
import { Link } from 'react-router-dom';

interface StatCardProps {
  icon: React.ReactNode;
  label: string;
  value: number | string;
  sub?: string;
  color: string;
}

function StatCard({ icon, label, value, sub, color }: StatCardProps) {
  return (
    <div className="card p-5">
      <div className="flex items-center gap-3">
        <div className={`p-3 rounded-xl ${color}`}>{icon}</div>
        <div>
          <p className="text-2xl font-bold text-gray-900">{value?.toLocaleString()}</p>
          <p className="text-sm font-medium text-gray-600">{label}</p>
          {sub && <p className="text-xs text-gray-400 mt-0.5">{sub}</p>}
        </div>
      </div>
    </div>
  );
}

export default function HomePage() {
  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['stats'],
    queryFn: fetchStats,
    refetchInterval: 10000,
  });

  const { data: scraper } = useQuery({
    queryKey: ['scraper-status'],
    queryFn: fetchScraperStatus,
    refetchInterval: 5000,
  });

  if (statsLoading) return <LoadingCenter />;

  return (
    <div className="max-w-5xl mx-auto py-8 px-4 space-y-8">
      {/* Hero */}
      <div className="text-center">
        <h1 className="text-3xl font-bold text-gray-900">MockEcom Explorer</h1>
        <p className="text-gray-500 mt-2 text-lg">
          Browse restaurants, menus, and products — all driven by live scraped & generated data
        </p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard
          icon={<Building2 size={22} className="text-orange-600" />}
          label="Cities"
          value={stats?.totalCities ?? 0}
          sub={`${stats?.scrapedCities ?? 0} scraped`}
          color="bg-orange-100"
        />
        <StatCard
          icon={<UtensilsCrossed size={22} className="text-green-600" />}
          label="Restaurants"
          value={stats?.totalRestaurants ?? 0}
          sub={`${stats?.restaurantsWithMenu ?? 0} with menus`}
          color="bg-green-100"
        />
        <StatCard
          icon={<BookOpen size={22} className="text-purple-600" />}
          label="Menu Items"
          value={stats?.totalMenuItems ?? 0}
          sub="across all restaurants"
          color="bg-purple-100"
        />
        <StatCard
          icon={<Activity size={22} className="text-blue-600" />}
          label="Products"
          value="∞"
          sub="mock-generated"
          color="bg-blue-100"
        />
      </div>

      {/* Quick links */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        <Link to="/restaurants" className="card p-6 group block hover:border-orange-200 border border-transparent transition-colors">
          <div className="flex items-start gap-4">
            <span className="text-4xl">🍽️</span>
            <div>
              <h2 className="text-lg font-bold text-gray-900 group-hover:text-orange-600 transition-colors">Restaurant Explorer</h2>
              <p className="text-sm text-gray-500 mt-1">
                Browse {stats?.totalRestaurants ?? 0} restaurants across {stats?.scrapedCities ?? 0} cities.
                Filter by city, cuisine, veg preference. Search menus.
              </p>
              <div className="flex flex-wrap gap-1.5 mt-3">
                {['City Filter', 'Cuisine Filter', 'Menu Search', 'Veg Only'].map(t => (
                  <span key={t} className="text-xs bg-orange-50 text-orange-600 border border-orange-100 px-2 py-0.5 rounded-full">{t}</span>
                ))}
              </div>
            </div>
          </div>
        </Link>

        <Link to="/products" className="card p-6 group block hover:border-blue-200 border border-transparent transition-colors">
          <div className="flex items-start gap-4">
            <span className="text-4xl">🛍️</span>
            <div>
              <h2 className="text-lg font-bold text-gray-900 group-hover:text-blue-600 transition-colors">Product Explorer</h2>
              <p className="text-sm text-gray-500 mt-1">
                Explore mock-generated products across Electronics, Fashion, Grocery, Beauty, and Home.
                Full search with filters.
              </p>
              <div className="flex flex-wrap gap-1.5 mt-3">
                {['Electronics', 'Fashion', 'Grocery', 'Beauty', 'Home'].map(t => (
                  <span key={t} className="text-xs bg-blue-50 text-blue-600 border border-blue-100 px-2 py-0.5 rounded-full">{t}</span>
                ))}
              </div>
            </div>
          </div>
        </Link>
      </div>

      {/* Scraper status */}
      {scraper && (
        <div className="card p-5">
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-bold text-gray-900">Swiggy Scraper Status</h3>
            <div className="flex items-center gap-2">
              {(scraper.restaurantScrapingRunning || scraper.menuScrapingRunning) && (
                <div className="flex items-center gap-1.5 text-xs text-orange-600 font-medium bg-orange-50 px-2.5 py-1 rounded-full border border-orange-200">
                  <RefreshCw size={12} className="animate-spin" />
                  Running
                </div>
              )}
            </div>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            {[
              { label: 'Cities Scraped', value: `${scraper.scrapedCities} / ${scraper.totalCities}`, pct: scraper.scrapedCities / scraper.totalCities, color: 'bg-orange-500' },
              { label: 'Restaurants', value: scraper.totalRestaurants, pct: null, color: 'bg-green-500' },
              { label: 'Menus Scraped', value: `${scraper.restaurantsWithMenu} / ${scraper.totalRestaurants}`,
                pct: scraper.totalRestaurants > 0 ? scraper.restaurantsWithMenu / scraper.totalRestaurants : 0, color: 'bg-purple-500' },
            ].map(item => (
              <div key={item.label}>
                <div className="flex justify-between text-sm mb-1">
                  <span className="text-gray-600">{item.label}</span>
                  <span className="font-semibold text-gray-900">{item.value}</span>
                </div>
                {item.pct !== null && (
                  <div className="h-1.5 bg-gray-100 rounded-full overflow-hidden">
                    <div className={`h-full ${item.color} rounded-full transition-all duration-500`}
                      style={{ width: `${Math.round(item.pct * 100)}%` }} />
                  </div>
                )}
              </div>
            ))}
          </div>

          <p className="text-xs text-gray-400 mt-4">
            To scrape live Swiggy data, use the MCP tool <code className="bg-gray-100 px-1 rounded">startRestaurantScraping</code> from Claude or another MCP client.
            The scraper runs in the background and this dashboard auto-refreshes.
          </p>
        </div>
      )}
    </div>
  );
}
