import React, { useState, useRef, useEffect } from 'react';
import { Card } from '@/components/ui/Card';
import { Send, Bot, User as UserIcon, Loader2, Sparkles, ArrowRight, ShieldCheck } from 'lucide-react';
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
            content: "Hello! I'm your AI Finance Assistant. I can help you analyze your spending, detect risks, and provide personalized financial advice.",
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

    // Hardcode a session ID for now
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

            // Handle standard ApiResponse wrapper: response.data.data
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
        <Card className="flex flex-col h-[600px] bg-[#F8FAFC]">
            <div className="bg-white border-b border-slate-200 p-4 rounded-t-xl">
                <div className="flex items-center gap-3">
                    <div className="bg-secondary/10 p-2 rounded-xl">
                        <Bot className="w-5 h-5 text-secondary" />
                    </div>
                    <div>
                        <h2 className="text-lg font-bold text-primary">AI Chat Assistant</h2>
                        <p className="text-xs text-slate-500 font-medium flex items-center gap-1">
                            <span className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse" />
                            Online & analyzing your context
                        </p>
                    </div>
                </div>
            </div>

            <div className="flex-1 overflow-y-auto p-4 space-y-4">
                {messages.map((msg, idx) => (
                    <div key={idx} className={clsx("flex", msg.role === 'user' ? 'justify-end' : 'justify-start')}>
                        <div className={clsx("flex gap-3 max-w-[85%]", msg.role === 'user' ? 'flex-row-reverse' : 'flex-row')}>
                            <div className={clsx("w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0", msg.role === 'user' ? 'bg-primary' : 'bg-secondary')}>
                                {msg.role === 'user' ? <UserIcon className="w-4 h-4 text-white" /> : <Bot className="w-4 h-4 text-white" />}
                            </div>
                            <div className="space-y-2">
                                <div className={clsx("p-3 rounded-2xl shadow-sm text-sm leading-relaxed", msg.role === 'user' ? 'bg-primary text-white rounded-tr-none' : 'bg-white text-slate-700 border border-slate-100 rounded-tl-none')}>
                                    {msg.content}
                                </div>
                                {msg.suggestions && msg.suggestions.length > 0 && (
                                    <div className="flex flex-wrap gap-2 pt-1">
                                        {msg.suggestions.map((suggestion, sIdx) => (
                                            <button
                                                key={sIdx}
                                                onClick={() => handleSend(suggestion)}
                                                className="bg-white border border-slate-200 text-slate-600 px-3 py-1 rounded-full text-[11px] font-bold hover:border-secondary hover:text-secondary hover:bg-teal-50/30 transition-all flex items-center gap-1"
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

            <div className="bg-white border-t border-slate-200 p-4 rounded-b-xl">
                <div className="relative flex items-center gap-3">
                    <div className="flex-1 relative">
                        <input
                            type="text"
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                            onKeyPress={(e) => e.key === 'Enter' && handleSend(input)}
                            placeholder="Ask me about your transactions, risks, or savings..."
                            className="w-full pl-4 pr-10 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-secondary/20 focus:border-secondary transition-all text-sm font-medium"
                            disabled={isLoading}
                        />
                        <div className="absolute right-3 top-1/2 -translate-y-1/2">
                            <Sparkles className={clsx("w-4 h-4", isLoading ? 'text-secondary animate-pulse' : 'text-slate-300')} />
                        </div>
                    </div>
                    <button
                        onClick={() => handleSend(input)}
                        disabled={!input.trim() || isLoading}
                        className="bg-secondary text-white p-2.5 rounded-xl shadow hover:bg-teal-700 disabled:opacity-50 transition-all"
                    >
                        <Send className="w-5 h-5" />
                    </button>
                </div>
            </div>
        </Card>
    );
}
