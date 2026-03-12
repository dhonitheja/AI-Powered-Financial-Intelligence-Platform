import React, { useState, useRef, useEffect } from 'react';
import { Card } from '@/components/ui/Card';
import { Send, Bot, User as UserIcon, Loader2, Sparkles, ArrowRight } from 'lucide-react';
import { chatService } from '@/services/api';
import clsx from 'clsx';

interface Message {
    role: 'user' | 'assistant';
    content: string;
    suggestions?: string[];
}

export function ChatBot() {
    const [messages, setMessages] = useState<Message[]>([
        {
            role: 'assistant',
            content: "Hello! I'm Jass, your Wealthix AI financial assistant. I can help you analyze your spending, detect risks, and provide personalized financial advice.",
            suggestions: [
                "Tell me about my recent spending",
                "Do I have any fraud risks?",
                "How can I save more money?"
            ]
        }
    ]);
    const [input, setInput] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const messagesEndRef = useRef<HTMLDivElement>(null);

    const sessionId = useRef(crypto.randomUUID());

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
            const response = await chatService.sendMessage({
                sessionId: sessionId.current,
                message: queryText
            });

            const data = response.data?.data || response.data;

            const assistantMessage: Message = {
                role: 'assistant',
                content: data.reply || "I couldn't process that request.",
                suggestions: data.suggestedActions
            };
            setMessages(prev => [...prev, assistantMessage]);
        } catch (error) {
            setMessages(prev => [...prev, {
                role: 'assistant',
                content: "I'm sorry, I'm having trouble connecting to the intelligence engine. Please check your network or try again."
            }]);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <Card className="flex flex-col h-[600px] bg-[#0D0B1E] border border-white/10">
            {/* Header */}
            <div className="bg-gradient-to-r from-[#0D0B1E] to-[#1E1B4B] border-b border-[#D4AF37]/20 p-4 rounded-t-xl">
                <div className="flex items-center gap-3">
                    <div className="relative">
                        <div className="w-9 h-9 bg-gradient-to-br from-[#D4AF37] to-[#B8962E] p-2 rounded-xl flex items-center justify-center shadow-gold">
                            <Sparkles className="w-4 h-4 text-[#0D0B1E]" />
                        </div>
                        <span className="absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 bg-emerald-400 rounded-full border-2 border-[#0D0B1E]" />
                    </div>
                    <div>
                        <h2 className="text-lg font-bold gold-text">Jass</h2>
                        <p className="text-xs text-[#D4AF37]/60 font-medium flex items-center gap-1">
                            <span className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse" />
                            Wealthix AI · Live context
                        </p>
                    </div>
                </div>
            </div>

            {/* Messages */}
            <div className="flex-1 overflow-y-auto p-4 space-y-4" style={{ background: "rgba(13,11,30,0.95)" }}>
                {messages.map((msg, idx) => (
                    <div key={idx} className={clsx("flex", msg.role === 'user' ? 'justify-end' : 'justify-start')}>
                        <div className={clsx("flex gap-3 max-w-[85%]", msg.role === 'user' ? 'flex-row-reverse' : 'flex-row')}>
                            <div className={clsx(
                                "w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0",
                                msg.role === 'user'
                                    ? 'bg-[#1E1B4B] border border-white/10'
                                    : 'bg-gradient-to-br from-[#D4AF37] to-[#B8962E]'
                            )}>
                                {msg.role === 'user'
                                    ? <UserIcon className="w-4 h-4 text-slate-300" />
                                    : <Bot className="w-4 h-4 text-[#0D0B1E]" />}
                            </div>
                            <div className="space-y-2">
                                <div className={clsx(
                                    "p-3 rounded-2xl text-sm leading-relaxed",
                                    msg.role === 'user'
                                        ? 'bg-[#1E1B4B] border border-[#D4AF37]/20 text-slate-200 rounded-tr-none'
                                        : 'bg-white/5 border border-white/10 text-slate-300 rounded-tl-none'
                                )}>
                                    {msg.content}
                                </div>
                                {msg.suggestions && msg.suggestions.length > 0 && (
                                    <div className="flex flex-wrap gap-2 pt-1">
                                        {msg.suggestions.map((suggestion, sIdx) => (
                                            <button
                                                key={sIdx}
                                                onClick={() => handleSend(suggestion)}
                                                className="bg-white/5 border border-white/10 text-slate-400 px-3 py-1 rounded-full text-[11px] font-bold hover:border-[#D4AF37]/40 hover:text-[#F5D67B] hover:bg-[#D4AF37]/5 transition-all flex items-center gap-1"
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
                            <div className="w-8 h-8 rounded-full bg-gradient-to-br from-[#D4AF37] to-[#B8962E] flex items-center justify-center">
                                <Loader2 className="w-4 h-4 text-[#0D0B1E] animate-spin" />
                            </div>
                            <div className="px-4 py-2 bg-white/5 border border-white/10 rounded-2xl rounded-tl-none text-slate-500 italic text-sm">
                                Jass is thinking...
                            </div>
                        </div>
                    </div>
                )}
                <div ref={messagesEndRef} />
            </div>

            {/* Input */}
            <div className="border-t border-white/10 p-4 rounded-b-xl" style={{ background: "#0D0B1E" }}>
                <div className="relative flex items-center gap-3">
                    <div className="flex-1 relative">
                        <input
                            type="text"
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                            onKeyPress={(e) => e.key === 'Enter' && handleSend(input)}
                            placeholder="Ask Jass about your transactions, risks, or savings..."
                            className="w-full pl-4 pr-10 py-2.5 bg-white/5 border border-white/10 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#D4AF37]/20 focus:border-[#D4AF37]/40 transition-all text-sm font-medium text-slate-200 placeholder:text-slate-600"
                            disabled={isLoading}
                        />
                        <div className="absolute right-3 top-1/2 -translate-y-1/2">
                            <Sparkles className={clsx("w-4 h-4", isLoading ? 'text-[#D4AF37] animate-pulse' : 'text-slate-600')} />
                        </div>
                    </div>
                    <button
                        onClick={() => handleSend(input)}
                        disabled={!input.trim() || isLoading}
                        className="bg-gradient-to-br from-[#D4AF37] to-[#B8962E] text-[#0D0B1E] p-2.5 rounded-xl shadow-gold hover:scale-105 disabled:opacity-40 disabled:cursor-not-allowed transition-all"
                    >
                        <Send className="w-5 h-5" />
                    </button>
                </div>
            </div>
        </Card>
    );
}
