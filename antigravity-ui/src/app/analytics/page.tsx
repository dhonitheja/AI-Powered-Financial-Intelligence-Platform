"use client";

import React, { useState, useEffect } from 'react';
import { Card } from '@/components/ui/Card';
import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
    ResponsiveContainer, Cell
} from 'recharts';
import {
    TrendingUp, TrendingDown, Minus, Download, Loader2
} from 'lucide-react';
import { analyticsService, ComparisonData } from '@/services/api';

const CHART_COLORS = ['#D4AF37', '#B8962E', '#F5D67B', '#8B7355', '#6B5A3E'];

export default function AnalyticsPage() {
    const [period, setPeriod] = useState<'weekly' | 'monthly'>('weekly');
    const [data, setData] = useState<ComparisonData | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(false);

    useEffect(() => {
        const fetch = async () => {
            setLoading(true);
            setError(false);
            try {
                const res = await analyticsService.getComparison(period);
                setData(res.data);
            } catch {
                setError(true);
            } finally {
                setLoading(false);
            }
        };
        fetch();
    }, [period]);

    const overallChangePct = data && data.previousTotal > 0
        ? ((data.currentTotal - data.previousTotal) / data.previousTotal) * 100
        : null;

    // Top 6 categories for bar chart
    const chartData = data?.categories.slice(0, 6).map(c => ({
        name: c.category.replace(/_/g, ' ').split(' ').map(w => w[0] + w.slice(1).toLowerCase()).join(' '),
        current: Math.round(c.currentPeriodSpend),
        previous: Math.round(c.previousPeriodSpend),
    })) ?? [];

    return (
        <div className="p-8 max-w-7xl mx-auto space-y-8">
            {/* Header */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h1 className="text-3xl font-black text-slate-100 tracking-tight">Financial Analytics</h1>
                    <p className="text-slate-500 mt-1">Spending breakdown and period-over-period comparison.</p>
                </div>
                <div className="flex items-center gap-3">
                    {(['weekly', 'monthly'] as const).map(p => (
                        <button key={p} onClick={() => setPeriod(p)}
                            className={`px-5 py-2 rounded-xl text-sm font-bold transition-all ${
                                period === p
                                    ? 'bg-gradient-to-r from-[#D4AF37] to-[#B8962E] text-[#0D0B1E] shadow-gold'
                                    : 'bg-white/5 border border-white/10 text-slate-400 hover:text-slate-200'
                            }`}>
                            {p.charAt(0).toUpperCase() + p.slice(1)}
                        </button>
                    ))}
                    <button className="flex items-center gap-2 px-4 py-2 bg-white/5 border border-white/10 rounded-xl text-sm font-medium text-slate-400 hover:text-slate-200 transition-all">
                        <Download className="w-4 h-4" /> Export
                    </button>
                </div>
            </div>

            {loading ? (
                <div className="flex items-center justify-center py-32">
                    <div className="text-center">
                        <Loader2 className="w-10 h-10 text-[#D4AF37] animate-spin mx-auto mb-3" />
                        <p className="text-slate-500 text-sm">Loading analytics...</p>
                    </div>
                </div>
            ) : error ? (
                <div className="flex items-center justify-center py-32">
                    <p className="text-slate-500 text-sm">Failed to load analytics. Make sure the backend is running.</p>
                </div>
            ) : data ? (
                <>
                    {/* Summary stats */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                        <Card>
                            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">{data.currentPeriodLabel} Spending</p>
                            <h3 className="text-2xl font-black text-slate-100 mt-2">
                                ${data.currentTotal.toLocaleString(undefined, { maximumFractionDigits: 0 })}
                            </h3>
                            <p className="text-xs text-slate-500 mt-1">Across {data.categories.length} categories</p>
                        </Card>
                        <Card>
                            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">{data.previousPeriodLabel} Spending</p>
                            <h3 className="text-2xl font-black text-slate-100 mt-2">
                                ${data.previousTotal.toLocaleString(undefined, { maximumFractionDigits: 0 })}
                            </h3>
                            <p className="text-xs text-slate-500 mt-1">Prior period baseline</p>
                        </Card>
                        <Card>
                            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Period Change</p>
                            {overallChangePct !== null ? (
                                <div className="flex items-center gap-2 mt-2">
                                    {overallChangePct > 0
                                        ? <TrendingUp className="w-6 h-6 text-rose-400" />
                                        : overallChangePct < 0
                                            ? <TrendingDown className="w-6 h-6 text-emerald-400" />
                                            : <Minus className="w-6 h-6 text-slate-400" />}
                                    <h3 className={`text-2xl font-black ${overallChangePct > 0 ? 'text-rose-400' : overallChangePct < 0 ? 'text-emerald-400' : 'text-slate-400'}`}>
                                        {overallChangePct > 0 ? '+' : ''}{overallChangePct.toFixed(1)}%
                                    </h3>
                                </div>
                            ) : (
                                <h3 className="text-2xl font-black text-slate-400 mt-2">—</h3>
                            )}
                            <p className="text-xs text-slate-500 mt-1">vs {data.previousPeriodLabel}</p>
                        </Card>
                    </div>

                    {/* Charts + Comparison Table */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                        {/* Bar chart: current vs previous top categories */}
                        <Card title="Top Categories" subtitle={`${data.currentPeriodLabel} vs ${data.previousPeriodLabel}`}>
                            {chartData.length > 0 ? (
                                <div className="h-[300px] mt-6">
                                    <ResponsiveContainer width="100%" height="100%">
                                        <BarChart data={chartData} layout="vertical">
                                            <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="rgba(255,255,255,0.05)" />
                                            <XAxis type="number" axisLine={false} tickLine={false}
                                                tick={{ fill: '#64748B', fontSize: 11 }}
                                                tickFormatter={v => `$${v}`} />
                                            <YAxis type="category" dataKey="name" axisLine={false} tickLine={false}
                                                tick={{ fill: '#94A3B8', fontSize: 11 }} width={110} />
                                            <Tooltip
                                                contentStyle={{ background: '#1E1B4B', border: '1px solid rgba(212,175,55,0.2)', borderRadius: 12 }}
                                                labelStyle={{ color: '#F5D67B', fontWeight: 700 }}
                                                formatter={(v: number) => [`$${v.toLocaleString()}`, '']} />
                                            <Bar dataKey="current" name={data.currentPeriodLabel} radius={[0, 4, 4, 0]} barSize={10}>
                                                {chartData.map((_, i) => <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />)}
                                            </Bar>
                                            <Bar dataKey="previous" name={data.previousPeriodLabel} fill="rgba(255,255,255,0.1)" radius={[0, 4, 4, 0]} barSize={6} />
                                        </BarChart>
                                    </ResponsiveContainer>
                                </div>
                            ) : (
                                <div className="flex items-center justify-center h-[300px] text-slate-500 text-sm">No transaction data yet</div>
                            )}
                        </Card>

                        {/* Full comparison table */}
                        <Card title="Category Breakdown"
                            subtitle={`${data.currentPeriodLabel} vs ${data.previousPeriodLabel} — all categories`}>
                            <div className="mt-4">
                                <div className="grid grid-cols-4 text-[10px] font-black text-slate-500 uppercase tracking-wider px-2 pb-2 border-b border-white/10">
                                    <span>Category</span>
                                    <span className="text-right">{data.currentPeriodLabel.split(' ')[0]}</span>
                                    <span className="text-right">{data.previousPeriodLabel.split(' ')[0]}</span>
                                    <span className="text-right">Change</span>
                                </div>
                                <div className="overflow-y-auto max-h-[260px] mt-1 space-y-0.5">
                                    {data.categories.length === 0 ? (
                                        <p className="text-slate-500 text-sm text-center py-8">No transactions in this period.</p>
                                    ) : data.categories.map(cat => (
                                        <div key={cat.category}
                                            className="grid grid-cols-4 items-center px-2 py-2 rounded-lg hover:bg-white/5 transition-colors">
                                            <span className="text-xs font-semibold text-slate-300 truncate pr-2">
                                                {cat.category.replace(/_/g, ' ')}
                                            </span>
                                            <span className="text-xs font-black text-slate-100 text-right">
                                                ${cat.currentPeriodSpend.toLocaleString(undefined, { maximumFractionDigits: 0 })}
                                            </span>
                                            <span className="text-xs text-slate-500 text-right">
                                                ${cat.previousPeriodSpend.toLocaleString(undefined, { maximumFractionDigits: 0 })}
                                            </span>
                                            <span className={`text-xs font-black text-right ${
                                                cat.changePercent > 10 ? 'text-rose-400'
                                                    : cat.changePercent < -10 ? 'text-emerald-400'
                                                    : 'text-slate-400'
                                            }`}>
                                                {cat.changePercent > 0 ? '+' : ''}{cat.changePercent.toFixed(1)}%
                                            </span>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </Card>
                    </div>

                    {/* Insights row */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        {data.categories.slice(0, 3).map((cat, i) => {
                            const isUp = cat.changePercent > 0;
                            const isDown = cat.changePercent < 0;
                            return (
                                <div key={cat.category} className="bg-white/5 border border-white/10 rounded-2xl p-5">
                                    <div className="flex items-center justify-between mb-3">
                                        <p className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                                            #{i + 1} Top Spend
                                        </p>
                                        <span className={`text-[11px] font-black px-2 py-0.5 rounded-full ${
                                            isUp ? 'bg-rose-500/15 text-rose-400' : isDown ? 'bg-emerald-500/15 text-emerald-400' : 'bg-slate-500/15 text-slate-400'
                                        }`}>
                                            {isUp ? '↑' : isDown ? '↓' : '–'} {Math.abs(cat.changePercent).toFixed(1)}%
                                        </span>
                                    </div>
                                    <p className="font-black text-slate-100 text-base truncate">{cat.category.replace(/_/g, ' ')}</p>
                                    <p className="text-2xl font-black gold-text mt-1">
                                        ${cat.currentPeriodSpend.toLocaleString(undefined, { maximumFractionDigits: 0 })}
                                    </p>
                                    <p className="text-xs text-slate-500 mt-1">
                                        vs ${cat.previousPeriodSpend.toLocaleString(undefined, { maximumFractionDigits: 0 })} prior
                                    </p>
                                </div>
                            );
                        })}
                    </div>
                </>
            ) : null}
        </div>
    );
}
