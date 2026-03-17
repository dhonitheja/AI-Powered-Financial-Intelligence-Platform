'use client';

import React from 'react';
import { AlertCircle, Clock, X } from 'lucide-react';
import type { AutoPaySchedule } from '@/services/autoPayService';
import { formatCurrency, daysUntilDue } from '@/services/autoPayService';

interface UpcomingPaymentsBannerProps {
    overdue: AutoPaySchedule[];
    dueSoon: AutoPaySchedule[];
    onDismiss?: () => void;
    onSelect?: (id: string) => void;
}

export function UpcomingPaymentsBanner({
    overdue,
    dueSoon,
    onDismiss,
    onSelect,
}: UpcomingPaymentsBannerProps) {
    const [dismissed, setDismissed] = React.useState(false);

    if (dismissed || (overdue.length === 0 && dueSoon.length === 0)) return null;

    const hasOverdue = overdue.length > 0;

    return (
        <div
            className={`relative rounded-2xl border p-4 ${hasOverdue
                    ? 'bg-red-500/10 border-red-500/30'
                    : 'bg-amber-500/10 border-amber-500/30'
                } animate-in slide-in-from-top-2 duration-300`}
        >
            <div className="flex items-start gap-3">
                <AlertCircle
                    size={18}
                    className={`mt-0.5 flex-shrink-0 ${hasOverdue ? 'text-red-400' : 'text-amber-400'}`}
                />

                <div className="flex-1 min-w-0">
                    {overdue.length > 0 && (
                        <div className="mb-2">
                            <p className="text-sm font-semibold text-red-400">
                                {overdue.length} overdue payment{overdue.length !== 1 ? 's' : ''}
                            </p>
                            <div className="flex flex-wrap gap-2 mt-1.5">
                                {overdue.slice(0, 3).map((s) => (
                                    <button
                                        key={s.id}
                                        onClick={() => onSelect?.(s.id)}
                                        className="text-xs px-2.5 py-1 rounded-lg bg-red-500/20 text-red-300 hover:bg-red-500/30 transition-colors"
                                    >
                                        {s.paymentName} · {formatCurrency(s.amount, s.currency)}
                                    </button>
                                ))}
                                {overdue.length > 3 && (
                                    <span className="text-xs text-red-400/70 self-center">
                                        +{overdue.length - 3} more
                                    </span>
                                )}
                            </div>
                        </div>
                    )}

                    {dueSoon.length > 0 && (
                        <div>
                            <div className="flex items-center gap-1.5">
                                <Clock size={13} className="text-amber-400" />
                                <p className="text-sm font-medium text-amber-400">
                                    {dueSoon.length} payment{dueSoon.length !== 1 ? 's' : ''} due within 7 days
                                </p>
                            </div>
                            <div className="flex flex-wrap gap-2 mt-1.5">
                                {dueSoon.slice(0, 4).map((s) => {
                                    const days = daysUntilDue(s.nextDueDate);
                                    return (
                                        <button
                                            key={s.id}
                                            onClick={() => onSelect?.(s.id)}
                                            className="text-xs px-2.5 py-1 rounded-lg bg-amber-500/20 text-amber-300 hover:bg-amber-500/30 transition-colors"
                                        >
                                            {s.paymentName} · {days === 0 ? 'Today' : `in ${days}d`}
                                        </button>
                                    );
                                })}
                            </div>
                        </div>
                    )}
                </div>

                <button
                    onClick={() => {
                        setDismissed(true);
                        onDismiss?.();
                    }}
                    className="text-slate-500 hover:text-slate-300 transition-colors"
                >
                    <X size={16} />
                </button>
            </div>
        </div>
    );
}
