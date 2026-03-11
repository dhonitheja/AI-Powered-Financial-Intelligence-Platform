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
                    <h1 className="text-3xl font-black text-primary tracking-tight">Transactions</h1>
                    <p className="text-slate-500 mt-1 font-medium">
                        AI-enriched records — {activeFilterLabel}
                    </p>
                </div>
                <div className="flex flex-wrap items-center gap-3">
                    <button
                        onClick={() => fetchData(period, accountType)}
                        className="p-2.5 bg-white border border-slate-200 rounded-xl text-slate-500 hover:bg-slate-50 transition-all shadow-sm"
                        title="Refresh"
                    >
                        <RefreshCw className={cn("w-4 h-4", loading && "animate-spin")} />
                    </button>
                    <div className="flex bg-white border border-slate-200 rounded-xl shadow-sm overflow-hidden">
                        <button
                            onClick={() => handleExport('csv')}
                            className="px-4 py-2.5 text-xs font-bold text-slate-600 hover:bg-slate-50 hover:text-primary transition-all border-r border-slate-100 flex items-center gap-2"
                        >
                            <Download className="w-3.5 h-3.5" /> CSV
                        </button>
                        <button
                            onClick={() => handleExport('pdf')}
                            className="px-4 py-2.5 text-xs font-bold text-slate-600 hover:bg-slate-50 hover:text-rose-600 transition-all flex items-center gap-2"
                        >
                            <Download className="w-3.5 h-3.5" /> PDF
                        </button>
                    </div>
                    <button
                        onClick={() => setIsModalOpen(true)}
                        className="flex items-center gap-2 bg-primary text-white px-5 py-2.5 rounded-xl font-bold hover:scale-[1.02] transition-all shadow-lg shadow-slate-900/10"
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
                <div className="p-4 border-b border-slate-100 flex flex-col md:flex-row gap-3 items-stretch md:items-center flex-wrap">

                    {/* Search */}
                    <div className="relative flex-1 min-w-[180px]">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                        <input
                            type="text"
                            placeholder="Search descriptions or categories..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full pl-9 pr-4 py-2.5 bg-slate-50 border border-slate-100 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-secondary/20 transition-all font-medium"
                        />
                    </div>

                    {/* Period Dropdown */}
                    <FilterSelect
                        icon={<CalendarDays className="w-4 h-4 text-secondary shrink-0" />}
                        value={period}
                        onChange={(v) => setPeriod(v as Period)}
                        options={Object.entries(PERIOD_LABELS) as [string, string][]}
                    />

                    {/* Account Type Dropdown */}
                    <FilterSelect
                        icon={accountType === 'credit'
                            ? <CreditCard className="w-4 h-4 text-rose-500 shrink-0" />
                            : <Building2 className="w-4 h-4 text-secondary shrink-0" />}
                        value={accountType}
                        onChange={(v) => setAccountType(v as AccountTypeFilter)}
                        options={Object.entries(TYPE_LABELS) as [string, string][]}
                        highlightValue={accountType !== 'all' ? accountType : undefined}
                    />

                    {/* Category Dropdown */}
                    <FilterSelect
                        icon={<Brain className="w-4 h-4 text-secondary shrink-0" />}
                        value={categoryFilter}
                        onChange={setCategoryFilter}
                        options={categories.map(c => [c, c === 'all' ? 'All Categories' : c])}
                    />
                </div>

                {/* ── Table */}
                <div className="overflow-x-auto max-h-[620px] overflow-y-auto">
                    <table className="w-full text-left border-collapse">
                        <thead className="sticky top-0 z-10 bg-slate-50/90 backdrop-blur text-slate-400 text-[10px] uppercase tracking-wider font-bold border-b border-slate-100">
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
                        <tbody className="divide-y divide-slate-50">
                            {loading ? (
                                [...Array(6)].map((_, i) => (
                                    <tr key={i}>
                                        <td colSpan={7} className="px-6 py-4">
                                            <div className="h-4 bg-slate-100 rounded-lg animate-pulse" style={{ width: `${60 + (i * 7) % 30}%` }} />
                                        </td>
                                    </tr>
                                ))
                            ) : filtered.length === 0 ? (
                                <tr>
                                    <td colSpan={7} className="py-24 text-center">
                                        <div className="flex flex-col items-center gap-3 text-slate-400">
                                            <Brain className="w-10 h-10 text-slate-200" />
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
                <div className="p-4 border-t border-slate-100 flex items-center justify-between text-xs text-slate-400 font-semibold">
                    <span>
                        {filtered.length} of {transactions.length} records — {activeFilterLabel}
                        {categoryFilter !== 'all' && ` · ${categoryFilter}`}
                        {searchTerm && ` · "${searchTerm}"`}
                    </span>
                    {(searchTerm || categoryFilter !== 'all') && (
                        <button
                            onClick={() => { setSearchTerm(''); setCategoryFilter('all'); }}
                            className="text-secondary hover:underline font-bold"
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
            "relative flex items-center gap-2 bg-white border rounded-xl px-4 py-2.5 shadow-sm shrink-0",
            highlightValue ? "border-secondary/40 ring-1 ring-secondary/20" : "border-slate-200"
        )}>
            {icon}
            <select
                value={value}
                onChange={(e) => onChange(e.target.value)}
                className="appearance-none bg-transparent text-sm font-bold text-primary pr-5 focus:outline-none cursor-pointer capitalize"
            >
                {options.map(([val, label]) => (
                    <option key={val} value={val}>{label}</option>
                ))}
            </select>
            <div className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 text-xs">▾</div>
        </div>
    );
}

function SummaryPill({ label, value, color }: { label: string; value: string; color: string }) {
    const styles: Record<string, string> = {
        slate: 'bg-slate-100 text-slate-600',
        rose: 'bg-rose-50 text-rose-700 border border-rose-100',
        emerald: 'bg-emerald-50 text-emerald-700 border border-emerald-100',
        amber: 'bg-amber-50 text-amber-700 border border-amber-100',
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
        if (r < 0.2) return { bar: 'bg-emerald-500', text: 'text-emerald-700', label: 'Safe' };
        if (r < 0.5) return { bar: 'bg-amber-500', text: 'text-amber-700', label: 'Medium' };
        return { bar: 'bg-rose-500', text: 'text-rose-700', label: 'High' };
    };

    const status = getStatus();
    const riskStyle = analyzed ? getRiskBar(risk) : null;

    return (
        <tr className="hover:bg-slate-50/60 transition-colors cursor-pointer group" onClick={onRowClick}>
            {/* Status */}
            <td className="px-6 py-4">
                {status === 'ANALYZED' && (
                    <div className="flex items-center gap-1.5 text-emerald-600 font-bold text-[10px] uppercase">
                        <CheckCircle2 className="w-3.5 h-3.5" /> Analyzed
                    </div>
                )}
                {status === 'ANALYZING' && (
                    <div className="flex items-center gap-1.5 text-secondary font-bold text-[10px] uppercase animate-pulse">
                        <Clock className="w-3.5 h-3.5" /> Analyzing
                    </div>
                )}
                {status === 'PENDING' && (
                    <div className="flex items-center gap-1.5 text-slate-400 font-bold text-[10px] uppercase">
                        <AlertCircle className="w-3.5 h-3.5" /> Pending
                    </div>
                )}
            </td>

            {/* Description + Date */}
            <td className="px-6 py-4">
                <p className="font-bold text-sm text-primary leading-tight">{tx.description}</p>
                <p className="text-[10px] text-slate-400 mt-0.5 font-medium uppercase tracking-tight">
                    {new Date(tx.transactionDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}
                </p>
            </td>

            {/* Category */}
            <td className="px-6 py-4">
                <span className="px-2.5 py-1 rounded-lg bg-slate-100 text-slate-600 text-[10px] font-black uppercase tracking-wider">
                    {tx.category}
                </span>
            </td>

            {/* Account Type Badge */}
            <td className="px-6 py-4">
                <div className={cn(
                    "inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-[10px] font-black uppercase",
                    isCredit ? "bg-rose-50 text-rose-700" : "bg-secondary/10 text-secondary"
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
                        <div className="w-20 h-1.5 bg-slate-100 rounded-full overflow-hidden">
                            <div className={cn("h-full rounded-full transition-all", riskStyle.bar)} style={{ width: `${risk * 100}%` }} />
                        </div>
                        <span className={cn("text-[9px] font-black uppercase", riskStyle.text)}>
                            {riskStyle.label} {(risk * 100).toFixed(0)}%
                        </span>
                    </div>
                ) : (
                    <span className="text-slate-300 text-[10px] font-bold uppercase block text-center">—</span>
                )}
            </td>

            {/* Amount */}
            <td className={cn("px-6 py-4 text-right font-black text-sm", tx.amount > 0 ? "text-emerald-600" : "text-primary")}>
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
                        className="text-[10px] font-extrabold text-secondary flex items-center gap-1 ml-auto hover:underline"
                    >
                        <Brain className="w-3 h-3" /> Analyze
                    </button>
                ) : (
                    <button className="text-slate-200 group-hover:text-secondary transition-colors ml-auto">
                        <ArrowUpRight className="w-4 h-4" />
                    </button>
                )}
            </td>
        </tr>
    );
}
