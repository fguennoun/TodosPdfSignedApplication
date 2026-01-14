import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Todo, User } from '../model/todo.model';

@Injectable({
  providedIn: 'root'
})
export class TodoService {
  private apiUrl = 'http://localhost:8080/api/todos';

  constructor(private http: HttpClient) {}

  syncTodos(): Observable<string> {
    return this.http.post(`${this.apiUrl}/sync`, {}, { responseType: 'text' });
  }

  getAllTodos(): Observable<Todo[]> {
    return this.http.get<Todo[]>(this.apiUrl);
  }

  getTodoById(id: number): Observable<Todo> {
    return this.http.get<Todo>(`${this.apiUrl}/${id}`);
  }

  createTodo(todo: Todo): Observable<Todo> {
    return this.http.post<Todo>(this.apiUrl, todo);
  }

  updateTodo(id: number, todo: Todo): Observable<Todo> {
    return this.http.put<Todo>(`${this.apiUrl}/${id}`, todo);
  }

  deleteTodo(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getAllUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/users`);
  }

  downloadUsersListPdf(): void {
    this.http.get(`${this.apiUrl}/users/pdf`, {
      responseType: 'blob'
    }).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'liste-utilisateurs.pdf';
      link.click();
      window.URL.revokeObjectURL(url);
    });
  }

  downloadTodoPdf(id: number): void {
    this.http.get(`${this.apiUrl}/${id}/pdf`, {
      responseType: 'blob'
    }).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `tache-${id}.pdf`;
      link.click();
      window.URL.revokeObjectURL(url);
    });
  }

  downloadTodoPdfWithSignature(id: number, signature: string): void {
    this.http.post(`${this.apiUrl}/${id}/pdf/sign`,
      { signature },
      { responseType: 'blob' }
    ).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `tache-signee-${id}.pdf`;
      link.click();
      window.URL.revokeObjectURL(url);
    });
  }
}
