"use client";

import { FormEvent, ReactNode, useCallback, useEffect, useMemo, useState } from "react";
import { api, Book, BookPage, BorrowRecord, Session } from "@/lib/api";

type View = "dashboard" | "books" | "loans";
type LoanFilter = "ALL" | BorrowRecord["status"];
type BookDraft = Omit<Book, "id">;
const emptyDraft: BookDraft = { title: "", author: "", isbn: "", price: 0.01, availableCopies: 1 };

function localDateInput(instant: string) {
  const date = new Date(instant);
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return localDate.toISOString().slice(0, 10);
}

function endOfLocalDay(dateValue: string) {
  const [year, month, day] = dateValue.split("-").map(Number);
  return new Date(year, month - 1, day, 23, 59, 59, 999).toISOString();
}

function Icon({ name, size = 18 }: { name: "dashboard" | "book" | "loan" | "search" | "plus" | "close" | "logout" | "back"; size?: number }) {
  const paths: Record<typeof name, ReactNode> = {
    dashboard: <><rect x="3" y="3" width="8" height="8" rx="1"/><rect x="13" y="3" width="8" height="5" rx="1"/><rect x="13" y="10" width="8" height="11" rx="1"/><rect x="3" y="13" width="8" height="8" rx="1"/></>,
    book: <><path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v17H6.5A2.5 2.5 0 0 1 4 17.5z"/><path d="M4 16.5A2.5 2.5 0 0 1 6.5 14H20M8 7h8M8 10h7"/></>,
    loan: <><path d="M4 5h16M4 10h16M4 15h10"/><path d="m16 16 2 2 4-5"/></>,
    search: <><circle cx="10.8" cy="10.8" r="6.5"/><path d="m16 16 4.5 4.5"/></>,
    plus: <><path d="M12 5v14M5 12h14"/></>,
    close: <><path d="m6 6 12 12M18 6 6 18"/></>,
    logout: <><path d="M10 17l5-5-5-5M15 12H3"/><path d="M12 3h7a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-7"/></>,
    back: <><path d="m15 18-6-6 6-6M9 12h12"/></>,
  };
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">{paths[name]}</svg>;
}

function ErrorText({ children }: { children: ReactNode }) {
  return <p className="notice error" role="alert">{children}</p>;
}

export default function Home() {
  const [session, setSession] = useState<Session | null>(null);
  const [ready, setReady] = useState(false);
  const [view, setView] = useState<View>("books");
  const [books, setBooks] = useState<BookPage | null>(null);
  const [totalBooks, setTotalBooks] = useState<number | null>(null);
  const [loans, setLoans] = useState<BorrowRecord[]>([]);
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(0);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [loginName, setLoginName] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [registering, setRegistering] = useState(false);
  const [loginMessage, setLoginMessage] = useState("");
  const [editing, setEditing] = useState<Book | "new" | null>(null);
  const [draft, setDraft] = useState<BookDraft>(emptyDraft);
  const [dueDateEditing, setDueDateEditing] = useState<BorrowRecord | null>(null);
  const [dueDateDraft, setDueDateDraft] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const [loanSearch, setLoanSearch] = useState("");
  const [loanFilter, setLoanFilter] = useState<LoanFilter>("ALL");

  const isAdmin = session?.roles.includes("ROLE_ADMIN") ?? false;
  const activeLoans = useMemo(() => loans.filter((loan) => loan.status !== "RETURNED").length, [loans]);
  const overdueLoans = useMemo(() => loans.filter((loan) => loan.status === "OVERDUE").length, [loans]);
  const visibleLoans = useMemo(() => {
    const query = loanSearch.trim().toLocaleLowerCase();
    return loans.filter((loan) => {
      const matchesStatus = loanFilter === "ALL" || loan.status === loanFilter;
      const searchableText = [loan.bookTitle, loan.id, ...(isAdmin ? [loan.borrowerName] : [])].join(" ").toLocaleLowerCase();
      return matchesStatus && (!query || searchableText.includes(query));
    });
  }, [isAdmin, loanFilter, loanSearch, loans]);

  useEffect(() => {
    const stored = window.localStorage.getItem("bookroom-session");
    if (stored) {
      try {
        const savedSession = JSON.parse(stored) as Session;
        setSession(savedSession);
        if (savedSession.roles.includes("ROLE_ADMIN")) setView("dashboard");
      } catch { window.localStorage.removeItem("bookroom-session"); }
    }
    setReady(true);
  }, []);

  const loadBooks = useCallback(async (token: string, currentPage = page, term = keyword) => {
    setBusy(true); setError("");
    try {
      const result = await api<BookPage>(`/api/books?keyword=${encodeURIComponent(term)}&page=${currentPage}&size=8`, token);
      setBooks(result);
    } catch (cause) {
      const reason = cause instanceof Error ? cause.message : "暂时无法读取书目";
      setError(reason);
      if (reason.includes("重新登录")) { window.localStorage.removeItem("bookroom-session"); setSession(null); }
    } finally { setBusy(false); }
  }, [keyword, page]);

  const loadLoans = useCallback(async (token: string) => {
    setBusy(true); setError("");
    try { setLoans(await api<BorrowRecord[]>("/api/borrow-records", token)); }
    catch (cause) {
      const reason = cause instanceof Error ? cause.message : "暂时无法读取借阅记录";
      setError(reason);
      if (reason.includes("重新登录")) { window.localStorage.removeItem("bookroom-session"); setSession(null); }
    } finally { setBusy(false); }
  }, []);

  const loadDashboard = useCallback(async (token: string) => {
    setBusy(true); setError("");
    try {
      const [bookSummary, records] = await Promise.all([
        api<BookPage>("/api/books?page=0&size=1", token),
        api<BorrowRecord[]>("/api/borrow-records", token),
      ]);
      setTotalBooks(bookSummary.totalElements);
      setLoans(records);
    } catch (cause) {
      const reason = cause instanceof Error ? cause.message : "暂时无法读取工作台数据";
      setError(reason);
      if (reason.includes("重新登录")) { window.localStorage.removeItem("bookroom-session"); setSession(null); }
    } finally { setBusy(false); }
  }, []);

  useEffect(() => {
    if (!session) return;
    if (view === "dashboard" && isAdmin) void loadDashboard(session.accessToken);
    else if (view === "books") void loadBooks(session.accessToken, page, keyword);
    else if (view === "loans") void loadLoans(session.accessToken);
  }, [session, view, page, keyword, isAdmin, loadBooks, loadLoans, loadDashboard]);

  async function login(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError(""); setLoginMessage("");
    try {
      if (registering) {
        if (password !== confirmPassword) throw new Error("两次输入的密码不一致");
        const registration = await fetch("/api/auth/register", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ username: loginName.trim(), password }) });
        const registrationData = await registration.json();
        if (!registration.ok) throw new Error(registrationData.message || "注册失败，请检查填写内容");
        setRegistering(false); setConfirmPassword(""); setPassword(""); setLoginMessage("读者账号已创建，请使用新账号登录。");
        return;
      }
      const result = await fetch("/api/auth/login", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ username: loginName.trim(), password }) });
      const data = await result.json();
      if (!result.ok) throw new Error(data.message || "用户名或密码不正确");
      const nextSession = data as Session;
      window.localStorage.setItem("bookroom-session", JSON.stringify(nextSession));
      setSession(nextSession); setView(nextSession.roles.includes("ROLE_ADMIN") ? "dashboard" : "books"); setPage(0); setPassword("");
    } catch (cause) { setError(cause instanceof Error ? cause.message : "登录失败，请检查后端是否已启动"); }
    finally { setBusy(false); }
  }

  function logout() {
    window.localStorage.removeItem("bookroom-session"); setSession(null); setBooks(null); setLoans([]); setError(""); setMessage("");
  }

  function openForm(book: Book | "new") {
    setEditing(book); setDraft(book === "new" ? { ...emptyDraft } : { title: book.title, author: book.author, isbn: book.isbn, price: book.price, availableCopies: book.availableCopies }); setError("");
  }

  async function saveBook(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!session) return;
    setBusy(true); setError("");
    try {
      const target = editing === "new" ? "/api/books" : `/api/books/${editing?.id}`;
      await api<Book>(target, session.accessToken, { method: editing === "new" ? "POST" : "PUT", body: JSON.stringify({ ...draft, price: Number(draft.price), availableCopies: Number(draft.availableCopies) }) });
      setEditing(null); setMessage(editing === "new" ? "书籍已加入馆藏。" : "书目信息已保存。");
      await loadBooks(session.accessToken, page, keyword);
    } catch (cause) { setError(cause instanceof Error ? cause.message : "保存失败，请检查填写内容"); }
    finally { setBusy(false); }
  }

  async function deleteBook(book: Book) {
    if (!session || !window.confirm(`确定删除《${book.title}》吗？`)) return;
    setBusy(true); setError("");
    try { await api<void>(`/api/books/${book.id}`, session.accessToken, { method: "DELETE" }); setMessage("书籍已删除。"); await loadBooks(session.accessToken, page, keyword); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "删除失败"); }
    finally { setBusy(false); }
  }

  async function borrowBook(book: Book) {
    if (!session || book.availableCopies < 1) return;
    setBusy(true); setError(""); setMessage("");
    try { await api<BorrowRecord>("/api/borrow-records", session.accessToken, { method: "POST", body: JSON.stringify({ bookId: book.id }) }); setMessage(`《${book.title}》已加入你的借阅记录。`); await loadBooks(session.accessToken, page, keyword); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "借阅失败，请稍后再试"); }
    finally { setBusy(false); }
  }

  async function returnBook(loan: BorrowRecord) {
    if (!session) return;
    setBusy(true); setError(""); setMessage("");
    try { await api<BorrowRecord>(`/api/borrow-records/${loan.id}/return`, session.accessToken, { method: "POST" }); setMessage(`《${loan.bookTitle}》已归还。`); await loadLoans(session.accessToken); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "归还失败，请稍后再试"); }
    finally { setBusy(false); }
  }

  function openDueDateEditor(loan: BorrowRecord) {
    setDueDateEditing(loan);
    setDueDateDraft(localDateInput(loan.dueAt));
    setError("");
  }

  async function saveDueDate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!session || !dueDateEditing) return;
    setBusy(true); setError(""); setMessage("");
    try {
      await api<BorrowRecord>(`/api/borrow-records/${dueDateEditing.id}/due-date`, session.accessToken, {
        method: "PUT",
        body: JSON.stringify({ dueAt: endOfLocalDay(dueDateDraft) }),
      });
      setDueDateEditing(null);
      setMessage(`《${dueDateEditing.bookTitle}》的应还日期已更新。`);
      await loadLoans(session.accessToken);
    } catch (cause) { setError(cause instanceof Error ? cause.message : "调整期限失败，请重试"); }
    finally { setBusy(false); }
  }

  function submitSearch(event: FormEvent<HTMLFormElement>) { event.preventDefault(); setPage(0); setKeyword(searchInput.trim()); }

  if (!ready) return <main className="loading-screen"><span className="loading-mark"/>正在打开书间…</main>;

  if (!session) return <main className="login-screen"><section className="login-panel"><a className="wordmark" href="#home">书间<span> / </span>阅览室</a><div className="login-copy"><h1>让每一本书，<br/>都找到下一位读者。</h1><p>{registering ? "创建读者账号，开启你的阅读旅程。" : "登录后查找馆藏、借阅书籍，也可以管理自己的借阅记录。"}</p></div><form onSubmit={login} className="login-form"><label>用户名<input autoComplete="username" minLength={registering ? 3 : undefined} maxLength={registering ? 50 : undefined} pattern={registering ? "[a-zA-Z0-9_]+" : undefined} value={loginName} onChange={(event) => setLoginName(event.target.value)} required placeholder="请输入用户名"/></label><label>密码<input autoComplete={registering ? "new-password" : "current-password"} type="password" minLength={registering ? 8 : undefined} maxLength={registering ? 72 : undefined} value={password} onChange={(event) => setPassword(event.target.value)} required placeholder={registering ? "至少 8 位" : "请输入密码"}/></label>{registering && <label>确认密码<input autoComplete="new-password" type="password" minLength={8} value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} required placeholder="再输入一次密码"/></label>}{error && <ErrorText>{error}</ErrorText>}{loginMessage && <p className="notice success" role="status">{loginMessage}</p>}<button className="primary-button wide" disabled={busy}>{busy ? (registering ? "正在创建…" : "正在登录…") : (registering ? "创建读者账号" : "进入书间")}</button></form><p className="login-note">{registering ? <>已经有账号？ <button className="text-button" onClick={() => { setRegistering(false); setError(""); }}>返回登录</button></> : <>第一次来？ <button className="text-button" onClick={() => { setRegistering(true); setError(""); setLoginMessage(""); }}>创建读者账号</button></>}</p></section><aside className="login-art" aria-label="书间阅览室"><div className="sun-disc"/><div className="book-stack"><span/><span/><span/></div><p>THE READING ROOM<br/><small>A place for books to find their readers.</small></p></aside></main>;

  return <div className="app-shell">
    <aside className="sidebar"><a className="wordmark" href="#home">书间<small>THE READING ROOM</small></a>{isAdmin && <><p className="nav-label">工作台</p><button className={"nav-link " + (view === "dashboard" ? "selected" : "")} onClick={() => { setView("dashboard"); setMessage(""); setError(""); }}><Icon name="dashboard"/>工作台</button></>}<p className="nav-label">馆藏</p><button className={`nav-link ${view === "books" ? "selected" : ""}`} onClick={() => { setView("books"); setMessage(""); setError(""); }}><Icon name="book"/>馆藏目录</button><button className={`nav-link ${view === "loans" ? "selected" : ""}`} onClick={() => { setView("loans"); setMessage(""); setError(""); }}><Icon name="loan"/>借阅记录{activeLoans > 0 && <span className="nav-count">{activeLoans}</span>}</button><div className="sidebar-bottom"><div className="side-profile"><span className="avatar">{session.username.slice(0, 1).toUpperCase()}</span><span><b>{session.username}</b><small>{isAdmin ? "馆藏管理员" : "读者"}</small></span></div><button className="logout-button" onClick={logout} aria-label="退出登录"><Icon name="logout" size={17}/></button></div></aside>
    <main className="main-area"><header className="topbar"><span className="breadcrumbs">书间 <span>/</span> {view === "dashboard" ? "工作台" : view === "books" ? "馆藏目录" : "借阅记录"}</span><div className="top-user"><span className="avatar">{session.username.slice(0, 1).toUpperCase()}</span>{session.username}</div><button className="mobile-logout" onClick={logout} aria-label="退出登录"><Icon name="logout" size={16}/><span>退出</span></button></header><nav className={"mobile-nav " + (isAdmin ? "admin" : "")} aria-label="主要导航">{isAdmin && <button className={view === "dashboard" ? "selected" : ""} onClick={() => setView("dashboard")}><Icon name="dashboard" size={16}/>工作台</button>}<button className={view === "books" ? "selected" : ""} onClick={() => setView("books")}><Icon name="book" size={16}/>馆藏目录</button><button className={view === "loans" ? "selected" : ""} onClick={() => setView("loans")}><Icon name="loan" size={16}/>借阅记录{activeLoans > 0 ? ` · ${activeLoans}` : ""}</button></nav>
      <div className="content"><div className="page-heading"><div><h1>{view === "dashboard" ? "工作台" : view === "books" ? (isAdmin ? "馆藏目录" : "发现一本好书") : "借阅记录"}</h1><p>{view === "dashboard" ? "查看馆藏和当前借阅概况。" : view === "books" ? (isAdmin ? "维护书目资料，查看当前可借册数。" : "浏览馆藏，找到你想读的下一本书。") : "你的借阅记录和归还状态都在这里。"}</p></div>{view === "books" && isAdmin && <button className="primary-button" onClick={() => openForm("new")}><Icon name="plus" size={17}/>添加入藏</button>}</div>
        {error && <ErrorText>{error}</ErrorText>}{message && <p className="notice success" role="status">{message}</p>}
        {view === "dashboard" && isAdmin && <section className="dashboard-area" aria-label="馆藏和借阅概况">
          <div className="dashboard-stats">
            <button className="dashboard-stat" onClick={() => { setView("books"); setPage(0); setKeyword(""); setSearchInput(""); }}>
              <span className="dashboard-stat-label">馆藏书目</span><strong>{totalBooks === null ? "—" : totalBooks}</strong><small>查看全部馆藏 <span>→</span></small>
            </button>
            <button className="dashboard-stat" onClick={() => { setLoanSearch(""); setLoanFilter("BORROWING"); setView("loans"); }}>
              <span className="dashboard-stat-label">当前借阅</span><strong>{busy && loans.length === 0 ? "—" : activeLoans}</strong><small>查看借阅中记录 <span>→</span></small>
            </button>
            <button className="dashboard-stat" onClick={() => { setLoanSearch(""); setLoanFilter("OVERDUE"); setView("loans"); }}>
              <span className="dashboard-stat-label">逾期未还</span><strong className="overdue-number">{busy && loans.length === 0 ? "—" : overdueLoans}</strong><small>查看逾期记录 <span>→</span></small>
            </button>
          </div>
          <section className="dashboard-recent" aria-labelledby="recent-loans-heading">
            <header><div><h2 id="recent-loans-heading">最近借阅</h2><p>按借出时间排列</p></div><button className="text-button" onClick={() => { setLoanSearch(""); setLoanFilter("ALL"); setView("loans"); }}>全部记录 <span>→</span></button></header>
            {busy && loans.length === 0 ? <div className="table-state">正在读取借阅记录…</div> : loans.length ? <div className="dashboard-recent-list">{loans.slice(0, 5).map((loan) => <button className="dashboard-recent-row" key={loan.id} onClick={() => { setLoanSearch(String(loan.id)); setLoanFilter("ALL"); setView("loans"); }}><span className="dashboard-recent-book"><b>{loan.bookTitle}</b><small>借阅编号 #{loan.id} · {loan.borrowerName}</small></span><span className="dashboard-recent-date">应还 {new Date(loan.dueAt).toLocaleDateString("zh-CN")}</span><i className={"loan-status " + (loan.status === "RETURNED" ? "returned" : loan.status === "OVERDUE" ? "overdue" : "ongoing")}>{loan.status === "RETURNED" ? "已归还" : loan.status === "OVERDUE" ? "已逾期" : "借阅中"}</i></button>)}</div> : <div className="empty-state"><h2>还没有借阅记录</h2><p>读者借阅书籍后，记录会显示在这里。</p></div>}
          </section>
        </section>}
        {view === "books" && <>
          <form className="search-row" onSubmit={submitSearch}><label className="search-field"><Icon name="search" size={19}/><input aria-label="搜索书名或作者" value={searchInput} onChange={(event) => setSearchInput(event.target.value)} placeholder="按书名、作者或 ISBN 查找"/><button type="submit">搜索</button></label><span className="result-count">{books ? `共 ${books.totalElements} 本` : "馆藏目录"}</span></form>
          <div className="book-table"><div className="table-head"><span>书目</span><span>ISBN</span><span>定价</span><span>在架情况</span><span>操作</span></div>
            {busy && !books ? <div className="table-state">正在整理馆藏目录…</div> : books?.content.length ? books.content.map((book, index) => <article className="book-row" key={book.id}><div className="book-title-cell"><span className={`cover cover-${index % 4}`} aria-hidden="true"><i/></span><span><b>{book.title}</b><small>{book.author}</small></span></div><span className="isbn">{book.isbn}</span><span>¥{Number(book.price).toFixed(2)}</span><span><i className={`stock ${book.availableCopies === 0 ? "unavailable" : book.availableCopies <= 1 ? "few" : ""}`}>{book.availableCopies > 0 ? `${book.availableCopies} 册在架` : "暂时借完"}</i></span><span className="row-actions">{isAdmin ? <><button onClick={() => openForm(book)}>编辑</button><button className="quiet-action" onClick={() => void deleteBook(book)}>删除</button></> : <button disabled={busy || book.availableCopies < 1} onClick={() => void borrowBook(book)}>{book.availableCopies > 0 ? "借阅" : "已借完"}</button>}</span></article>) : !busy && !error ? <div className="empty-state"><h2>{keyword ? "没有找到相关书目" : "馆藏还没有书"}</h2><p>{keyword ? "试试换一个书名或作者。" : "添加书目后，就可以在这里开始管理馆藏。"}</p></div> : null}
            {books && books.totalPages > 1 && <div className="pagination"><span>第 {books.number + 1} / {books.totalPages} 页</span><div><button disabled={page <= 0 || busy} onClick={() => setPage((value) => Math.max(0, value - 1))}><Icon name="back" size={15}/>上一页</button><button disabled={page + 1 >= books.totalPages || busy} onClick={() => setPage((value) => value + 1)}>下一页<Icon name="back" size={15}/></button></div></div>}
          </div>
        </>}
        {view === "loans" && <div className="loan-tools"><label className="loan-search search-field"><Icon name="search" size={18}/><input aria-label="搜索借阅记录" value={loanSearch} onChange={(event) => setLoanSearch(event.target.value)} placeholder={isAdmin ? "搜索书名、读者或借阅编号" : "搜索书名或借阅编号"}/>{loanSearch && <button type="button" onClick={() => setLoanSearch("")}>清除</button>}</label><div className="loan-filter" role="group" aria-label="按借阅状态筛选">{([{ value: "ALL", label: "全部" }, { value: "BORROWING", label: "借阅中" }, { value: "OVERDUE", label: "已逾期" }, { value: "RETURNED", label: "已归还" }] as const).map((option) => <button key={option.value} className={loanFilter === option.value ? "selected" : ""} aria-pressed={loanFilter === option.value} onClick={() => setLoanFilter(option.value)}>{option.label}</button>)}</div><span className="loan-result-count">显示 {visibleLoans.length} / {loans.length} 条</span></div>}
        {view === "loans" && <div className="loan-list">{busy && !loans.length ? <div className="table-state">正在读取借阅记录…</div> : visibleLoans.length ? visibleLoans.map((loan) => <article className="loan-row" key={loan.id}><div className="loan-book-icon"><Icon name="book" size={22}/></div><div className="loan-info"><b>{loan.bookTitle}</b><span>借阅编号 #{loan.id} · 借出 {new Date(loan.borrowedAt).toLocaleDateString("zh-CN")} · 应还 {new Date(loan.dueAt).toLocaleDateString("zh-CN")}</span></div><i className={`loan-status ${loan.status === "RETURNED" ? "returned" : loan.status === "OVERDUE" ? "overdue" : "ongoing"}`}>{loan.status === "RETURNED" ? "已归还" : loan.status === "OVERDUE" ? "已逾期" : "借阅中"}</i>{loan.status !== "RETURNED" && <>{isAdmin && <button className="outline-button" disabled={busy} onClick={() => openDueDateEditor(loan)}>调整期限</button>}<button className="outline-button" disabled={busy} onClick={() => void returnBook(loan)}>归还</button></>}</article>) : loans.length ? <div className="empty-state"><h2>没有找到匹配的记录</h2><p>换个关键词或状态筛选。</p><button className="text-button" onClick={() => { setLoanSearch(""); setLoanFilter("ALL"); }}>清除筛选</button></div> : !error ? <div className="empty-state"><h2>还没有借阅记录</h2><p>从馆藏目录借一本书，记录会显示在这里。</p><button className="text-button" onClick={() => setView("books")}>去看看馆藏 <span>→</span></button></div> : null}</div>}
        <footer className="page-footer">读间 · 让书籍继续流动</footer>
      </div>
    </main>
    {editing && <div className="drawer-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setEditing(null); }}><section className="book-drawer" role="dialog" aria-modal="true" aria-labelledby="drawer-title"><div className="drawer-heading"><div><h2 id="drawer-title">{editing === "new" ? "添加入藏" : "编辑书目"}</h2><p>请填写书籍的基本信息。</p></div><button className="icon-button" onClick={() => setEditing(null)} aria-label="关闭"><Icon name="close"/></button></div><form className="book-form" onSubmit={saveBook}><label>书名<input autoFocus maxLength={200} value={draft.title} onChange={(event) => setDraft({ ...draft, title: event.target.value })} required/></label><label>作者<input maxLength={100} value={draft.author} onChange={(event) => setDraft({ ...draft, author: event.target.value })} required/></label><label>ISBN<input maxLength={20} value={draft.isbn} onChange={(event) => setDraft({ ...draft, isbn: event.target.value })} required/></label><div className="form-split"><label>定价（元）<input type="number" min="0.01" step="0.01" value={draft.price} onChange={(event) => setDraft({ ...draft, price: Number(event.target.value) })} required/></label><label>可借册数<input type="number" min="0" step="1" value={draft.availableCopies} onChange={(event) => setDraft({ ...draft, availableCopies: Number(event.target.value) })} required/></label></div>{error && <ErrorText>{error}</ErrorText>}<div className="drawer-actions"><button type="button" className="outline-button" onClick={() => setEditing(null)}>取消</button><button className="primary-button" disabled={busy}>{busy ? "正在保存…" : "保存书目"}</button></div></form></section></div>}
    {dueDateEditing && <div className="drawer-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setDueDateEditing(null); }}><section className="book-drawer" role="dialog" aria-modal="true" aria-labelledby="due-date-title"><div className="drawer-heading"><div><h2 id="due-date-title">调整借阅期限</h2><p>《{dueDateEditing.bookTitle}》 · 借阅编号 #{dueDateEditing.id}</p></div><button className="icon-button" onClick={() => setDueDateEditing(null)} aria-label="关闭"><Icon name="close"/></button></div><form className="book-form" onSubmit={saveDueDate}><label>新的应还日期<input autoFocus type="date" value={dueDateDraft} onChange={(event) => setDueDateDraft(event.target.value)} required/></label><p className="drawer-help">归还日期会按所选日期的当天结束时间保存；更新后会立即刷新逾期状态。</p>{error && <ErrorText>{error}</ErrorText>}<div className="drawer-actions"><button type="button" className="outline-button" onClick={() => setDueDateEditing(null)}>取消</button><button className="primary-button" disabled={busy}>{busy ? "正在保存…" : "保存期限"}</button></div></form></section></div>}
  </div>;
}
