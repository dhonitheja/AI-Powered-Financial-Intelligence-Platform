"use client";

import React from 'react';
import { Card } from '@/components/ui/Card';
import {
    LineChart, Line, AreaChart, Area, BarChart, Bar,
    XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
    PieChart, Pie, Cell, Legend
} from 'recharts';
import {
    TrendingUp,
    ArrowUpRight,
    ArrowDownRight,
    Calendar,
    Filter,
    Download
} from 'lucide-react';

const COLORS = ['#14B8A6', '#F59E0B', '#6366F1', '#E11D48', '#8B5CF6'];

const netWorthData = [
    { month: 'Sep', amount: 45000 },
    { month: 'Oct', amount: 48500 },
    { month: 'Nov', amount: 47000 },
    { month: 'Dec', amount: 52000 },
    { month: 'Jan', amount: 55000 },
    { month: 'Feb', amount: 58000 },
];

const incomeExpenseData = [
    { month: 'Sep', income: 5000, expense: 3200 },
    { month: 'Oct', income: 5000, expense: 3800 },
    { month: 'Nov', income: 5200, expense: 4100 },
    { month: 'Dec', income: 6500, expense: 4800 },
    { month: 'Jan', income: 5500, expense: 3500 },
    { month: 'Feb', income: 5800, expense: 3700 },
];

const assetAllocation = [
    { name: 'Stocks', value: 35000 },
    { name: 'Cash', value: 12000 },
    { name: 'Crypto', value: 8000 },
    { name: 'Real Estate', value: 3000 },
];

export default function AnalyticsPage() {
    return (
        <div className="p-8 max-w-7xl mx-auto space-y-8">
            {/* Header */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h1 className="text-3xl font-bold text-primary tracking-tight">Financial Analytics</h1>
                    <p className="text-slate-500 mt-1">Deep dive into your financial performance and trends.</p>
                </div>
                <div className="flex items-center gap-3">
                    <button className="flex items-center gap-2 px-4 py-2 bg-white border border-slate-200 rounded-lg text-sm font-medium text-slate-600 hover:bg-slate-50 transition-all">
                        <Calendar className="w-4 h-4" />
                        Last 6 Months
                    </button>
                    <button className="flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium shadow-sm hover:opacity-90 transition-all">
                        <Download className="w-4 h-4" />
                        Export Report
                    </button>
                </div>
            </div>

            {/* Quick Stats */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <Card className="flex flex-col justify-between">
                    <div className="flex justify-between items-start">
                        <p className="text-sm font-medium text-slate-500">Savings Rate</p>
                        <span className="text-xs font-bold text-success bg-emerald-50 px-2 py-1 rounded-full">+4.2%</span>
                    </div>
                    <div className="mt-4">
                        <h3 className="text-2xl font-bold text-primary">32.5%</h3>
                        <p className="text-xs text-slate-400 mt-1">Compared to 28.3% last month</p>
                    </div>
                </Card>
                <Card className="flex flex-col justify-between">
                    <div className="flex justify-between items-start">
                        <p className="text-sm font-medium text-slate-500">Avg. Monthly Cash Flow</p>
                        <span className="text-xs font-bold text-success bg-emerald-50 px-2 py-1 rounded-full">+12%</span>
                    </div>
                    <div className="mt-4">
                        <h3 className="text-2xl font-bold text-primary">+$1,840</h3>
                        <p className="text-xs text-slate-400 mt-1">Based on last 6 months</p>
                    </div>
                </Card>
                <Card className="flex flex-col justify-between">
                    <div className="flex justify-between items-start">
                        <p className="text-sm font-medium text-slate-500">Investment Growth</p>
                        <span className="text-xs font-bold text-secondary bg-teal-50 px-2 py-1 rounded-full">Steady</span>
                    </div>
                    <div className="mt-4">
                        <h3 className="text-2xl font-bold text-primary">8.4% APY</h3>
                        <p className="text-xs text-slate-400 mt-1">Current portfolio performance</p>
                    </div>
                </Card>
            </div>

            {/* Main Charts */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <Card title="Net Worth Growth" subtitle="Tracking your total equity over time">
                    <div className="h-[300px] w-full mt-6">
                        <ResponsiveContainer width="100%" height="100%">
                            <AreaChart data={netWorthData}>
                                <defs>
                                    <linearGradient id="colorAmount" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="5%" stopColor="#14B8A6" stopOpacity={0.1} />
                                        <stop offset="95%" stopColor="#14B8A6" stopOpacity={0} />
                                    </linearGradient>
                                </defs>
                                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" />
                                <XAxis
                                    dataKey="month"
                                    axisLine={false}
                                    tickLine={false}
                                    tick={{ fill: '#94A3B8', fontSize: 12 }}
                                    dy={10}
                                />
                                <YAxis
                                    axisLine={false}
                                    tickLine={false}
                                    tick={{ fill: '#94A3B8', fontSize: 12 }}
                                    tickFormatter={(value) => `$${value / 1000}k`}
                                />
                                <Tooltip
                                    contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)' }}
                                    formatter={(value: any) => [`$${value.toLocaleString()}`, 'Net Worth']}
                                />
                                <Area type="monotone" dataKey="amount" stroke="#14B8A6" strokeWidth={3} fillOpacity={1} fill="url(#colorAmount)" />
                            </AreaChart>
                        </ResponsiveContainer>
                    </div>
                </Card>

                <Card title="Income vs Expenses" subtitle="Monthly cash flow comparison">
                    <div className="h-[300px] w-full mt-6">
                        <ResponsiveContainer width="100%" height="100%">
                            <BarChart data={incomeExpenseData}>
                                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" />
                                <XAxis
                                    dataKey="month"
                                    axisLine={false}
                                    tickLine={false}
                                    tick={{ fill: '#94A3B8', fontSize: 12 }}
                                    dy={10}
                                />
                                <YAxis
                                    axisLine={false}
                                    tickLine={false}
                                    tick={{ fill: '#94A3B8', fontSize: 12 }}
                                />
                                <Tooltip
                                    contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)' }}
                                />
                                <Legend verticalAlign="top" align="right" height={36} iconType="circle" />
                                <Bar dataKey="income" name="Income" fill="#14B8A6" radius={[4, 4, 0, 0]} barSize={20} />
                                <Bar dataKey="expense" name="Expense" fill="#E11D48" radius={[4, 4, 0, 0]} barSize={20} />
                            </BarChart>
                        </ResponsiveContainer>
                    </div>
                </Card>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <Card title="Asset Allocation" subtitle="Current portfolio distribution" className="lg:col-span-1">
                    <div className="h-[250px] w-full mt-4">
                        <ResponsiveContainer width="100%" height="100%">
                            <PieChart>
                                <Pie
                                    data={assetAllocation}
                                    innerRadius={60}
                                    outerRadius={80}
                                    paddingAngle={5}
                                    dataKey="value"
                                >
                                    {assetAllocation.map((entry, index) => (
                                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                    ))}
                                </Pie>
                                <Tooltip />
                            </PieChart>
                        </ResponsiveContainer>
                    </div>
                    <div className="mt-4 space-y-3">
                        {assetAllocation.map((item, index) => (
                            <div key={item.name} className="flex justify-between items-center">
                                <div className="flex items-center gap-2">
                                    <div className="w-3 h-3 rounded-full" style={{ backgroundColor: COLORS[index % COLORS.length] }} />
                                    <span className="text-sm font-medium text-slate-600">{item.name}</span>
                                </div>
                                <span className="text-sm font-bold text-primary">${item.value.toLocaleString()}</span>
                            </div>
                        ))}
                    </div>
                </Card>

                <Card title="AI Predictive Analysis" subtitle="Projected growth based on spending patterns" className="lg:col-span-2">
                    <div className="flex flex-col h-full">
                        <div className="flex-1 flex items-center justify-center py-10">
                            <div className="text-center space-y-4">
                                <TrendingUp className="w-12 h-12 text-secondary mx-auto opacity-50" />
                                <div className="space-y-2">
                                    <h4 className="text-lg font-bold text-primary">On track for $100k net worth</h4>
                                    <p className="text-sm text-slate-500 max-w-sm mx-auto">
                                        Based on your current savings rate of 32.5%, you are projected to reach your goal in approximately 14 months.
                                    </p>
                                </div>
                                <div className="pt-4">
                                    <button className="text-secondary font-bold text-sm hover:underline">View Improvement Suggestions</button>
                                </div>
                            </div>
                        </div>
                        <div className="border-t border-slate-100 pt-6 mt-auto">
                            <div className="grid grid-cols-2 gap-4">
                                <div className="p-4 bg-slate-50 rounded-xl">
                                    <p className="text-xs text-slate-500">Projected (1yr)</p>
                                    <p className="text-lg font-bold text-primary">$72,400</p>
                                </div>
                                <div className="p-4 bg-slate-50 rounded-xl">
                                    <p className="text-xs text-slate-500">Confidence Score</p>
                                    <p className="text-lg font-bold text-secondary">94%</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </Card>
            </div>
        </div>
    );
}
