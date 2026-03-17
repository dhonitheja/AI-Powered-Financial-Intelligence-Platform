'use client';

import React from 'react';

interface PaymentHealthScoreProps {
    score: number;
    label: string;
}

const COLORS = {
    Excellent: { stroke: '#22c55e', glow: '#22c55e33', text: 'text-emerald-400' },
    Good: { stroke: '#3b82f6', glow: '#3b82f633', text: 'text-blue-400' },
    Fair: { stroke: '#f59e0b', glow: '#f59e0b33', text: 'text-amber-400' },
    'At Risk': { stroke: '#ef4444', glow: '#ef444433', text: 'text-red-400' },
};

export function PaymentHealthScore({ score, label }: PaymentHealthScoreProps) {
    const color = COLORS[label as keyof typeof COLORS] || COLORS['At Risk'];
    const radius = 52;
    const circumference = 2 * Math.PI * radius;
    const strokeDashoffset = circumference - (score / 100) * circumference;

    return (
        <div className="flex flex-col items-center gap-3">
            <div className="relative">
                {/* Glow effect */}
                <div
                    className="absolute inset-0 rounded-full blur-xl opacity-40"
                    style={{ backgroundColor: color.glow }}
                />
                <svg width="140" height="140" className="relative">
                    {/* Track */}
                    <circle
                        cx="70" cy="70" r={radius}
                        fill="none"
                        stroke="rgba(255,255,255,0.06)"
                        strokeWidth="10"
                    />
                    {/* Progress */}
                    <circle
                        cx="70" cy="70" r={radius}
                        fill="none"
                        stroke={color.stroke}
                        strokeWidth="10"
                        strokeLinecap="round"
                        strokeDasharray={circumference}
                        strokeDashoffset={strokeDashoffset}
                        transform="rotate(-90 70 70)"
                        style={{ transition: 'stroke-dashoffset 1s ease-in-out' }}
                    />
                    {/* Score text */}
                    <text
                        x="70" y="66"
                        textAnchor="middle"
                        dominantBaseline="middle"
                        className={`text-3xl font-bold ${color.text}`}
                        fill="currentColor"
                        style={{ fontSize: '28px', fontWeight: 700, fill: color.stroke }}
                    >
                        {score}
                    </text>
                    <text
                        x="70" y="85"
                        textAnchor="middle"
                        fill="rgba(148,163,184,0.8)"
                        style={{ fontSize: '11px' }}
                    >
                        / 100
                    </text>
                </svg>
            </div>
            <div className="text-center">
                <span className={`font-semibold text-sm ${color.text}`}>{label}</span>
                <p className="text-xs text-slate-500 mt-0.5">Payment Health</p>
            </div>
        </div>
    );
}
