"use client";

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
    BarChart3,
    CreditCard,
    Home,
    Landmark,
    Settings,
    Sparkles,
    LogOut,
    Search,
    User
} from 'lucide-react';
import { cn } from '@/components/ui/Card';
import { useAuth } from '@/services/authStore';
import { authService } from '@/services/api';
import NotificationBell from '@/components/shared/NotificationBell';

const navItems = [
    { name: 'Assistant', href: '/assistant', icon: Sparkles },
    { name: 'Dashboard', href: '/dashboard', icon: Home },
    { name: 'Transactions', href: '/transactions', icon: CreditCard },
    { name: 'Loans & EMI', href: '/dashboard/loans', icon: Landmark },
    { name: 'Analytics', href: '/analytics', icon: BarChart3 },
];

const authPages = ['/login', '/register', '/forgot-password', '/reset-password'];

export default function Shell({ children }: { children: React.ReactNode }) {
    const pathname = usePathname();
    const isAuthPage = authPages.includes(pathname);
    const { user, logout } = useAuth();

    const handleLogout = async () => {
        try {
            await authService.logout();
        } finally {
            logout();
            window.location.href = '/login';
        }
    };

    if (isAuthPage) {
        return <main className="min-h-screen bg-[#0D0B1E] grid-bg">{children}</main>;
    }

    return (
        <div className="flex h-screen bg-[#0D0B1E] grid-bg">
            {/* ── Sidebar ──────────────────────────────────────────────────────── */}
            <aside className="w-64 bg-[#0D0B1E]/95 border-r border-white/10 text-white flex flex-col p-6 backdrop-blur-xl">
                {/* Brand */}
                <div className="flex items-center gap-3 mb-10 px-2">
                    <div className="relative">
                        <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-[#D4AF37] to-[#B8962E] flex items-center justify-center shadow-gold">
                            <Sparkles className="w-5 h-5 text-[#0D0B1E]" />
                        </div>
                        <span className="absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 bg-emerald-400 rounded-full border-2 border-[#0D0B1E]" />
                    </div>
                    <div>
                        <span className="text-xl font-extrabold tracking-tight gold-text">Wealthix</span>
                        <p className="text-[10px] text-[#D4AF37]/60 font-semibold uppercase tracking-widest mt-0.5">by Jass AI</p>
                    </div>
                </div>

                {/* Nav */}
                <nav className="flex-1 space-y-1">
                    {navItems.map((item) => {
                        const isActive = item.href === '/dashboard'
                            ? pathname === item.href
                            : pathname === item.href || pathname.startsWith(item.href + '/');
                        return (
                            <Link
                                key={item.href}
                                href={item.href}
                                className={cn(
                                    "flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 group",
                                    isActive
                                        ? "bg-gradient-to-r from-[#D4AF37]/20 to-[#B8962E]/10 border border-[#D4AF37]/30 text-[#F5D67B] shadow-gold"
                                        : "text-slate-500 hover:text-slate-200 hover:bg-white/5"
                                )}
                            >
                                <item.icon className={cn(
                                    "w-5 h-5 transition-colors",
                                    isActive ? "text-[#D4AF37]" : "text-slate-600 group-hover:text-slate-300"
                                )} />
                                <span className={cn("font-semibold", isActive && "text-[#F5D67B]")}>{item.name}</span>
                                {isActive && (
                                    <span className="ml-auto w-1.5 h-1.5 rounded-full bg-[#D4AF37]" />
                                )}
                            </Link>
                        );
                    })}
                </nav>

                {/* Bottom */}
                <div className="pt-6 border-t border-white/10 space-y-1">
                    <Link
                        href="/settings"
                        className="flex items-center gap-3 px-4 py-3 rounded-xl text-slate-500 hover:text-slate-200 hover:bg-white/5 transition-all"
                    >
                        <Settings className="w-5 h-5" />
                        <span className="font-semibold">Settings</span>
                    </Link>
                    <button
                        onClick={handleLogout}
                        className="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-rose-500 hover:bg-rose-500/10 hover:text-rose-400 transition-all"
                    >
                        <LogOut className="w-5 h-5" />
                        <span className="font-semibold">Logout</span>
                    </button>
                </div>
            </aside>

            {/* ── Main Content ─────────────────────────────────────────────────── */}
            <main className="flex-1 overflow-y-auto">
                {/* Top Navbar */}
                <header className="h-16 border-b border-white/10 bg-[#0D0B1E]/80 backdrop-blur-xl px-8 flex items-center justify-between sticky top-0 z-10">
                    <div className="relative w-96">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                        <input
                            type="text"
                            placeholder="Search transactions or insights..."
                            className="pl-10 pr-4 py-2 bg-white/5 border border-white/10 rounded-xl w-full text-sm text-slate-300 placeholder:text-slate-600 focus:outline-none focus:ring-2 focus:ring-[#D4AF37]/30 focus:border-[#D4AF37]/40 transition-all"
                        />
                    </div>

                    <div className="flex items-center gap-4">
                        <NotificationBell />
                        <div className="h-8 w-[1px] bg-white/10 mx-2" />
                        <div className="flex items-center gap-3 pl-2">
                            <div className="text-right hidden sm:block">
                                <p className="text-sm font-bold text-slate-200 leading-none">{user?.name || 'User'}</p>
                                <p className="text-[10px] font-semibold uppercase tracking-wider mt-1 gold-text">Premium Plan</p>
                            </div>
                            <div className="h-10 w-10 rounded-full bg-[#1E1B4B] border-2 border-white/10 overflow-hidden shadow-sm hover:ring-2 hover:ring-[#D4AF37]/40 transition-all cursor-pointer">
                                <div className="w-full h-full flex items-center justify-center">
                                    <User className="text-[#D4AF37] w-5 h-5" />
                                </div>
                            </div>
                        </div>
                    </div>
                </header>

                {/* Dynamic Content */}
                {children}
            </main>
        </div>
    );
}
