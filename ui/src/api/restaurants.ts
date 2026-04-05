import client from './client';
import type { City, Restaurant, RestaurantMenu, MenuItemResult, ScraperStatus } from '../types';

export interface RestaurantSearchParams {
  cityName?: string;
  cuisine?: string;
  name?: string;
  isPureVeg?: boolean;
  page?: number;
  size?: number;
}

export interface RestaurantPage {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  restaurants: Restaurant[];
}

export interface MenuItemPage {
  page: number;
  totalElements: number;
  totalPages: number;
  items: MenuItemResult[];
}

export const fetchCities = async (): Promise<{ cities: City[]; total: number }> => {
  const { data } = await client.get('/cities');
  return data;
};

export const fetchCuisines = async (cityName?: string): Promise<string[]> => {
  const { data } = await client.get('/cuisines', {
    params: cityName ? { cityName } : {}
  });
  return data;
};

export const searchRestaurants = async (params: RestaurantSearchParams): Promise<RestaurantPage> => {
  const { data } = await client.get('/restaurants', { params });
  return data;
};

export const fetchRestaurant = async (swiggyId: string): Promise<Restaurant> => {
  const { data } = await client.get(`/restaurants/${swiggyId}`);
  return data;
};

export const fetchRestaurantMenu = async (swiggyId: string): Promise<RestaurantMenu> => {
  const { data } = await client.get(`/restaurants/${swiggyId}/menu`);
  return data;
};

export const searchMenuItems = async (
  q: string,
  cityName?: string,
  page = 0,
  size = 20
): Promise<MenuItemPage> => {
  const { data } = await client.get('/menu-items', {
    params: { q, cityName, page, size }
  });
  return data;
};

export const fetchScraperStatus = async (): Promise<ScraperStatus> => {
  const { data } = await client.get('/scraper/status');
  return data;
};

export const fetchStats = async () => {
  const { data } = await client.get('/stats');
  return data;
};
