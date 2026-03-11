"use client";

import React from 'react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ChatBot } from "@/components/chat/ChatBot";
import { BudgetInsights } from "@/components/budget/BudgetInsights";
import { SpendingAnomalies } from "@/components/budget/SpendingAnomalies";
import { Bot, LineChart, AlertTriangle } from 'lucide-react';

export default function AssistantPage() {
    return (
        <div className="flex flex-col h-[calc(100vh-64px)] bg-[#F8FAFC]">
            {/* Header Area */}
            <div className="bg-white border-b border-slate-200 p-6">
                <div className="max-w-6xl mx-auto flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <div className="bg-secondary/10 p-2 rounded-xl">
                            <Bot className="w-6 h-6 text-secondary" />
                        </div>
                        <div>
                            <h1 className="text-xl font-bold text-primary">Advanced AI & Personalization</h1>
                            <p className="text-sm text-slate-500 font-medium">
                                Wealthix Intelligence Engine (Gemini 2.5 Flash)
                            </p>
                        </div>
                    </div>
                </div>
            </div>

            {/* Main Content Area */}
            <div className="flex-1 overflow-y-auto p-6">
                <div className="max-w-6xl mx-auto">
                    <Tabs defaultValue="chat" className="w-full">
                        <TabsList className="mb-6 h-auto p-1 bg-white border border-slate-200 rounded-xl flex">
                            <TabsTrigger value="chat" className="flex-1 py-2.5 rounded-lg flex items-center gap-2">
                                <Bot className="w-4 h-4" /> AI Assistant
                            </TabsTrigger>
                            <TabsTrigger value="budget" className="flex-1 py-2.5 rounded-lg flex items-center gap-2">
                                <LineChart className="w-4 h-4" /> Budget Insights
                            </TabsTrigger>
                            <TabsTrigger value="anomalies" className="flex-1 py-2.5 rounded-lg flex items-center gap-2">
                                <AlertTriangle className="w-4 h-4" /> Spending Anomalies
                            </TabsTrigger>
                        </TabsList>
                        
                        <TabsContent value="chat" className="m-0 focus:outline-none placeholder-animate-in slide-in-from-left-4 duration-500">
                            <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
                                <div className="lg:col-span-1 hidden lg:block space-y-4">
                                    <div className="p-5 bg-white rounded-xl border border-slate-200">
                                        <h3 className="font-bold text-slate-800 text-sm mb-2">Capabilities</h3>
                                        <ul className="text-sm text-slate-600 space-y-2 list-disc list-inside marker:text-emerald-500">
                                            <li>Chat with your data safely</li>
                                            <li>Ask about recent spending</li>
                                            <li>Analyze subscription waste</li>
                                            <li>Get actionable tips</li>
                                        </ul>
                                    </div>
                                    <div className="p-5 bg-teal-50/50 rounded-xl border border-teal-100">
                                        <h3 className="font-bold text-teal-900 text-sm mb-1">Zero PII Risk</h3>
                                        <p className="text-xs text-teal-800/80 leading-relaxed">
                                            Your account numbers and SSN are never sent to external AI engines. We only analyze anonymized amounts and categories.
                                        </p>
                                    </div>
                                </div>
                                <div className="lg:col-span-3">
                                    <ChatBot />
                                </div>
                            </div>
                        </TabsContent>
                        
                        <TabsContent value="budget" className="m-0 focus:outline-none placeholder-animate-in slide-in-from-left-4 duration-500">
                            <BudgetInsights />
                        </TabsContent>
                        
                        <TabsContent value="anomalies" className="m-0 focus:outline-none placeholder-animate-in slide-in-from-left-4 duration-500">
                            <SpendingAnomalies />
                        </TabsContent>
                    </Tabs>
                </div>
            </div>
        </div>
    );
}
