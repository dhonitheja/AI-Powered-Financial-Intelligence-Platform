"use client";

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Sparkles, Loader2, Mail, Lock, ArrowRight, AlertCircle, ShieldCheck } from 'lucide-react';
import { useAuth } from '@/services/authStore';
import { authService } from '@/services/api';

export default function LoginPage() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [otpCode, setOtpCode] = useState('');
    const [showOtp, setShowOtp] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const router = useRouter();
    const login = useAuth((state) => state.login);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (isLoading) return;
        setIsLoading(true);
        setError('');

        try {
            const response = await authService.login({ email, password });

            if (response.status === 202 && response.data.twoFactorRequired) {
                setShowOtp(true);
                return;
            }

            if (response.status === 200 || response.status === 201) {
                const data = response.data;
                login({ id: String(data.id), name: data.username, email: data.email });
                router.push('/dashboard');
            }
        } catch (err: any) {
            setError(err.response?.data?.message || 'Invalid credentials');
        } finally {
            setIsLoading(false);
        }
    };

    const handleOtpSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsLoading(true);
        setError('');

        try {
            const response = await authService.verify2FA({ email, code: otpCode });
            const data = response.data;
            login({ id: String(data.id), name: data.username, email: data.email });
            router.push('/dashboard');
        } catch (err: any) {
            setError(err.response?.data?.message || 'Invalid 2FA code');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-[#0D0B1E] grid-bg flex flex-col justify-center items-center p-4">
            {/* Ambient glow */}
            <div className="fixed inset-0 pointer-events-none">
                <div className="absolute top-1/4 left-1/2 -translate-x-1/2 w-96 h-96 bg-[#D4AF37]/5 rounded-full blur-3xl" />
                <div className="absolute bottom-1/4 left-1/3 w-64 h-64 bg-[#1E1B4B]/60 rounded-full blur-3xl" />
            </div>

            <div className="relative w-full max-w-md">
                {/* Logo + Heading */}
                <div className="flex flex-col items-center mb-10">
                    <div className="relative mb-5">
                        <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-[#D4AF37] to-[#B8962E] flex items-center justify-center shadow-gold-lg">
                            <Sparkles className="w-8 h-8 text-[#0D0B1E]" />
                        </div>
                        <span className="absolute -bottom-1 -right-1 w-4 h-4 bg-emerald-400 rounded-full border-2 border-[#0D0B1E] animate-pulse" />
                    </div>
                    <h1 className="text-4xl font-black tracking-tight gold-text">
                        Welcome to Wealthix
                    </h1>
                    <p className="text-slate-500 mt-3 text-center text-sm">
                        {showOtp
                            ? 'Enter the 6-digit code from your authenticator app.'
                            : 'Sign in to access your financial intelligence.'}
                    </p>
                </div>

                {/* Card */}
                <div className="bg-white/5 backdrop-blur-xl rounded-3xl border border-white/10 shadow-gold p-8 sm:p-10">
                    {error && (
                        <div className="mb-6 p-4 rounded-xl bg-rose-500/10 text-rose-400 flex items-center gap-3 text-sm font-medium border border-rose-500/20">
                            <AlertCircle className="w-5 h-5 shrink-0" />
                            {error}
                        </div>
                    )}

                    {!showOtp ? (
                        <form onSubmit={handleSubmit} className="space-y-6">
                            <div className="space-y-2">
                                <label className="text-sm font-semibold text-slate-300 ml-1">Email Address</label>
                                <div className="relative">
                                    <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500" />
                                    <input
                                        type="email"
                                        required
                                        value={email}
                                        onChange={(e) => setEmail(e.target.value)}
                                        className="w-full pl-11 pr-4 py-3 bg-white/5 border border-white/10 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#D4AF37]/20 focus:border-[#D4AF37]/40 text-slate-200 placeholder:text-slate-600 transition-all"
                                        placeholder="name@example.com"
                                    />
                                </div>
                            </div>

                            <div className="space-y-2">
                                <div className="flex justify-between items-center ml-1">
                                    <label className="text-sm font-semibold text-slate-300">Password</label>
                                    <Link href="/forgot-password" className="text-xs font-bold text-[#D4AF37] hover:text-[#F5D67B] transition-colors">
                                        Forgot password?
                                    </Link>
                                </div>
                                <div className="relative">
                                    <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500" />
                                    <input
                                        type="password"
                                        required
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        className="w-full pl-11 pr-4 py-3 bg-white/5 border border-white/10 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#D4AF37]/20 focus:border-[#D4AF37]/40 text-slate-200 placeholder:text-slate-600 transition-all"
                                        placeholder="••••••••"
                                    />
                                </div>
                            </div>

                            <button
                                type="submit"
                                disabled={isLoading}
                                className="w-full bg-gradient-to-r from-[#D4AF37] to-[#B8962E] text-[#0D0B1E] py-4 rounded-xl font-black shadow-gold hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center justify-center gap-2 group disabled:opacity-70"
                            >
                                {isLoading ? (
                                    <Loader2 className="w-5 h-5 animate-spin" />
                                ) : (
                                    <>
                                        Sign In
                                        <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                                    </>
                                )}
                            </button>
                        </form>
                    ) : (
                        <form onSubmit={handleOtpSubmit} className="space-y-6">
                            <div className="space-y-2 text-center">
                                <ShieldCheck className="w-12 h-12 text-[#D4AF37] mx-auto mb-4" />
                                <label className="text-sm font-semibold text-slate-300">Verification Code</label>
                                <input
                                    type="text"
                                    required
                                    autoFocus
                                    maxLength={6}
                                    value={otpCode}
                                    onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ''))}
                                    className="w-full text-center text-3xl tracking-[1em] py-4 bg-white/5 border border-white/10 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#D4AF37]/20 focus:border-[#D4AF37]/40 text-slate-200 transition-all"
                                    placeholder="000000"
                                />
                            </div>

                            <button
                                type="submit"
                                disabled={isLoading || otpCode.length !== 6}
                                className="w-full bg-gradient-to-r from-[#D4AF37] to-[#B8962E] text-[#0D0B1E] py-4 rounded-xl font-black shadow-gold hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center justify-center gap-2 disabled:opacity-50"
                            >
                                {isLoading ? <Loader2 className="w-5 h-5 animate-spin" /> : 'Verify Code'}
                            </button>

                            <button
                                type="button"
                                onClick={() => setShowOtp(false)}
                                className="w-full text-slate-500 text-sm font-bold hover:text-slate-300 transition-colors"
                            >
                                Back to login
                            </button>
                        </form>
                    )}

                    <div className="mt-8 pt-8 border-t border-white/10 text-center">
                        <p className="text-sm text-slate-500">
                            Don&apos;t have an account?{' '}
                            <Link href="/register" className="font-bold text-[#D4AF37] hover:text-[#F5D67B] transition-colors">
                                Create an account
                            </Link>
                        </p>
                    </div>
                </div>

                <p className="mt-10 text-center text-[10px] text-slate-600 font-bold uppercase tracking-widest">
                    SECURE ENCRYPTED ACCESS · POWERED BY JASS · WEALTHIX AI
                </p>
            </div>
        </div>
    );
}
