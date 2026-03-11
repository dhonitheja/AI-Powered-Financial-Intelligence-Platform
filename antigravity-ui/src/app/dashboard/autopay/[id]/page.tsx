'use client';

import React from 'react';
import { useParams, useRouter } from 'next/navigation';
import { toast } from 'sonner';
import {
    ArrowLeft, Edit, Pause, Play, Trash2, Zap, Clock, CheckCircle, XCircle, AlertCircle,
} from 'lucide-react';
import autoPayService, {
    type AutoPaySchedule,
    type AutoPayExecutionLog,
    type PageResponse,
    formatCurrency,
    daysUntilDue,
    FREQUENCY_LABELS,
    STATUS_COLORS,
    STATUS_BG,
} from '@/services/autoPayService';
import { CategoryIcon } from '@/components/autopay/CategoryIcon';

const STATUS_ICON: Record<string, React.ElementType> = {
    SUCCESS: CheckCircle,
    FAILED: XCircle,
    PENDING: Clock,
    SKIPPED: AlertCircle,
    CANCELLED: XCircle,
};

const STATUS_CLR: Record<string, string> = {
    SUCCESS: 'text-emerald-400',
    FAILED: 'text-red-400',
    PENDING: 'text-amber-400',
    SKIPPED: 'text-slate-400',
    CANCELLED: 'text-slate-400',
};

export default function AutoPayDetailPage() {
    const { id } = useParams<{ id: string }>();
    const router = useRouter();
    const [schedule, setSchedule] = React.useState<AutoPaySchedule | null>(null);
    const [logs, setLogs] = React.useState<PageResponse<AutoPayExecutionLog> | null>(null);
    const [loading, setLoading] = React.useState(true);

    const load = React.useCallback(async () => {
        try {
            const [schedRes, logsRes] = await Promise.all([
                autoPayService.getSchedule(id),
                autoPayService.getLogsForSchedule(id),
            ]);
            setSchedule(schedRes.data);
            setLogs(logsRes.data);
        } catch {
            toast.error('Schedule not found');
            router.push('/dashboard/autopay');
        } finally {
            setLoading(false);
        }
    }, [id]);

    React.useEffect(() => { load(); }, [load]);

    const handleToggle = async () => {
        if (!schedule) return;
        const res = await autoPayService.toggleSchedule(schedule.id);
        setSchedule(res.data);
        toast.success(res.data.active ? 'Payment resumed' : 'Payment paused');
    };

    const handleDelete = async () => {
        if (!confirm('Deactivate this schedule? It can be reactivated later.')) return;
        await autoPayService.deleteSchedule(id);
        toast.success('Schedule deactivated');
        router.push('/dashboard/autopay');
    };

    const handleExecute = async () => {
        await autoPayService.executePayment(id);
        toast.success('Payment execution triggered');
        load();
    };

    if (loading) {
        return (
            <div className="p-6 max-w-4xl mx-auto space-y-4">
                <div className="h-8 w-48 rounded-xl bg-white/5 animate-pulse" />
                <div className="h-40 rounded-2xl bg-white/5 animate-pulse" />
                <div className="h-64 rounded-2xl bg-white/5 animate-pulse" />
            </div>
        );
    }

    if (!schedule) return null;

    const days = daysUntilDue(schedule.nextDueDate);

    return (
        <div className="p-6 max-w-4xl mx-auto space-y-6">
            {/* Breadcrumb */}
            <button
                onClick={() => router.push('/dashboard/autopay')}
                className="flex items-center gap-2 text-slate-400 hover:text-white text-sm transition-colors"
            >
                <ArrowLeft size={16} />
                AutoPay Hub
            </button>

            {/* Header card */}
            <div className="bg-white/5 border border-white/10 rounded-2xl p-6">
                <div className="flex items-start gap-4">
                    <CategoryIcon category={schedule.paymentCategory} size={24} />
                    <div className="flex-1">
                        <div className="flex items-start justify-between gap-4">
                            <div>
                                <h1 className="text-xl font-bold text-white">{schedule.paymentName}</h1>
                                {schedule.paymentProvider && (
                                    <p className="text-slate-400 text-sm">{schedule.paymentProvider}</p>
                                )}
                            </div>
                            <div className="flex items-center gap-2">
                                <button
                                    onClick={() => router.push(`/dashboard/autopay/${id}/edit`)}
                                    className="p-2 rounded-xl bg-white/5 border border-white/10 text-slate-400 hover:text-white hover:bg-white/10 transition-all"
                                >
                                    <Edit size={15} />
                                </button>
                                <button
                                    onClick={handleExecute}
                                    className="p-2 rounded-xl bg-violet-500/15 border border-violet-500/30 text-violet-400 hover:bg-violet-500/25 transition-all"
                                    title="Execute now"
                                >
                                    <Zap size={15} />
                                </button>
                                <button
                                    onClick={handleToggle}
                                    className="p-2 rounded-xl bg-amber-500/15 border border-amber-500/30 text-amber-400 hover:bg-amber-500/25 transition-all"
                                >
                                    {schedule.active ? <Pause size={15} /> : <Play size={15} />}
                                </button>
                                <button
                                    onClick={handleDelete}
                                    className="p-2 rounded-xl bg-red-500/15 border border-red-500/30 text-red-400 hover:bg-red-500/25 transition-all"
                                >
                                    <Trash2 size={15} />
                                </button>
                            </div>
                        </div>

                        <div className="mt-4 grid grid-cols-2 sm:grid-cols-4 gap-4 text-sm">
                            <div>
                                <p className="text-xs text-slate-500 mb-0.5">Amount</p>
                                <p className="font-bold text-white text-lg">
                                    {formatCurrency(schedule.amount, schedule.currency)}
                                </p>
                            </div>
                            <div>
                                <p className="text-xs text-slate-500 mb-0.5">Frequency</p>
                                <p className="text-white">{FREQUENCY_LABELS[schedule.frequency]}</p>
                            </div>
                            <div>
                                <p className="text-xs text-slate-500 mb-0.5">Next Due</p>
                                <p className={days < 0 ? 'text-red-400' : days <= 7 ? 'text-amber-400' : 'text-white'}>
                                    {schedule.nextDueDate}
                                    {days < 0 && <span className="text-xs ml-1">({Math.abs(days)}d overdue)</span>}
                                    {days >= 0 && days <= 7 && (
                                        <span className="text-xs ml-1">({days === 0 ? 'today' : `${days}d`})</span>
                                    )}
                                </p>
                            </div>
                            <div>
                                <p className="text-xs text-slate-500 mb-0.5">Status</p>
                                <span
                                    className={`text-xs px-2 py-0.5 rounded-full ${STATUS_BG[schedule.status]} ${STATUS_COLORS[schedule.status]}`}
                                >
                                    {schedule.status.replace('_', ' ')}
                                </span>
                            </div>
                        </div>

                        {schedule.accountNumberMasked && (
                            <p className="mt-3 text-xs text-slate-500 font-mono">
                                Account: {schedule.accountNumberMasked}
                                {schedule.hasRoutingNumber && ' · Routing: ****'}
                            </p>
                        )}
                    </div>
                </div>
            </div>

            {/* Execution history */}
            <div className="bg-white/5 border border-white/10 rounded-2xl p-6">
                <h2 className="text-sm font-semibold text-white mb-4">Execution History</h2>
                {!logs || logs.content.length === 0 ? (
                    <p className="text-slate-500 text-sm text-center py-8">No payment history yet</p>
                ) : (
                    <div className="space-y-2">
                        {logs.content.map((log) => {
                            const Icon = STATUS_ICON[log.status] || Clock;
                            const verifyColor =
                                log.plaidVerificationStatus === 'VERIFIED' ? 'text-emerald-400' :
                                    log.plaidVerificationStatus === 'NEEDS_REVIEW' ? 'text-amber-400' :
                                        'text-slate-500';
                            return (
                                <div
                                    key={log.id}
                                    className="flex items-start gap-3 px-4 py-3 bg-white/3 rounded-xl"
                                >
                                    <Icon size={15} className={`mt-0.5 ${STATUS_CLR[log.status] || 'text-slate-400'}`} />
                                    <div className="flex-1 min-w-0">
                                        <p className="text-sm text-white flex items-center gap-2">
                                            {formatCurrency(log.amountPaid, log.currency)}
                                            <span className="text-slate-500 text-xs">
                                                {new Date(log.executionDate).toLocaleDateString()}
                                            </span>
                                        </p>
                                        {log.failureReason && (
                                            <p className="text-xs text-red-400 mt-0.5">{log.failureReason}</p>
                                        )}
                                        <div className="flex items-center gap-3 mt-1">
                                            {/* Plaid verification badge */}
                                            {log.status === 'SUCCESS' && (
                                                <span className={`text-xs ${verifyColor}`}>
                                                    {log.plaidVerificationStatus === 'VERIFIED' && '✓ Bank verified'}
                                                    {log.plaidVerificationStatus === 'NEEDS_REVIEW' && '⚠ Needs review'}
                                                    {log.plaidVerificationStatus === 'UNVERIFIED' && '· Pending verification'}
                                                </span>
                                            )}
                                            {/* Stripe PaymentIntent (truncated) */}
                                            {log.stripePaymentIntentId && (
                                                <span className="text-xs text-slate-600 font-mono">
                                                    pi_···{log.stripePaymentIntentId.slice(-6)}
                                                </span>
                                            )}
                                        </div>
                                    </div>
                                    <span
                                        className={`text-xs font-medium px-2 py-0.5 rounded-full ${STATUS_CLR[log.status]
                                            } bg-white/5 shrink-0`}
                                    >
                                        {log.status}
                                    </span>
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>
        </div>
    );
}
