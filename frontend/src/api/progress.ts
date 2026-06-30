import { api } from './http';
import type {
  ApiResponse,
  ProgressDashboard,
  SubmitAttemptsRequest,
  SubmitAttemptsResponse,
} from './types';

export function submitAttempts(
  body: SubmitAttemptsRequest,
): Promise<ApiResponse<SubmitAttemptsResponse>> {
  return api.post<ApiResponse<SubmitAttemptsResponse>>('/api/v1/attempts', body);
}

export function myProgress(): Promise<ApiResponse<ProgressDashboard>> {
  return api.get<ApiResponse<ProgressDashboard>>('/api/v1/users/me/progress');
}
