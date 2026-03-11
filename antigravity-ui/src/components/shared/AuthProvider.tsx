'use client';

import React, { useEffect } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { useAuth } from '@/services/authStore';
import { authService } from '@/services/api';
import { Loader2 } from 'lucide-react';

// Routes that don't require authentication
const PUBLIC_ROUTES = ['/login', '/register', '/forgot-password', '/reset-password'];

export default function AuthProvider({ children }: { children: React.ReactNode }) {
    const { isAuthenticated, isValidating, login, logout, setValidating } = useAuth();
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
                    if (!isAuthenticated) {
                       login({
                           id: String(data.id),
                           name: data.username,
                           email: data.email,
                           twoFactorEnabled: data.twoFactorEnabled,
                       });
                    }
                }
            } catch (error: any) {
                if (isMounted) {
                    console.warn('[AuthProvider] Session invalid:', error.response?.status);
                    logout();
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
    useEffect(() => {
        if (isValidating) return;

        if (isAuthenticated && PUBLIC_ROUTES.includes(pathname)) {
            router.push('/dashboard');
        } else if (!isAuthenticated && !PUBLIC_ROUTES.includes(pathname)) {
            router.push('/login');
        }
    }, [isAuthenticated, isValidating, pathname, router]);

    // Show spinner during initial load (only if NOT on public route since we can eagerly show those)
    if (isValidating && !PUBLIC_ROUTES.includes(pathname)) {
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
