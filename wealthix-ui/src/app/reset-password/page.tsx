"use client";

import React, { useState, Suspense } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { Sparkles, Loader2, Lock, ArrowRight, AlertCircle, CheckCircle } from 'lucide-react';
import { authService } from '@/services/api';
import { Card } from '@/components/ui/Card';

function ResetPasswordForm() {
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const router = useRouter();
    const searchParams = useSearchParams();
    const token = searchParams.get('token');

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!token) {
            setError('Invalid or missing reset token.');
            return;
        }
        if (newPassword !== confirmPassword) {
            setError('Passwords do not match.');
            return;
        }
        if (newPassword.length < 8) {
            setError('Password must be at least 8 characters long.');
            return;
        }

        if (isLoading) return;
        setIsLoading(true);
        setError('');
        setSuccess('');

        try {
            const response = await authService.resetPassword({ token, newPassword });
            setSuccess(response.data.message || 'Password reset successfully.');
            setTimeout(() => router.push('/login'), 3000);
        } catch (err: any) {
            setError(err.response?.data?.message || 'Failed to reset password. The link might be expired.');
        } finally {
            setIsLoading(false);
        }
    };

    if (!token) {
        return (
            <Card className="p-8 sm:p-10 text-center">
                <div className="mb-6 p-4 rounded-xl bg-rose-500/10 text-rose-400 flex items-center justify-center gap-3 text-sm font-medium border border-rose-500/20">
                    <AlertCircle className="w-5 h-5 shrink-0" />
                    Invalid or missing reset link.
                </div>
                <Link href="/forgot-password" className="font-bold text-[#D4AF37] hover:text-[#F5D67B] transition-colors">
                    Request a new link
                </Link>
            </Card>
        );
    }

    return (
        <Card className="p-8 sm:p-10">
            {error && (
                <div className="mb-6 p-4 rounded-xl bg-rose-500/10 text-rose-400 flex items-center gap-3 text-sm font-medium border border-rose-500/20">
                    <AlertCircle className="w-5 h-5 shrink-0" />
                    {error}
                </div>
            )}

            {success && (
                <div className="mb-6 p-4 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center gap-3 text-sm font-medium border border-emerald-500/20">
                    <CheckCircle className="w-5 h-5 shrink-0" />
                    {success} Redirecting to login…
                </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-6">
                <div className="space-y-2">
                    <label className="text-sm font-semibold text-slate-300 ml-1">New Password</label>
                    <div className="relative">
                        <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500" />
                        <input
                            type="password"
                            required
                            value={newPassword}
                            onChange={(e) => setNewPassword(e.target.value)}
                            className="w-full pl-11 pr-4 py-3 bg-white/5 border border-white/10 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#D4AF37]/20 focus:border-[#D4AF37]/40 text-slate-200 placeholder:text-slate-600 transition-all"
                            placeholder="••••••••"
                        />
                    </div>
                </div>

                <div className="space-y-2">
                    <label className="text-sm font-semibold text-slate-300 ml-1">Confirm New Password</label>
                    <div className="relative">
                        <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500" />
                        <input
                            type="password"
                            required
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            className="w-full pl-11 pr-4 py-3 bg-white/5 border border-white/10 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#D4AF37]/20 focus:border-[#D4AF37]/40 text-slate-200 placeholder:text-slate-600 transition-all"
                            placeholder="••••••••"
                        />
                    </div>
                </div>

                <button
                    type="submit"
                    disabled={isLoading || !newPassword || !confirmPassword || !!success}
                    className="w-full bg-gradient-to-r from-[#D4AF37] to-[#B8962E] text-[#0D0B1E] py-4 rounded-xl font-black shadow-gold hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center justify-center gap-2 group disabled:opacity-70"
                >
                    {isLoading ? (
                        <Loader2 className="w-5 h-5 animate-spin" />
                    ) : (
                        <>
                            Reset Password
                            <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                        </>
                    )}
                </button>
            </form>

            <div className="mt-8 pt-8 border-t border-white/10 text-center">
                <Link href="/login" className="font-bold text-[#D4AF37] hover:text-[#F5D67B] transition-colors text-sm">
                    Back to login
                </Link>
            </div>
        </Card>
    );
}


export default function ResetPasswordPage() {
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
                    </div>
                    <h1 className="text-4xl font-black tracking-tight gold-text">Create New Password</h1>
                    <p className="text-slate-500 mt-3 text-center text-sm">
                        Enter your new secure password below to regain access.
                    </p>
                </div>

                <Suspense fallback={
                    <div className="flex justify-center py-20">
                        <Loader2 className="w-8 h-8 animate-spin text-[#D4AF37]" />
                    </div>
                }>
                    <ResetPasswordForm />
                </Suspense>

                <p className="mt-10 text-center text-[10px] text-slate-600 font-bold uppercase tracking-widest">
                    SECURE ENCRYPTED ACCESS · POWERED BY JASS · WEALTHIX AI
                </p>
            </div>
        </div>
    );
}
