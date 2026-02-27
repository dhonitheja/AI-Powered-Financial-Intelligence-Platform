import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface User {
    id: string;
    name: string;
    email: string;
    twoFactorEnabled?: boolean;
}

interface AuthState {
    user: User | null;
    isAuthenticated: boolean;
    isValidating: boolean;
    login: (user: User) => void;
    logout: () => void;
    setValidating: (val: boolean) => void;
}

export const useAuth = create<AuthState>()(
    persist(
        (set) => ({
            user: null,
            isAuthenticated: false,
            isValidating: true,
            login: (user) => {
                set({ user, isAuthenticated: true, isValidating: false });
            },
            logout: () => {
                set({ user: null, isAuthenticated: false, isValidating: false });
            },
            setValidating: (val) => {
                set({ isValidating: val });
            },
        }),
        {
            name: 'antigravity-auth-storage',
            // Only persist user and isAuthenticated — not isValidating
            partialize: (state) => ({
                user: state.user,
                isAuthenticated: state.isAuthenticated,
            }),
        }
    )
);
