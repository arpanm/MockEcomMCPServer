export interface City {
  id: number;
  name: string;
  state: string;
  restaurantCount: number;
  restaurantsScraped: boolean;
  lastScrapedAt: string | null;
}

export interface Restaurant {
  id: number;
  swiggyId: string;
  name: string;
  city: string;
  locality: string;
  areaName: string;
  cuisines: string;
  avgRating: number;
  totalRatings: string;
  costForTwo: number;
  costForTwoMessage: string;
  deliveryTime: number;
  isOpen: boolean;
  isPureVeg: boolean;
  discount: string;
  imageUrl: string | null;
  menuScraped: boolean;
}

export interface MenuItem {
  id: number;
  name: string;
  description: string;
  price: number;
  isVeg: boolean;
  inStock: boolean;
  isBestSeller: boolean;
  imageUrl: string | null;
  avgRating: string | null;
}

export interface MenuCategory {
  category: string;
  itemCount: number;
  items: MenuItem[];
}

export interface RestaurantMenu {
  restaurantId: string;
  restaurantName: string;
  city: string;
  totalCategories: number;
  totalItems: number;
  menu: MenuCategory[];
}

export interface MenuItemResult extends MenuItem {
  restaurantName: string;
  restaurantSwiggyId: string;
  cityName: string;
}

export interface Product {
  id: string;
  title: string;
  category: string;
  subCategory: string;
  brand: string;
  model: string;
  price: number;
  mrp: number;
  averageRating: number;
  reviewCount: number;
  stockQuantity: number;
  imageUrl: string;
  description: string;
}

export interface ProductDetail extends Product {
  size: string;
  color: string;
  material: string;
  weight: string;
  additionalImages: string;
  currency: string;
  attributes: { name: string; value: string }[];
}

export interface ProductReview {
  id: string;
  rating: number;
  title: string;
  description: string;
  isVerifiedPurchase: boolean;
  helpfulCount: number;
  createdAt: string;
}

export interface ProductFilters {
  categories: { name: string; count: number }[];
  subCategories: { name: string; count: number }[];
  brands: { name: string; count: number }[];
}

export interface PagedResponse<T> {
  page: number;
  size?: number;
  pageSize?: number;
  totalElements: number;
  totalPages: number;
}

export interface ScraperStatus {
  totalCities: number;
  scrapedCities: number;
  pendingCities: number;
  totalRestaurants: number;
  restaurantsWithMenu: number;
  restaurantsPendingMenu: number;
  restaurantScrapingRunning: boolean;
  menuScrapingRunning: boolean;
}
