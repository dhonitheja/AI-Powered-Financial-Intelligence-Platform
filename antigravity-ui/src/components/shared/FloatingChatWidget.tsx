"use client";

import React, { useState, useRef, useEffect, useCallback } from "react";
import { chatService } from "@/services/api";
import { useAuth } from "@/services/authStore";
import {
    MessageCircle, X, Send, Bot, User, Loader2,
    Minimize2, Maximize2, Sparkles, AlertCircle, RefreshCw
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

    // ── Auto-scroll to latest message ─────────────────────────────────────────
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages]);

    // ── Focus input when opening ──────────────────────────────────────────────
    useEffect(() => {
        if (isOpen && !hasGreeted) {
            setHasGreeted(true);
            setMessages([{
                id: "greeting",
                role: "assistant",
                content: "Hi! I'm **Antigravity**, your AI financial assistant. I have access to your live account data and spending patterns. What would you like to know?",
                timestamp: new Date(),
            }]);
        }
        if (isOpen) {
            setTimeout(() => inputRef.current?.focus(), 150);
        }
    }, [isOpen, hasGreeted]);

    // ── Send message ───────────────────────────────────────────────────────────
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

    // ─── Dimensions ────────────────────────────────────────────────────────────
    const widgetW = isExpanded ? "w-[520px]" : "w-[380px]";
    const widgetH = isExpanded ? "h-[680px]" : "h-[520px]";

    // Only render the widget HTML if user is authenticated
    if (!isAuthenticated) return null;

    return (
        <>
            {/* ── Floating Bubble ──────────────────────────────────────────── */}
            {!isOpen && (
                <button
                    id="chat-widget-toggle"
                    onClick={() => setIsOpen(true)}
                    className="fixed bottom-6 right-6 z-50 group flex items-center gap-3 bg-gradient-to-br from-slate-900 to-slate-800 hover:from-indigo-900 hover:to-purple-900 text-white px-5 py-3.5 rounded-2xl shadow-2xl shadow-slate-900/40 transition-all duration-300 hover:scale-105"
                    aria-label="Open AI Chat"
                >
                    <div className="relative">
                        <Sparkles className="w-5 h-5 text-indigo-400 group-hover:text-purple-300 transition-colors" />
                        <span className="absolute -top-1 -right-1 w-2 h-2 bg-emerald-400 rounded-full animate-pulse" />
                    </div>
                    <span className="text-sm font-bold tracking-tight">Ask Antigravity</span>
                </button>
            )}

            {/* ── Chat Panel ───────────────────────────────────────────────── */}
            {isOpen && (
                <div
                    className={cn(
                        "fixed bottom-6 right-6 z-50 flex flex-col rounded-3xl shadow-2xl shadow-slate-900/30 border border-white/10 overflow-hidden transition-all duration-300 bg-white",
                        widgetW, widgetH
                    )}
                    id="chat-panel"
                >
                    {/* ── Header */}
                    <div className="flex items-center justify-between px-5 py-4 bg-gradient-to-r from-slate-900 to-slate-800 flex-shrink-0">
                        <div className="flex items-center gap-3">
                            <div className="relative">
                                <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center shadow-lg">
                                    <Sparkles className="w-4 h-4 text-white" />
                                </div>
                                <span className="absolute -bottom-0.5 -right-0.5 w-3 h-3 bg-emerald-400 rounded-full border-2 border-slate-900" />
                            </div>
                            <div>
                                <p className="text-sm font-extrabold text-white leading-tight">Antigravity AI</p>
                                <p className="text-[10px] text-slate-400 font-medium">
                                    {remaining !== null ? `${remaining} messages left` : "Financial Assistant"}
                                </p>
                            </div>
                        </div>
                        <div className="flex items-center gap-1.5">
                            <button
                                onClick={() => setIsExpanded(e => !e)}
                                className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-white/10 transition-all"
                                title={isExpanded ? "Minimize" : "Expand"}
                            >
                                {isExpanded ? <Minimize2 className="w-4 h-4" /> : <Maximize2 className="w-4 h-4" />}
                            </button>
                            <button
                                onClick={() => {
                                    setIsOpen(false);
                                    setIsExpanded(false);
                                }}
                                className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-white/10 transition-all"
                                title="Close"
                            >
                                <X className="w-4 h-4" />
                            </button>
                        </div>
                    </div>

                    {/* ── Messages */}
                    <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-slate-50/50" id="chat-messages">
                        {messages.map((msg) => (
                            <ChatBubble key={msg.id} message={msg} />
                        ))}

                        {/* Loading indicator */}
                        {isLoading && (
                            <div className="flex items-end gap-2">
                                <div className="w-7 h-7 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center flex-shrink-0">
                                    <Bot className="w-3.5 h-3.5 text-white" />
                                </div>
                                <div className="bg-white border border-slate-100 rounded-2xl rounded-bl-sm px-4 py-3 shadow-sm">
                                    <div className="flex items-center gap-1.5">
                                        <div className="w-1.5 h-1.5 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: "0ms" }} />
                                        <div className="w-1.5 h-1.5 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: "150ms" }} />
                                        <div className="w-1.5 h-1.5 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: "300ms" }} />
                                    </div>
                                </div>
                            </div>
                        )}
                        <div ref={messagesEndRef} />
                    </div>

                    {/* ── Suggestions (shown when no user messages yet) */}
                    {messages.length <= 1 && !isLoading && (
                        <div className="px-4 pb-3 bg-slate-50/50 flex flex-wrap gap-2 flex-shrink-0">
                            {SUGGESTIONS.map((s) => (
                                <button
                                    key={s}
                                    onClick={() => sendMessage(s)}
                                    className="text-xs px-3 py-1.5 bg-white border border-slate-200 rounded-full text-slate-600 hover:border-indigo-300 hover:text-indigo-700 hover:bg-indigo-50 transition-all font-semibold"
                                >
                                    {s}
                                </button>
                            ))}
                        </div>
                    )}

                    {/* ── Rate limit bar */}
                    {remaining !== null && (
                        <div className="px-4 pt-1 bg-white flex-shrink-0">
                            <div className="h-0.5 bg-slate-100 rounded-full overflow-hidden">
                                <div
                                    className={cn(
                                        "h-full rounded-full transition-all duration-500",
                                        remaining > 5 ? "bg-emerald-400" : remaining > 2 ? "bg-amber-400" : "bg-rose-400"
                                    )}
                                    style={{ width: `${(remaining / 10) * 100}%` }}
                                />
                            </div>
                        </div>
                    )}

                    {/* ── Input */}
                    <div className="flex items-end gap-3 px-4 py-3 bg-white border-t border-slate-100 flex-shrink-0">
                        <textarea
                            ref={inputRef}
                            id="chat-input"
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                            onKeyDown={handleKeyDown}
                            placeholder={rateLimited ? "Rate limit reached — wait a moment..." : "Ask about your finances..."}
                            disabled={isLoading || rateLimited}
                            rows={1}
                            className={cn(
                                "flex-1 resize-none bg-slate-50 border rounded-xl px-4 py-3 text-sm font-medium focus:outline-none focus:ring-2 transition-all max-h-[100px]",
                                rateLimited
                                    ? "border-rose-200 text-rose-400 cursor-not-allowed"
                                    : "border-slate-200 text-primary focus:ring-indigo-300/30 focus:border-indigo-300",
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
                                    ? "bg-gradient-to-br from-indigo-600 to-purple-600 text-white hover:scale-105 shadow-md shadow-indigo-500/25"
                                    : "bg-slate-100 text-slate-300 cursor-not-allowed"
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

    // Parse **bold** markdown
    const formatContent = (text: string) => {
        const parts = text.split(/(\*\*.*?\*\*)/g);
        return parts.map((part, i) =>
            part.startsWith("**") && part.endsWith("**")
                ? <strong key={i}>{part.slice(2, -2)}</strong>
                : part
        );
    };

    return (
        <div className={cn("flex items-end gap-2", isUser ? "flex-row-reverse" : "flex-row")}>
            {/* Avatar */}
            {!isUser && (
                <div className="w-7 h-7 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center flex-shrink-0">
                    <Bot className="w-3.5 h-3.5 text-white" />
                </div>
            )}
            {isUser && (
                <div className="w-7 h-7 rounded-xl bg-slate-800 flex items-center justify-center flex-shrink-0">
                    <User className="w-3.5 h-3.5 text-white" />
                </div>
            )}

            {/* Bubble */}
            <div className={cn(
                "max-w-[80%] px-4 py-3 rounded-2xl text-sm leading-relaxed",
                isUser
                    ? "bg-slate-900 text-white rounded-br-sm"
                    : message.isError
                        ? "bg-rose-50 border border-rose-200 text-rose-700 rounded-bl-sm"
                        : "bg-white border border-slate-100 text-slate-800 shadow-sm rounded-bl-sm"
            )}>
                {message.isError && (
                    <div className="flex items-center gap-1.5 mb-1 text-rose-600 font-bold text-[10px] uppercase">
                        <AlertCircle className="w-3 h-3" /> Error
                    </div>
                )}
                <p className="whitespace-pre-line">{formatContent(message.content)}</p>
                <p className={cn(
                    "text-[9px] mt-1.5 font-medium",
                    isUser ? "text-slate-400 text-right" : "text-slate-400"
                )}>
                    {message.timestamp.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                </p>
            </div>
        </div>
    );
}
