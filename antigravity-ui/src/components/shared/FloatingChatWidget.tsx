"use client";

import React, { useState, useRef, useEffect, useCallback } from "react";
import { chatService } from "@/services/api";
import { useAuth } from "@/services/authStore";
import {
    X, Send, Bot, User, Loader2,
    Minimize2, Maximize2, Sparkles, AlertCircle
} from "lucide-react";
import { cn } from "@/components/ui/Card";

// ─── Types ────────────────────────────────────────────────────────────────────
interface ChatMessage {
    id: string;
    role: "user" | "assistant";
    content: string;
    timestamp: Date;
    isError?: boolean;
}

// ─── Starter suggestions ──────────────────────────────────────────────────────
const SUGGESTIONS = [
    "What's my net worth?",
    "How much did I spend last month?",
    "Am I overspending on credit?",
    "What's my biggest spending category?",
];

// ─── Chat Widget ──────────────────────────────────────────────────────────────
export default function FloatingChatWidget() {
    const { isAuthenticated } = useAuth();
    const [isOpen, setIsOpen] = useState(false);
    const [isExpanded, setIsExpanded] = useState(false);
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [input, setInput] = useState("");
    const [isLoading, setIsLoading] = useState(false);
    const [remaining, setRemaining] = useState<number | null>(null);
    const [rateLimited, setRateLimited] = useState(false);
    const [hasGreeted, setHasGreeted] = useState(false);
    const messagesEndRef = useRef<HTMLDivElement>(null);
    const inputRef = useRef<HTMLTextAreaElement>(null);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages]);

    useEffect(() => {
        if (isOpen && !hasGreeted) {
            setHasGreeted(true);
            setMessages([{
                id: "greeting",
                role: "assistant",
                content: "Hi! I'm **Jass**, your Wealthix AI financial assistant. I have access to your live account data and spending patterns. What would you like to know?",
                timestamp: new Date(),
            }]);
        }
        if (isOpen) {
            setTimeout(() => inputRef.current?.focus(), 150);
        }
    }, [isOpen, hasGreeted]);

    const sendMessage = useCallback(async (text: string) => {
        const trimmed = text.trim();
        if (!trimmed || isLoading) return;

        const userMsg: ChatMessage = {
            id: Date.now().toString(),
            role: "user",
            content: trimmed,
            timestamp: new Date(),
        };

        setMessages(prev => [...prev, userMsg]);
        setInput("");
        setIsLoading(true);

        try {
            const res = await chatService.sendMessage({
                sessionId: "floating-widget",
                message: trimmed
            });
            const data = res.data?.data || res.data;
            const reply = data.reply;
            const remainingMessages = data.remainingMessages;
            setRemaining(remainingMessages);
            setRateLimited(false);

            setMessages(prev => [...prev, {
                id: Date.now().toString() + "-ai",
                role: "assistant",
                content: reply || "Sorry, I couldn't generate a response.",
                timestamp: new Date(),
            }]);
        } catch (err: any) {
            const status = err?.response?.status;
            if (status === 429) {
                setRateLimited(true);
                setMessages(prev => [...prev, {
                    id: Date.now().toString() + "-err",
                    role: "assistant",
                    content: "You've reached the message limit (10/minute). Please wait a moment before sending more.",
                    timestamp: new Date(),
                    isError: true,
                }]);
            } else if (status === 401) {
                setMessages(prev => [...prev, {
                    id: Date.now().toString() + "-err",
                    role: "assistant",
                    content: "Session expired. Please refresh the page.",
                    timestamp: new Date(),
                    isError: true,
                }]);
            } else {
                setMessages(prev => [...prev, {
                    id: Date.now().toString() + "-err",
                    role: "assistant",
                    content: "Something went wrong. Please try again.",
                    timestamp: new Date(),
                    isError: true,
                }]);
            }
        } finally {
            setIsLoading(false);
            setTimeout(() => inputRef.current?.focus(), 100);
        }
    }, [isLoading]);

    const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            sendMessage(input);
        }
    };

    const widgetW = isExpanded ? "w-[520px]" : "w-[380px]";
    const widgetH = isExpanded ? "h-[680px]" : "h-[520px]";

    if (!isAuthenticated) return null;

    return (
        <>
            {/* ── Floating Bubble ──────────────────────────────────────────── */}
            {!isOpen && (
                <button
                    id="chat-widget-toggle"
                    onClick={() => setIsOpen(true)}
                    className="fixed bottom-6 right-6 z-50 group flex items-center gap-3 bg-gradient-to-br from-[#0D0B1E] to-[#1E1B4B] hover:from-[#1E1B4B] hover:to-[#2D2870] text-white px-5 py-3.5 rounded-2xl shadow-gold-lg border border-[#D4AF37]/30 transition-all duration-300 hover:scale-105 hover:shadow-gold"
                    aria-label="Open Jass AI Chat"
                >
                    <div className="relative">
                        <Sparkles className="w-5 h-5 text-[#D4AF37] group-hover:text-[#F5D67B] transition-colors" />
                        <span className="absolute -top-1 -right-1 w-2 h-2 bg-emerald-400 rounded-full animate-pulse" />
                    </div>
                    <span className="text-sm font-bold tracking-tight">Ask Jass</span>
                </button>
            )}

            {/* ── Chat Panel ───────────────────────────────────────────────── */}
            {isOpen && (
                <div
                    className={cn(
                        "fixed bottom-6 right-6 z-50 flex flex-col rounded-3xl shadow-gold-lg border border-[#D4AF37]/20 overflow-hidden transition-all duration-300",
                        widgetW, widgetH
                    )}
                    style={{ background: "#0D0B1E" }}
                    id="chat-panel"
                >
                    {/* ── Header */}
                    <div className="flex items-center justify-between px-5 py-4 bg-gradient-to-r from-[#0D0B1E] to-[#1E1B4B] border-b border-[#D4AF37]/20 flex-shrink-0">
                        <div className="flex items-center gap-3">
                            <div className="relative">
                                <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-[#D4AF37] to-[#B8962E] flex items-center justify-center shadow-gold">
                                    <Sparkles className="w-4 h-4 text-[#0D0B1E]" />
                                </div>
                                <span className="absolute -bottom-0.5 -right-0.5 w-3 h-3 bg-emerald-400 rounded-full border-2 border-[#0D0B1E]" />
                            </div>
                            <div>
                                <p className="text-sm font-extrabold text-white leading-tight gold-text">Jass</p>
                                <p className="text-[10px] text-[#D4AF37]/60 font-medium">
                                    {remaining !== null ? `${remaining} messages left` : "Wealthix AI · Financial Assistant"}
                                </p>
                            </div>
                        </div>
                        <div className="flex items-center gap-1.5">
                            <button
                                onClick={() => setIsExpanded(e => !e)}
                                className="p-1.5 rounded-lg text-slate-500 hover:text-white hover:bg-white/10 transition-all"
                                title={isExpanded ? "Minimize" : "Expand"}
                            >
                                {isExpanded ? <Minimize2 className="w-4 h-4" /> : <Maximize2 className="w-4 h-4" />}
                            </button>
                            <button
                                onClick={() => {
                                    setIsOpen(false);
                                    setIsExpanded(false);
                                }}
                                className="p-1.5 rounded-lg text-slate-500 hover:text-white hover:bg-white/10 transition-all"
                                title="Close"
                            >
                                <X className="w-4 h-4" />
                            </button>
                        </div>
                    </div>

                    {/* ── Messages */}
                    <div className="flex-1 overflow-y-auto p-4 space-y-4" id="chat-messages"
                        style={{ background: "rgba(13,11,30,0.8)" }}>
                        {messages.map((msg) => (
                            <ChatBubble key={msg.id} message={msg} />
                        ))}

                        {isLoading && (
                            <div className="flex items-end gap-2">
                                <div className="w-7 h-7 rounded-xl bg-gradient-to-br from-[#D4AF37] to-[#B8962E] flex items-center justify-center flex-shrink-0">
                                    <Bot className="w-3.5 h-3.5 text-[#0D0B1E]" />
                                </div>
                                <div className="bg-white/5 border border-white/10 rounded-2xl rounded-bl-sm px-4 py-3">
                                    <div className="flex items-center gap-1.5">
                                        <div className="w-1.5 h-1.5 bg-[#D4AF37] rounded-full animate-bounce" style={{ animationDelay: "0ms" }} />
                                        <div className="w-1.5 h-1.5 bg-[#D4AF37] rounded-full animate-bounce" style={{ animationDelay: "150ms" }} />
                                        <div className="w-1.5 h-1.5 bg-[#D4AF37] rounded-full animate-bounce" style={{ animationDelay: "300ms" }} />
                                    </div>
                                </div>
                            </div>
                        )}
                        <div ref={messagesEndRef} />
                    </div>

                    {/* ── Suggestions */}
                    {messages.length <= 1 && !isLoading && (
                        <div className="px-4 pb-3 flex flex-wrap gap-2 flex-shrink-0 border-t border-white/5 pt-3"
                            style={{ background: "rgba(13,11,30,0.8)" }}>
                            {SUGGESTIONS.map((s) => (
                                <button
                                    key={s}
                                    onClick={() => sendMessage(s)}
                                    className="text-xs px-3 py-1.5 bg-white/5 border border-white/10 rounded-full text-slate-400 hover:border-[#D4AF37]/50 hover:text-[#F5D67B] hover:bg-[#D4AF37]/5 transition-all font-semibold"
                                >
                                    {s}
                                </button>
                            ))}
                        </div>
                    )}

                    {/* ── Rate limit bar */}
                    {remaining !== null && (
                        <div className="px-4 pt-1 flex-shrink-0" style={{ background: "#0D0B1E" }}>
                            <div className="h-0.5 bg-white/10 rounded-full overflow-hidden">
                                <div
                                    className={cn(
                                        "h-full rounded-full transition-all duration-500",
                                        remaining > 5 ? "bg-[#D4AF37]" : remaining > 2 ? "bg-amber-400" : "bg-rose-400"
                                    )}
                                    style={{ width: `${(remaining / 10) * 100}%` }}
                                />
                            </div>
                        </div>
                    )}

                    {/* ── Input */}
                    <div className="flex items-end gap-3 px-4 py-3 border-t border-white/10 flex-shrink-0"
                        style={{ background: "#0D0B1E" }}>
                        <textarea
                            ref={inputRef}
                            id="chat-input"
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                            onKeyDown={handleKeyDown}
                            placeholder={rateLimited ? "Rate limit reached — wait a moment..." : "Ask Jass about your finances..."}
                            disabled={isLoading || rateLimited}
                            rows={1}
                            className={cn(
                                "flex-1 resize-none border rounded-xl px-4 py-3 text-sm font-medium focus:outline-none focus:ring-2 transition-all max-h-[100px] bg-white/5 text-slate-200 placeholder:text-slate-600",
                                rateLimited
                                    ? "border-rose-500/30 text-rose-400 cursor-not-allowed"
                                    : "border-white/10 focus:ring-[#D4AF37]/20 focus:border-[#D4AF37]/40",
                            )}
                            style={{ overflowY: "auto" }}
                        />
                        <button
                            id="chat-send-btn"
                            onClick={() => sendMessage(input)}
                            disabled={isLoading || !input.trim() || rateLimited}
                            className={cn(
                                "p-3 rounded-xl transition-all flex-shrink-0",
                                input.trim() && !isLoading && !rateLimited
                                    ? "bg-gradient-to-br from-[#D4AF37] to-[#B8962E] text-[#0D0B1E] hover:scale-105 shadow-gold"
                                    : "bg-white/5 text-slate-600 cursor-not-allowed"
                            )}
                        >
                            {isLoading
                                ? <Loader2 className="w-4 h-4 animate-spin" />
                                : <Send className="w-4 h-4" />}
                        </button>
                    </div>
                </div>
            )}
        </>
    );
}

// ─── Chat Bubble ──────────────────────────────────────────────────────────────
function ChatBubble({ message }: { message: ChatMessage }) {
    const isUser = message.role === "user";

    const formatContent = (text: string) => {
        const parts = text.split(/(\*\*.*?\*\*)/g);
        return parts.map((part, i) =>
            part.startsWith("**") && part.endsWith("**")
                ? <strong key={i} className="text-[#F5D67B]">{part.slice(2, -2)}</strong>
                : part
        );
    };

    return (
        <div className={cn("flex items-end gap-2", isUser ? "flex-row-reverse" : "flex-row")}>
            {!isUser && (
                <div className="w-7 h-7 rounded-xl bg-gradient-to-br from-[#D4AF37] to-[#B8962E] flex items-center justify-center flex-shrink-0">
                    <Bot className="w-3.5 h-3.5 text-[#0D0B1E]" />
                </div>
            )}
            {isUser && (
                <div className="w-7 h-7 rounded-xl bg-[#1E1B4B] border border-white/10 flex items-center justify-center flex-shrink-0">
                    <User className="w-3.5 h-3.5 text-slate-300" />
                </div>
            )}

            <div className={cn(
                "max-w-[80%] px-4 py-3 rounded-2xl text-sm leading-relaxed",
                isUser
                    ? "bg-[#1E1B4B] border border-[#D4AF37]/20 text-slate-200 rounded-br-sm"
                    : message.isError
                        ? "bg-rose-500/10 border border-rose-500/20 text-rose-400 rounded-bl-sm"
                        : "bg-white/5 border border-white/10 text-slate-300 rounded-bl-sm"
            )}>
                {message.isError && (
                    <div className="flex items-center gap-1.5 mb-1 text-rose-400 font-bold text-[10px] uppercase">
                        <AlertCircle className="w-3 h-3" /> Error
                    </div>
                )}
                <p className="whitespace-pre-line">{formatContent(message.content)}</p>
                <p className={cn(
                    "text-[9px] mt-1.5 font-medium text-slate-600",
                    isUser && "text-right"
                )}>
                    {message.timestamp.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                </p>
            </div>
        </div>
    );
}
