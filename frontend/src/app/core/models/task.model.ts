export type Priority   = 'LOW' | 'MEDIUM' | 'HIGH';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

export interface Task {
  id: number;
  uuid: string | null;
  categoryId: number;
  title: string;
  description: string | null;
  dueDate: string | null;
  priority: Priority;
  status: TaskStatus;
  createdAt: string;
  updatedAt: string;
  tagName:  string | null;
  tagColor: string | null;
}

export const PRIORITY_LABEL: Record<Priority, string> = {
  HIGH:   'Alta',
  MEDIUM: 'Média',
  LOW:    'Baixa',
};

export const STATUS_LABEL: Record<TaskStatus, string> = {
  TODO:        'A Fazer',
  IN_PROGRESS: 'Em Progresso',
  DONE:        'Concluída',
};
