"use client";

import React, { useState } from 'react';
import Link from 'next/link';
import { BrainCircuit, Loader2, Mail, ArrowRight, AlertCircle, CheckCircle } from 'lucide-react';
import { authService } from '@/services/api';

export default function ForgotPasswordPage() {
    const [email, setEmail] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (isLoading) return;
        setIsLoading(true);
        setError('');
        setSuccess('');

        try {
            const response = await authService.forgotPassword({ email });
            setSuccess(response.data.message || 'If your email is registered, you will receive a reset link shortly.');
            setEmail('');
        } catch (err: any) {
            setError(err.response?.data?.message || 'Something went wrong. Please try again.');
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
                        Reset Password
                    </h1>
                    <p className="text-slate-500 mt-2 text-center text-sm">
                        Enter your email address and we'll send you a link to reset your password.
                    </p>
                </div>

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

                        <button
                            type="submit"
                            disabled={isLoading || !email}
                            className="w-full bg-primary text-white py-4 rounded-xl font-bold shadow-lg shadow-slate-900/20 hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center justify-center gap-2 group disabled:opacity-70"
                        >
                            {isLoading ? (
                                <Loader2 className="w-5 h-5 animate-spin" />
                            ) : (
                                <>
                                    Send Reset Link
                                    <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                                </>
                            )}
                        </button>
                    </form>

                    <div className="mt-8 pt-8 border-t border-slate-100 text-center">
                        <p className="text-sm text-slate-500">
                            Remembered your password?{' '}
                            <Link href="/login" className="font-bold text-primary hover:text-secondary transition-colors">
                                Back to login
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
