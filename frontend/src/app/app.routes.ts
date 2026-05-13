import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/home/home.component').then(m => m.HomeComponent),
  },
  {
    path: 'categories/:id',
    loadComponent: () =>
      import('./features/category/category.component').then(m => m.CategoryComponent),
  },
  { path: '**', redirectTo: '' },
];
