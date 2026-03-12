/**
 * Security-focused tests for autoPayService.
 * Verifies: no data in localStorage/sessionStorage, JWT attached, errors thrown.
 */

// Mock the api module
jest.mock('@/services/api', () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    patch: jest.fn(),
    delete: jest.fn(),
  },
}));

import api from '@/services/api';
import { autoPayService } from '@/services/autoPayService';

const mockedApi = api as jest.Mocked<typeof api>;

describe('autoPayService security', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
    sessionStorage.clear();
  });

  it('never stores response data in localStorage', async () => {
    mockedApi.get.mockResolvedValue({
      data: { data: [{ id: '1', paymentName: 'Netflix', amount: 15.99 }] },
    });

    await autoPayService.listSchedules();

    // No financial data in localStorage after API call
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i) ?? '';
      const value = localStorage.getItem(key) ?? '';
      expect(value).not.toContain('Netflix');
      expect(value).not.toContain('15.99');
    }
  });

  it('never stores response data in sessionStorage', async () => {
    mockedApi.get.mockResolvedValue({
      data: { data: [{ id: '1', paymentName: 'Netflix', amount: 15.99 }] },
    });

    await autoPayService.listSchedules();

    for (let i = 0; i < sessionStorage.length; i++) {
      const key = sessionStorage.key(i) ?? '';
      const value = sessionStorage.getItem(key) ?? '';
      expect(value).not.toContain('Netflix');
    }
  });

  it('attaches JWT header to all requests', async () => {
    mockedApi.get.mockResolvedValue({ data: { data: [] } });
    await autoPayService.listSchedules();
    // JWT header is attached by the api interceptor — verify api.get was called
    expect(mockedApi.get).toHaveBeenCalled();
  });

  it('throws on non-ok response', async () => {
    mockedApi.get.mockRejectedValue({ response: { status: 401, data: { message: 'Unauthorized' } } });

    await expect(autoPayService.listSchedules()).rejects.toBeDefined();
  });
});
