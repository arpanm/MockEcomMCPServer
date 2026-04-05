import client from './client';
import type { Product, ProductDetail, ProductReview, ProductFilters } from '../types';

export interface ProductSearchParams {
  q?: string;
  category?: string;
  subCategory?: string;
  brand?: string;
  page?: number;
  size?: number;
}

export interface ProductPage {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  products: Product[];
}

export interface ReviewPage {
  page: number;
  totalElements: number;
  totalPages: number;
  reviews: ProductReview[];
  averageRating: number;
}

export const searchProducts = async (params: ProductSearchParams): Promise<ProductPage> => {
  const { data } = await client.get('/products', { params });
  return data;
};

export const fetchProductFilters = async (q = ''): Promise<ProductFilters> => {
  const { data } = await client.get('/products/filters', { params: { q } });
  return data;
};

export const fetchProduct = async (productId: string): Promise<ProductDetail> => {
  const { data } = await client.get(`/products/${productId}`);
  return data;
};

export const fetchProductReviews = async (
  productId: string,
  page = 0,
  size = 10
): Promise<ReviewPage> => {
  const { data } = await client.get(`/products/${productId}/reviews`, {
    params: { page, size }
  });
  return data;
};
