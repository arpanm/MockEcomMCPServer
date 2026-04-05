import { HashRouter, Routes, Route, NavLink } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { UtensilsCrossed, ShoppingBag, Home, Activity } from 'lucide-react';
import HomePage from './pages/HomePage';
import RestaurantsPage from './pages/RestaurantsPage';
import ProductsPage from './pages/ProductsPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30000,
      retry: 1,
    }
  }
});

function Navbar() {
  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all ${
      isActive
        ? 'bg-orange-500 text-white shadow-sm'
        : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
    }`;

  return (
    <header className="h-[60px] bg-white border-b border-gray-100 px-5 flex items-center justify-between shadow-sm shrink-0">
      <div className="flex items-center gap-3">
        <span className="text-2xl">🛒</span>
        <div>
          <span className="font-bold text-gray-900">MockEcom</span>
          <span className="text-gray-400 text-xs ml-2">Data Explorer</span>
        </div>
      </div>

      <nav className="flex items-center gap-1">
        <NavLink to="/" end className={linkClass}>
          <Home size={15} />
          <span className="hidden sm:inline">Home</span>
        </NavLink>
        <NavLink to="/restaurants" className={linkClass}>
          <UtensilsCrossed size={15} />
          <span className="hidden sm:inline">Restaurants</span>
        </NavLink>
        <NavLink to="/products" className={linkClass}>
          <ShoppingBag size={15} />
          <span className="hidden sm:inline">Products</span>
        </NavLink>
        <a
          href="/h2-console"
          target="_blank"
          rel="noopener noreferrer"
          className="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium text-gray-500 hover:text-gray-700 hover:bg-gray-100 transition-all"
        >
          <Activity size={15} />
          <span className="hidden sm:inline">DB</span>
        </a>
      </nav>
    </header>
  );
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <HashRouter>
        <div className="flex flex-col h-screen bg-gray-50">
          <Navbar />
          <div className="flex-1 overflow-hidden">
            <Routes>
              <Route path="/" element={<HomePage />} />
              <Route path="/restaurants" element={<RestaurantsPage />} />
              <Route path="/products" element={<ProductsPage />} />
            </Routes>
          </div>
        </div>
      </HashRouter>
    </QueryClientProvider>
  );
}
