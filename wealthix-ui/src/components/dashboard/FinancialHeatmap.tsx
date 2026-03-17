"use client";

import React from 'react';
import { ComparisonData } from '@/services/api';

interface HeatmapProps {
    data: ComparisonData;
}

export function FinancialHeatmap({ data }: HeatmapProps) {
    const maxSpend = Math.max(...data.categories.map(c => c.currentPeriodSpend), 1);

    return (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
            {data.categories.map((cat) => {
                const intensity = (cat.currentPeriodSpend / maxSpend);
                // Map intensity to a blue/purple/gold gradient range
                const opacity = Math.min(Math.max(intensity, 0.1), 0.9);
                
                return (
                    <div 
                        key={cat.category}
                        className="heatmap-cell group flex flex-col justify-between p-4 h-24 bg-white/[0.03] border border-white/[0.05] hover:border-blue-500/30 overflow-hidden relative"
                        style={{
                            backgroundColor: `rgba(59, 130, 246, ${opacity * 0.2})`,
                        }}
                    >
                        {/* Shimmer effect for high intensity */}
                        {intensity > 0.7 && (
                            <div className="absolute inset-0 bg-gradient-to-tr from-transparent via-white/[0.05] to-transparent animate-pulse" />
                        )}
                        
                        <span className="text-[10px] font-black uppercase tracking-tighter text-slate-500 group-hover:text-blue-400 transition-colors">
                            {cat.category.replace(/_/g, ' ')}
                        </span>
                        <div className="flex flex-col">
                            <span className="text-sm font-black text-white">
                                ${Math.round(cat.currentPeriodSpend).toLocaleString()}
                            </span>
                            <div className="flex items-center gap-1">
                                <div className="h-0.5 flex-1 bg-white/5 rounded-full overflow-hidden">
                                    <div 
                                        className="h-full bg-blue-500 shadow-[0_0_8px_rgba(59,130,246,0.5)]" 
                                        style={{ width: `${intensity * 100}%` }}
                                    />
                                </div>
                            </div>
                        </div>
                    </div>
                );
            })}
        </div>
    );
}
