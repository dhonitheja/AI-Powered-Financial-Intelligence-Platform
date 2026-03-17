import api from './api';

// ── Types ─────────────────────────────────────────────────────────────────────

export type PaymentCategory =
    | 'HOME_LOAN' | 'AUTO_LOAN' | 'PERSONAL_LOAN' | 'EDUCATION_LOAN'
    | 'CREDIT_CARD' | 'HEALTH_INSURANCE' | 'HOME_INSURANCE' | 'AUTO_INSURANCE'
    | 'LIFE_INSURANCE' | 'TERM_INSURANCE' | 'UTILITY' | 'SUBSCRIPTION'
    | 'SIP' | 'RENT' | 'CUSTOM';

export type PaymentFrequency =
    | 'DAILY' | 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'ANNUALLY';

export type ScheduleStatus = 'ACTIVE' | 'DUE_SOON' | 'OVERDUE' | 'INACTIVE';

export type ExecutionStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'SKIPPED' | 'CANCELLED';

export interface AutoPaySchedule {
    id: string;
    paymentName: string;
    paymentCategory: PaymentCategory;
    categoryDisplayName: string;
    paymentProvider: string | null;
    accountNumberMasked: string | null;  // Always "****XXXX" — never full number
    hasRoutingNumber: boolean;
    hasNotes: boolean;
    amount: number;
    currency: string;
    monthlyEquivalent: number;
    frequency: PaymentFrequency;
    nextDueDate: string;         // ISO date string
    dueDayOfMonth: number | null;
    autoExecute: boolean;
    reminderDaysBefore: number;
    active: boolean;
    status: ScheduleStatus;
    createdAt: string;
    updatedAt: string;
}

export type PlaidVerificationStatus = 'UNVERIFIED' | 'VERIFIED' | 'NEEDS_REVIEW';

export interface AutoPayExecutionLog {
    id: string;
    scheduleId: string;
    paymentName: string;
    executionDate: string;
    amountPaid: number;
    currency: string;
    status: ExecutionStatus;
    failureReason: string | null;
    plaidTransactionId: string | null;
    stripePaymentIntentId: string | null;
    plaidVerificationStatus: PlaidVerificationStatus;
    plaidMatchedTransactionId: string | null;
    createdAt: string;
}

/**
 * A recurring payment detected from Plaid transaction history.
 * This is a suggestion — not yet saved to the database.
 * Contains ONLY anonymised data (description, amounts, category).
 */
export interface DetectedRecurringPayment {
    paymentName: string;
    merchantDescription: string;
    averageAmount: number;
    minAmount: number;
    maxAmount: number;
    suggestedCategory: PaymentCategory;
    suggestedFrequency: PaymentFrequency;
    suggestedDayOfMonth: number;
    occurrenceCount: number;
    confidenceScore: number;
    currency: string;
}

export interface CategoryStat {
    count: number;
    monthlyTotal: number;
}

export interface AutoPayDashboard {
    totalMonthlyObligations: number;
    totalAnnualObligations: number;
    activeScheduleCount: number;
    dueSoonCount: number;
    overdueCount: number;
    categoryBreakdown: Record<string, CategoryStat>;
    upcomingPayments: AutoPaySchedule[];
    paymentHealthScore: number;
    healthLabel: string;
}

export interface CategoryMetadata {
    value: PaymentCategory;
    displayName: string;
    icon: string;
    color: string;
}

export interface CreateAutoPayRequest {
    paymentName: string;
    paymentCategory: PaymentCategory;
    paymentProvider?: string;
    accountNumber?: string;   // Sent over HTTPS only, encrypted server-side
    routingNumber?: string;   // Sent over HTTPS only, encrypted server-side
    notes?: string;
    amount: number;
    currency?: string;
    frequency: PaymentFrequency;
    nextDueDate: string;
    dueDayOfMonth?: number;
    autoExecute?: boolean;
    reminderDaysBefore?: number;
}

export interface UpdateAutoPayRequest extends Partial<CreateAutoPayRequest> { }

export interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}


// ── API Client ────────────────────────────────────────────────────────────────

const BASE = '/v1/autopay';

export const autoPayService = {
    // ── Dashboard ───────────────────────────────────────────────────────────────
    getDashboard: () =>
        api.get<AutoPayDashboard>(`${BASE}/dashboard`),

    // ── Schedules ───────────────────────────────────────────────────────────────
    listSchedules: (page = 0, size = 20) =>
        api.get<PageResponse<AutoPaySchedule>>(`${BASE}/schedules`, {
            params: { page, size, sort: 'nextDueDate,asc' },
        }),

    getSchedule: (id: string) =>
        api.get<AutoPaySchedule>(`${BASE}/schedules/${id}`),

    createSchedule: (data: CreateAutoPayRequest) =>
        api.post<AutoPaySchedule>(`${BASE}/schedules`, data),

    updateSchedule: (id: string, data: UpdateAutoPayRequest) =>
        api.put<AutoPaySchedule>(`${BASE}/schedules/${id}`, data),

    deleteSchedule: (id: string) =>
        api.delete(`${BASE}/schedules/${id}`),

    toggleSchedule: (id: string) =>
        api.patch<AutoPaySchedule>(`${BASE}/schedules/${id}/toggle`),

    getUpcoming: (days = 30) =>
        api.get<AutoPaySchedule[]>(`${BASE}/schedules/upcoming`, { params: { days } }),

    getOverdue: () =>
        api.get<AutoPaySchedule[]>(`${BASE}/schedules/overdue`),

    // ── Execution ───────────────────────────────────────────────────────────────
    executePayment: (id: string) =>
        api.post<AutoPayExecutionLog>(`${BASE}/schedules/${id}/execute`),

    // ── Execution Logs ──────────────────────────────────────────────────────────
    getExecutionLogs: (page = 0, size = 20) =>
        api.get<PageResponse<AutoPayExecutionLog>>(`${BASE}/execution-logs`, {
            params: { page, size },
        }),

    getLogsForSchedule: (scheduleId: string, page = 0, size = 20) =>
        api.get<PageResponse<AutoPayExecutionLog>>(
            `${BASE}/execution-logs/${scheduleId}`,
            { params: { page, size } }
        ),

    // ── Category Metadata ───────────────────────────────────────────────────────
    getCategories: () =>
        api.get<CategoryMetadata[]>(`${BASE}/categories`),

    // ── Plaid Intelligence ──────────────────────────────────────────────────────
    detectRecurring: () =>
        api.get<DetectedRecurringPayment[]>(`${BASE}/detect-recurring`),

    addDetectedPayment: (detected: DetectedRecurringPayment) => {
        const today = new Date();
        const nextDue = new Date(today.getFullYear(), today.getMonth(), detected.suggestedDayOfMonth);
        if (nextDue < today) nextDue.setMonth(nextDue.getMonth() + 1);
        const request: CreateAutoPayRequest = {
            paymentName: detected.paymentName,
            paymentCategory: detected.suggestedCategory,
            amount: detected.averageAmount,
            currency: detected.currency,
            frequency: detected.suggestedFrequency,
            nextDueDate: nextDue.toISOString().split('T')[0],
            dueDayOfMonth: detected.suggestedDayOfMonth,
            reminderDaysBefore: 3,
        };
        return api.post<AutoPaySchedule>(`${BASE}/schedules`, request);
    },

    verifyPayments: () =>
        api.post<{ message: string }>(`${BASE}/verify-payments`),

    // ── Stripe ──────────────────────────────────────────────────────────────────
    createSetupIntent: () =>
        api.post<{ clientSecret: string; publishableKey: string }>(`${BASE}/stripe/setup-intent`),

    attachPaymentMethod: (paymentMethodId: string) =>
        api.post<{ paymentMethodId: string; brand: string; last4: string; expMonth: number; expYear: number; isDefault: boolean }>(`${BASE}/stripe/payment-methods`, { paymentMethodId }),

    listPaymentMethods: () =>
        api.get<{ paymentMethodId: string; brand: string; last4: string; expMonth: number; expYear: number; isDefault: boolean }[]>(`${BASE}/stripe/payment-methods`),

    detachPaymentMethod: (paymentMethodId: string) =>
        api.delete(`${BASE}/stripe/payment-methods/${paymentMethodId}`),
};

export default autoPayService;

// ── Helper utilities ──────────────────────────────────────────────────────────

export const FREQUENCY_LABELS: Record<PaymentFrequency, string> = {
    DAILY: 'Daily',
    WEEKLY: 'Weekly',
    BIWEEKLY: 'Bi-Weekly',
    MONTHLY: 'Monthly',
    QUARTERLY: 'Quarterly',
    ANNUALLY: 'Annually',
};

export const STATUS_COLORS: Record<ScheduleStatus, string> = {
    ACTIVE: 'text-emerald-400',
    DUE_SOON: 'text-amber-400',
    OVERDUE: 'text-red-400',
    INACTIVE: 'text-slate-400',
};

export const STATUS_BG: Record<ScheduleStatus, string> = {
    ACTIVE: 'bg-emerald-500/15',
    DUE_SOON: 'bg-amber-500/15',
    OVERDUE: 'bg-red-500/15',
    INACTIVE: 'bg-slate-500/15',
};

export function formatCurrency(amount: number, currency = 'USD'): string {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency,
        minimumFractionDigits: 2,
    }).format(amount);
}

export function daysUntilDue(nextDueDate: string): number {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const due = new Date(nextDueDate);
    due.setHours(0, 0, 0, 0);
    return Math.ceil((due.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
}
