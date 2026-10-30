export interface FieldErrors {
  cardNumber?: string;
  cvv?: string;
  phoneNumber?: string;
  password?: string;
}

export function validatePhone(phone: string): string | undefined {
  return /^9\d{8}$/.test(phone) ? undefined : 'Ingresa un celular peruano válido de 9 dígitos que empiece con 9.';
}

export function validatePassword(password: string): string | undefined {
  return password.length >= 8 && password.length <= 128
    ? undefined
    : 'La contraseña debe tener entre 8 y 128 caracteres.';
}

export function validateCvv(cvv: string): string | undefined {
  return /^\d{3}$/.test(cvv) ? undefined : 'Ingresa un CVV de 3 dígitos.';
}

export function validateCard(cardNumber: string): string | undefined {
  return /^\d{1,16}$/.test(cardNumber) ? undefined : 'Ingresa el número de tarjeta: solo números, hasta 16 dígitos.';
}

export function validateRegistration(cardNumber: string, cvv: string, phone: string, password: string): FieldErrors {
  return {
    cardNumber: validateCard(cardNumber),
    cvv: validateCvv(cvv),
    phoneNumber: validatePhone(phone),
    password: validatePassword(password)
  };
}

export function hasErrors(errors: FieldErrors): boolean {
  return Object.values(errors).some(Boolean);
}

export function formatCardNumber(value: string): string {
  return value.replace(/\D/g, '').slice(0, 16).replace(/(\d{4})(?=\d)/g, '$1 ');
}

export function digitsOnly(value: string, maximumLength: number): string {
  return value.replace(/\D/g, '').slice(0, maximumLength);
}
