import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import FloatingChatWidget from '@/components/shared/FloatingChatWidget';
import { chatService } from '@/services/api';
import { useAuth } from '@/services/authStore';

// Mock dependencies
jest.mock('@/services/api');
jest.mock('@/services/authStore', () => ({
  useAuth: jest.fn(() => ({
    isAuthenticated: true,
  })),
}));

describe('WealthixChat (FloatingChatWidget)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    window.HTMLElement.prototype.scrollIntoView = jest.fn();
  });

  it('shows disclaimer about financial advice in greeting', async () => {
    render(<FloatingChatWidget />);
    
    const toggle = screen.getByLabelText(/open jass ai chat/i);
    fireEvent.click(toggle);

    await waitFor(() => {
      expect(screen.getByText(/personalized financial advice/i)).toBeInTheDocument();
    });
  });

  it('sends message via chatService', async () => {
    (chatService.sendMessage as jest.Mock).mockResolvedValue({
      data: { reply: 'Here is some advice.' }
    });
    
    render(<FloatingChatWidget />);
    fireEvent.click(screen.getByLabelText(/open jass ai chat/i));

    const input = screen.getByPlaceholderText(/ask jass about your finances/i);
    fireEvent.change(input, { target: { value: 'How can I save money?' } });
    fireEvent.click(screen.getByLabelText(/send/i));

    await waitFor(() => {
      expect(chatService.sendMessage).toHaveBeenCalledWith(expect.objectContaining({
        message: 'How can I save money?'
      }));
      expect(screen.getByText('Here is some advice.')).toBeInTheDocument();
    });
  });

  it('clears messages on panel close', async () => {
    render(<FloatingChatWidget />);
    fireEvent.click(screen.getByLabelText(/open jass ai chat/i));
    
    await waitFor(() => expect(screen.getByText(/Hi! I'm/)).toBeInTheDocument());
    
    const closeBtn = screen.getByLabelText(/close/i);
    fireEvent.click(closeBtn);

    // Re-open
    fireEvent.click(screen.getByLabelText(/open jass ai chat/i));
    
    await waitFor(() => {
      const messages = screen.getAllByText(/Hi! I'm/);
      expect(messages).toHaveLength(1);
    });
  });
});
