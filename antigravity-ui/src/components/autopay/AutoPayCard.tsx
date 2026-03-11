'use client';

import React from 'react';
import { AutoPaySchedule, formatCurrency, daysUntilDue, STATUS_COLORS, STATUS_BG, FREQUENCY_LABELS } from '@/services/autoPayService';
import { CategoryIcon } from './CategoryIcon';
import { Calendar, ChevronRight, Pause, Play, Trash2, Zap } from 'lucide-react';

interface AutoPayCardProps {
    schedule: AutoPaySchedule;
    onToggle: (id: string) => void;
    onDelete: (id: string) => void;
    onExecute: (id: string) => void;
    onClick: (id: string) => void;
}

export function AutoPayCard({ schedule, onToggle, onDelete, onExecute, onClick }: AutoPayCardProps) {
    const days = daysUntilDue(schedule.nextDueDate);
    const isOverdue = days < 0;
    const isDueSoon = days >= 0 && days <= 7;

    const dueLabel = isOverdue
        ? `${Math.abs(days)}d overdue`
        : days === 0
            ? 'Due today'
            : `${days}d left`;

    return (
        <div
            className="group relative bg-white/5 border border-white/10 rounded-2xl p-5 hover:bg-white/8 hover:border-white/20 transition-all cursor-pointer"
            onClick={() => onClick(schedule.id)}
        >
            {/* Status indicator strip */}
            {schedule.active && (
                <div
                    className={`absolute left-0 top-4 bottom-4 w-0.5 rounded-full ${isOverdue ? 'bg-red-400' : isDueSoon ? 'bg-amber-400' : 'bg-emerald-400'
                        }`}
                />
            )}

            <div className="flex items-start gap-4">
                {/* Icon */}
                <CategoryIcon category={schedule.paymentCategory} size={18} />

                {/* Details */}
                <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2">
                        <div className="min-w-0">
                            <h3 className="font-semibold text-white truncate text-sm">
                                {schedule.paymentName}
                            </h3>
                            {schedule.paymentProvider && (
                                <p className="text-xs text-slate-400 truncate mt-0.5">
                                    {schedule.paymentProvider}
                                </p>
                            )}
                        </div>
                        <span className="text-lg font-bold text-white whitespace-nowrap">
                            {formatCurrency(schedule.amount, schedule.currency)}
                        </span>
                    </div>

                    <div className="mt-3 flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            {/* Frequency badge */}
                            <span className="text-xs px-2 py-0.5 rounded-full bg-white/10 text-slate-300">
                                {FREQUENCY_LABELS[schedule.frequency]}
                            </span>

                            {/* Status badge */}
                            <span
                                className={`text-xs px-2 py-0.5 rounded-full ${STATUS_BG[schedule.status]} ${STATUS_COLORS[schedule.status]} font-medium`}
                            >
                                {schedule.status === 'ACTIVE' ? 'On Track' : schedule.status.replace('_', ' ')}
                            </span>
                        </div>

                        {/* Due date */}
                        <div className={`flex items-center gap-1 text-xs font-medium ${isOverdue ? 'text-red-400' : isDueSoon ? 'text-amber-400' : 'text-slate-400'
                            }`}>
                            <Calendar size={12} />
                            {dueLabel}
                        </div>
                    </div>

                    {/* Account mask (security: only last 4 shown) */}
                    {schedule.accountNumberMasked && (
                        <p className="mt-2 text-xs text-slate-500 font-mono">
                            Acct: {schedule.accountNumberMasked}
                        </p>
                    )}
                </div>

                <ChevronRight size={16} className="text-slate-500 group-hover:text-slate-300 transition-colors flex-shrink-0 mt-1" />
            </div>

            {/* Action buttons — stop propagation so they don't trigger onClick */}
            <div
                className="absolute top-3 right-3 hidden group-hover:flex items-center gap-1.5 bg-slate-900/90 border border-white/10 rounded-xl p-1.5 backdrop-blur-sm"
                onClick={(e) => e.stopPropagation()}
            >
                <button
                    title="Execute now"
                    onClick={() => onExecute(schedule.id)}
                    className="p-1.5 rounded-lg hover:bg-violet-500/20 text-violet-400 transition-colors"
                >
                    <Zap size={13} />
                </button>
                <button
                    title={schedule.active ? 'Pause' : 'Resume'}
                    onClick={() => onToggle(schedule.id)}
                    className="p-1.5 rounded-lg hover:bg-amber-500/20 text-amber-400 transition-colors"
                >
                    {schedule.active ? <Pause size={13} /> : <Play size={13} />}
                </button>
                <button
                    title="Delete"
                    onClick={() => onDelete(schedule.id)}
                    className="p-1.5 rounded-lg hover:bg-red-500/20 text-red-400 transition-colors"
                >
                    <Trash2 size={13} />
                </button>
            </div>
        </div>
    );
}
