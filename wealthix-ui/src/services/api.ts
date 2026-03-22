import axios from 'axios';
import { useAuth } from './authStore';

const api = axios.create({
    // Prefer relative '/api' for Next.js rewrites (handles CORS automatically)
    // Fallback to absolute URL if needed for specific environments
    baseURL: process.env.NEXT_PUBLIC_API_URL || '/api',
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true,
    timeout: 10000,
});

// ─── Request Interceptor ───────────────────────────────────────────────────
api.interceptors.request.use(
    (config) => {
        const token = useAuth.getState().user?.token;
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        
        console.log(`[API] ${config.method?.toUpperCase()} → ${config.url}`);
        return config;
    },
    (error) => {
        console.error('[API] Request setup error:', error);
        return Promise.reject(error);
    }
);

// ─── Response Interceptor ──────────────────────────────────────────────────
api.interceptors.response.use(
    (response) => {
        console.log(`[API] ${response.status} ← ${response.config.url}`);
        
        // Unwrap backend's ApiResponse envelope if present
        if (response.data && typeof response.data.success === 'boolean' && 'data' in response.data) {
            console.log(`[API] Unwrapping ApiResponse from ${response.config.url}`);
            response.data = response.data.data;
        }
        
        return response;
    },
    (error) => {
        const status = error.response?.status;
        const url = error.config?.url ?? '';

        console.error(`[API] Error ${status} ← ${url}`, error.message);

        // On 401, clear auth state and redirect to login
        // Exclude these endpoints: they handle their own 401 logic
        const isAuthCall = url.includes('/auth/signin')
            || url.includes('/auth/verify-2fa')
            || url.includes('/auth/me'); // AuthProvider handles /me failures itself

        if (status === 403) {
            console.error('[API] 403 Forbidden – Likely a CORS mismatch or CSRF issue. Check backend allowed origins.');
        }

        if (status === 401 && !isAuthCall) {
            console.warn('[API] Session expired or invalid – clearing auth state');
            useAuth.getState().logout();
        }

        return Promise.reject(error);
    }
);

export default api;

// ─── Auth Service ──────────────────────────────────────────────────────────
export const authService = {
    /** Login with email + password. Returns 200 on success, 202 if 2FA required. */
    login: (data: { email: string; password: string }) => api.post('/auth/signin', data),

    /** Register a new account. */
    register: (data: any) => api.post('/auth/signup', data),

    /** Request a password reset. */
    forgotPassword: (data: { email: string }) => api.post('/auth/forgot-password', data),

    /** Reset password using the token sent to email. */
    resetPassword: (data: { token: string; newPassword: string }) => api.post('/auth/reset-password', data),

    /** Clear the JWT cookie on the backend. */
    logout: () => api.post('/auth/logout'),

    /**
     * Validate the current session against the backend.
     * Call this on app startup. Returns 200 + user info if valid, 401 if not.
     */
    me: () => api.get('/auth/me'),

    /** Submit a TOTP code after password verification. */
    verify2FA: (data: { email: string; code: string }) => api.post('/auth/verify-2fa', data),

    /** Generate a new TOTP secret and QR code URI. Requires authentication. */
    setup2FA: () => api.get('/auth/2fa/setup'),

    /** Enable or disable 2FA for the current user. Requires authentication. */
    toggle2FA: (enable: boolean) => api.post('/auth/2fa/toggle', { enable }),
};

// ─── Transaction Service ───────────────────────────────────────────────────
export const transactionService = {
    getAll: (period?: string, type?: string) => {
        const p: Record<string, string> = {};
        if (period) p['period'] = period;
        if (type) p['type'] = type;
        return api.get('/transactions', { params: p });
    },
    getById: (id: string) => api.get(`/transactions/${id}`),
    create: (data: any) => api.post('/transactions', data),
    getSummary: (period?: string, type?: string) => {
        const p: Record<string, string> = {};
        if (period) p['period'] = period;
        if (type) p['type'] = type;
        return api.get('/summary', { params: p });
    },
    triggerAnalysis: (id: string) => api.post(`/transactions/${id}/analyze`),
    askAssistant: (query: string) => api.post('/assistant', { message: query }),
};


// ─── Plaid Service ─────────────────────────────────────────────────────────
export const plaidService = {
    createLinkToken: () =>
        api.post('/v1/plaid/link-token'),
    exchangePublicToken: (publicToken: string) =>
        api.post('/v1/plaid/exchange', { publicToken }),
    syncTransactions: () =>
        api.post('/v1/plaid/sync'),
    /** Returns totalAssets, totalLiabilities, netWorth, creditUtilization, accounts[] */
    getAccountSummary: () => api.get('/v1/plaid/accounts'),
    /** Lightweight polling endpoint — returns { accountCount, lastSyncedAt } — no userId needed */
    getSyncStatus: () => api.get('/v1/plaid/sync-status'),
    /** Returns all linked bank/credit connections for the authenticated user */
    getConnections: () => api.get('/v1/plaid/connections'),
    /** Disconnects a linked bank account by connection ID */
    disconnectBank: (connectionId: string) => api.delete(`/v1/plaid/connections/${connectionId}`),
};

// ─── Budget Service ─────────────────────────────────────────────────────────
export type BudgetPeriod = 'WEEKLY' | 'MONTHLY';

export interface CategoryBudget {
    id: string;
    category: string;
    limitAmount: number;
    period: BudgetPeriod;
    active: boolean;
    createdAt: string;
}

export const budgetService = {
    getAll: () => api.get<CategoryBudget[]>('/v1/budgets'),
    create: (data: { category: string; limitAmount: number; period: BudgetPeriod }) =>
        api.post<CategoryBudget>('/v1/budgets', data),
    update: (id: string, data: { category: string; limitAmount: number; period: BudgetPeriod }) =>
        api.put<CategoryBudget>(`/v1/budgets/${id}`, data),
    remove: (id: string) => api.delete(`/v1/budgets/${id}`),
};

// ─── Analytics Service ──────────────────────────────────────────────────────
export interface CategoryComparison {
    category: string;
    currentPeriodSpend: number;
    previousPeriodSpend: number;
    changePercent: number;
}

export interface ComparisonData {
    period: string;
    currentPeriodLabel: string;
    previousPeriodLabel: string;
    currentTotal: number;
    previousTotal: number;
    categories: CategoryComparison[];
}

export const analyticsService = {
    getComparison: (period: 'weekly' | 'monthly') =>
        api.get<ComparisonData>('/analytics/comparison', { params: { period } }),
    getRangeSpending: (startDate: string, endDate: string) =>
        api.get('/summary/range', { params: { startDate, endDate } }),
};

// ─── AI Chat & Analytics Service ──────────────────────────────────────────────
export const chatService = {
    /**
     * Sends a chat message. userId is resolved server-side from JWT.
     */
    sendMessage: (data: { sessionId: string; message: string }) =>
        api.post('/v1/ai/chat', data),

    getAnomalies: () => api.get('/v1/ai/anomalies'),
    acknowledgeAnomaly: (id: string) => api.post(`/v1/ai/anomalies/${id}/acknowledge`),

    getForecast: () => api.get('/v1/ai/forecast'),

    getBudgetRecommendations: () => api.get('/v1/ai/budget-recommendations'),

    getSavingsGoals: () => api.get('/v1/ai/savings-goals'),
    createSavingsGoal: (data: any) => api.post('/v1/ai/savings-goals', data),

    /** Fetches the latest Jass report/Wealth Tip from the DB */
    getLatestInsight: () => api.get('/ai/latest-insight'),
};

// ─── Gamification Service ───────────────────────────────────────────────────
export const gamificationService = {
    getProfile: () => api.get('/v1/gamification/profile'),
};

// ─── Export Service ─────────────────────────────────────────────────────────
export const exportService = {
    exportCsv: (period?: string) => api.get('/v1/export/csv', { params: { period }, responseType: 'blob' }),
    exportPdf: (period?: string) => api.get('/v1/export/pdf', { params: { period }, responseType: 'blob' })
};
