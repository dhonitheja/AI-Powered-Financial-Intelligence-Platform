export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';

export interface Transaction {
    id: string;
    description: string;
    amount: number;
    transactionDate: string;
    category: string;
    fraudRiskScore: number;
    aiExplanation?: string;
    status: 'ANALYZING' | 'ANALYZED' | 'PENDING';
}

export interface SpendingSummary {
    category: string;
    totalAmount: number;
}
