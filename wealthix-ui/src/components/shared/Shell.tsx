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
        return (
            <main className="min-h-screen relative overflow-hidden">
                <div className="mesh-glow" />
                <div className="relative z-10 h-full">{children}</div>
            </main>
        );
    }

    return (
        <div className="flex h-screen overflow-hidden font-sans">
            <div className="mesh-glow" />
            
            {/* ── Sidebar ──────────────────────────────────────────────────────── */}
            <aside className="w-[280px] h-screen bg-black/40 backdrop-blur-3xl border-r border-white/5 text-white flex flex-col p-8 sticky top-0 z-50 overflow-hidden premium-border">
                {/* Branding Fluid Glow */}
                <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-transparent via-blue-500/50 to-transparent opacity-50" />
                
                {/* Brand */}
                <div className="flex items-center gap-4 mb-12 px-2 group cursor-pointer">
                    <div className="relative">
                        <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-blue-600 via-indigo-600 to-purple-600 flex items-center justify-center shadow-[0_0_30px_rgba(37,99,235,0.4)] group-hover:scale-105 transition-transform duration-500 premium-border">
                            <Sparkles className="w-6 h-6 text-white animate-pulse" />
                        </div>
                        <div className="absolute -inset-1 bg-blue-500/20 blur-xl rounded-full opacity-0 group-hover:opacity-100 transition-opacity" />
                    </div>
                    <div>
                        <span className="text-2xl font-black tracking-tight text-gradient-gold">Wealthix</span>
                        <p className="text-[10px] text-blue-400 font-black uppercase tracking-[0.2em] mt-0.5">by Jass AI</p>
                    </div>
                </div>

                {/* Nav */}
                <nav className="flex-1 space-y-2">
                    {navItems.map((item) => {
                        const isActive = item.href === '/dashboard'
                            ? pathname === item.href
                            : pathname === item.href || pathname.startsWith(item.href + '/');
                        return (
                            <Link
                                key={item.href}
                                href={item.href}
                                className={cn(
                                    "flex items-center gap-4 px-4 py-4 rounded-xl transition-all duration-300 group relative",
                                    isActive
                                        ? "bg-white/[0.08] text-white shadow-[inset_0_0_20px_rgba(255,255,255,0.05)] border border-white/10 premium-border"
                                        : "text-slate-400 hover:text-white hover:bg-white/[0.04]"
                                )}
                            >
                                {isActive && (
                                    <div className="absolute left-0 w-1 h-6 bg-blue-500 rounded-r-full shadow-[0_0_10px_rgba(59,130,246,0.8)]" />
                                )}
                                <item.icon className={cn(
                                    "w-5 h-5 transition-all duration-300",
                                    isActive ? "text-blue-400 drop-shadow-[0_0_8px_rgba(59,130,246,0.5)]" : "text-slate-500 group-hover:text-slate-300"
                                )} />
                                <span className={cn("font-bold tracking-wide text-sm transition-colors", isActive ? "text-white" : "group-hover:text-white")}>
                                    {item.name}
                                </span>
                            </Link>
                        );
                    })}
                </nav>

                {/* Bottom Profile/Settings */}
                <div className="mt-auto pt-8 border-t border-white/5 space-y-2">
                    <Link
                        href="/settings"
                        className="flex items-center gap-4 px-4 py-4 rounded-xl text-slate-400 hover:text-white transition-all group"
                    >
                        <Settings className="w-5 h-5 group-hover:rotate-45 transition-transform duration-500" />
                        <span className="font-bold tracking-wide text-sm">Design Suite</span>
                    </Link>
                    <button
                        onClick={handleLogout}
                        className="w-full flex items-center gap-4 px-4 py-4 rounded-xl text-rose-500/80 hover:bg-rose-500/10 hover:text-rose-400 transition-all group"
                    >
                        <LogOut className="w-5 h-5 group-hover:-translate-x-1 transition-transform" />
                        <span className="font-bold tracking-wide text-sm">Sign Out</span>
                    </button>
                </div>
            </aside>

            {/* ── Main Content ─────────────────────────────────────────────────── */}
            <main className="flex-1 overflow-y-auto relative scroll-smooth">
                {/* Top Navbar */}
                <header className="h-20 border-b border-white/5 bg-black/20 backdrop-blur-3xl px-12 flex items-center justify-between sticky top-0 z-40 transition-all duration-500">
                    <div className="relative w-[400px] group">
                        <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500 group-focus-within:text-blue-400 transition-colors" />
                        <input
                            type="text"
                            placeholder="Universal Search..."
                            className="h-11 pl-12 pr-4 bg-white/[0.03] border border-white/5 rounded-xl w-full text-sm text-slate-300 placeholder:text-slate-600 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500/40 focus:bg-white/[0.06] transition-all"
                        />
                    </div>

                    <div className="flex items-center gap-6">
                        <NotificationBell />
                        
                        <div className="flex items-center gap-4 pl-4 border-l border-white/5">
                            <div className="text-right hidden sm:block">
                                <p className="text-[13px] font-black text-white leading-none mb-1 uppercase tracking-wider">{user?.name || 'Observer'}</p>
                                <div className="flex items-center gap-1.5 justify-end">
                                    <span className="w-1.5 h-1.5 rounded-full bg-blue-500 animate-pulse" />
                                    <p className="text-[9px] font-black uppercase tracking-[0.15em] text-blue-400/80">Quantum Intelligence</p>
                                </div>
                            </div>
                            <div className="relative group p-0.5 rounded-2xl bg-gradient-to-br from-white/10 to-transparent hover:from-blue-500/40 transition-all duration-500">
                                <div className="h-11 w-11 rounded-2xl bg-[#1a1a1e] flex items-center justify-center overflow-hidden cursor-pointer premium-border">
                                    <User className="text-slate-400 w-5 h-5 group-hover:text-white transition-colors" />
                                </div>
                            </div>
                        </div>
                    </div>
                </header>

                {/* Dashboard Viewport */}
                <div className="p-12 animate-in fade-in slide-in-from-bottom-4 duration-1000">
                    {children}
                </div>
            </main>
        </div>
    );
}
