"use client";

import React, { useState, useEffect, useMemo } from 'react';
import Link from 'next/link';
import {
    Landmark, Calculator, TrendingDown, CircleDollarSign,
    CreditCard, Home, Car, GraduationCap, User2,
    CalendarClock, CheckCircle2, XCircle, AlertTriangle, Plus
} from 'lucide-react';
import {
    autoPayService,
    AutoPaySchedule,
    formatCurrency,
    daysUntilDue,
    STATUS_COLORS,
    STATUS_BG,
    FREQUENCY_LABELS,
    PaymentCategory,
} from '@/services/autoPayService';
import { cn } from '@/components/ui/Card';

// ── Loan category config ───────────────────────────────────────────────────────
const LOAN_CATEGORIES: PaymentCategory[] = [
    'HOME_LOAN', 'AUTO_LOAN', 'PERSONAL_LOAN', 'EDUCATION_LOAN',
];

const LOAN_META: Record<string, { label: string; icon: React.ElementType; color: string; bg: string }> = {
    HOME_LOAN:      { label: 'Home Loan',      icon: Home,           color: 'text-blue-400',    bg: 'bg-blue-500/15' },
    AUTO_LOAN:      { label: 'Auto Loan',       icon: Car,            color: 'text-violet-400',  bg: 'bg-violet-500/15' },
    PERSONAL_LOAN:  { label: 'Personal Loan',   icon: User2,          color: 'text-amber-400',   bg: 'bg-amber-500/15' },
    EDUCATION_LOAN: { label: 'Education Loan',  icon: GraduationCap,  color: 'text-emerald-400', bg: 'bg-emerald-500/15' },
};

// ── EMI formula (standard PMT) ─────────────────────────────────────────────────
function calcEmi(principal: number, annualRate: number, tenureMonths: number) {
    if (!principal || !annualRate || !tenureMonths) return null;
    const r = annualRate / 12 / 100;
    const n = tenureMonths;
    const emi = (principal * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
    const total = emi * n;
    const interest = total - principal;
    return { emi, total, interest };
}

// ── Stat Card ──────────────────────────────────────────────────────────────────
function StatCard({
    icon: Icon, label, value, sub, iconColor, iconBg,
}: {
    icon: React.ElementType; label: string; value: string; sub?: string;
    iconColor: string; iconBg: string;
}) {
    return (
        <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-5 flex items-center gap-4">
            <div className={cn('w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0', iconBg)}>
                <Icon className={cn('w-6 h-6', iconColor)} />
            </div>
            <div>
                <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-0.5">{label}</p>
                <p className="text-2xl font-black text-slate-100 leading-none">{value}</p>
                {sub && <p className="text-xs text-slate-500 mt-1">{sub}</p>}
            </div>
        </div>
    );
}

// ── Loan Card ─────────────────────────────────────────────────────────────────
function LoanCard({ schedule }: { schedule: AutoPaySchedule }) {
    const meta = LOAN_META[schedule.paymentCategory] ?? LOAN_META['PERSONAL_LOAN'];
    const Icon = meta.icon;
    const days = daysUntilDue(schedule.nextDueDate);
    const daysLabel = days < 0
        ? `${Math.abs(days)}d overdue`
        : days === 0 ? 'Due today' : `${days}d left`;
    const daysColor = days < 0
        ? 'text-rose-400 bg-rose-500/15'
        : days <= 7 ? 'text-amber-400 bg-amber-500/15'
        : 'text-emerald-400 bg-emerald-500/15';

    return (
        <Link
            href={`/dashboard/autopay/${schedule.id}`}
            className="group bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-5 flex items-center gap-4 hover:border-[#D4AF37]/30 hover:bg-white/8 transition-all"
        >
            <div className={cn('w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0', meta.bg)}>
                <Icon className={cn('w-6 h-6', meta.color)} />
            </div>

            <div className="flex-1 min-w-0">
                <div className="flex items-start justify-between gap-2">
                    <div className="min-w-0">
                        <p className="font-bold text-slate-200 truncate text-sm">{schedule.paymentName}</p>
                        <p className={cn('text-[11px] font-semibold px-2 py-0.5 rounded-full inline-block mt-1', meta.bg, meta.color)}>
                            {meta.label}
                        </p>
                    </div>
                    <div className="text-right flex-shrink-0">
                        <p className="font-black text-slate-100 text-base">{formatCurrency(schedule.amount, schedule.currency)}</p>
                        <p className="text-[11px] text-slate-500 font-medium">{FREQUENCY_LABELS[schedule.frequency]}</p>
                    </div>
                </div>

                <div className="flex items-center gap-3 mt-3">
                    <div className="flex items-center gap-1.5 text-slate-500 text-xs">
                        <CalendarClock className="w-3.5 h-3.5" />
                        <span>{new Date(schedule.nextDueDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}</span>
                    </div>
                    <span className={cn('text-[11px] font-bold px-2 py-0.5 rounded-full', daysColor)}>
                        {daysLabel}
                    </span>
                    <span className={cn('ml-auto text-[11px] font-bold px-2 py-0.5 rounded-full', STATUS_BG[schedule.status], STATUS_COLORS[schedule.status])}>
                        {schedule.status.replace('_', ' ')}
                    </span>
                </div>
            </div>
        </Link>
    );
}

// ── EMI Calculator ────────────────────────────────────────────────────────────
function EmiCalculator() {
    const [principal, setPrincipal] = useState('');
    const [rate, setRate] = useState('');
    const [tenure, setTenure] = useState('');
    const [result, setResult] = useState<{ emi: number; total: number; interest: number } | null>(null);

    const calculate = () => {
        const res = calcEmi(Number(principal), Number(rate), Number(tenure));
        setResult(res);
    };

    const inputClass = "w-full px-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-slate-200 placeholder:text-slate-600 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-[#D4AF37]/20 focus:border-[#D4AF37]/40 transition-all";

    return (
        <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6 sticky top-6">
            <div className="flex items-center gap-3 mb-5">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-[#D4AF37] to-[#B8962E] flex items-center justify-center shadow-gold">
                    <Calculator className="w-5 h-5 text-[#0D0B1E]" />
                </div>
                <div>
                    <h3 className="font-bold text-slate-100 text-sm">EMI Calculator</h3>
                    <p className="text-[11px] text-slate-500">Standard reducing-balance method</p>
                </div>
            </div>

            <div className="space-y-4">
                <div>
                    <label className="text-xs font-semibold text-slate-400 mb-1.5 block">Loan Amount ($)</label>
                    <input
                        type="number"
                        value={principal}
                        onChange={e => setPrincipal(e.target.value)}
                        placeholder="e.g. 500000"
                        className={inputClass}
                    />
                </div>
                <div>
                    <label className="text-xs font-semibold text-slate-400 mb-1.5 block">Annual Interest Rate (%)</label>
                    <input
                        type="number"
                        value={rate}
                        onChange={e => setRate(e.target.value)}
                        placeholder="e.g. 10"
                        step="0.1"
                        className={inputClass}
                    />
                </div>
                <div>
                    <label className="text-xs font-semibold text-slate-400 mb-1.5 block">Loan Tenure (months)</label>
                    <input
                        type="number"
                        value={tenure}
                        onChange={e => setTenure(e.target.value)}
                        placeholder="e.g. 60"
                        className={inputClass}
                    />
                </div>

                <button
                    onClick={calculate}
                    disabled={!principal || !rate || !tenure}
                    className="w-full bg-gradient-to-r from-[#D4AF37] to-[#B8962E] text-[#0D0B1E] py-3 rounded-xl font-black text-sm shadow-gold hover:scale-[1.02] active:scale-[0.98] transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                >
                    Calculate EMI
                </button>
            </div>

            {result && (
                <div className="mt-5 space-y-3 border-t border-white/10 pt-5">
                    <div className="flex justify-between items-center">
                        <span className="text-xs font-semibold text-slate-400">Monthly EMI</span>
                        <span className="font-black text-lg gold-text">{formatCurrency(result.emi)}</span>
                    </div>
                    <div className="flex justify-between items-center">
                        <span className="text-xs font-semibold text-slate-400">Total Interest</span>
                        <span className="font-bold text-rose-400 text-sm">{formatCurrency(result.interest)}</span>
                    </div>
                    <div className="flex justify-between items-center border-t border-white/10 pt-3">
                        <span className="text-xs font-semibold text-slate-400">Total Payment</span>
                        <span className="font-bold text-slate-200 text-sm">{formatCurrency(result.total)}</span>
                    </div>

                    {/* Interest ratio bar */}
                    <div className="mt-2">
                        <div className="flex justify-between text-[10px] text-slate-600 mb-1">
                            <span>Principal</span><span>Interest</span>
                        </div>
                        <div className="h-2 bg-white/10 rounded-full overflow-hidden flex">
                            <div
                                className="h-full bg-gradient-to-r from-[#D4AF37] to-[#B8962E] rounded-l-full"
                                style={{ width: `${(Number(principal) / result.total) * 100}%` }}
                            />
                            <div className="h-full flex-1 bg-rose-500/40 rounded-r-full" />
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

// ── Page ──────────────────────────────────────────────────────────────────────
export default function LoansPage() {
    const [schedules, setSchedules] = useState<AutoPaySchedule[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        autoPayService.listSchedules(0, 100)
            .then(res => setSchedules(res.data.content))
            .catch(() => {})
            .finally(() => setIsLoading(false));
    }, []);

    const loanSchedules = useMemo(
        () => schedules.filter(s => LOAN_CATEGORIES.includes(s.paymentCategory)),
        [schedules]
    );

    const totalMonthly = loanSchedules.reduce((sum, s) => sum + (s.monthlyEquivalent ?? 0), 0);
    const totalAmount = loanSchedules.reduce((sum, s) => sum + s.amount, 0);
    const activeCount = loanSchedules.filter(s => s.active).length;

    return (
        <div className="p-8 min-h-full">
            {/* ── Header */}
            <div className="flex items-center justify-between mb-8">
                <div>
                    <div className="flex items-center gap-3 mb-1">
                        <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-[#D4AF37] to-[#B8962E] flex items-center justify-center shadow-gold">
                            <Landmark className="w-5 h-5 text-[#0D0B1E]" />
                        </div>
                        <h1 className="text-3xl font-black tracking-tight text-slate-100">Loans & EMI</h1>
                    </div>
                    <p className="text-slate-500 text-sm ml-12">Your loan obligations at a glance</p>
                </div>
                <Link
                    href="/dashboard/autopay/new"
                    className="flex items-center gap-2 bg-gradient-to-r from-[#D4AF37] to-[#B8962E] text-[#0D0B1E] px-4 py-2.5 rounded-xl font-bold text-sm shadow-gold hover:scale-[1.02] transition-all"
                >
                    <Plus className="w-4 h-4" />
                    Add Loan
                </Link>
            </div>

            {/* ── Stats */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
                <StatCard
                    icon={CircleDollarSign}
                    label="Total Monthly EMI"
                    value={formatCurrency(totalMonthly)}
                    sub="Combined monthly obligations"
                    iconColor="text-[#D4AF37]"
                    iconBg="bg-[#D4AF37]/15"
                />
                <StatCard
                    icon={TrendingDown}
                    label="Total Outstanding"
                    value={formatCurrency(totalAmount)}
                    sub="Sum of all loan amounts"
                    iconColor="text-rose-400"
                    iconBg="bg-rose-500/15"
                />
                <StatCard
                    icon={CreditCard}
                    label="Active Loans"
                    value={String(activeCount)}
                    sub={`of ${loanSchedules.length} total loan schedules`}
                    iconColor="text-emerald-400"
                    iconBg="bg-emerald-500/15"
                />
            </div>

            {/* ── Main layout */}
            <div className="flex gap-6 items-start">
                {/* Left: loan list */}
                <div className="flex-1 min-w-0">
                    <h2 className="text-sm font-bold text-slate-400 uppercase tracking-widest mb-4">
                        Loan Schedules ({loanSchedules.length})
                    </h2>

                    {isLoading ? (
                        <div className="space-y-3">
                            {[1, 2, 3].map(i => (
                                <div key={i} className="bg-white/5 border border-white/10 rounded-2xl p-5 h-24 animate-pulse" />
                            ))}
                        </div>
                    ) : loanSchedules.length === 0 ? (
                        <div className="bg-white/5 border border-white/10 rounded-2xl p-12 text-center">
                            <Landmark className="w-12 h-12 text-slate-600 mx-auto mb-3" />
                            <p className="text-slate-400 font-semibold mb-1">No loan schedules yet</p>
                            <p className="text-slate-600 text-sm mb-5">
                                Add your home, auto, personal, or education loans to track them here.
                            </p>
                            <Link
                                href="/dashboard/autopay/new"
                                className="inline-flex items-center gap-2 bg-gradient-to-r from-[#D4AF37] to-[#B8962E] text-[#0D0B1E] px-5 py-2.5 rounded-xl font-bold text-sm shadow-gold hover:scale-[1.02] transition-all"
                            >
                                <Plus className="w-4 h-4" /> Add your first loan
                            </Link>
                        </div>
                    ) : (
                        <div className="space-y-3">
                            {loanSchedules.map(s => <LoanCard key={s.id} schedule={s} />)}
                        </div>
                    )}
                </div>

                {/* Right: EMI calculator */}
                <div className="w-80 flex-shrink-0">
                    <EmiCalculator />
                </div>
            </div>
        </div>
    );
}
