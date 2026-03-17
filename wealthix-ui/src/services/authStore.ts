import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface User {
    id: string;
    name: string;
    email: string;
    twoFactorEnabled?: boolean;
    token?: string;
}

interface AuthState {
    user: User | null;
    isAuthenticated: boolean;
    isValidating: boolean;
    /** True once zustand-persist has finished reading from localStorage */
    _hasHydrated: boolean;
    login: (user: User) => void;
    logout: () => void;
    setValidating: (val: boolean) => void;
    setHasHydrated: (val: boolean) => void;
}

export const useAuth = create<AuthState>()(
    persist(
        (set) => ({
            user: null,
            isAuthenticated: false,
            isValidating: true,
            _hasHydrated: false,
            login: (user) => set({ user, isAuthenticated: true, isValidating: false }),
            logout: () => set({ user: null, isAuthenticated: false, isValidating: false }),
            setValidating: (val) => set({ isValidating: val }),
            setHasHydrated: (val) => set({ _hasHydrated: val }),
        }),
        {
            name: 'antigravity-auth-storage',
            // Only persist user and isAuthenticated — not isValidating or _hasHydrated
            partialize: (state) => ({
                user: state.user,
                isAuthenticated: state.isAuthenticated,
            }),
            onRehydrateStorage: () => (state) => {
                state?.setHasHydrated(true);
            },
        }
    )
);
