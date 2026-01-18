import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TodoService } from './service/todo.service';
import { Todo } from './model/todo.model';

@Component({
    selector: 'app-todos',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './todos.component.html',
    styleUrls: ['./todos.component.css']
})
export class TodosComponent implements OnInit {
    @ViewChild('signatureCanvas') signatureCanvas!: ElementRef<HTMLCanvasElement>;

    todos: Todo[] = [];
    selectedTodo: Todo | null = null;
    isEditing = false;
    showSignatureModal = false;
    todoToSign: Todo | null = null;

    newTodo: Todo = {
        username: '',
        title: '',
        completed: false
    };

    // Pagination state
    currentPage = 0;
    pageSize = 10;
    totalPages = 0;
    totalElements = 0;

    private ctx: CanvasRenderingContext2D | null = null;
    private isDrawing = false;

    constructor(private todoService: TodoService) { }

    ngOnInit(): void {
        this.loadTodos();
    }

    syncTodos(): void {
        this.todoService.syncTodos().subscribe({
            next: () => {
                alert('Synchronisation réussie!');
                this.loadTodos();
            },
            error: (err) => console.error('Erreur de synchronisation', err)
        });
    }

    loadTodos(): void {
        this.todoService.getAllTodos(this.currentPage, this.pageSize).subscribe({
            next: (data) => {
                this.todos = data.content;
                this.totalPages = data.totalPages;
                this.totalElements = data.totalElements;
            },
            error: (err) => console.error('Erreur de chargement', err)
        });
    }

    onPageChange(page: number): void {
        this.currentPage = page;
        this.loadTodos();
    }

    nextPage(): void {
        if (this.currentPage < this.totalPages - 1) {
            this.currentPage++;
            this.loadTodos();
        }
    }

    previousPage(): void {
        if (this.currentPage > 0) {
            this.currentPage--;
            this.loadTodos();
        }
    }

    getPages(): number[] {
        const pages = [];
        const maxPagesToShow = 5;
        let startPage = Math.max(0, this.currentPage - Math.floor(maxPagesToShow / 2));
        let endPage = Math.min(this.totalPages - 1, startPage + maxPagesToShow - 1);

        if (endPage - startPage + 1 < maxPagesToShow) {
            startPage = Math.max(0, endPage - maxPagesToShow + 1);
        }

        for (let i = startPage; i <= endPage; i++) {
            pages.push(i);
        }
        return pages;
    }

    createTodo(): void {
        if (this.newTodo.username && this.newTodo.title) {
            this.todoService.createTodo(this.newTodo).subscribe({
                next: () => {
                    this.loadTodos();
                    this.newTodo = { username: '', title: '', completed: false };
                },
                error: (err) => console.error('Erreur de création', err)
            });
        }
    }

    editTodo(todo: Todo): void {
        this.selectedTodo = { ...todo };
        this.isEditing = true;
    }

    updateTodo(): void {
        if (this.selectedTodo && this.selectedTodo.id) {
            this.todoService.updateTodo(this.selectedTodo.id, this.selectedTodo).subscribe({
                next: () => {
                    this.loadTodos();
                    this.cancelEdit();
                },
                error: (err) => console.error('Erreur de mise à jour', err)
            });
        }
    }

    deleteTodo(id: number | undefined): void {
        if (id && confirm('Voulez-vous vraiment supprimer cette tâche?')) {
            this.todoService.deleteTodo(id).subscribe({
                next: () => this.loadTodos(),
                error: (err) => console.error('Erreur de suppression', err)
            });
        }
    }

    cancelEdit(): void {
        this.selectedTodo = null;
        this.isEditing = false;
    }

    downloadUsersListPdf(): void {
        this.todoService.downloadUsersListPdf();
    }

    downloadTodoPdf(id: number | undefined): void {
        if (id) {
            this.todoService.downloadTodoPdf(id);
        }
    }

    openSignatureModal(todo: Todo): void {
        this.todoToSign = todo;
        this.showSignatureModal = true;
        setTimeout(() => this.initSignaturePad(), 100);
    }

    closeSignatureModal(): void {
        this.showSignatureModal = false;
        this.todoToSign = null;
    }

    initSignaturePad(): void {
        const canvas = this.signatureCanvas.nativeElement;
        this.ctx = canvas.getContext('2d');

        if (this.ctx) {
            this.ctx.strokeStyle = '#000';
            this.ctx.lineWidth = 2;
            this.ctx.lineCap = 'round';
        }
    }

    startDrawing(event: MouseEvent): void {
        this.isDrawing = true;
        if (this.ctx) {
            const rect = this.signatureCanvas.nativeElement.getBoundingClientRect();
            this.ctx.beginPath();
            this.ctx.moveTo(event.clientX - rect.left, event.clientY - rect.top);
        }
    }

    draw(event: MouseEvent): void {
        if (!this.isDrawing || !this.ctx) return;

        const rect = this.signatureCanvas.nativeElement.getBoundingClientRect();
        this.ctx.lineTo(event.clientX - rect.left, event.clientY - rect.top);
        this.ctx.stroke();
    }

    stopDrawing(): void {
        this.isDrawing = false;
    }

    clearSignature(): void {
        if (this.ctx) {
            const canvas = this.signatureCanvas.nativeElement;
            this.ctx.clearRect(0, 0, canvas.width, canvas.height);
        }
    }

    saveSignedPdf(): void {
        if (this.todoToSign && this.todoToSign.id) {
            const canvas = this.signatureCanvas.nativeElement;
            const signatureData = canvas.toDataURL('image/png');

            this.todoService.downloadTodoPdfWithSignature(this.todoToSign.id, signatureData);
            this.closeSignatureModal();
        }
    }
}
