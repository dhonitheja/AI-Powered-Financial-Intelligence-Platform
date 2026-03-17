'use client';

import React from 'react';
import type { AutoPaySchedule } from '@/services/autoPayService';
import { AutoPayCard } from './AutoPayCard';
import { Search, SlidersHorizontal } from 'lucide-react';

interface AutoPayListProps {
    schedules: AutoPaySchedule[];
    loading?: boolean;
    onToggle: (id: string) => void;
    onDelete: (id: string) => void;
    onExecute: (id: string) => void;
    onSelect: (id: string) => void;
}

const SKELETON = Array.from({ length: 4 });

export function AutoPayList({
    schedules,
    loading,
    onToggle,
    onDelete,
    onExecute,
    onSelect,
}: AutoPayListProps) {
    const [search, setSearch] = React.useState('');
    const [statusFilter, setStatusFilter] = React.useState<string>('ALL');
    const [categoryFilter, setCategoryFilter] = React.useState<string>('ALL');
    const [sortBy, setSortBy] = React.useState<string>('nextDueDate');

    const categories = React.useMemo(
        () => ['ALL', ...Array.from(new Set(schedules.map((s) => s.paymentCategory)))],
        [schedules]
    );

    const filtered = React.useMemo(() => {
        let list = [...schedules];
        if (search.trim()) {
            const q = search.toLowerCase();
            list = list.filter(
                (s) =>
                    s.paymentName.toLowerCase().includes(q) ||
                    s.paymentProvider?.toLowerCase().includes(q) ||
                    s.categoryDisplayName.toLowerCase().includes(q)
            );
        }
        if (statusFilter !== 'ALL') list = list.filter((s) => s.status === statusFilter);
        if (categoryFilter !== 'ALL') list = list.filter((s) => s.paymentCategory === categoryFilter);

        list.sort((a, b) => {
            if (sortBy === 'nextDueDate') return a.nextDueDate.localeCompare(b.nextDueDate);
            if (sortBy === 'amount') return b.amount - a.amount;
            if (sortBy === 'name') return a.paymentName.localeCompare(b.paymentName);
            return 0;
        });

        return list;
    }, [schedules, search, statusFilter, categoryFilter, sortBy]);

    return (
        <div className="space-y-4">
            {/* Filter bar */}
            <div className="flex flex-col sm:flex-row gap-3">
                {/* Search */}
                <div className="relative flex-1">
                    <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                    <input
                        type="text"
                        placeholder="Search payments…"
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        className="w-full pl-9 pr-4 py-2 bg-white/5 border border-white/10 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:border-violet-500/50 transition-colors"
                    />
                </div>

                {/* Status filter */}
                <select
                    value={statusFilter}
                    onChange={(e) => setStatusFilter(e.target.value)}
                    className="px-3 py-2 bg-white/5 border border-white/10 rounded-xl text-sm text-slate-300 focus:outline-none focus:border-violet-500/50"
                >
                    <option value="ALL">All Status</option>
                    <option value="ACTIVE">Active</option>
                    <option value="DUE_SOON">Due Soon</option>
                    <option value="OVERDUE">Overdue</option>
                    <option value="INACTIVE">Inactive</option>
                </select>

                {/* Sort */}
                <div className="flex items-center gap-2">
                    <SlidersHorizontal size={14} className="text-slate-400" />
                    <select
                        value={sortBy}
                        onChange={(e) => setSortBy(e.target.value)}
                        className="px-3 py-2 bg-white/5 border border-white/10 rounded-xl text-sm text-slate-300 focus:outline-none focus:border-violet-500/50"
                    >
                        <option value="nextDueDate">Sort: Due Date</option>
                        <option value="amount">Sort: Amount</option>
                        <option value="name">Sort: Name</option>
                    </select>
                </div>
            </div>

            {/* Results count */}
            {!loading && (
                <p className="text-xs text-slate-500">
                    {filtered.length} of {schedules.length} payment{schedules.length !== 1 ? 's' : ''}
                </p>
            )}

            {/* Loading skeletons */}
            {loading && (
                <div className="space-y-3">
                    {SKELETON.map((_, i) => (
                        <div key={i} className="h-24 rounded-2xl bg-white/5 animate-pulse" />
                    ))}
                </div>
            )}

            {/* Empty state */}
            {!loading && filtered.length === 0 && (
                <div className="text-center py-16 text-slate-500">
                    <p className="text-4xl mb-3">💳</p>
                    <p className="font-medium">No payments found</p>
                    <p className="text-sm mt-1">Try adjusting your filters</p>
                </div>
            )}

            {/* Cards */}
            {!loading && (
                <div className="space-y-3">
                    {filtered.map((schedule) => (
                        <AutoPayCard
                            key={schedule.id}
                            schedule={schedule}
                            onToggle={onToggle}
                            onDelete={onDelete}
                            onExecute={onExecute}
                            onClick={onSelect}
                        />
                    ))}
                </div>
            )}
        </div>
    );
}
