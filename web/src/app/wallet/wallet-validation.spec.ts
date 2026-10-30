import { describe, expect, it } from 'vitest';
import {
  digitsOnly,
  formatCardNumber,
  hasErrors,
  validateCard,
  validateCvv,
  validatePhone,
  validatePassword,
  validateRegistration
} from './wallet-validation';

describe('validatePhone', () => {
  it('accepts a Peruvian mobile number', () => {
    expect(validatePhone('987654321')).toBeUndefined();
  });

  it.each(['98765432', '9876543210', '887654321', '987 654 321', '+51987654321'])('rejects %s', (phone) => {
    expect(validatePhone(phone)).toContain('9 dígitos');
  });
});

describe('validatePassword', () => {
  it('accepts 8 and 128 characters with any composition', () => {
    expect(validatePassword('abcdefgh')).toBeUndefined();
    expect(validatePassword('a'.repeat(128))).toBeUndefined();
  });

  it('rejects passwords outside the length range', () => {
    expect(validatePassword('abc1234')).toContain('8 y 128');
    expect(validatePassword('a'.repeat(129))).toContain('8 y 128');
  });
});

describe('validateCvv', () => {
  it('accepts exactly three digits', () => {
    const cvv = '123';
    expect(validateCvv(cvv)).toBeUndefined();
  });

  it.each(['12', '1234', '12a'])('rejects %s', (cvv) => {
    expect(validateCvv(cvv)).toContain('3 dígitos');
  });
});

describe('validateCard', () => {
  it.each(['1', '1234567890123456', '4111111111111112'])('accepts digits only up to 16: %s', (card) => {
    expect(validateCard(card)).toBeUndefined();
  });

  it.each(['', '4111 1111 1111 1111', '41111111111111111', '4111-1111', 'abcd'])('rejects "%s"', (card) => {
    expect(validateCard(card)).toContain('hasta 16 dígitos');
  });
});

describe('formatters and form errors', () => {
  it('formats a card number in groups of four and retains only digits', () => {
    expect(formatCardNumber('4111-1111 1111 1111 1234')).toBe('4111 1111 1111 1111');
  });

  it('limits numeric fields to their maximum length', () => {
    expect(digitsOnly('9a876543210', 9)).toBe('987654321');
  });

  it('detects invalid registration fields', () => {
    const errors = validateRegistration('41111111111111112', '12', '123', 'short');
    expect(errors).toMatchObject({ cardNumber: expect.any(String), cvv: expect.any(String), phoneNumber: expect.any(String), password: expect.any(String) });
    expect(hasErrors(errors)).toBe(true);
    expect(hasErrors(validateRegistration('4111111111111111', '123', '987654321', 'abcdefgh'))).toBe(false);
  });
});
