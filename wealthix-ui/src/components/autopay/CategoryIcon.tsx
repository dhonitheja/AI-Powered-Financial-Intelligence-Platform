'use client';

import React from 'react';
import {
    Home, Car, GraduationCap, CreditCard, Heart, Shield,
    Zap, Play, TrendingUp, MoreHorizontal, Building,
    LucideIcon,
} from 'lucide-react';
import type { PaymentCategory } from '@/services/autoPayService';

const ICON_MAP: Record<string, LucideIcon> = {
    Home, Car, GraduationCap, CreditCard, Heart, Shield,
    Zap, Play, TrendingUp, MoreHorizontal, Building,
};

const CATEGORY_ICONS: Record<PaymentCategory, string> = {
    HOME_LOAN: 'Home',
    AUTO_LOAN: 'Car',
    PERSONAL_LOAN: 'GraduationCap',
    EDUCATION_LOAN: 'GraduationCap',
    CREDIT_CARD: 'CreditCard',
    HEALTH_INSURANCE: 'Heart',
    HOME_INSURANCE: 'Home',
    AUTO_INSURANCE: 'Car',
    LIFE_INSURANCE: 'Shield',
    TERM_INSURANCE: 'Shield',
    UTILITY: 'Zap',
    SUBSCRIPTION: 'Play',
    SIP: 'TrendingUp',
    RENT: 'Building',
    CUSTOM: 'MoreHorizontal',
};

const CATEGORY_COLORS: Record<PaymentCategory, string> = {
    HOME_LOAN: '#6366f1',
    AUTO_LOAN: '#3b82f6',
    PERSONAL_LOAN: '#8b5cf6',
    EDUCATION_LOAN: '#a78bfa',
    CREDIT_CARD: '#f59e0b',
    HEALTH_INSURANCE: '#ef4444',
    HOME_INSURANCE: '#10b981',
    AUTO_INSURANCE: '#06b6d4',
    LIFE_INSURANCE: '#14b8a6',
    TERM_INSURANCE: '#0d9488',
    UTILITY: '#f97316',
    SUBSCRIPTION: '#a855f7',
    SIP: '#22c55e',
    RENT: '#64748b',
    CUSTOM: '#94a3b8',
};

interface CategoryIconProps {
    category: PaymentCategory;
    size?: number;
    className?: string;
}

export function CategoryIcon({ category, size = 20, className = '' }: CategoryIconProps) {
    const iconName = CATEGORY_ICONS[category] || 'MoreHorizontal';
    const color = CATEGORY_COLORS[category] || '#94a3b8';
    const Icon = ICON_MAP[iconName] || MoreHorizontal;

    return (
        <div
            className={`inline-flex items-center justify-center rounded-xl ${className}`}
            style={{
                width: size + 16,
                height: size + 16,
                backgroundColor: `${color}22`,
                border: `1px solid ${color}44`,
            }}
        >
            <Icon size={size} style={{ color }} />
        </div>
    );
}

export { CATEGORY_COLORS, CATEGORY_ICONS };
