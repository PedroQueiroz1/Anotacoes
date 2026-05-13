import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-category',
  standalone: true,
  template: `<h2>Categoria {{ id }}</h2><p>Implementação na Etapa 4.</p>`,
})
export class CategoryComponent {
  id = '';

  constructor(private route: ActivatedRoute) {
    this.id = this.route.snapshot.paramMap.get('id') ?? '';
  }
}
