"use client";

import { useState, useEffect, useCallback, useRef } from "react";
import axios from "axios";
import { toast } from "sonner";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import { useAuth } from "@/services/authStore";

interface Notification {
  id: string;
  type: string;
  title: string;
  message: string;
  read: boolean;
  actionUrl?: string;
  createdAt: string;
}

const api = axios.create({
  baseURL: "/api",
  headers: { "Content-Type": "application/json" },
  withCredentials: true,
});

export default function NotificationBell() {
  const [open, setOpen] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const { isAuthenticated, user } = useAuth();

  const fetchUnreadCount = useCallback(async () => {
    if (!isAuthenticated) return;
    try {
      const res = await api.get("/v1/notifications/unread/count");
      const count = res.data.data?.count || res.data.count || 0;
      setUnreadCount(count);
    } catch {
      // silently ignore
    }
  }, [isAuthenticated]);

  useEffect(() => {
    fetchUnreadCount();
    const interval = setInterval(fetchUnreadCount, 60_000);
    return () => clearInterval(interval);
  }, [fetchUnreadCount]);

  // WebSocket for Real-time Notifications
  useEffect(() => {
    if (!isAuthenticated || !user?.email) return;

    const socketUrl = "http://localhost:8080/ws";
    const socket = new SockJS(socketUrl);
    
    const stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      debug: (str) => {
        // console.log("[STOMP]", str);
      },
      onConnect: () => {
        stompClient.subscribe(`/user/${user.email}/queue/notifications`, (message) => {
          const newNotif = JSON.parse(message.body);
          
          setUnreadCount(prev => prev + 1);
          setNotifications(prev => [newNotif, ...prev].slice(0, 50));
          
          toast.info(newNotif.message, {
            description: newNotif.type,
          });
        });
      },
    });

    stompClient.activate();

    return () => {
      stompClient.deactivate();
    };
  }, [isAuthenticated, user]);

  // Close on outside click
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const fetchNotifications = async () => {
    setLoading(true);
    try {
      const res = await api.get("/v1/notifications?size=10");
      const list = res.data.data?.content || res.data.content || [];
      setNotifications(list);
    } catch {
      // non-critical
    } finally {
      setLoading(false);
    }
  };

  const togglePanel = () => {
    if (!open) fetchNotifications();
    setOpen(!open);
  };

  const markRead = async (id: string) => {
    try {
      await api.patch(`/v1/notifications/${id}/read`);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, read: true } : n))
      );
      setUnreadCount((c) => Math.max(0, c - 1));
    } catch {
      // non-critical
    }
  };

  const markAllRead = async () => {
    try {
      await api.patch("/v1/notifications/read-all");
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch {
      // non-critical
    }
  };

  const typeIcon: Record<string, string> = {
    SUCCESS: "✅",
    ALERT: "⚠️",
    INFO: "ℹ️",
  };

  const timeAgo = (dateStr: string) => {
    if (!dateStr) return "just now";
    const diff = Date.now() - new Date(dateStr).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return "just now";
    if (mins < 60) return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h ago`;
    const days = Math.floor(hrs / 24);
    return `${days}d ago`;
  };

  return (
    <div ref={ref} className="relative">
      {/* Bell button */}
      <button
        id="notification-bell"
        onClick={togglePanel}
        className="p-2 text-slate-400 hover:text-primary relative hover:bg-slate-50 rounded-full transition-all"
        aria-label="Notifications"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-5 w-5"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
          />
        </svg>
        {unreadCount > 0 && (
          <span className="absolute -top-0.5 -right-0.5 bg-rose-500 text-white text-[10px]
                         font-bold rounded-full h-4 w-4 flex items-center justify-center
                         animate-pulse shadow-sm">
            {unreadCount > 9 ? "9+" : unreadCount}
          </span>
        )}
      </button>

      {/* Dropdown panel */}
      {open && (
        <div
          className="absolute right-0 mt-2 w-80 bg-white border border-slate-200
                     rounded-xl shadow-xl z-50 overflow-hidden
                     animate-in fade-in slide-in-from-top-2 duration-200"
        >
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 border-b border-slate-100 bg-slate-50/50">
            <h3 className="text-sm font-semibold text-primary">
              Notifications
            </h3>
            {unreadCount > 0 && (
              <button
                onClick={markAllRead}
                className="text-xs text-secondary hover:text-teal-700 font-medium transition-colors"
              >
                Mark all read
              </button>
            )}
          </div>

          {/* List */}
          <div className="max-h-[360px] overflow-y-auto">
            {loading ? (
              <div className="py-8 text-center text-slate-400 text-sm">
                Loading...
              </div>
            ) : notifications.length === 0 ? (
              <div className="py-8 text-center text-slate-400 text-sm">
                No notifications yet
              </div>
            ) : (
              notifications.map((n) => (
                <div
                  key={n.id}
                  onClick={() => !n.read && markRead(n.id)}
                  className={`px-4 py-3 border-b border-slate-50 cursor-pointer
                             hover:bg-slate-50 transition-colors
                             ${!n.read ? "bg-teal-50/30" : ""}`}
                >
                  <div className="flex gap-3">
                    <span className="text-lg mt-0.5">
                      {typeIcon[n.type] || "📌"}
                    </span>
                    <div className="flex-1 min-w-0">
                      <p
                        className={`text-sm leading-tight ${
                          !n.read
                            ? "text-primary font-semibold"
                            : "text-slate-600"
                        }`}
                      >
                        {n.message}
                      </p>
                      <p className="text-[10px] text-slate-400 mt-1 font-medium">
                        {timeAgo(n.createdAt)}
                      </p>
                    </div>
                    {!n.read && (
                      <span className="h-2 w-2 mt-1.5 rounded-full bg-secondary flex-shrink-0 animate-pulse" />
                    )}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
