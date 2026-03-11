'use client';

import React, { useState } from 'react';
import {
    Sparkles, CheckCircle, X, ChevronRight,
    Building2, Zap, ShieldCheck, CreditCard, TrendingUp, Home, Car
} from 'lucide-react';
import { toast } from 'sonner';
import type { DetectedRecurringPayment } from '@/services/autoPayService';

interface Props {
    payments: DetectedRecurringPayment[];
    onAddToHub: (payment: DetectedRecurringPayment) => Promise<void>;
    onDismiss: () => void;
}

const CATEGORY_ICONS: Record<string, React.ReactNode> = {
    HOME_LOAN: <Home className="w-4 h-4" />,
    AUTO_LOAN: <Car className="w-4 h-4" />,
    PERSONAL_LOAN: <Building2 className="w-4 h-4" />,
    CREDIT_CARD: <CreditCard className="w-4 h-4" />,
    HEALTH_INSURANCE: <ShieldCheck className="w-4 h-4" />,
    AUTO_INSURANCE: <Car className="w-4 h-4" />,
    LIFE_INSURANCE: <ShieldCheck className="w-4 h-4" />,
    TERM_INSURANCE: <ShieldCheck className="w-4 h-4" />,
    UTILITY: <Zap className="w-4 h-4" />,
    SUBSCRIPTION: <TrendingUp className="w-4 h-4" />,
    SIP: <TrendingUp className="w-4 h-4" />,
    RENT: <Home className="w-4 h-4" />,
    CUSTOM: <Building2 className="w-4 h-4" />,
};

function ConfidenceBadge({ score }: { score: number }) {
    const color =
        score >= 80 ? 'text-emerald-400 bg-emerald-400/10' :
            score >= 50 ? 'text-amber-400 bg-amber-400/10' :
                'text-rose-400 bg-rose-400/10';
    const label =
        score >= 80 ? 'High confidence' :
            score >= 50 ? 'Medium confidence' :
                'Low confidence';
    return (
        <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${color}`}>
            {label} ({score}%)
        </span>
    );
}

export function DetectedPaymentsBanner({ payments, onAddToHub, onDismiss }: Props) {
    const [added, setAdded] = useState<Set<number>>(new Set());
    const [loading, setLoading] = useState<Set<number>>(new Set());
    const [dismissed, setDismissed] = useState<Set<number>>(new Set());
    const [expanded, setExpanded] = useState(true);

    const visiblePayments = payments.filter((_, i) => !dismissed.has(i));

    if (visiblePayments.length === 0) return null;

    async function handleAdd(payment: DetectedRecurringPayment, idx: number) {
        setLoading(prev => new Set(prev).add(idx));
        try {
            await onAddToHub(payment);
            setAdded(prev => new Set(prev).add(idx));
            toast.success(`${payment.paymentName} added to AutoPay Hub!`);
        } catch {
            toast.error('Failed to add payment. Please try again.');
        } finally {
            setLoading(prev => { const s = new Set(prev); s.delete(idx); return s; });
        }
    }

    function dismissOne(idx: number) {
        setDismissed(prev => new Set(prev).add(idx));
        if (visiblePayments.length === 1) onDismiss();
    }

    return (
        <div className="relative bg-gradient-to-r from-violet-900/40 via-purple-900/30 to-indigo-900/40
                    border border-violet-500/30 rounded-2xl overflow-hidden mb-6">
            {/* Ambient glow */}
            <div className="absolute inset-0 bg-gradient-to-br from-violet-600/5 to-transparent pointer-events-none" />

            {/* Header */}
            <div className="flex items-center justify-between px-5 py-4 border-b border-violet-500/20">
                <div className="flex items-center gap-3">
                    <div className="p-2 rounded-xl bg-violet-500/20">
                        <Sparkles className="w-5 h-5 text-violet-400" />
                    </div>
                    <div>
                        <h3 className="font-semibold text-white text-sm">
                            Smart Detection Found {payments.length} Recurring {payments.length === 1 ? 'Payment' : 'Payments'}
                        </h3>
                        <p className="text-xs text-slate-400 mt-0.5">
                            Based on your Plaid transaction history — add them to AutoPay Hub in one click
                        </p>
                    </div>
                </div>
                <div className="flex items-center gap-2">
                    <button
                        onClick={() => setExpanded(e => !e)}
                        className="text-xs text-violet-400 hover:text-violet-300 px-3 py-1.5 rounded-lg
                       bg-violet-500/10 hover:bg-violet-500/20 transition-colors"
                    >
                        {expanded ? 'Collapse' : 'Expand'}
                    </button>
                    <button
                        onClick={onDismiss}
                        className="p-1.5 rounded-lg text-slate-500 hover:text-slate-300 hover:bg-white/5 transition-colors"
                        aria-label="Dismiss all"
                    >
                        <X className="w-4 h-4" />
                    </button>
                </div>
            </div>

            {/* Payment cards */}
            {expanded && (
                <div className="p-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                    {payments.map((payment, idx) => {
                        if (dismissed.has(idx)) return null;
                        const isAdded = added.has(idx);
                        const isLoading = loading.has(idx);

                        return (
                            <div
                                key={idx}
                                className={`relative rounded-xl border p-4 transition-all duration-300
                  ${isAdded
                                        ? 'border-emerald-500/40 bg-emerald-500/5'
                                        : 'border-white/10 bg-white/5 hover:bg-white/8 hover:border-violet-500/30'
                                    }`}
                            >
                                {/* Dismiss single card */}
                                {!isAdded && (
                                    <button
                                        onClick={() => dismissOne(idx)}
                                        className="absolute top-2 right-2 p-1 rounded-md text-slate-600
                               hover:text-slate-400 hover:bg-white/5 transition-colors"
                                    >
                                        <X className="w-3.5 h-3.5" />
                                    </button>
                                )}

                                <div className="flex items-start gap-3 mb-3">
                                    <div className="p-2 rounded-lg bg-violet-500/15 text-violet-400 shrink-0">
                                        {CATEGORY_ICONS[payment.suggestedCategory] ?? <Building2 className="w-4 h-4" />}
                                    </div>
                                    <div className="min-w-0">
                                        <p className="font-medium text-white text-sm truncate">{payment.paymentName}</p>
                                        <p className="text-xs text-slate-500 truncate">{payment.merchantDescription}</p>
                                    </div>
                                </div>

                                <div className="flex items-baseline gap-1 mb-2">
                                    <span className="text-lg font-bold text-white">
                                        ${payment.averageAmount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                    </span>
                                    <span className="text-xs text-slate-500">/ {payment.suggestedFrequency.toLowerCase()}</span>
                                </div>

                                <div className="flex items-center gap-2 mb-3">
                                    <ConfidenceBadge score={payment.confidenceScore} />
                                    <span className="text-xs text-slate-500">
                                        {payment.occurrenceCount} months detected
                                    </span>
                                </div>

                                {/* Action button */}
                                {isAdded ? (
                                    <div className="flex items-center gap-2 text-emerald-400 text-sm font-medium">
                                        <CheckCircle className="w-4 h-4" />
                                        Added to Hub
                                    </div>
                                ) : (
                                    <button
                                        onClick={() => handleAdd(payment, idx)}
                                        disabled={isLoading}
                                        className="w-full flex items-center justify-center gap-2 py-2 px-3 rounded-lg
                               bg-violet-500/20 hover:bg-violet-500/30 text-violet-300 text-sm font-medium
                               border border-violet-500/30 hover:border-violet-400/40
                               transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
                                    >
                                        {isLoading ? (
                                            <div className="w-4 h-4 border-2 border-violet-400/30 border-t-violet-400 rounded-full animate-spin" />
                                        ) : (
                                            <>
                                                Add to Hub
                                                <ChevronRight className="w-4 h-4" />
                                            </>
                                        )}
                                    </button>
                                )}
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}
