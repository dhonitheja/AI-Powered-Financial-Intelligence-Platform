"use client";

import React, { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { usePollingSync } from '@/hooks/usePollingSync';
import { Card } from '@/components/ui/Card';
import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
    PieChart, Pie, Cell
} from 'recharts';
import {
    Brain, ShieldAlert, TrendingDown, Wallet,
    ArrowUpRight, ArrowDownRight, RefreshCw, Loader2, CalendarDays
} from 'lucide-react';
import AddTransactionModal from '@/components/shared/AddTransactionModal';
import AIDrawer from '@/components/shared/AIDrawer';
import { GamificationCard } from '@/components/dashboard/GamificationCard';
import { transactionService, plaidService } from '@/services/api';
import { cn } from '@/components/ui/Card';

const COLORS = ['#14B8A6', '#F59E0B', '#0F172A', '#E11D48', '#6366F1', '#8B5CF6', '#EC4899'];

type Period = 'weekly' | 'monthly' | 'yearly' | 'all';

const PERIOD_LABELS: Record<Period, string> = {
    weekly: 'Last 7 Days',
    monthly: 'Last 30 Days',
    yearly: 'Last 12 Months',
    all: 'All Time',
};

export default function Dashboard() {
    const [transactions, setTransactions] = useState<any[]>([]);
    const [summary, setSummary] = useState<any[]>([]);
    const [financialSummary, setFinancialSummary] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [period, setPeriod] = useState<Period>('monthly');
    const [syncToast, setSyncToast] = useState(false);

    const [selectedTx, setSelectedTx] = useState<any>(null);
    const [isDrawerOpen, setIsDrawerOpen] = useState(false);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const router = useRouter();

    const fetchData = useCallback(async (activePeriod: Period) => {
        console.log(`[Dashboard] Fetching data — period=${activePeriod}`);
        setLoading(true);
        const periodParam = activePeriod === 'all' ? undefined : activePeriod;
        try {
            const [txRes, summaryRes, accountRes] = await Promise.all([
                transactionService.getAll(periodParam),
                transactionService.getSummary(periodParam),
                plaidService.getAccountSummary().catch(() => null),
            ]);

            const txData = Array.isArray(txRes.data) ? txRes.data : [];
            setTransactions(
                txData.sort((a: any, b: any) =>
                    new Date(b.transactionDate).getTime() - new Date(a.transactionDate).getTime()
                )
            );
            setSummary(Array.isArray(summaryRes.data) ? summaryRes.data : []);
            if (accountRes?.data) setFinancialSummary(accountRes.data);
            console.log(`[Dashboard] Loaded ${txData.length} transactions`);
        } catch (error: any) {
            console.error('[Dashboard] Fetch error:', error);
            if (error.response?.status === 401) {
                router.push('/login');
            }
        } finally {
            setLoading(false);
        }
    }, [router]);

    useEffect(() => {
        fetchData(period);
    }, [period, fetchData]);

    // ── Silent background polling: detects new Plaid sync, refreshes data ──────
    usePollingSync({
        enabled: true,
        onNewData: () => {
            setSyncToast(true);
            fetchData(period);
            setTimeout(() => setSyncToast(false), 4000);
        },
    });

    const handleRowClick = (tx: any) => {
        setSelectedTx({ ...tx, risk: tx.fraudRiskScore, date: new Date(tx.transactionDate).toLocaleDateString() });
        setIsDrawerOpen(true);
    };

    const handleTriggerAnalysis = async (e: React.MouseEvent, id: string) => {
        e.stopPropagation();
        try {
            await transactionService.triggerAnalysis(id);
            setTimeout(() => fetchData(period), 1500);
        } catch (err) {
            console.error('[Dashboard] Analysis trigger failed', err);
        }
    };

    // ── Derived Metrics ──────────────────────────────────────────────────────
    const totalSpent = transactions
        .filter(tx => tx.amount < 0)
        .reduce((acc, tx) => acc + Math.abs(tx.amount), 0);

    const totalIncome = transactions
        .filter(tx => tx.amount > 0)
        .reduce((acc, tx) => acc + tx.amount, 0);

    const highRiskCount = transactions.filter(tx => tx.fraudRiskScore > 0.6).length;
    const netFlow = totalIncome - totalSpent;

    const pieData = (summary || []).map((item, i) => ({
        name: item.category,
        value: Math.abs(item.totalSpending),
        color: COLORS[i % COLORS.length],
    }));

    // Build bar chart data from transactions grouped by time bucket
    const barData = buildBarChartData(transactions, period);

    return (
        <div className="p-6 md:p-8 max-w-7xl mx-auto space-y-8 animate-in fade-in duration-700">
            <AIDrawer isOpen={isDrawerOpen} onClose={() => setIsDrawerOpen(false)} transaction={selectedTx} />
            <AddTransactionModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                onSuccess={() => fetchData(period)}
                createTransaction={(data) => transactionService.create(data)}
            />

            {/* ── Sync Toast ───────────────────────────────────────────────── */}
            {syncToast && (
                <div className="fixed bottom-6 right-6 z-50 flex items-center gap-3 bg-slate-900 text-white px-5 py-3.5 rounded-2xl shadow-2xl animate-in slide-in-from-bottom-4 duration-300">
                    <div className="w-2 h-2 rounded-full bg-secondary animate-pulse" />
                    <span className="text-sm font-bold">New transactions synced from your bank</span>
                </div>
            )}

            {/* ── Header ──────────────────────────────────────────────────── */}
            <div className="flex flex-wrap justify-between items-end gap-4">
                <div>
                    <h1 className="text-3xl font-black text-primary tracking-tight">AI Finance Assistant</h1>
                    <p className="text-slate-500 mt-1 font-medium">Intelligent insights powered by Gemini AI.</p>
                </div>
                <div className="flex items-center gap-3">
                    {/* Period Dropdown */}
                    <div className="relative flex items-center gap-2 bg-white border border-slate-200 rounded-xl px-4 py-2.5 shadow-sm">
                        <CalendarDays className="w-4 h-4 text-secondary shrink-0" />
                        <select
                            value={period}
                            onChange={(e) => setPeriod(e.target.value as Period)}
                            className="appearance-none bg-transparent text-sm font-bold text-primary pr-6 focus:outline-none cursor-pointer"
                        >
                            {(Object.keys(PERIOD_LABELS) as Period[]).map((p) => (
                                <option key={p} value={p}>{PERIOD_LABELS[p]}</option>
                            ))}
                        </select>
                        <div className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2">
                            <ArrowDownRight className="w-3 h-3 text-slate-400" />
                        </div>
                    </div>

                    <button
                        onClick={() => fetchData(period)}
                        className="p-2.5 bg-white border border-slate-200 rounded-xl text-slate-600 hover:bg-slate-50 transition-all"
                        title="Refresh"
                    >
                        <RefreshCw className={cn("w-5 h-5", loading && "animate-spin")} />
                    </button>
                    <button
                        onClick={() => setIsModalOpen(true)}
                        className="bg-primary text-white px-5 py-2.5 rounded-xl font-bold shadow-lg shadow-slate-900/20 hover:scale-105 active:scale-95 transition-all"
                    >
                        Add Transaction
                    </button>
                </div>
            </div>
            
            <GamificationCard />

            {/* ── Stat Cards ──────────────────────────────────────────────── */}
            {loading ? (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                    {[...Array(4)].map((_, i) => (
                        <div key={i} className="h-32 bg-slate-100 rounded-3xl animate-pulse" />
                    ))}
                </div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                    <StatCard
                        title="Net Cash Flow"
                        value={`${netFlow >= 0 ? '+' : '-'}$${Math.abs(netFlow).toLocaleString(undefined, { maximumFractionDigits: 0 })}`}
                        icon={<Wallet className="text-secondary" />}
                        badge={PERIOD_LABELS[period]}
                        isPositive={netFlow >= 0}
                    />
                    <StatCard
                        title="Total Spending"
                        value={`$${totalSpent.toLocaleString(undefined, { maximumFractionDigits: 0 })}`}
                        icon={<TrendingDown className="text-warning" />}
                        badge={PERIOD_LABELS[period]}
                        isPositive={false}
                    />
                    <StatCard
                        title="AI Risk Alerts"
                        value={highRiskCount.toString()}
                        icon={<ShieldAlert className="text-danger" />}
                        badge={highRiskCount > 0 ? "Critical" : "All Clear"}
                        isPositive={highRiskCount === 0}
                    />
                    <StatCard
                        title="Transactions"
                        value={transactions.length.toString()}
                        icon={<Brain className="text-secondary" />}
                        badge={PERIOD_LABELS[period]}
                        isPositive
                    />
                </div>
            )}

            {/* ── Accounts Section ──────────────────────────────────────── */}
            {financialSummary && (
                <AccountsSection summary={financialSummary} />
            )}

            {/* ── Charts ──────────────────────────────────────────────────── */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Pie chart */}
                <Card title="Spending by Category" subtitle={PERIOD_LABELS[period]} className="lg:col-span-1">
                    {loading ? (
                        <div className="h-64 flex items-center justify-center">
                            <Loader2 className="w-8 h-8 text-slate-200 animate-spin" />
                        </div>
                    ) : pieData.length > 0 ? (
                        <>
                            <div className="h-[200px] w-full mt-4">
                                <ResponsiveContainer width="100%" height="100%">
                                    <PieChart>
                                        <Pie data={pieData} innerRadius={55} outerRadius={75} paddingAngle={4} dataKey="value">
                                            {pieData.map((entry, index) => (
                                                <Cell key={`cell-${index}`} fill={entry.color} />
                                            ))}
                                        </Pie>
                                        <Tooltip formatter={(val: any) => `$${Number(val).toLocaleString()}`} />
                                    </PieChart>
                                </ResponsiveContainer>
                            </div>
                            <div className="mt-3 space-y-2 max-h-[140px] overflow-y-auto pr-1">
                                {pieData.map((item) => (
                                    <div key={item.name} className="flex justify-between items-center text-xs">
                                        <span className="flex items-center gap-2 font-medium text-slate-600">
                                            <div className="w-2.5 h-2.5 rounded-full shrink-0" style={{ backgroundColor: item.color }} />
                                            {item.name}
                                        </span>
                                        <span className="font-black text-primary">${item.value.toLocaleString(undefined, { maximumFractionDigits: 0 })}</span>
                                    </div>
                                ))}
                            </div>
                        </>
                    ) : (
                        <div className="h-64 flex items-center justify-center text-slate-400 text-sm italic">
                            No spending data for this period.
                        </div>
                    )}
                </Card>

                {/* Bar chart */}
                <Card title="Spending Trend" subtitle={PERIOD_LABELS[period]} className="lg:col-span-2">
                    {loading ? (
                        <div className="h-64 flex items-center justify-center">
                            <Loader2 className="w-8 h-8 text-slate-200 animate-spin" />
                        </div>
                    ) : barData.length > 0 ? (
                        <div className="h-[300px] w-full mt-4">
                            <ResponsiveContainer width="100%" height="100%">
                                <BarChart data={barData} barSize={period === 'yearly' ? 18 : 24}>
                                    <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" />
                                    <XAxis dataKey="label" tick={{ fontSize: 10, fill: '#94A3B8', fontWeight: 700 }} axisLine={false} tickLine={false} />
                                    <YAxis tick={{ fontSize: 10, fill: '#94A3B8' }} axisLine={false} tickLine={false} tickFormatter={(v) => `$${v}`} />
                                    <Tooltip
                                        formatter={(val: any) => [`$${Number(val).toLocaleString()}`, 'Spending']}
                                        contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 20px rgba(0,0,0,0.1)' }}
                                    />
                                    <Bar dataKey="spending" fill="#14B8A6" radius={[6, 6, 0, 0]} />
                                </BarChart>
                            </ResponsiveContainer>
                        </div>
                    ) : (
                        <div className="h-64 flex items-center justify-center text-slate-400 text-sm italic">
                            No trend data for this period.
                        </div>
                    )}
                </Card>
            </div>

            {/* ── Transactions Table ──────────────────────────────────────── */}
            <Card
                title="Transactions"
                subtitle={`${transactions.length} records — ${PERIOD_LABELS[period]}`}
            >
                <div className="overflow-x-auto mt-4 max-h-[520px] overflow-y-auto">
                    <table className="w-full text-left">
                        <thead className="sticky top-0 bg-white z-10 border-b border-slate-100 uppercase text-[10px] tracking-widest text-slate-400 font-bold">
                            <tr>
                                <th className="pb-3 px-4">Description</th>
                                <th className="pb-3 px-4">Category</th>
                                <th className="pb-3 px-4">Amount</th>
                                <th className="pb-3 px-4">Risk Score</th>
                                <th className="pb-3 px-4 text-right">Self-Audit</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-50">
                            {loading ? (
                                [...Array(5)].map((_, i) => (
                                    <tr key={i}>
                                        <td colSpan={5} className="py-4 px-4">
                                            <div className="h-4 bg-slate-100 rounded animate-pulse" />
                                        </td>
                                    </tr>
                                ))
                            ) : transactions.length === 0 ? (
                                <tr>
                                    <td colSpan={5} className="py-20 text-center text-slate-400 font-medium">
                                        No transactions found for {PERIOD_LABELS[period].toLowerCase()}.
                                    </td>
                                </tr>
                            ) : (
                                transactions.map((tx) => (
                                    <TransactionRow
                                        key={tx.id}
                                        description={tx.description}
                                        category={tx.category}
                                        amount={tx.amount}
                                        risk={tx.fraudRiskScore}
                                        date={new Date(tx.transactionDate).toLocaleDateString()}
                                        onClick={() => handleRowClick(tx)}
                                        onAnalyze={(e: React.MouseEvent) => handleTriggerAnalysis(e, tx.id)}
                                    />
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </Card>
        </div>
    );
}

// ── Helpers ─────────────────────────────────────────────────────────────────

function buildBarChartData(transactions: any[], period: Period) {
    const spending = transactions.filter(tx => tx.amount < 0);
    if (spending.length === 0) return [];

    const buckets: Record<string, number> = {};

    spending.forEach(tx => {
        const date = new Date(tx.transactionDate);
        let label: string;
        if (period === 'yearly') {
            label = date.toLocaleString('default', { month: 'short' });
        } else if (period === 'monthly') {
            const day = date.getDate();
            const week = Math.ceil(day / 7);
            label = `Wk ${week}`;
        } else {
            label = date.toLocaleString('default', { weekday: 'short' });
        }
        buckets[label] = (buckets[label] || 0) + Math.abs(tx.amount);
    });

    return Object.entries(buckets).map(([label, spending]) => ({ label, spending: Math.round(spending) }));
}

// ── Sub-components ───────────────────────────────────────────────────────────

function StatCard({ title, value, icon, badge, isPositive }: any) {
    return (
        <Card className="hover:border-secondary transition-all duration-300 hover:shadow-md">
            <div className="flex justify-between items-start">
                <div className="p-2.5 bg-slate-50 rounded-xl border border-slate-100">{icon}</div>
                <div className={cn(
                    "text-[9px] font-black uppercase px-2 py-1 rounded-full tracking-wider",
                    isPositive ? "text-success bg-emerald-50 border border-emerald-100" : "text-warning bg-amber-50 border border-amber-100"
                )}>
                    {badge}
                </div>
            </div>
            <div className="mt-4">
                <p className="text-xs font-bold text-slate-400 uppercase tracking-wider">{title}</p>
                <h4 className="text-2xl font-black text-primary mt-1">{value}</h4>
            </div>
        </Card>
    );
}

function TransactionRow({ description, category, amount, risk, date, onClick, onAnalyze }: any) {
    const getRiskStyle = (r: number) => {
        if (r == null) return 'bg-slate-50 text-slate-400 border-slate-100';
        if (r < 0.2) return 'bg-emerald-50 text-emerald-700 border-emerald-100';
        if (r < 0.5) return 'bg-amber-50 text-amber-700 border-amber-100';
        return 'bg-rose-50 text-rose-700 border-rose-100';
    };
    const getRiskLabel = (r: number) => {
        if (r == null) return 'Unanalyzed';
        if (r < 0.2) return 'Safe';
        if (r < 0.5) return 'Medium';
        return 'High Risk';
    };
    return (
        <tr className="hover:bg-slate-50/50 transition-colors group cursor-pointer" onClick={onClick}>
            <td className="py-4 px-4">
                <p className="font-bold text-sm text-primary">{description}</p>
                <p className="text-[10px] text-slate-400 uppercase tracking-tighter font-medium">{date}</p>
            </td>
            <td className="py-4 px-4">
                <span className="px-2 py-1 rounded-md bg-slate-100 text-slate-600 text-[9px] font-black uppercase tracking-wider">{category}</span>
            </td>
            <td className={cn("py-4 px-4 font-black text-sm flex items-center gap-1", amount < 0 ? "text-primary" : "text-success")}>
                {amount < 0 ? <ArrowDownRight className="w-3 h-3" /> : <ArrowUpRight className="w-3 h-3" />}
                ${Math.abs(amount).toLocaleString()}
            </td>
            <td className="py-4 px-4">
                <div className={cn("inline-flex items-center gap-1.5 px-2 py-1 rounded-full border text-[9px] font-black uppercase", getRiskStyle(risk))}>
                    <div className={cn("w-1.5 h-1.5 rounded-full", risk > 0.5 ? "bg-rose-500" : risk > 0.2 ? "bg-amber-500" : risk == null ? "bg-slate-300" : "bg-emerald-500")} />
                    {getRiskLabel(risk)} {risk != null && `(${(risk * 100).toFixed(0)}%)`}
                </div>
            </td>
            <td className="py-4 px-4 text-right">
                {risk == null ? (
                    <button onClick={onAnalyze} className="text-[10px] font-extrabold text-secondary flex items-center gap-1 ml-auto hover:underline">
                        <Brain className="w-3 h-3" /> Analyze
                    </button>
                ) : (
                    <button className="text-slate-300 group-hover:text-primary transition-colors ml-auto">
                        <ArrowUpRight className="w-4 h-4" />
                    </button>
                )}
            </td>
        </tr>
    );
}

// ── AccountsSection ──────────────────────────────────────────────────────────

function AccountsSection({ summary }: { summary: any }) {
    const bankAccounts = (summary.accounts || []).filter((a: any) => a.accountType !== 'CREDIT');
    const creditAccounts = (summary.accounts || []).filter((a: any) => a.accountType === 'CREDIT');
    const netWorthPositive = summary.netWorth >= 0;

    return (
        <div className="space-y-4">
            {/* Net Worth Banner */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <NetWorthPill
                    label="Total Assets"
                    value={`$${Number(summary.totalAssets).toLocaleString(undefined, { maximumFractionDigits: 0 })}`}
                    color="emerald"
                />
                <NetWorthPill
                    label="Total Liabilities"
                    value={`$${Number(summary.totalLiabilities).toLocaleString(undefined, { maximumFractionDigits: 0 })}`}
                    color="rose"
                />
                <NetWorthPill
                    label="Net Worth"
                    value={`${netWorthPositive ? '+' : ''}$${Math.abs(summary.netWorth).toLocaleString(undefined, { maximumFractionDigits: 0 })}`}
                    color={netWorthPositive ? 'teal' : 'rose'}
                    highlight
                />
                <NetWorthPill
                    label="Credit Utilization"
                    value={`${summary.creditUtilization ?? 0}%`}
                    color={summary.creditUtilization > 70 ? 'rose' : summary.creditUtilization > 30 ? 'amber' : 'emerald'}
                />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Bank Accounts */}
                {bankAccounts.length > 0 && (
                    <Card title="Bank Accounts" subtitle={`${bankAccounts.length} linked`}>
                        <div className="space-y-3 mt-4">
                            {bankAccounts.map((acc: any, i: number) => (
                                <div key={i} className="flex items-center justify-between p-3 bg-slate-50 rounded-xl">
                                    <div className="flex items-center gap-3">
                                        <div className={cn(
                                            "w-9 h-9 rounded-xl flex items-center justify-center text-xs font-black",
                                            acc.accountType === 'SAVINGS'
                                                ? "bg-emerald-100 text-emerald-700"
                                                : "bg-secondary/10 text-secondary"
                                        )}>
                                            {acc.accountType === 'SAVINGS' ? 'SAV' : 'CHK'}
                                        </div>
                                        <div>
                                            <p className="font-bold text-sm text-primary">{acc.accountName || acc.institutionName}</p>
                                            <p className="text-[10px] text-slate-400 uppercase font-bold">{acc.accountType}</p>
                                        </div>
                                    </div>
                                    <div className="text-right">
                                        <p className="font-black text-primary text-sm">
                                            ${Number(acc.currentBalance || 0).toLocaleString(undefined, { maximumFractionDigits: 0 })}
                                        </p>
                                        <p className="text-[9px] text-slate-400 uppercase font-bold">Available</p>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </Card>
                )}

                {/* Credit Accounts */}
                {creditAccounts.length > 0 && (
                    <Card title="Credit Cards" subtitle={`${creditAccounts.length} linked`}>
                        <div className="space-y-4 mt-4">
                            {creditAccounts.map((acc: any, i: number) => {
                                const used = acc.currentBalance || 0;
                                const limit = acc.creditLimit || 0;
                                const pct = limit > 0 ? Math.min((used / limit) * 100, 100) : 0;
                                const barColor = pct > 70 ? 'bg-rose-500' : pct > 30 ? 'bg-amber-500' : 'bg-emerald-500';
                                return (
                                    <div key={i} className="p-3 bg-slate-50 rounded-xl space-y-2">
                                        <div className="flex items-center justify-between">
                                            <div className="flex items-center gap-3">
                                                <div className="w-9 h-9 rounded-xl bg-rose-100 text-rose-700 flex items-center justify-center text-[9px] font-black">CC</div>
                                                <div>
                                                    <p className="font-bold text-sm text-primary">{acc.accountName || acc.institutionName}</p>
                                                    <p className="text-[10px] text-slate-400 uppercase font-bold">Credit Card</p>
                                                </div>
                                            </div>
                                            <div className="text-right">
                                                <p className="font-black text-rose-600 text-sm">
                                                    ${Number(used).toLocaleString(undefined, { maximumFractionDigits: 0 })}
                                                </p>
                                                <p className="text-[9px] text-slate-400 font-bold">
                                                    of ${Number(limit).toLocaleString(undefined, { maximumFractionDigits: 0 })} limit
                                                </p>
                                            </div>
                                        </div>
                                        {/* Utilization bar */}
                                        <div>
                                            <div className="flex justify-between text-[9px] font-bold text-slate-400 mb-1">
                                                <span>Utilization</span>
                                                <span className={cn(pct > 70 ? 'text-rose-500' : pct > 30 ? 'text-amber-500' : 'text-emerald-500')}>
                                                    {pct.toFixed(1)}%
                                                </span>
                                            </div>
                                            <div className="w-full h-2 bg-slate-200 rounded-full overflow-hidden">
                                                <div
                                                    className={cn("h-full rounded-full transition-all duration-700", barColor)}
                                                    style={{ width: `${pct}%` }}
                                                />
                                            </div>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    </Card>
                )}
            </div>
        </div>
    );
}

function NetWorthPill({ label, value, color, highlight }: { label: string; value: string; color: string; highlight?: boolean }) {
    const styles: Record<string, string> = {
        emerald: 'border-emerald-100 bg-emerald-50 text-emerald-700',
        rose: 'border-rose-100 bg-rose-50 text-rose-700',
        amber: 'border-amber-100 bg-amber-50 text-amber-700',
        teal: 'border-teal-100 bg-teal-50 text-teal-700',
    };
    return (
        <div className={cn("p-4 rounded-2xl border", styles[color] || styles.emerald, highlight && "ring-2 ring-offset-1 ring-current/20")}>
            <p className="text-[10px] font-black uppercase tracking-wider opacity-70">{label}</p>
            <p className="text-xl font-black mt-1">{value}</p>
        </div>
    );
}
