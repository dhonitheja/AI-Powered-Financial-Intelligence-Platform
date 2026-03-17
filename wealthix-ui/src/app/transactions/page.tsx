"use client";

import React, { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { Card } from '@/components/ui/Card';
import {
    Search, Plus, CalendarDays, ArrowDownRight, ArrowUpRight,
    CheckCircle2, Clock, AlertCircle, Brain, RefreshCw, CreditCard, Building2, Download
} from 'lucide-react';
import { cn } from '@/components/ui/Card';
import { transactionService, exportService } from '@/services/api';
import AddTransactionModal from '@/components/shared/AddTransactionModal';
import AIDrawer from '@/components/shared/AIDrawer';

// ─── Filter type definitions ─────────────────────────────────────────────────
type Period = 'weekly' | 'monthly' | '6months' | 'yearly' | 'all';
type AccountTypeFilter = 'all' | 'credit' | 'debit';

const PERIOD_LABELS: Record<Period, string> = {
    weekly: 'Last 7 Days',
    monthly: 'Last 30 Days',
    '6months': 'Last 6 Months',
    yearly: 'Last 12 Months',
    all: 'All Time',
};

const TYPE_LABELS: Record<AccountTypeFilter, string> = {
    all: 'All Accounts',
    credit: 'Credit Cards',
    debit: 'Bank / Debit',
};

export default function TransactionsPage() {
    const [transactions, setTransactions] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [period, setPeriod] = useState<Period>('monthly');
    const [accountType, setAccountType] = useState<AccountTypeFilter>('all');
    const [searchTerm, setSearchTerm] = useState('');
    const [categoryFilter, setCategoryFilter] = useState('all');
    const [selectedTx, setSelectedTx] = useState<any>(null);
    const [isDrawerOpen, setIsDrawerOpen] = useState(false);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const router = useRouter();

    const fetchData = useCallback(async (activePeriod: Period, activeType: AccountTypeFilter) => {
        setLoading(true);
        const periodParam = activePeriod === 'all' ? undefined : activePeriod;
        const typeParam = activeType === 'all' ? undefined : activeType;
        try {
            const res = await transactionService.getAll(periodParam, typeParam);
            const data = Array.isArray(res.data) ? res.data : [];
            setTransactions(
                data.sort((a: any, b: any) =>
                    new Date(b.transactionDate).getTime() - new Date(a.transactionDate).getTime()
                )
            );
        } catch (err: any) {
            if (err.response?.status === 401) router.push('/login');
        } finally {
            setLoading(false);
        }
    }, [router]);

    useEffect(() => {
        fetchData(period, accountType);
    }, [period, accountType, fetchData]);

    const handleTriggerAnalysis = async (e: React.MouseEvent, id: string) => {
        e.stopPropagation();
        try {
            await transactionService.triggerAnalysis(id);
            setTimeout(() => fetchData(period, accountType), 1500);
        } catch (_) { }
    };

    const handleExport = async (format: 'csv' | 'pdf') => {
        try {
            const periodParam = period === 'all' ? undefined : period;
            const res = format === 'csv'
                ? await exportService.exportCsv(periodParam)
                : await exportService.exportPdf(periodParam);

            const url = window.URL.createObjectURL(new Blob([res.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `transactions_export.${format}`);
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch (error) {
            console.error('Failed to export', error);
        }
    };

    // ── Derived: unique categories from DB-filtered result ────────────────────
    const categories = ['all', ...Array.from(new Set(transactions.map(tx => tx.category).filter(Boolean)))];

    // ── Client-side search + category layered on top of DB-filtered data ──────
    const filtered = transactions.filter(tx => {
        const matchesSearch = !searchTerm ||
            tx.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
            tx.category?.toLowerCase().includes(searchTerm.toLowerCase());
        const matchesCategory = categoryFilter === 'all' || tx.category === categoryFilter;
        return matchesSearch && matchesCategory;
    });

    // ── Summary pills ──────────────────────────────────────────────────────────
    const totalSpending = filtered.filter(tx => tx.amount < 0).reduce((s, tx) => s + Math.abs(tx.amount), 0);
    const totalIncome = filtered.filter(tx => tx.amount > 0).reduce((s, tx) => s + tx.amount, 0);
    const highRisk = filtered.filter(tx => tx.fraudRiskScore > 0.6).length;

    const activeFilterLabel = [
        PERIOD_LABELS[period],
        accountType !== 'all' ? TYPE_LABELS[accountType] : null,
    ].filter(Boolean).join(' · ');

    return (
        <div className="p-6 md:p-8 max-w-7xl mx-auto space-y-6 animate-in fade-in duration-500">
            <AddTransactionModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                onSuccess={() => fetchData(period, accountType)}
                createTransaction={(data) => transactionService.create(data)}
            />
            <AIDrawer
                isOpen={isDrawerOpen}
                onClose={() => setIsDrawerOpen(false)}
                transaction={selectedTx}
            />

            {/* ── Header */}
            <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
                <div>
                    <h1 className="text-3xl font-black text-slate-100 tracking-tight">Transactions</h1>
                    <p className="text-slate-500 mt-1 font-medium">
                        AI-enriched records — {activeFilterLabel}
                    </p>
                </div>
                <div className="flex flex-wrap items-center gap-3">
                    <button
                        onClick={() => fetchData(period, accountType)}
                        className="p-2.5 bg-white/5 border border-white/10 rounded-xl text-slate-400 hover:bg-white/10 transition-all"
                        title="Refresh"
                    >
                        <RefreshCw className={cn("w-4 h-4", loading && "animate-spin")} />
                    </button>
                    <div className="flex bg-white/5 border border-white/10 rounded-xl overflow-hidden">
                        <button
                            onClick={() => handleExport('csv')}
                            className="px-4 py-2.5 text-xs font-bold text-slate-400 hover:bg-white/10 hover:text-slate-200 transition-all border-r border-white/10 flex items-center gap-2"
                        >
                            <Download className="w-3.5 h-3.5" /> CSV
                        </button>
                        <button
                            onClick={() => handleExport('pdf')}
                            className="px-4 py-2.5 text-xs font-bold text-slate-400 hover:bg-white/10 hover:text-rose-400 transition-all flex items-center gap-2"
                        >
                            <Download className="w-3.5 h-3.5" /> PDF
                        </button>
                    </div>
                    <button
                        onClick={() => setIsModalOpen(true)}
                        className="flex items-center gap-2 bg-gradient-to-r from-[#D4AF37] to-[#B8962E] text-[#0D0B1E] px-5 py-2.5 rounded-xl font-bold hover:scale-[1.02] transition-all shadow-gold"
                    >
                        <Plus className="w-4 h-4" />
                        Add Transaction
                    </button>
                </div>
            </div>

            {/* ── Summary Pills */}
            <div className="flex flex-wrap gap-3">
                <SummaryPill label="Showing" value={`${filtered.length} transactions`} color="slate" />
                <SummaryPill label="Spending" value={`$${totalSpending.toLocaleString(undefined, { maximumFractionDigits: 0 })}`} color="rose" />
                <SummaryPill label="Income" value={`$${totalIncome.toLocaleString(undefined, { maximumFractionDigits: 0 })}`} color="emerald" />
                {highRisk > 0 && (
                    <SummaryPill label="High Risk" value={`${highRisk} flagged`} color="amber" />
                )}
            </div>

            <Card className="p-0 overflow-hidden">
                {/* ── Filter Bar */}
                <div className="p-4 border-b border-white/10 flex flex-col md:flex-row gap-3 items-stretch md:items-center flex-wrap">

                    {/* Search */}
                    <div className="relative flex-1 min-w-[180px]">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                        <input
                            type="text"
                            placeholder="Search descriptions or categories..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full pl-9 pr-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-sm text-slate-200 placeholder:text-slate-600 focus:outline-none focus:ring-2 focus:ring-[#D4AF37]/20 focus:border-[#D4AF37]/30 transition-all font-medium"
                        />
                    </div>

                    {/* Period Dropdown */}
                    <FilterSelect
                        icon={<CalendarDays className="w-4 h-4 text-[#D4AF37] shrink-0" />}
                        value={period}
                        onChange={(v) => setPeriod(v as Period)}
                        options={Object.entries(PERIOD_LABELS) as [string, string][]}
                    />

                    {/* Account Type Dropdown */}
                    <FilterSelect
                        icon={accountType === 'credit'
                            ? <CreditCard className="w-4 h-4 text-rose-400 shrink-0" />
                            : <Building2 className="w-4 h-4 text-[#D4AF37] shrink-0" />}
                        value={accountType}
                        onChange={(v) => setAccountType(v as AccountTypeFilter)}
                        options={Object.entries(TYPE_LABELS) as [string, string][]}
                        highlightValue={accountType !== 'all' ? accountType : undefined}
                    />

                    {/* Category Dropdown */}
                    <FilterSelect
                        icon={<Brain className="w-4 h-4 text-[#D4AF37] shrink-0" />}
                        value={categoryFilter}
                        onChange={setCategoryFilter}
                        options={categories.map(c => [c, c === 'all' ? 'All Categories' : c])}
                    />
                </div>

                {/* ── Table */}
                <div className="overflow-x-auto max-h-[620px] overflow-y-auto">
                    <table className="w-full text-left border-collapse">
                        <thead className="sticky top-0 z-10 bg-[#0D0B1E]/95 backdrop-blur text-slate-500 text-[10px] uppercase tracking-wider font-bold border-b border-white/10">
                            <tr>
                                <th className="px-6 py-4">Status</th>
                                <th className="px-6 py-4">Transaction Details</th>
                                <th className="px-6 py-4">Category</th>
                                <th className="px-6 py-4">Account</th>
                                <th className="px-6 py-4 text-center">AI Risk</th>
                                <th className="px-6 py-4 text-right">Amount</th>
                                <th className="px-6 py-4 text-right pr-6">Action</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-white/5">
                            {loading ? (
                                [...Array(6)].map((_, i) => (
                                    <tr key={i}>
                                        <td colSpan={7} className="px-6 py-4">
                                            <div className="h-4 bg-white/5 rounded-lg animate-pulse" style={{ width: `${60 + (i * 7) % 30}%` }} />
                                        </td>
                                    </tr>
                                ))
                            ) : filtered.length === 0 ? (
                                <tr>
                                    <td colSpan={7} className="py-24 text-center">
                                        <div className="flex flex-col items-center gap-3 text-slate-500">
                                            <Brain className="w-10 h-10 text-slate-700" />
                                            <p className="font-bold">No transactions found</p>
                                            <p className="text-xs">Try adjusting the period, type, or search term</p>
                                        </div>
                                    </td>
                                </tr>
                            ) : (
                                filtered.map((tx) => (
                                    <TransactionRow
                                        key={tx.id}
                                        tx={tx}
                                        onRowClick={() => {
                                            setSelectedTx({ ...tx, risk: tx.fraudRiskScore, date: new Date(tx.transactionDate).toLocaleDateString() });
                                            setIsDrawerOpen(true);
                                        }}
                                        onAnalyze={(e: React.MouseEvent) => handleTriggerAnalysis(e, tx.id)}
                                    />
                                ))
                            )}
                        </tbody>
                    </table>
                </div>

                {/* ── Footer */}
                <div className="p-4 border-t border-white/10 flex items-center justify-between text-xs text-slate-500 font-semibold">
                    <span>
                        {filtered.length} of {transactions.length} records — {activeFilterLabel}
                        {categoryFilter !== 'all' && ` · ${categoryFilter}`}
                        {searchTerm && ` · "${searchTerm}"`}
                    </span>
                    {(searchTerm || categoryFilter !== 'all') && (
                        <button
                            onClick={() => { setSearchTerm(''); setCategoryFilter('all'); }}
                            className="text-[#D4AF37] hover:text-[#F5D67B] hover:underline font-bold transition-colors"
                        >
                            Clear search filters
                        </button>
                    )}
                </div>
            </Card>
        </div>
    );
}

// ─── Sub-components ─────────────────────────────────────────────────────────────

function FilterSelect({
    icon, value, onChange, options, highlightValue,
}: {
    icon: React.ReactNode;
    value: string;
    onChange: (v: string) => void;
    options: [string, string][];
    highlightValue?: string;
}) {
    return (
        <div className={cn(
            "relative flex items-center gap-2 bg-white/5 border rounded-xl px-4 py-2.5 shrink-0",
            highlightValue ? "border-[#D4AF37]/40 ring-1 ring-[#D4AF37]/20" : "border-white/10"
        )}>
            {icon}
            <select
                value={value}
                onChange={(e) => onChange(e.target.value)}
                className="appearance-none bg-transparent text-sm font-bold text-slate-200 pr-5 focus:outline-none cursor-pointer capitalize"
            >
                {options.map(([val, label]) => (
                    <option key={val} value={val} style={{ backgroundColor: '#0D0B1E', color: '#e2e8f0' }}>{label}</option>
                ))}
            </select>
            <div className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 text-xs">▾</div>
        </div>
    );
}

function SummaryPill({ label, value, color }: { label: string; value: string; color: string }) {
    const styles: Record<string, string> = {
        slate: 'bg-white/5 text-slate-400 border border-white/10',
        rose: 'bg-rose-500/15 text-rose-400 border border-rose-500/20',
        emerald: 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/20',
        amber: 'bg-amber-500/15 text-amber-400 border border-amber-500/20',
    };
    return (
        <div className={cn("flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold", styles[color])}>
            <span className="opacity-60 font-semibold">{label}</span>
            <span>{value}</span>
        </div>
    );
}

function TransactionRow({ tx, onRowClick, onAnalyze }: { tx: any; onRowClick: () => void; onAnalyze: (e: React.MouseEvent) => void }) {
    const risk = tx.fraudRiskScore;
    const analyzed = risk !== null && risk !== undefined;
    const isCredit = tx.accountType === 'CREDIT';

    const getStatus = () => {
        if (!analyzed) return 'PENDING';
        if (tx.aiExplanation) return 'ANALYZED';
        return 'ANALYZING';
    };

    const getRiskBar = (r: number) => {
        if (r < 0.2) return { bar: 'bg-emerald-500', text: 'text-emerald-400', label: 'Safe' };
        if (r < 0.5) return { bar: 'bg-amber-500', text: 'text-amber-400', label: 'Medium' };
        return { bar: 'bg-rose-500', text: 'text-rose-400', label: 'High' };
    };

    const status = getStatus();
    const riskStyle = analyzed ? getRiskBar(risk) : null;

    return (
        <tr className="hover:bg-white/5 transition-colors cursor-pointer group" onClick={onRowClick}>
            {/* Status */}
            <td className="px-6 py-4">
                {status === 'ANALYZED' && (
                    <div className="flex items-center gap-1.5 text-emerald-400 font-bold text-[10px] uppercase">
                        <CheckCircle2 className="w-3.5 h-3.5" /> Analyzed
                    </div>
                )}
                {status === 'ANALYZING' && (
                    <div className="flex items-center gap-1.5 text-[#D4AF37] font-bold text-[10px] uppercase animate-pulse">
                        <Clock className="w-3.5 h-3.5" /> Analyzing
                    </div>
                )}
                {status === 'PENDING' && (
                    <div className="flex items-center gap-1.5 text-slate-500 font-bold text-[10px] uppercase">
                        <AlertCircle className="w-3.5 h-3.5" /> Pending
                    </div>
                )}
            </td>

            {/* Description + Date */}
            <td className="px-6 py-4">
                <p className="font-bold text-sm text-slate-200 leading-tight">{tx.description}</p>
                <p className="text-[10px] text-slate-500 mt-0.5 font-medium uppercase tracking-tight">
                    {new Date(tx.transactionDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}
                </p>
            </td>

            {/* Category */}
            <td className="px-6 py-4">
                <span className="px-2.5 py-1 rounded-lg bg-white/10 text-slate-400 text-[10px] font-black uppercase tracking-wider">
                    {tx.category}
                </span>
            </td>

            {/* Account Type Badge */}
            <td className="px-6 py-4">
                <div className={cn(
                    "inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-[10px] font-black uppercase",
                    isCredit ? "bg-rose-500/15 text-rose-400" : "bg-[#D4AF37]/15 text-[#D4AF37]"
                )}>
                    {isCredit
                        ? <CreditCard className="w-3 h-3" />
                        : <Building2 className="w-3 h-3" />}
                    {tx.accountType ?? 'DEBIT'}
                </div>
            </td>

            {/* AI Risk */}
            <td className="px-6 py-4">
                {analyzed && riskStyle ? (
                    <div className="flex flex-col items-center gap-1">
                        <div className="w-20 h-1.5 bg-white/10 rounded-full overflow-hidden">
                            <div className={cn("h-full rounded-full transition-all", riskStyle.bar)} style={{ width: `${risk * 100}%` }} />
                        </div>
                        <span className={cn("text-[9px] font-black uppercase", riskStyle.text)}>
                            {riskStyle.label} {(risk * 100).toFixed(0)}%
                        </span>
                    </div>
                ) : (
                    <span className="text-slate-600 text-[10px] font-bold uppercase block text-center">—</span>
                )}
            </td>

            {/* Amount */}
            <td className={cn("px-6 py-4 text-right font-black text-sm", tx.amount > 0 ? "text-emerald-400" : "text-slate-200")}>
                <span className="flex items-center justify-end gap-1">
                    {tx.amount > 0 ? <ArrowUpRight className="w-3 h-3" /> : <ArrowDownRight className="w-3 h-3" />}
                    ${Math.abs(tx.amount).toLocaleString()}
                </span>
            </td>

            {/* Action */}
            <td className="px-6 py-4 text-right pr-6">
                {!analyzed ? (
                    <button
                        onClick={onAnalyze}
                        className="text-[10px] font-extrabold text-[#D4AF37] flex items-center gap-1 ml-auto hover:underline"
                    >
                        <Brain className="w-3 h-3" /> Analyze
                    </button>
                ) : (
                    <button className="text-slate-600 group-hover:text-[#D4AF37] transition-colors ml-auto">
                        <ArrowUpRight className="w-4 h-4" />
                    </button>
                )}
            </td>
        </tr>
    );
}
