import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ErrorToastService } from '../services/error-toast.service';

export const httpErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ErrorToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const serverMessage: string | undefined = error.error?.message;

      switch (error.status) {
        case 0:
          toast.show('Sem conexão com o servidor. Verifique se o backend está rodando.');
          break;
        case 400:
          toast.show(serverMessage ?? 'Dados inválidos. Verifique os campos preenchidos.');
          break;
        case 404:
          toast.show(serverMessage ?? 'Recurso não encontrado.');
          break;
        case 422:
          toast.show(serverMessage ?? 'Operação não permitida.');
          break;
        case 500:
          toast.show('Erro interno do servidor. Tente novamente mais tarde.');
          break;
        default:
          if (error.status >= 500) {
            toast.show('Erro no servidor. Tente novamente mais tarde.');
          }
      }

      return throwError(() => error);
    })
  );
};
