import React, { useEffect, useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { chatService } from '@/services/api';
import { Sparkles, TrendingUp, Target, CreditCard, Activity, ArrowRight } from 'lucide-react';

export function BudgetInsights() {
    const [budgetData, setBudgetData] = useState<any>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const loadBudget = async () => {
            try {
                const res = await chatService.getBudgetRecommendations();
                setBudgetData(res.data?.data || res.data);
            } catch (e) {
                console.error("Failed to fetch budget insights", e);
            } finally {
                setLoading(false);
            }
        };
        loadBudget();
    }, []);

    if (loading) {
        return (
            <Card className="animate-pulse flex items-center justify-center p-8 h-64">
                <p className="text-slate-400 font-medium flex items-center gap-2">
                    <Activity className="w-5 h-5 animate-spin" /> Gathering AI insights...
                </p>
            </Card>
        );
    }

    if (!budgetData) return null;

    return (
        <div className="space-y-6">
            <Card>
                <CardHeader className="border-b border-slate-100 bg-slate-50 rounded-t-xl">
                    <CardTitle className="text-lg flex items-center gap-2">
                        <Sparkles className="w-5 h-5 text-indigo-500" />
                        AI Budget Recommendations
                    </CardTitle>
                    <p className="text-xs text-slate-500 mt-1">
                        Personalized insights based on your spending and 50/30/20 rule
                    </p>
                </CardHeader>
                <CardContent className="p-0">
                    <div className="grid grid-cols-1 md:grid-cols-3 divide-y-2 md:divide-y-0 md:divide-x-2 divide-slate-50">
                        {budgetData.recommendations.map((rec: string, idx: number) => (
                            <div key={idx} className="p-6">
                                <div className="w-10 h-10 rounded-full bg-indigo-50/50 flex items-center justify-center mb-4">
                                    {idx === 0 ? <TrendingUp className="w-5 h-5 text-indigo-500" /> : idx === 1 ? <Target className="w-5 h-5 text-indigo-500" /> : <CreditCard className="w-5 h-5 text-indigo-500" />}
                                </div>
                                <h4 className="font-bold text-slate-800 text-sm">{rec}</h4>
                            </div>
                        ))}
                    </div>
                </CardContent>
            </Card>

            {budgetData.debt_payoff_plan && budgetData.debt_payoff_plan.length > 0 && (
                <Card>
                    <CardHeader className="border-b border-slate-100 bg-slate-50 rounded-t-xl">
                        <CardTitle className="text-lg flex items-center gap-2">
                            <Target className="w-5 h-5 text-emerald-500" />
                            Smart Debt Strategy (Snowball)
                        </CardTitle>
                    </CardHeader>
                    <CardContent className="p-0 divide-y divide-slate-100">
                        {budgetData.debt_payoff_plan.map((debt: any, idx: number) => (
                            <div key={idx} className="p-5 flex items-center justify-between hover:bg-slate-50 transition-colors">
                                <div className="flex items-center gap-4">
                                    <div className="w-8 h-8 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold text-sm">
                                        #{debt.priority}
                                    </div>
                                    <div>
                                        <p className="font-bold text-slate-800">{debt.category.replace('_', ' ')}</p>
                                        <p className="text-sm text-slate-500">{debt.strategy}</p>
                                    </div>
                                </div>
                                <div>
                                    {debt.priority === 1 && (
                                        <span className="px-3 py-1 bg-emerald-100 text-emerald-700 rounded-full text-[10px] font-bold uppercase tracking-wider flex items-center gap-1">
                                            Extra Payments <ArrowRight className="w-3 h-3" />
                                        </span>
                                    )}
                                </div>
                            </div>
                        ))}
                    </CardContent>
                </Card>
            )}
        </div>
    );
}
