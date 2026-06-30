import { api } from './http';
import type { ApiResponse, LessonDetail, LessonSummary, ModuleSummary } from './types';

export function listModules(page = 0, size = 20): Promise<ApiResponse<ModuleSummary[]>> {
  return api.get<ApiResponse<ModuleSummary[]>>(`/api/v1/modules?page=${page}&size=${size}`);
}

export function listLessons(
  moduleId: string,
  page = 0,
  size = 50,
): Promise<ApiResponse<LessonSummary[]>> {
  return api.get<ApiResponse<LessonSummary[]>>(
    `/api/v1/modules/${moduleId}/lessons?page=${page}&size=${size}`,
  );
}

export function getLesson(lessonId: string): Promise<ApiResponse<LessonDetail>> {
  return api.get<ApiResponse<LessonDetail>>(`/api/v1/lessons/${lessonId}`);
}
