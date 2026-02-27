"use client";

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { BrainCircuit, Loader2, Mail, Lock, ArrowRight, AlertCircle, ShieldCheck } from 'lucide-react';
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
        console.log("--- LOGIN ATTEMPT START ---");

        if (isLoading) return;

        setIsLoading(true);
        setError('');

        try {
            const response = await authService.login({ email, password });

            if (response.status === 202 && response.data.twoFactorRequired) {
                console.log("2FA Required for user:", email);
                setShowOtp(true);
                return;
            }

            if (response.status === 200 || response.status === 201) {
                const data = response.data;
                login({
                    id: String(data.id),
                    name: data.username,
                    email: data.email
                });
                router.push('/dashboard');
            }
        } catch (err: any) {
            console.error("Login attempt failed:", err);
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
            login({
                id: String(data.id),
                name: data.username,
                email: data.email
            });
            router.push('/dashboard');
        } catch (err: any) {
            setError(err.response?.data?.message || 'Invalid 2FA code');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-[#F8FAFC] flex flex-col justify-center items-center p-4">
            <div className="w-full max-w-md">
                <div className="flex flex-col items-center mb-10">
                    <div className="bg-primary p-3 rounded-2xl shadow-xl shadow-slate-900/20 mb-4">
                        <BrainCircuit className="w-8 h-8 text-secondary" />
                    </div>
                    <h1 className="text-3xl font-bold text-primary tracking-tight">
                        {showOtp ? 'Security Check' : 'Welcome Back'}
                    </h1>
                    <p className="text-slate-500 mt-2 text-center text-sm">
                        {showOtp
                            ? 'Enter the 6-digit code from your authenticator app.'
                            : 'Enter your details to access your financial intelligence.'}
                    </p>
                </div>

                <div className="bg-white rounded-3xl border border-slate-200 shadow-soft p-8 sm:p-10">
                    {error && (
                        <div className="mb-6 p-4 rounded-xl bg-red-50 text-red-600 flex items-center gap-3 text-sm font-medium border border-red-100">
                            <AlertCircle className="w-5 h-5 shrink-0" />
                            {error}
                        </div>
                    )}

                    {!showOtp ? (
                        <form onSubmit={handleSubmit} className="space-y-6">
                            <div className="space-y-2">
                                <label className="text-sm font-semibold text-primary ml-1">Email Address</label>
                                <div className="relative">
                                    <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                                    <input
                                        type="email"
                                        required
                                        value={email}
                                        onChange={(e) => setEmail(e.target.value)}
                                        className="w-full pl-11 pr-4 py-3 bg-slate-50 border border-slate-100 rounded-xl focus:ring-2 focus:ring-secondary/20 focus:border-secondary outline-none transition-all"
                                        placeholder="name@example.com"
                                    />
                                </div>
                            </div>

                            <div className="space-y-2">
                                <div className="flex justify-between items-center ml-1">
                                    <label className="text-sm font-semibold text-primary">Password</label>
                                    <Link href="#" className="text-xs font-bold text-secondary hover:underline">Forgot password?</Link>
                                </div>
                                <div className="relative">
                                    <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                                    <input
                                        type="password"
                                        required
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        className="w-full pl-11 pr-4 py-3 bg-slate-50 border border-slate-100 rounded-xl focus:ring-2 focus:ring-secondary/20 focus:border-secondary outline-none transition-all"
                                        placeholder="••••••••"
                                    />
                                </div>
                            </div>

                            <button
                                type="submit"
                                disabled={isLoading}
                                className="w-full bg-primary text-white py-4 rounded-xl font-bold shadow-lg shadow-slate-900/20 hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center justify-center gap-2 group disabled:opacity-70"
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
                                <ShieldCheck className="w-12 h-12 text-secondary mx-auto mb-4" />
                                <label className="text-sm font-semibold text-primary">Verification Code</label>
                                <input
                                    type="text"
                                    required
                                    autoFocus
                                    maxLength={6}
                                    value={otpCode}
                                    onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ''))}
                                    className="w-full text-center text-3xl tracking-[1em] py-4 bg-slate-50 border border-slate-100 rounded-xl focus:ring-2 focus:ring-secondary/20 focus:border-secondary outline-none transition-all"
                                    placeholder="000000"
                                />
                            </div>

                            <button
                                type="submit"
                                disabled={isLoading || otpCode.length !== 6}
                                className="w-full bg-secondary text-white py-4 rounded-xl font-bold shadow-lg shadow-teal-900/20 hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center justify-center gap-2 disabled:opacity-50"
                            >
                                {isLoading ? <Loader2 className="w-5 h-5 animate-spin" /> : 'Verify Code'}
                            </button>

                            <button
                                type="button"
                                onClick={() => setShowOtp(false)}
                                className="w-full text-slate-400 text-sm font-bold hover:text-primary transition-colors"
                            >
                                Back to login
                            </button>
                        </form>
                    )}

                    <div className="mt-8 pt-8 border-t border-slate-100 text-center">
                        <p className="text-sm text-slate-500">
                            Don&apos;t have an account?{' '}
                            <Link href="/register" className="font-bold text-primary hover:text-secondary transition-colors">
                                Create an account
                            </Link>
                        </p>
                    </div>
                </div>

                <p className="mt-10 text-center text-[10px] text-slate-400 font-bold uppercase tracking-widest">
                    SECURE ENCRYPTED ACCESS • POWERED BY ANTIGRAVITY AI
                </p>
            </div>
        </div>
    );
}
