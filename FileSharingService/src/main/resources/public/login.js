const authForm = document.getElementById('auth-form');
const authStatus = document.getElementById('auth-status');
const authUsername = document.getElementById('auth-username');
const authPassword = document.getElementById('auth-password');
const authTabs = document.querySelectorAll('.tab');

let authMode = 'login';

function setStatus(message) {
  authStatus.textContent = message;
}

function setToken(token) {
  if (token) {
    localStorage.setItem('jwt', token);
  } else {
    localStorage.removeItem('jwt');
  }
}

function getToken() {
  return localStorage.getItem('jwt');
}

function redirectToProfile() {
  window.location.href = '/profile';
}

if (getToken()) {
  redirectToProfile();
}

authTabs.forEach((tab) => {
  tab.addEventListener('click', () => {
    authTabs.forEach((btn) => btn.classList.remove('tab--active'));
    tab.classList.add('tab--active');
    authMode = tab.dataset.mode;
    authForm.querySelector('button').textContent = authMode === 'login' ? 'Войти' : 'Создать аккаунт';
    authPassword.autocomplete = authMode === 'login' ? 'current-password' : 'new-password';
    setStatus('');
  });
});

authForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const username = authUsername.value.trim();
  const password = authPassword.value.trim();
  if (!username || !password) {
    setStatus('Введите логин и пароль.');
    return;
  }
  const endpoint = authMode === 'login' ? '/api/login' : '/api/register';
  try {
    const response = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    if (!response.ok) {
      setStatus('Не удалось авторизоваться.');
      return;
    }
    const payload = await response.json();
    setToken(payload.token);
    redirectToProfile();
  } catch (error) {
    setStatus('Ошибка сети.');
  }
});
