import { Component, inject } from '@angular/core';
import { ErrorToastService } from '../../../core/services/error-toast.service';

@Component({
  selector: 'app-error-toast',
  standalone: true,
  imports: [],
  templateUrl: './error-toast.component.html',
  styleUrl: './error-toast.component.scss',
})
export class ErrorToastComponent {
  readonly toastService = inject(ErrorToastService);
}
