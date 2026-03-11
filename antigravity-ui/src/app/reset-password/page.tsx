"use client";

import React, { useState, Suspense } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { BrainCircuit, Loader2, Lock, ArrowRight, AlertCircle, CheckCircle } from 'lucide-react';
import { authService } from '@/services/api';

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
            // Redirect after a short delay
            setTimeout(() => {
                router.push('/login');
            }, 3000);
        } catch (err: any) {
            setError(err.response?.data?.message || 'Failed to reset password. The link might be expired.');
        } finally {
            setIsLoading(false);
        }
    };

    if (!token) {
        return (
            <div className="bg-white rounded-3xl border border-slate-200 shadow-soft p-8 sm:p-10 text-center">
                <div className="mb-6 p-4 rounded-xl bg-red-50 text-red-600 flex items-center justify-center gap-3 text-sm font-medium border border-red-100">
                    <AlertCircle className="w-5 h-5 shrink-0" />
                    Invalid reset link.
                </div>
                <Link href="/forgot-password" className="text-secondary hover:underline font-bold">
                    Request a new link
                </Link>
            </div>
        );
    }

    return (
        <div className="bg-white rounded-3xl border border-slate-200 shadow-soft p-8 sm:p-10">
            {error && (
                <div className="mb-6 p-4 rounded-xl bg-red-50 text-red-600 flex items-center gap-3 text-sm font-medium border border-red-100">
                    <AlertCircle className="w-5 h-5 shrink-0" />
                    {error}
                </div>
            )}
            
            {success && (
                <div className="mb-6 p-4 rounded-xl bg-green-50 text-green-700 flex items-center gap-3 text-sm font-medium border border-green-100">
                    <CheckCircle className="w-5 h-5 shrink-0" />
                    {success}
                </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-6">
                <div className="space-y-2">
                    <label className="text-sm font-semibold text-primary ml-1">New Password</label>
                    <div className="relative">
                        <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                        <input
                            type="password"
                            required
                            value={newPassword}
                            onChange={(e) => setNewPassword(e.target.value)}
                            className="w-full pl-11 pr-4 py-3 bg-slate-50 border border-slate-100 rounded-xl focus:ring-2 focus:ring-secondary/20 focus:border-secondary outline-none transition-all"
                            placeholder="••••••••"
                        />
                    </div>
                </div>

                <div className="space-y-2">
                    <label className="text-sm font-semibold text-primary ml-1">Confirm New Password</label>
                    <div className="relative">
                        <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                        <input
                            type="password"
                            required
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            className="w-full pl-11 pr-4 py-3 bg-slate-50 border border-slate-100 rounded-xl focus:ring-2 focus:ring-secondary/20 focus:border-secondary outline-none transition-all"
                            placeholder="••••••••"
                        />
                    </div>
                </div>

                <button
                    type="submit"
                    disabled={isLoading || !newPassword || !confirmPassword || !!success}
                    className="w-full bg-primary text-white py-4 rounded-xl font-bold shadow-lg shadow-slate-900/20 hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center justify-center gap-2 group disabled:opacity-70"
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

            <div className="mt-8 pt-8 border-t border-slate-100 text-center">
                <Link href="/login" className="font-bold text-primary hover:text-secondary transition-colors text-sm">
                    Back to login
                </Link>
            </div>
        </div>
    );
}

export default function ResetPasswordPage() {
    return (
        <div className="min-h-screen bg-[#F8FAFC] flex flex-col justify-center items-center p-4">
            <div className="w-full max-w-md">
                <div className="flex flex-col items-center mb-10">
                    <div className="bg-primary p-3 rounded-2xl shadow-xl shadow-slate-900/20 mb-4">
                        <BrainCircuit className="w-8 h-8 text-secondary" />
                    </div>
                    <h1 className="text-3xl font-bold text-primary tracking-tight">
                        Create New Password
                    </h1>
                    <p className="text-slate-500 mt-2 text-center text-sm">
                        Enter your new secure password below to regain access.
                    </p>
                </div>

                <Suspense fallback={<div className="text-center p-10"><Loader2 className="w-8 h-8 animate-spin mx-auto text-secondary"/></div>}>
                    <ResetPasswordForm />
                </Suspense>

                <p className="mt-10 text-center text-[10px] text-slate-400 font-bold uppercase tracking-widest">
                    SECURE ENCRYPTED ACCESS • POWERED BY ANTIGRAVITY AI
                </p>
            </div>
        </div>
    );
}
