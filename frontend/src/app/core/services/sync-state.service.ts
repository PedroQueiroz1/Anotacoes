import { Injectable } from '@angular/core';

interface SyncEntry {
  loadedUpdatedAt: string;
  isDirty: boolean;
}

@Injectable({ providedIn: 'root' })
export class SyncStateService {
  private readonly state = new Map<string, SyncEntry>();

  private key(type: 'note' | 'task', id: number): string {
    return `${type}:${id}`;
  }

  init(type: 'note' | 'task', id: number, updatedAt: string): void {
    this.state.set(this.key(type, id), { loadedUpdatedAt: updatedAt, isDirty: false });
  }

  markDirty(type: 'note' | 'task', id: number): void {
    const k = this.key(type, id);
    const s = this.state.get(k);
    if (s) this.state.set(k, { ...s, isDirty: true });
  }

  isDirty(type: 'note' | 'task', id: number): boolean {
    return this.state.get(this.key(type, id))?.isDirty ?? false;
  }

  getLoadedUpdatedAt(type: 'note' | 'task', id: number): string | undefined {
    return this.state.get(this.key(type, id))?.loadedUpdatedAt;
  }

  clear(type: 'note' | 'task', id: number): void {
    this.state.delete(this.key(type, id));
  }
}
