"use client";

import React, { useState, useRef, useEffect } from 'react';
import { Card } from '@/components/ui/Card';
import {
    Send,
    Bot,
    User as UserIcon,
    Loader2,
    Sparkles,
    ArrowRight,
    Search,
    History as HistoryIcon,
    ShieldCheck
} from 'lucide-react';
import { transactionService } from '@/services/api';

interface Message {
    role: 'user' | 'assistant';
    content: string;
    confidence_score?: number;
    suggestions?: string[];
}

const SUGGESTED_QUESTIONS = [
    "Tell me about my recent spending",
    "Do I have any fraud risks?",
    "How can I save more money?",
    "Show me my top spending categories"
];

export default function AssistantPage() {
    const [messages, setMessages] = useState<Message[]>([
        {
            role: 'assistant',
            content: "Hello! I'm your AI Finance Assistant. I can help you analyze your spending, detect risks, and provide personalized financial advice. How can I help you today?",
            suggestions: SUGGESTED_QUESTIONS
        }
    ]);
    const [input, setInput] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const messagesEndRef = useRef<HTMLDivElement>(null);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    const handleSend = async (queryText: string) => {
        if (!queryText.trim() || isLoading) return;

        const userMessage: Message = { role: 'user', content: queryText };
        setMessages(prev => [...prev, userMessage]);
        setInput('');
        setIsLoading(true);

        try {
            const response = await transactionService.askAssistant(queryText);
            const assistantMessage: Message = {
                role: 'assistant',
                content: response.data.reply
            };
            setMessages(prev => [...prev, assistantMessage]);
        } catch (error) {
            setMessages(prev => [...prev, {
                role: 'assistant',
                content: "I'm sorry, I'm having trouble connecting to the intelligence engine. Please check your backend status."
            }]);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="flex flex-col h-[calc(100vh-64px)] bg-[#F8FAFC]">
            {/* Header Area */}
            <div className="bg-white border-b border-slate-200 p-6">
                <div className="max-w-4xl mx-auto flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <div className="bg-secondary/10 p-2 rounded-xl">
                            <Bot className="w-6 h-6 text-secondary" />
                        </div>
                        <div>
                            <h1 className="text-xl font-bold text-primary">AI Finance Assistant</h1>
                            <p className="text-xs text-slate-500 font-medium flex items-center gap-1">
                                <span className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse" />
                                Online & analyzing your records
                            </p>
                        </div>
                    </div>
                </div>
            </div>

            {/* Messages Area */}
            <div className="flex-1 overflow-y-auto p-6">
                <div className="max-w-3xl mx-auto space-y-6">
                    {messages.map((msg, idx) => (
                        <div
                            key={idx}
                            className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'} animate-in fade-in slide-in-from-bottom-2 duration-300`}
                        >
                            <div className={`flex gap-3 max-w-[85%] ${msg.role === 'user' ? 'flex-row-reverse' : 'flex-row'}`}>
                                <div className={`w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 ${msg.role === 'user' ? 'bg-primary' : 'bg-secondary'
                                    }`}>
                                    {msg.role === 'user' ? <UserIcon className="w-4 h-4 text-white" /> : <Bot className="w-4 h-4 text-white" />}
                                </div>
                                <div className="space-y-2">
                                    <div className={`p-4 rounded-2xl shadow-sm text-sm leading-relaxed ${msg.role === 'user'
                                        ? 'bg-primary text-white rounded-tr-none'
                                        : 'bg-white text-slate-700 border border-slate-100 rounded-tl-none'
                                        }`}>
                                        {msg.content}
                                    </div>

                                    {msg.role === 'assistant' && msg.confidence_score !== undefined && (
                                        <div className="flex items-center gap-1.5 px-2 text-[10px] font-bold text-slate-400">
                                            <ShieldCheck className="w-3 h-3 text-secondary" />
                                            Confidence: {(msg.confidence_score * 100).toFixed(0)}%
                                        </div>
                                    )}

                                    {msg.suggestions && msg.suggestions.length > 0 && (
                                        <div className="flex flex-wrap gap-2 pt-2">
                                            {msg.suggestions.map((suggestion, sIdx) => (
                                                <button
                                                    key={sIdx}
                                                    onClick={() => handleSend(suggestion)}
                                                    className="bg-white border border-slate-200 text-slate-600 px-3 py-1.5 rounded-full text-[11px] font-bold hover:border-secondary hover:text-secondary hover:bg-teal-50/30 transition-all flex items-center gap-1"
                                                >
                                                    {suggestion}
                                                    <ArrowRight className="w-3 h-3" />
                                                </button>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>
                    ))}
                    {isLoading && (
                        <div className="flex justify-start">
                            <div className="flex gap-3 max-w-[85%] items-center">
                                <div className="w-8 h-8 rounded-full bg-secondary flex items-center justify-center">
                                    <Loader2 className="w-4 h-4 text-white animate-spin" />
                                </div>
                                <div className="px-4 py-2 bg-white border border-slate-100 rounded-2xl rounded-tl-none text-slate-400 italic text-sm">
                                    Thinking...
                                </div>
                            </div>
                        </div>
                    )}
                    <div ref={messagesEndRef} />
                </div>
            </div>

            {/* Input Area */}
            <div className="bg-white border-t border-slate-200 p-6 sticky bottom-0">
                <div className="max-w-3xl mx-auto">
                    <div className="relative flex items-center gap-3">
                        <div className="flex-1 relative">
                            <input
                                type="text"
                                value={input}
                                onChange={(e) => setInput(e.target.value)}
                                onKeyPress={(e) => e.key === 'Enter' && handleSend(input)}
                                placeholder="Ask me about your transactions, risks, or savings..."
                                className="w-full pl-4 pr-12 py-3 bg-slate-50 border border-slate-200 rounded-2xl focus:ring-2 focus:ring-secondary/20 focus:border-secondary transition-all text-sm font-medium"
                                disabled={isLoading}
                            />
                            <div className="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-2">
                                <Sparkles className={`w-4 h-4 ${isLoading ? 'text-secondary animate-pulse' : 'text-slate-300'}`} />
                            </div>
                        </div>
                        <button
                            onClick={() => handleSend(input)}
                            disabled={!input.trim() || isLoading}
                            className="bg-secondary text-white p-3 rounded-2xl shadow-lg shadow-teal-900/20 hover:scale-105 active:scale-95 disabled:opacity-50 disabled:scale-100 transition-all"
                        >
                            <Send className="w-5 h-5" />
                        </button>
                    </div>
                    <div className="mt-3 flex items-center justify-center gap-4 text-[10px] text-slate-400 font-bold uppercase tracking-widest">
                        <span className="flex items-center gap-1"><ShieldAlert className="w-3 h-3" /> Secure AI Engine</span>
                        <span className="w-1 h-1 bg-slate-200 rounded-full" />
                        <span className="flex items-center gap-1"><Sparkles className="w-3 h-3" /> Gemini 1.5 Powered</span>
                    </div>
                </div>
            </div>
        </div>
    );
}

function ShieldAlert({ className }: { className?: string }) {
    return (
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={className}>
            <path d="M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.5 3.8 17 5 19 5a1 1 0 0 1 1 1z" />
            <path d="M12 8v4" />
            <path d="M12 16h.01" />
        </svg>
    )
}
