'use client';

import React, { useEffect } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { useAuth } from '@/services/authStore';
import { authService } from '@/services/api';
import { Loader2 } from 'lucide-react';

// Routes that don't require authentication
const PUBLIC_ROUTES = ['/login', '/register'];

export default function AuthProvider({ children }: { children: React.ReactNode }) {
    const { isAuthenticated, isValidating, login, logout, setValidating } = useAuth();
    const router = useRouter();
    const pathname = usePathname();

    useEffect(() => {
        const validateSession = async () => {
            console.log('[AuthProvider] Validating session...');
            setValidating(true);

            try {
                // Call /api/auth/me to confirm the JWT cookie is still valid with the backend
                const response = await authService.me();
                const data = response.data;

                console.log('[AuthProvider] Session valid for:', data.email);
                login({
                    id: String(data.id),
                    name: data.username,
                    email: data.email,
                    twoFactorEnabled: data.twoFactorEnabled,
                });

                // Redirect away from public routes if already logged in
                if (PUBLIC_ROUTES.includes(pathname)) {
                    router.replace('/dashboard');
                }
            } catch (error: any) {
                const status = error.response?.status;
                console.warn('[AuthProvider] Session invalid:', status);
                logout();

                // Redirect to login only if on a protected route
                if (!PUBLIC_ROUTES.includes(pathname)) {
                    router.replace('/login');
                }
            } finally {
                // Always stop validating — prevent infinite spinner
                setValidating(false);
            }
        };

        validateSession();
    }, []); // Run once on mount

    // While validating, show a full-screen spinner so the user never sees a partial state
    if (isValidating) {
        return (
            <div className="min-h-screen bg-[#F8FAFC] flex items-center justify-center">
                <div className="flex flex-col items-center gap-4">
                    <div className="w-12 h-12 rounded-2xl bg-primary flex items-center justify-center shadow-xl shadow-slate-900/20">
                        <Loader2 className="w-6 h-6 text-secondary animate-spin" />
                    </div>
                    <p className="text-slate-400 text-xs font-bold uppercase tracking-widest animate-pulse">
                        Verifying session...
                    </p>
                </div>
            </div>
        );
    }

    return <>{children}</>;
}
