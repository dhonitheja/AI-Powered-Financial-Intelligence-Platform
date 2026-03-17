'use client';

import React, { useEffect } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { useAuth } from '@/services/authStore';
import { authService } from '@/services/api';
import { Loader2 } from 'lucide-react';

// Routes that don't require authentication
const PUBLIC_ROUTES = ['/login', '/register', '/forgot-password', '/reset-password'];

export default function AuthProvider({ children }: { children: React.ReactNode }) {
    const { isAuthenticated, isValidating, login, logout, setValidating, _hasHydrated } = useAuth();
    const router = useRouter();
    const pathname = usePathname();

    useEffect(() => {
        let isMounted = true;

        const validateSession = async () => {
            if (!PUBLIC_ROUTES.includes(pathname)) {
                console.log('[AuthProvider] Validating session...', pathname);
            }
            
            setValidating(true);

            try {
                // Call /api/auth/me to confirm the JWT cookie is still valid with the backend
                const response = await authService.me();
                const data = response.data;

                if (isMounted) {
                    const currentToken = useAuth.getState().user?.token;
                    login({
                        id: String(data.id),
                        name: data.username,
                        email: data.email,
                        twoFactorEnabled: data.twoFactorEnabled,
                        token: data.token || currentToken,
                    });
                }
            } catch (error: any) {
                if (isMounted) {
                    const status = error.response?.status;
                    console.warn('[AuthProvider] Session validation error:', status ?? 'network error');
                    if (status === 401) {
                        console.warn('[AuthProvider] Session invalid (401) – User is not logged in.');
                        logout();
                    } else if (status === 403) {
                        console.error('[AuthProvider] Session access forbidden (403) – Check CORS/Security.');
                    } else {
                        console.error('[AuthProvider] Network or Server Error:', status || 'CONNECTION_FAILED');
                    }
                    setValidating(false);
                }
            } finally {
                if (isMounted) {
                    setValidating(false);
                }
            }
        };

        validateSession();

        return () => {
            isMounted = false;
        };
    }, []); // Only run once on mount

    // Handle routing logic safely inside useEffect
    // Gate on _hasHydrated so we don't redirect before zustand-persist finishes reading localStorage
    useEffect(() => {
        if (!_hasHydrated) return;
        if (isValidating) return;

        if (isAuthenticated && PUBLIC_ROUTES.includes(pathname)) {
            router.push('/dashboard');
        } else if (!isAuthenticated && !PUBLIC_ROUTES.includes(pathname)) {
            router.push('/login');
        }
    }, [isAuthenticated, isValidating, pathname, router, _hasHydrated]);

    // Before hydration: render a plain dark screen — no spinner, no redirect.
    // This prevents a flash of "Verifying session..." on every page load for users with a valid session.
    if (!_hasHydrated) {
        return <div className="min-h-screen bg-[#0D0B1E]" />;
    }

    // Show spinner only when hydrated but auth state is unknown (not authenticated and still validating).
    // If already authenticated from persisted state, render children immediately — validate silently in background.
    if (isValidating && !isAuthenticated && !PUBLIC_ROUTES.includes(pathname)) {
        return (
            <div className="min-h-screen bg-[#0D0B1E] flex items-center justify-center">
                <div className="flex flex-col items-center gap-4">
                    <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-[#D4AF37] to-[#B8962E] flex items-center justify-center shadow-gold-lg">
                        <Loader2 className="w-6 h-6 text-[#0D0B1E] animate-spin" />
                    </div>
                    <p className="text-slate-500 text-xs font-bold uppercase tracking-widest animate-pulse">
                        Verifying session...
                    </p>
                </div>
            </div>
        );
    }

    return <>{children}</>;
}
