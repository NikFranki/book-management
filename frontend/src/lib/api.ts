export type Session = {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  username: string;
  roles: string[];
};

export type Book = {
  id: number;
  title: string;
  author: string;
  isbn: string;
  price: number;
  availableCopies: number;
};

export type BookPage = {
  content: Book[];
  number: number;
  size: number;
  totalPages: number;
  totalElements: number;
};

export type BorrowRecord = {
  id: number;
  bookId: number;
  bookTitle: string;
  borrowerName: string;
  borrowedAt: string;
  dueAt: string;
  returnedAt: string | null;
  status: "BORROWING" | "OVERDUE" | "RETURNED";
};

type ApiError = { message?: string; detail?: string; error?: string };

export async function api<T>(path: string, token: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Authorization", `Bearer ${token}`);
  if (init.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");

  const response = await fetch(path, { ...init, headers, cache: "no-store" });
  if (response.status === 204) return undefined as T;

  const body = (await response.json().catch(() => ({}))) as ApiError;
  if (!response.ok) {
    if (response.status === 401) throw new Error("登录已过期，请重新登录");
    throw new Error(body.message || body.detail || body.error || `请求失败（${response.status}）`);
  }
  return body as T;
}
