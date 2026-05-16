export interface Subtask {
  id: number;
  uuid: string | null;
  taskId: number;
  text: string;
  done: boolean;
  createdAt: string;
}
