"use client";

import React, { useState } from 'react';
import {
    X,
    Brain,
    AlertTriangle,
    ShieldCheck,
    History,
    ExternalLink,
    ChevronRight
} from 'lucide-react';
import { cn } from '@/components/ui/Card';

interface AIDrawerProps {
    isOpen: boolean;
    onClose: () => void;
    transaction: any;
}

export default function AIDrawer({ isOpen, onClose, transaction }: AIDrawerProps) {
    if (!transaction) return null;

    const risk = transaction.risk || 0;

    return (
        <>
            {/* Overlay */}
            <div
                className={cn(
                    "fixed inset-0 bg-primary/20 backdrop-blur-sm z-[100] transition-opacity duration-300",
                    isOpen ? "opacity-100" : "opacity-0 pointer-events-none"
                )}
                onClick={onClose}
            />

            {/* Drawer */}
            <div
                className={cn(
                    "fixed top-0 right-0 h-full w-full max-w-md bg-white shadow-2xl z-[101] transform transition-transform duration-500 ease-out p-0",
                    isOpen ? "translate-x-0" : "translate-x-full"
                )}
            >
                <div className="flex flex-col h-full">
                    {/* Header */}
                    <div className="p-6 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
                        <div className="flex items-center gap-2">
                            <div className="p-2 bg-secondary/10 rounded-lg text-secondary">
                                <Brain className="w-5 h-5" />
                            </div>
                            <h2 className="text-xl font-bold text-primary">AI Intelligence</h2>
                        </div>
                        <button onClick={onClose} className="p-2 hover:bg-slate-100 rounded-full transition-all text-slate-400">
                            <X className="w-6 h-6" />
                        </button>
                    </div>

                    <div className="flex-1 overflow-y-auto p-8 space-y-8">
                        {/* Transaction Brief */}
                        <div className="flex justify-between items-start">
                            <div>
                                <p className="text-xs font-bold text-slate-400 uppercase tracking-widest mb-1">Transaction</p>
                                <h3 className="text-lg font-bold text-primary leading-tight">{transaction.description}</h3>
                                <p className="text-sm text-slate-500 mt-1">{transaction.date}</p>
                            </div>
                            <p className="text-2xl font-black text-primary">${Math.abs(transaction.amount).toLocaleString()}</p>
                        </div>

                        {/* Risk Assessment */}
                        <div className="space-y-4">
                            <div className="flex items-center justify-between">
                                <h4 className="text-sm font-bold text-primary uppercase tracking-wider">Risk Analysis</h4>
                                <div className={cn(
                                    "px-2 py-1 rounded-md text-[10px] font-black uppercase border",
                                    risk > 0.6 ? "bg-rose-50 text-danger border-rose-100" :
                                        risk > 0.2 ? "bg-amber-50 text-warning border-amber-100" :
                                            "bg-emerald-50 text-success border-emerald-100"
                                )}>
                                    {risk > 0.6 ? 'High Risk' : risk > 0.2 ? 'Moderate' : 'Safe'}
                                </div>
                            </div>

                            <div className="relative h-4 bg-slate-100 rounded-full overflow-hidden">
                                <div
                                    className={cn("absolute h-full transition-all duration-1000",
                                        risk > 0.6 ? "bg-danger" : risk > 0.2 ? "bg-warning" : "bg-success"
                                    )}
                                    style={{ width: `${risk * 100}%` }}
                                />
                            </div>
                            <p className="text-xs text-slate-500 text-center font-medium italic">
                                AI confidence score: {((1 - Math.abs(0.5 - risk) * 2) * 100).toFixed(0)}%
                            </p>
                        </div>

                        {/* Gemini Explanation */}
                        <div className="bg-primary rounded-2xl p-6 text-white relative overflow-hidden">
                            <div className="relative z-10">
                                <div className="flex items-center gap-2 mb-4">
                                    <ShieldCheck className="w-5 h-5 text-secondary" />
                                    <h5 className="font-bold">Gemini Explanation</h5>
                                </div>
                                <p className="text-sm text-slate-300 leading-relaxed italic">
                                    "{transaction.aiExplanation || "This purchase matches your established behavioral profile. However, the transaction occurred at an unusual hour (3:45 AM). We've categorized this as 'Low Risk' but suggest verifying your automated subscription settings."}"
                                </p>
                            </div>
                            <Brain className="absolute -right-4 -bottom-4 w-24 h-24 text-white/5 -rotate-12" />
                        </div>

                        {/* Behavioral Data */}
                        <div className="space-y-4">
                            <h4 className="text-sm font-bold text-primary uppercase tracking-wider flex items-center gap-2">
                                <History className="w-4 h-4 text-slate-400" />
                                Contextual Data
                            </h4>
                            <div className="grid grid-cols-2 gap-3">
                                <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
                                    <p className="text-[10px] font-bold text-slate-400 uppercase mb-1">Category Fit</p>
                                    <p className="text-sm font-bold text-primary">High (92%)</p>
                                </div>
                                <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
                                    <p className="text-[10px] font-bold text-slate-400 uppercase mb-1">Frequency</p>
                                    <p className="text-sm font-bold text-primary">Monthly</p>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Footer Actions */}
                    <div className="p-6 border-t border-slate-100 bg-slate-50/50 space-y-3">
                        <button className="w-full flex items-center justify-between p-4 bg-white border border-slate-200 rounded-xl hover:border-secondary transition-all group">
                            <span className="text-sm font-bold text-primary">View Full Report</span>
                            <ChevronRight className="w-4 h-4 text-slate-300 group-hover:text-secondary group-hover:translate-x-1 transition-all" />
                        </button>
                        <button className="w-full py-4 bg-rose-50 text-danger border border-rose-100 rounded-xl font-bold text-sm hover:bg-rose-100 transition-all">
                            Mark as Unauthorized
                        </button>
                    </div>
                </div>
            </div>
        </>
    );
}
