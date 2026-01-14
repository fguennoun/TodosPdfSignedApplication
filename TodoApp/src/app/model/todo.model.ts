
export interface Todo {
  id?: number;
  username: string;
  title: string;
  completed: boolean;
  userId?: number;
}

export interface User {
  id: number;
  name: string;
  username: string;
  email: string;
  phone: string;
  website?: string;
}
