import React, { useState } from 'react';
import { usePhoneOtpAuth } from '../../hooks/usePhoneOtpAuth';
import { colors } from '../../styles/theme';
import { AppShell } from '../../components/layout/AppShell';
import { PrimaryButton } from '../../components/ui/PrimaryButton';
import { FormField } from '../../components/ui/FormField';
import { PrimaryCard } from '../../components/ui/PrimaryCard';

const COUNTRIES = [
  { code: '+1', label: 'US/CA' },
  { code: '+44', label: 'UK' },
  { code: '+91', label: 'IN' },
  { code: '+61', label: 'AU' },
  { code: '+81', label: 'JP' },
  { code: '+49', label: 'DE' },
  { code: '+33', label: 'FR' },
  { code: '+86', label: 'CN' }
];

export const PhoneAuthScreen: React.FC<{ onNavigate: (route: string) => void }> = ({ onNavigate }) => {
  const {
    state,
    setPhone,
    setOtp,
    requestOtp,
    verifyOtp,
    backToPhone,
    setCaptchaCompleted,
    cooldownSeconds,
    isBusy,
    canSubmit,
    transition
  } = usePhoneOtpAuth({
    onAuthenticated: () => {
      onNavigate('home');
    }
  });

  const [countryCode, setCountryCode] = useState('+1');
  const [localPhone, setLocalPhone] = useState('');

  const handlePhoneSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const fullPhone = countryCode + localPhone;
    setPhone(fullPhone);
    requestOtp();
  };

  const handleOtpSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    verifyOtp();
  };

  return (
    <AppShell hideBottomNav>
      <div className={`flex-grow flex flex-col justify-center py-6 px-2 min-h-[85vh] ${transition.className}`}>
        <div className="flex flex-col gap-6 w-full max-w-sm mx-auto">
          
          <div className="text-center flex flex-col gap-1.5 mb-2">
            <span className="material-symbols-outlined text-[44px]" style={{ color: colors.primary }}>
              phone_iphone
            </span>
            <h2 className="text-2xl font-extrabold" style={{ color: colors.primary, fontFamily: 'Plus Jakarta Sans' }}>
              {state.phoneStage === 'enter_otp' ? 'Verify your number' : 'Continue with Phone'}
            </h2>
            <p className="text-xs" style={{ color: colors.onSurfaceVariant }}>
              {state.phoneStage === 'enter_otp' 
                ? `Code sent to ${state.maskedPhone || 'your phone'}`
                : 'Your private health details remain secure on your hardware 🔐'}
            </p>
          </div>

          {state.error && (
            <div className="p-3 rounded bg-red-50 text-red-700 text-sm font-bold border border-red-100">
              ⚠️ {state.error}
            </div>
          )}

          {cooldownSeconds > 0 && (
            <div className="p-3 rounded bg-orange-50 text-orange-700 text-sm font-bold border border-orange-100 flex justify-between">
              <span>Rate limit active.</span>
              <span>Wait {cooldownSeconds}s</span>
            </div>
          )}

          {state.captchaRequired && (
            <PrimaryCard className="p-4 flex flex-col gap-3">
              <p className="text-sm font-bold text-center">Security Verification Required</p>
              <PrimaryButton 
                label="Complete Captcha" 
                onClick={() => setCaptchaCompleted()}
              />
            </PrimaryCard>
          )}

          {state.phoneStage === 'enter_phone' || state.phoneStage === 'requesting_otp' ? (
            <form onSubmit={handlePhoneSubmit} className="flex flex-col gap-4">
              <div className="flex gap-2">
                <select 
                  value={countryCode} 
                  onChange={e => setCountryCode(e.target.value)}
                  className="p-3 rounded border border-gray-300 bg-white"
                  style={{ color: colors.onSurface }}
                  disabled={!canSubmit}
                >
                  {COUNTRIES.map(c => (
                    <option key={c.code} value={c.code}>{c.code} {c.label}</option>
                  ))}
                </select>
                <div className="flex-grow">
                  <FormField
                    label="Phone Number"
                    type="tel"
                    placeholder="555 123 4567"
                    value={localPhone}
                    onChange={(e) => setLocalPhone(e.target.value)}
                    icon="phone"
                    required
                  />
                </div>
              </div>
              <PrimaryButton 
                label={isBusy ? "Sending..." : "Send Code"} 
                type="submit"
                fullWidth
                disabled={!canSubmit}
                style={{ marginTop: '8px' }}
              />
            </form>
          ) : state.phoneStage === 'enter_otp' || state.phoneStage === 'verifying_otp' ? (
            <form onSubmit={handleOtpSubmit} className="flex flex-col gap-4">
              <FormField
                label="6-Digit Code"
                type="text"
                placeholder="123456"
                value={state.otp}
                onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').substring(0, 6))}
                icon="password"
                required
              />
              <PrimaryButton 
                label={isBusy ? "Verifying..." : "Verify and Continue"} 
                type="submit"
                fullWidth
                disabled={!canSubmit || state.otp.length < 6}
                style={{ marginTop: '8px' }}
              />
              
              <div className="flex justify-between mt-2">
                <button 
                  type="button"
                  onClick={requestOtp}
                  disabled={!canSubmit}
                  className="text-xs font-bold hover:underline" 
                  style={{ color: colors.primary, opacity: canSubmit ? 1 : 0.5 }}
                >
                  Resend Code
                </button>
                <button 
                  type="button"
                  onClick={backToPhone}
                  className="text-xs font-bold hover:underline" 
                  style={{ color: colors.secondary }}
                >
                  Change Number
                </button>
              </div>
            </form>
          ) : (
            <div className="text-center p-6 font-bold text-green-600">
              Authenticated successfully!
            </div>
          )}

          <div className="flex flex-col gap-3 text-center mt-6">
            <button 
              onClick={() => onNavigate('login')}
              className="text-xs font-bold"
              style={{ color: colors.onSurfaceVariant }}
            >
              Back to Login Options
            </button>
          </div>
          
        </div>
      </div>
    </AppShell>
  );
};
export default PhoneAuthScreen;
