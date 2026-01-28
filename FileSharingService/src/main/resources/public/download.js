const form = document.getElementById('download-login');
const statusText = document.getElementById('download-status');

function setStatus(message) {
  statusText.textContent = message;
}

function extractToken() {
  const parts = window.location.pathname.split('/');
  return parts.length >= 3 ? parts[2] : null;
}

async function downloadWithToken(token, jwt, fallbackName) {
  const response = await fetch(`/api/download/${token}`, {
    headers: { 'Authorization': `Bearer ${jwt}` }
  });
  if (response.status === 401) {
    setStatus('Нужна авторизация.');
    return;
  }
  if (!response.ok) {
    setStatus('Файл не найден или доступ запрещен.');
    return;
  }

  const disposition = response.headers.get('Content-Disposition') || '';
  const match = disposition.match(/filename\*?=(?:UTF-8'')?([^;]+)/i);
  const filename = match ? decodeURIComponent(match[1].replace(/"/g, '')) : fallbackName;

  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename || 'file';
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  const username = document.getElementById('download-username').value.trim();
  const password = document.getElementById('download-password').value.trim();
  if (!username || !password) {
    setStatus('Введите логин и пароль.');
    return;
  }
  const token = extractToken();
  if (!token) {
    setStatus('Ссылка некорректна.');
    return;
  }
  try {
    const login = await fetch('/api/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    if (!login.ok) {
      setStatus('Неверный логин или пароль.');
      return;
    }
    const payload = await login.json();
    await downloadWithToken(token, payload.token, 'file');
  } catch (error) {
    setStatus('Ошибка сети.');
  }
});

const token = extractToken();
if (!token) {
  setStatus('Ссылка некорректна.');
}
