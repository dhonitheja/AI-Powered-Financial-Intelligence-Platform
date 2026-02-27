"use client";

import React from 'react';
import { Card } from '@/components/ui/Card';
import {
    BrainCircuit,
    TrendingUp,
    ShieldCheck,
    AlertTriangle,
    Lightbulb,
    Zap,
    ArrowRight,
    Sparkles
} from 'lucide-react';
import {
    AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer
} from 'recharts';
import { cn } from '@/components/ui/Card';

const anomalyData = [
    { day: 'Mon', score: 20 },
    { day: 'Tue', score: 25 },
    { day: 'Wed', score: 85 }, // Anomaly
    { day: 'Thu', score: 30 },
    { day: 'Fri', score: 35 },
    { day: 'Sat', score: 40 },
    { day: 'Sun', score: 30 },
];

export default function InsightsPage() {
    return (
        <div className="p-8 max-w-7xl mx-auto space-y-8">
            {/* Header with Sparkle effect */}
            <div className="relative overflow-hidden bg-primary rounded-3xl p-8 text-white shadow-2xl shadow-slate-900/30">
                <div className="relative z-10">
                    <div className="flex items-center gap-2 text-secondary mb-2">
                        <Sparkles className="w-5 h-5" />
                        <span className="text-xs font-bold uppercase tracking-widest">Powered by Gemini 1.5 Flash</span>
                    </div>
                    <h1 className="text-4xl font-bold tracking-tight">AI Financial Insights</h1>
                    <p className="text-slate-400 mt-2 max-w-xl">
                        We've analyzed your spending patterns over the last 30 days. Here is what you need to know about your financial health and security.
                    </p>
                </div>
                {/* Background decorative brain */}
                <BrainCircuit className="absolute -right-10 -bottom-10 w-64 h-64 text-white/5 rotate-12" />
            </div>

            {/* Insight Grid */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

                {/* Anomaly Detection */}
                <Card title="Risk Anomaly Detection" className="lg:col-span-2">
                    <div className="h-[300px] w-full mt-6">
                        <ResponsiveContainer width="100%" height="100%">
                            <AreaChart data={anomalyData}>
                                <defs>
                                    <linearGradient id="colorScore" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="5%" stopColor="#14B8A6" stopOpacity={0.3} />
                                        <stop offset="95%" stopColor="#14B8A6" stopOpacity={0} />
                                    </linearGradient>
                                </defs>
                                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" />
                                <XAxis dataKey="day" axisLine={false} tickLine={false} tick={{ fill: '#64748B', fontSize: 12 }} />
                                <YAxis axisLine={false} tickLine={false} tick={{ fill: '#64748B', fontSize: 12 }} />
                                <Tooltip />
                                <Area type="monotone" dataKey="score" stroke="#14B8A6" strokeWidth={3} fillOpacity={1} fill="url(#colorScore)" />
                            </AreaChart>
                        </ResponsiveContainer>
                    </div>
                    <div className="mt-6 p-4 bg-rose-50 rounded-2xl border border-rose-100 flex gap-4">
                        <AlertTriangle className="w-6 h-6 text-danger shrink-0 mt-1" />
                        <div>
                            <p className="text-sm font-bold text-danger">Suspicious Activity Detected</p>
                            <p className="text-xs text-rose-700 mt-1">
                                On Wednesday, we detected a transaction that deviates significantly from your typical spending in the "Technology" category. We recommend reviewing your AWS statement.
                            </p>
                        </div>
                    </div>
                </Card>

                {/* AI Summary Side Panel */}
                <div className="space-y-6">
                    <Card className="bg-gradient-to-br from-slate-50 to-white">
                        <div className="flex items-center gap-3 mb-4">
                            <div className="p-2 bg-secondary/10 rounded-lg text-secondary">
                                <Zap className="w-5 h-5" />
                            </div>
                            <h3 className="font-bold text-primary">Quick Summary</h3>
                        </div>
                        <ul className="space-y-4">
                            <SummaryItem icon={<TrendingUp className="text-success" />} text="Spending is down 14% vs last month." />
                            <SummaryItem icon={<ShieldCheck className="text-secondary" />} text="98% of Transactions are Verified Safe." />
                            <SummaryItem icon={<Lightbulb className="text-warning" />} text="You could save $45/mo by canceling unused subscriptions." />
                        </ul>
                    </Card>

                    <Card className="border-secondary/20 bg-teal-50/30">
                        <h4 className="text-sm font-bold text-teal-900 mb-2">Next Step Recommendation</h4>
                        <p className="text-xs text-teal-700 leading-relaxed">
                            Based on your current trajectory, you will hit your savings goal 12 days earlier than projected.
                        </p>
                        <button className="mt-4 flex items-center gap-2 text-xs font-bold text-secondary group">
                            View Savings Plan
                            <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                        </button>
                    </Card>
                </div>
            </div>

            {/* High Risk Transactions Section */}
            <h2 className="text-xl font-bold text-primary mt-10 mb-4 flex items-center gap-2">
                <AlertTriangle className="w-5 h-5 text-danger" />
                High Risk Transactions (Needs Attention)
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <InsightTransactionCard
                    description="Foreign ATM Withdrawal"
                    location="Bucharest, RO"
                    amount="$400.00"
                    riskScore={0.92}
                    reason="Transaction location is 8,000 miles from your primary spending hub."
                />
                <InsightTransactionCard
                    description="Recurring Cloud Service"
                    location="Online"
                    amount="$1,240.50"
                    riskScore={0.75}
                    reason="Suddenly spiked from average $120. Potential account compromise or misconfiguration."
                />
            </div>
        </div>
    );
}

function SummaryItem({ icon, text }: { icon: React.ReactNode, text: string }) {
    return (
        <li className="flex gap-3">
            <div className="mt-0.5">{icon}</div>
            <p className="text-sm font-medium text-slate-600 leading-tight">{text}</p>
        </li>
    );
}

function InsightTransactionCard({ description, location, amount, riskScore, reason }: any) {
    return (
        <Card className="p-0 overflow-hidden border-rose-100 shadow-rose-900/5">
            <div className="p-6">
                <div className="flex justify-between items-start mb-4">
                    <div>
                        <h4 className="font-bold text-primary">{description}</h4>
                        <p className="text-xs text-slate-400">{location}</p>
                    </div>
                    <div className="text-right">
                        <p className="font-bold text-primary tracking-tight">{amount}</p>
                        <div className="flex items-center gap-1.5 text-danger text-[10px] font-black uppercase mt-1">
                            Risk: {(riskScore * 100).toFixed(0)}%
                        </div>
                    </div>
                </div>
                <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 flex gap-3">
                    <BrainCircuit className="w-5 h-5 text-secondary shrink-0" />
                    <p className="text-xs text-slate-600 italic">"{reason}"</p>
                </div>
            </div>
            <div className="px-6 py-3 bg-rose-50/50 border-t border-rose-100 flex justify-between items-center">
                <span className="text-[10px] font-bold text-rose-500 uppercase">Action Required</span>
                <button className="text-xs font-bold text-primary hover:text-secondary transition-colors">Flag as Fraud</button>
            </div>
        </Card>
    );
}
