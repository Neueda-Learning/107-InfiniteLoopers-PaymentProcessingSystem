const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api').replace(/\/$/, '');
export const DEMO_MODE = (import.meta.env.VITE_DEMO_MODE || 'false').toLowerCase() === 'true';

export class ApiError extends Error {
  constructor(message, status, details) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.details = details;
  }
}

export function isBackendUnavailable(error) {
  return error instanceof ApiError && error.status === 0;
}

async function parseResponse(response) {
  const contentType = response.headers.get('content-type') || '';

  if (contentType.includes('application/json')) {
    return response.json();
  }

  const text = await response.text();
  return text ? { message: text } : null;
}

export async function apiRequest(path, options = {}) {
  let response;

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      headers: {
        Accept: 'application/json',
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...options.headers,
      },
      ...options,
    });
  } catch (error) {
    throw new ApiError(
      'Backend is unreachable. Start the Spring Boot app on http://localhost:8080 or enable frontend demo mode.',
      0,
      { cause: error?.message || 'Network error' },
    );
  }

  const payload = await parseResponse(response);

  if (!response.ok) {
    const message = payload?.message || `Request failed with status ${response.status}`;
    throw new ApiError(message, response.status, payload);
  }

  return payload;
}


