import { useState, useEffect, useCallback } from 'react';
import { requestPhoneOtp, verifyPhoneOtp, maskPhone } from '../lib/auth/phoneAuth';

export type AuthProvider = "google" | "facebook" | "tiktok" | "email" | "phone";

export type PhoneOtpStage = "enter_phone" | "requesting_otp" | "enter_otp" | "verifying_otp" | "authenticated";

export interface AuthUiState {
  mode: "login" | "signup";
  selectedMethod: AuthProvider | null;
  activeProvider: AuthProvider | null;
  error: string | null;
  cooldownUntil: number | null;
  captchaRequired: boolean;
  phoneStage: PhoneOtpStage;
  phone: string;
  maskedPhone: string | null;
  otp: string;
}

export interface UsePhoneOtpAuthOptions {
  initialMode?: "login" | "signup";
  onAuthenticated?: () => void;
}

export function usePhoneOtpAuth(options?: UsePhoneOtpAuthOptions) {
  const [state, setState] = useState<AuthUiState>({
    mode: options?.initialMode || "login",
    selectedMethod: "phone",
    activeProvider: "phone",
    error: null,
    cooldownUntil: null,
    captchaRequired: false,
    phoneStage: "enter_phone",
    phone: "",
    maskedPhone: null,
    otp: ""
  });

  const [cooldownSeconds, setCooldownSeconds] = useState(0);
  const [prefersReducedMotion, setPrefersReducedMotion] = useState(false);

  useEffect(() => {
    const mediaQuery = window.matchMedia("(prefers-reduced-motion: reduce)");
    setPrefersReducedMotion(mediaQuery.matches);
    const handler = (e: MediaQueryListEvent) => setPrefersReducedMotion(e.matches);
    mediaQuery.addEventListener("change", handler);
    return () => mediaQuery.removeEventListener("change", handler);
  }, []);

  useEffect(() => {
    let interval: ReturnType<typeof setInterval>;
    if (state.cooldownUntil) {
      interval = setInterval(() => {
        const remaining = Math.ceil((state.cooldownUntil! - Date.now()) / 1000);
        if (remaining <= 0) {
          setState(prev => ({ ...prev, cooldownUntil: null }));
          setCooldownSeconds(0);
        } else {
          setCooldownSeconds(remaining);
        }
      }, 1000);
    } else {
      setCooldownSeconds(0);
    }
    return () => clearInterval(interval);
  }, [state.cooldownUntil]);

  const updateState = (updates: Partial<AuthUiState>) => {
    setState(prev => ({ ...prev, ...updates }));
  };

  const setPhone = useCallback((phone: string) => {
    updateState({ phone, error: null });
  }, []);

  const setOtp = useCallback((otp: string) => {
    updateState({ otp, error: null });
  }, []);

  const clearError = useCallback(() => {
    updateState({ error: null });
  }, []);

  const setCaptchaCompleted = useCallback(() => {
    updateState({ captchaRequired: false });
  }, []);

  const backToPhone = useCallback(() => {
    updateState({ phoneStage: "enter_phone", error: null, otp: "" });
  }, []);

  const reset = useCallback(() => {
    updateState({
      error: null,
      cooldownUntil: null,
      captchaRequired: false,
      phoneStage: "enter_phone",
      phone: "",
      maskedPhone: null,
      otp: ""
    });
  }, []);

  const isBusy = state.phoneStage === "requesting_otp" || state.phoneStage === "verifying_otp";
  const canSubmit = !isBusy && cooldownSeconds === 0 && !state.captchaRequired;

  const requestOtp = useCallback(async () => {
    if (!canSubmit) return;
    
    updateState({ phoneStage: "requesting_otp", error: null });
    const result = await requestPhoneOtp(state.phone);
    
    if (result.ok) {
      updateState({ 
        phoneStage: "enter_otp", 
        maskedPhone: result.maskedPhone,
        captchaRequired: result.captchaRequired,
        otp: ""
      });
    } else {
      updateState({
        phoneStage: "enter_phone",
        error: result.message,
        cooldownUntil: result.retryAfterSeconds ? Date.now() + result.retryAfterSeconds * 1000 : state.cooldownUntil,
        captchaRequired: result.captchaRequired || state.captchaRequired
      });
    }
  }, [state.phone, state.cooldownUntil, state.captchaRequired, canSubmit]);

  const verifyOtp = useCallback(async () => {
    if (!canSubmit) return;
    
    updateState({ phoneStage: "verifying_otp", error: null });
    const result = await verifyPhoneOtp(state.phone, state.otp);
    
    if (result.ok) {
      updateState({ phoneStage: "authenticated", error: null });
      if (options?.onAuthenticated) {
        options.onAuthenticated();
      }
    } else {
      updateState({
        phoneStage: "enter_otp",
        error: result.message,
        cooldownUntil: result.retryAfterSeconds ? Date.now() + result.retryAfterSeconds * 1000 : state.cooldownUntil,
        captchaRequired: result.captchaRequired || state.captchaRequired
      });
    }
  }, [state.phone, state.otp, state.cooldownUntil, state.captchaRequired, canSubmit, options]);

  const transition = {
    shouldAnimate: !prefersReducedMotion,
    durationMs: prefersReducedMotion ? 0 : 200,
    className: prefersReducedMotion ? "" : "transition-all duration-200"
  };

  return {
    state,
    setPhone,
    setOtp,
    requestOtp,
    verifyOtp,
    backToPhone,
    clearError,
    setCaptchaCompleted,
    reset,
    cooldownSeconds,
    isBusy,
    canSubmit,
    prefersReducedMotion,
    transition
  };
}
