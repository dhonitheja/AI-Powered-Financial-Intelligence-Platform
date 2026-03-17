'use client';

import React from 'react';
import {
    Calendar, TrendingUp, AlertTriangle,
    Plus, RefreshCw, CreditCard
} from 'lucide-react';
import { useRouter } from 'next/navigation';
import { toast } from 'sonner';

import autoPayService, {
    type AutoPayDashboard,
    type AutoPaySchedule,
    type DetectedRecurringPayment,
    formatCurrency,
} from '@/services/autoPayService';
import { PaymentHealthScore } from './PaymentHealthScore';
import { AutoPayList } from './AutoPayList';
import { UpcomingPaymentsBanner } from './UpcomingPaymentsBanner';
import { DetectedPaymentsBanner } from './DetectedPaymentsBanner';
import PaymentMethodsModal from './PaymentMethodsModal';
import { CATEGORY_COLORS } from './CategoryIcon';

// ── Summary stat card ─────────────────────────────────────────────────────────
function StatCard({
    label, value, subLabel, icon: Icon, color = '#6366f1',
}: {
    label: string; value: string; subLabel?: string;
    icon: React.ElementType; color?: string;
}) {
    return (
        <div className="bg-white/5 border border-white/10 rounded-2xl p-5 hover:bg-white/8 transition-all">
            <div className="flex items-start justify-between">
                <div>
                    <p className="text-xs text-slate-400 font-medium uppercase tracking-wider">{label}</p>
                    <p className="text-2xl font-bold text-white mt-1">{value}</p>
                    {subLabel && <p className="text-xs text-slate-500 mt-0.5">{subLabel}</p>}
                </div>
                <div
                    className="p-2.5 rounded-xl"
                    style={{ backgroundColor: `${color}22`, border: `1px solid ${color}33` }}
                >
                    <Icon size={20} style={{ color }} />
                </div>
            </div>
        </div>
    );
}

// ── Main Dashboard ────────────────────────────────────────────────────────────
export function AutoPayDashboard() {
    const router = useRouter();
    const [dashboard, setDashboard] = React.useState<AutoPayDashboard | null>(null);
    const [schedules, setSchedules] = React.useState<AutoPaySchedule[]>([]);
    const [loading, setLoading] = React.useState(true);
    const [viewToggle, setViewToggle] = React.useState<'monthly' | 'annual'>('monthly');
    const [detectedPayments, setDetectedPayments] = React.useState<DetectedRecurringPayment[]>([]);
    const [showDetectedBanner, setShowDetectedBanner] = React.useState(false);
    const [detectedLoaded, setDetectedLoaded] = React.useState(false);
    const [showPaymentMethods, setShowPaymentMethods] = React.useState(false);

    const loadData = React.useCallback(async () => {
        setLoading(true);
        try {
            const [dashRes, schedulesRes] = await Promise.all([
                autoPayService.getDashboard(),
                autoPayService.listSchedules(0, 50),
            ]);
            setDashboard(dashRes.data);
            setSchedules(schedulesRes.data.content);
        } catch {
            toast.error('Failed to load AutoPay data');
        } finally {
            setLoading(false);
        }
    }, []);

    // Fetch detected recurring payments once on first load
    React.useEffect(() => {
        if (detectedLoaded) return;
        autoPayService.detectRecurring()
            .then(res => {
                if (res.data.length > 0) {
                    setDetectedPayments(res.data);
                    setShowDetectedBanner(true);
                }
            })
            .catch(() => { /* silently ignore — detection is best-effort */ })
            .finally(() => setDetectedLoaded(true));
    }, [detectedLoaded]);

    React.useEffect(() => { loadData(); }, [loadData]);

    const handleToggle = async (id: string) => {
        try {
            const res = await autoPayService.toggleSchedule(id);
            setSchedules((prev) =>
                prev.map((s) => (s.id === id ? res.data : s))
            );
            toast.success(res.data.active ? 'Payment resumed' : 'Payment paused');
        } catch {
            toast.error('Failed to update schedule');
        }
    };

    const handleDelete = async (id: string) => {
        if (!confirm('Deactivate this autopay schedule? It can be reactivated later.')) return;
        try {
            await autoPayService.deleteSchedule(id);
            setSchedules((prev) => prev.filter((s) => s.id !== id));
            toast.success('Schedule deactivated');
        } catch {
            toast.error('Failed to delete schedule');
        }
    };

    const handleExecute = async (id: string) => {
        try {
            await autoPayService.executePayment(id);
            toast.success('Payment execution triggered');
            loadData();
        } catch {
            toast.error('Execution failed');
        }
    };

    const overdue = schedules.filter((s) => s.status === 'OVERDUE');
    const dueSoon = schedules.filter((s) => s.status === 'DUE_SOON');

    const totalDisplayed = viewToggle === 'monthly'
        ? dashboard?.totalMonthlyObligations ?? 0
        : dashboard?.totalAnnualObligations ?? 0;

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold text-white">AutoPay Hub</h1>
                    <p className="text-slate-400 text-sm mt-0.5">
                        Manage all your recurring payments in one place
                    </p>
                </div>
                <div className="flex items-center gap-2">
                    <button
                        onClick={loadData}
                        className="p-2 rounded-xl bg-white/5 border border-white/10 text-slate-400 hover:text-white hover:bg-white/10 transition-all"
                        title="Refresh"
                    >
                        <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
                    </button>
                    <button
                        onClick={() => setShowPaymentMethods(true)}
                        className="flex items-center gap-2 px-4 py-2 bg-white/10 hover:bg-white/15 text-white rounded-xl text-sm font-medium transition-all"
                    >
                        <CreditCard size={16} />
                        Payment Methods
                    </button>
                    <button
                        onClick={() => router.push('/dashboard/autopay/new')}
                        className="flex items-center gap-2 px-4 py-2 bg-violet-600 hover:bg-violet-500 text-white rounded-xl text-sm font-medium transition-all shadow-lg shadow-violet-500/20"
                    >
                        <Plus size={16} />
                        Add Payment
                    </button>
                </div>
            </div>

            {/* Detected Recurring Payments Banner */}
            {showDetectedBanner && detectedPayments.length > 0 && (
                <DetectedPaymentsBanner
                    payments={detectedPayments}
                    onAddToHub={async (payment) => {
                        await autoPayService.addDetectedPayment(payment);
                        loadData();
                    }}
                    onDismiss={() => setShowDetectedBanner(false)}
                />
            )}

            {/* Alert banner */}
            <UpcomingPaymentsBanner
                overdue={overdue}
                dueSoon={dueSoon}
                onSelect={(id) => router.push(`/dashboard/autopay/${id}`)}
            />

            <PaymentMethodsModal
                isOpen={showPaymentMethods}
                onClose={() => setShowPaymentMethods(false)}
            />

            {/* Stats row */}
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                {/* Monthly/Annual toggle */}
                <div className="col-span-2 lg:col-span-1">
                    {loading ? (
                        <div className="h-28 rounded-2xl bg-white/5 animate-pulse" />
                    ) : (
                        <div className="bg-white/5 border border-white/10 rounded-2xl p-5">
                            <div className="flex items-center justify-between mb-1">
                                <p className="text-xs text-slate-400 font-medium uppercase tracking-wider">
                                    Obligations
                                </p>
                                <div className="flex text-xs rounded-lg overflow-hidden border border-white/10">
                                    {(['monthly', 'annual'] as const).map((v) => (
                                        <button
                                            key={v}
                                            onClick={() => setViewToggle(v)}
                                            className={`px-2 py-0.5 transition-colors ${viewToggle === v
                                                ? 'bg-violet-600 text-white'
                                                : 'text-slate-400 hover:text-white'
                                                }`}
                                        >
                                            {v === 'monthly' ? 'Mo' : 'Yr'}
                                        </button>
                                    ))}
                                </div>
                            </div>
                            <p className="text-2xl font-bold text-white">
                                {formatCurrency(totalDisplayed)}
                            </p>
                            <p className="text-xs text-slate-500 mt-0.5">
                                {dashboard?.activeScheduleCount ?? 0} active schedules
                            </p>
                        </div>
                    )}
                </div>

                {loading ? (
                    <>
                        <div className="h-28 rounded-2xl bg-white/5 animate-pulse" />
                        <div className="h-28 rounded-2xl bg-white/5 animate-pulse" />
                        <div className="h-28 rounded-2xl bg-white/5 animate-pulse" />
                    </>
                ) : (
                    <>
                        <StatCard
                            label="Due Soon"
                            value={String(dashboard?.dueSoonCount ?? 0)}
                            subLabel="Next 7 days"
                            icon={Calendar}
                            color="#f59e0b"
                        />
                        <StatCard
                            label="Overdue"
                            value={String(dashboard?.overdueCount ?? 0)}
                            subLabel="Action required"
                            icon={AlertTriangle}
                            color="#ef4444"
                        />
                        <StatCard
                            label="Schedules"
                            value={String(dashboard?.activeScheduleCount ?? 0)}
                            subLabel="Active payments"
                            icon={TrendingUp}
                            color="#22c55e"
                        />
                    </>
                )}
            </div>

            {/* Main content: list + health score */}
            <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
                {/* Schedule list (2/3 width) */}
                <div className="xl:col-span-2">
                    <AutoPayList
                        schedules={schedules}
                        loading={loading}
                        onToggle={handleToggle}
                        onDelete={handleDelete}
                        onExecute={handleExecute}
                        onSelect={(id) => router.push(`/dashboard/autopay/${id}`)}
                    />
                </div>

                {/* Sidebar: health + category breakdown */}
                <div className="space-y-5">
                    {/* Health score */}
                    {loading ? (
                        <div className="h-52 rounded-2xl bg-white/5 animate-pulse" />
                    ) : (
                        <div className="bg-white/5 border border-white/10 rounded-2xl p-6 flex flex-col items-center">
                            <PaymentHealthScore
                                score={dashboard?.paymentHealthScore ?? 0}
                                label={dashboard?.healthLabel ?? 'At Risk'}
                            />
                        </div>
                    )}

                    {/* Category breakdown */}
                    {!loading && dashboard && Object.keys(dashboard.categoryBreakdown).length > 0 && (
                        <div className="bg-white/5 border border-white/10 rounded-2xl p-5">
                            <p className="text-sm font-semibold text-white mb-4">By Category</p>
                            <div className="space-y-3">
                                {Object.entries(dashboard.categoryBreakdown)
                                    .sort((a, b) => b[1].monthlyTotal - a[1].monthlyTotal)
                                    .slice(0, 6)
                                    .map(([cat, stat]) => {
                                        const pct = dashboard.totalMonthlyObligations > 0
                                            ? (stat.monthlyTotal / dashboard.totalMonthlyObligations) * 100
                                            : 0;
                                        const color = CATEGORY_COLORS[cat as keyof typeof CATEGORY_COLORS] || '#94a3b8';
                                        return (
                                            <div key={cat}>
                                                <div className="flex justify-between text-xs mb-1">
                                                    <span className="text-slate-300">
                                                        {cat.replace(/_/g, ' ')}
                                                    </span>
                                                    <span className="text-slate-400">
                                                        {formatCurrency(stat.monthlyTotal)}/mo
                                                    </span>
                                                </div>
                                                <div className="h-1.5 bg-white/10 rounded-full overflow-hidden">
                                                    <div
                                                        className="h-full rounded-full transition-all"
                                                        style={{
                                                            width: `${Math.min(100, pct)}%`,
                                                            backgroundColor: color,
                                                        }}
                                                    />
                                                </div>
                                            </div>
                                        );
                                    })}
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
