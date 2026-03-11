"use client";

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
    BarChart3,
    CreditCard,
    Home,
    Settings,
    BrainCircuit,
    LogOut,
    Bell,
    Search,
    User
} from 'lucide-react';
import { cn } from '@/components/ui/Card';
import { useAuth } from '@/services/authStore';
import { authService } from '@/services/api';
import NotificationBell from '@/components/shared/NotificationBell';

const navItems = [
    { name: 'Assistant', href: '/assistant', icon: BrainCircuit },
    { name: 'Dashboard', href: '/dashboard', icon: Home },
    { name: 'Transactions', href: '/transactions', icon: CreditCard },
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
        return <main className="min-h-screen bg-[#F8FAFC]">{children}</main>;
    }

    return (
        <div className="flex h-screen bg-[#F8FAFC]">
            {/* Sidebar */}
            <aside className="w-64 bg-primary text-white flex flex-col p-6">
                <div className="flex items-center gap-3 mb-10 px-2">
                    <div className="bg-secondary p-2 rounded-xl">
                        <BrainCircuit className="w-6 h-6 text-white" />
                    </div>
                    <span className="text-xl font-bold tracking-tight">AI Finance Assistant</span>
                </div>

                <nav className="flex-1 space-y-1">
                    {navItems.map((item) => {
                        const isActive = pathname === item.href;
                        return (
                            <Link
                                key={item.href}
                                href={item.href}
                                className={cn(
                                    "flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 group",
                                    isActive
                                        ? "bg-secondary text-white shadow-lg shadow-teal-900/40"
                                        : "text-slate-400 hover:text-white hover:bg-slate-800"
                                )}
                            >
                                <item.icon className={cn("w-5 h-5", isActive ? "text-white" : "text-slate-500 group-hover:text-white")} />
                                <span className="font-semibold">{item.name}</span>
                            </Link>
                        );
                    })}
                </nav>

                <div className="pt-6 border-t border-slate-800 space-y-1">
                    <Link href="/settings" className="flex items-center gap-3 px-4 py-3 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 transition-all">
                        <Settings className="w-5 h-5" />
                        <span className="font-semibold">Settings</span>
                    </Link>
                    <button onClick={handleLogout} className="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-rose-400 hover:bg-rose-900/20 hover:text-rose-300 transition-all">
                        <LogOut className="w-5 h-5" />
                        <span className="font-semibold">Logout</span>
                    </button>
                </div>
            </aside>

            {/* Main Content Area */}
            <main className="flex-1 overflow-y-auto">
                {/* Top Navbar */}
                <header className="h-16 border-b border-slate-200 bg-white px-8 flex items-center justify-between sticky top-0 z-10">
                    <div className="relative w-96">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                        <input
                            type="text"
                            placeholder="Search transactions or insights..."
                            className="pl-10 pr-4 py-2 bg-slate-50 border-none rounded-lg w-full text-sm focus:ring-2 focus:ring-secondary/20 transition-all"
                        />
                    </div>

                    <div className="flex items-center gap-4">
                        <NotificationBell />
                        <div className="h-8 w-[1px] bg-slate-100 mx-2" />
                        <div className="flex items-center gap-3 pl-2">
                            <div className="text-right hidden sm:block">
                                <p className="text-sm font-bold text-primary leading-none">{user?.name || 'User'}</p>
                                <p className="text-[10px] text-slate-400 font-medium uppercase tracking-wider mt-1">Premium Plan</p>
                            </div>
                            <div className="h-10 w-10 rounded-full bg-slate-100 border-2 border-slate-50 overflow-hidden shadow-sm hover:ring-2 hover:ring-secondary/30 transition-all cursor-pointer">
                                <div className="w-full h-full flex items-center justify-center bg-teal-50">
                                    <User className="text-secondary w-5 h-5" />
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
