import axios from 'axios';
import { useAuth } from './authStore';

const api = axios.create({
    baseURL: '/api',
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true, // Send HTTP-only cookies on every request
});

// ─── Request Interceptor ───────────────────────────────────────────────────
api.interceptors.request.use(
    (config) => {
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

        if (status === 401 && !isAuthCall) {
            console.warn('[API] Session expired or invalid – clearing state and redirecting to /login');
            useAuth.getState().logout();
            setTimeout(() => {
                window.location.href = '/login';
            }, 100);
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
    createLinkToken: (userId: string = 'default-user') =>
        api.post('/plaid/create-link-token', { userId }),
    exchangePublicToken: (publicToken: string, userId: string = 'default-user') =>
        api.post('/plaid/exchange-public-token', { public_token: publicToken, userId }),
    syncTransactions: (userId: string = 'default-user') =>
        api.post('/plaid/sync', { userId }),
    /** Returns totalAssets, totalLiabilities, netWorth, creditUtilization, accounts[] */
    getAccountSummary: () => api.get('/plaid/accounts'),
    /** Lightweight polling endpoint — returns { accountCount, lastSyncedAt } — no userId needed */
    getSyncStatus: () => api.get('/plaid/sync-status'),
};

// ─── AI Chat Service ──────────────────────────────────────────────────────────
export const chatService = {
    /**
     * Sends a chat message. userId is resolved server-side from JWT.
     * Financial context is assembled server-side and injected into the prompt.
     */
    sendMessage: (message: string) =>
        api.post('/ai/chat', { message }),
};



