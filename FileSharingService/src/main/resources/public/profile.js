const form = document.getElementById('upload-form');
const fileInput = document.getElementById('file-input');
const fileName = document.getElementById('file-name');
const progressBar = document.getElementById('progress-bar');
const statusText = document.getElementById('status-text');
const result = document.getElementById('result');
const downloadLink = document.getElementById('download-link');
const copyLink = document.getElementById('copy-link');
const downloadNow = document.getElementById('download-now');
const resultMeta = document.getElementById('result-meta');

const statsList = document.getElementById('stats-list');
const refreshStats = document.getElementById('refresh-stats');
const logoutButton = document.getElementById('logout');

function setStatus(message) {
  statusText.textContent = message;
}

function setProgress(value) {
  progressBar.style.width = `${value}%`;
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

function ensureAuth() {
  const token = getToken();
  if (!token) {
    window.location.href = '/login';
    return null;
  }
  return token;
}

fileInput.addEventListener('change', () => {
  const file = fileInput.files[0];
  fileName.textContent = file ? `${file.name} (${formatBytes(file.size)})` : 'Файл не выбран';
  result.hidden = true;
  setProgress(0);
  setStatus('Файл выбран, можно загружать.');
});

form.addEventListener('submit', (event) => {
  event.preventDefault();
  const file = fileInput.files[0];
  if (!file) {
    setStatus('Выберите файл перед загрузкой.');
    return;
  }

  const token = ensureAuth();
  if (!token) {
    return;
  }

  const formData = new FormData();
  formData.append('file', file);

  const xhr = new XMLHttpRequest();
  xhr.open('POST', '/api/upload');
  xhr.setRequestHeader('Authorization', `Bearer ${token}`);

  xhr.upload.addEventListener('progress', (evt) => {
    if (evt.lengthComputable) {
      const percent = Math.round((evt.loaded / evt.total) * 100);
      setProgress(percent);
      setStatus(`Загрузка… ${percent}%`);
    }
  });

  xhr.addEventListener('load', () => {
    if (xhr.status >= 200 && xhr.status < 300) {
      try {
        const payload = JSON.parse(xhr.responseText);
        downloadLink.value = payload.link;
        downloadNow.dataset.token = payload.token;
        downloadNow.dataset.name = payload.name;
        resultMeta.textContent = `Файл: ${payload.name} • ${formatBytes(payload.size)}`;
        result.hidden = false;
        setStatus('Готово!');
        fetchStats();
      } catch (error) {
        setStatus('Не удалось разобрать ответ сервера.');
      }
    } else if (xhr.status === 401) {
      setToken(null);
      window.location.href = '/login';
    } else {
      setStatus(`Ошибка загрузки: ${xhr.responseText || xhr.status}`);
    }
  });

  xhr.addEventListener('error', () => {
    setStatus('Ошибка сети при загрузке.');
  });

  xhr.send(formData);
  setStatus('Начинаем загрузку…');
});

copyLink.addEventListener('click', async () => {
  if (!downloadLink.value) {
    return;
  }
  try {
    await navigator.clipboard.writeText(downloadLink.value);
    copyLink.textContent = 'Скопировано';
    setTimeout(() => {
      copyLink.textContent = 'Скопировать';
    }, 1500);
  } catch (error) {
    setStatus('Не удалось скопировать ссылку.');
  }
});

downloadNow.addEventListener('click', async () => {
  const token = downloadNow.dataset.token;
  const name = downloadNow.dataset.name || 'file';
  if (!token) {
    return;
  }
  await downloadFile(token, name);
});

refreshStats.addEventListener('click', fetchStats);
logoutButton.addEventListener('click', () => {
  setToken(null);
  window.location.href = '/login';
});

async function fetchStats() {
  const token = ensureAuth();
  if (!token) {
    return;
  }
  try {
    const response = await fetch('/api/files', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    if (response.status === 401) {
      setToken(null);
      window.location.href = '/login';
      return;
    }
    const payload = await response.json();
    renderStats(payload.files || []);
  } catch (error) {
    statsList.innerHTML = '<p class="hint">Не удалось загрузить статистику.</p>';
  }
}

function renderStats(files) {
  if (!files.length) {
    statsList.innerHTML = '<p class="hint">Пока нет загруженных файлов.</p>';
    return;
  }
  statsList.innerHTML = files.map((file) => {
    const downloads = file.downloads || [];
    const downloadList = downloads.map((entry) => {
      return `<li>${entry.user} • ${formatDate(entry.downloadedAt)}</li>`;
    }).join('');
    return `
      <article class="card">
        <div>
          <h3>${escapeHtml(file.name)}</h3>
          <p class="hint">${formatBytes(file.size)} • загружен ${formatDate(file.uploadedAt)}</p>
          <p class="hint">Скачиваний: ${file.downloadCount}</p>
        </div>
        <div>
          <p class="hint">Последнее скачивание: ${formatDate(file.lastDownloaded)}</p>
          <ul class="downloads">${downloadList || '<li>Нет скачиваний</li>'}</ul>
          <div class="stats__actions">
            <button class="btn btn--ghost" type="button" data-download="${file.token}" data-name="${escapeAttr(file.name)}">Скачать</button>
            <button class="btn btn--ghost" type="button" data-copy="${file.token}">Скопировать ссылку</button>
          </div>
        </div>
      </article>
    `;
  }).join('');

  statsList.querySelectorAll('[data-download]').forEach((button) => {
    button.addEventListener('click', async () => {
      const token = button.dataset.download;
      const name = button.dataset.name || 'file';
      await downloadFile(token, name);
    });
  });

  statsList.querySelectorAll('[data-copy]').forEach((button) => {
    button.addEventListener('click', async () => {
      const token = button.dataset.copy;
      if (!token) {
        return;
      }
      const link = `${window.location.origin}/download/${token}`;
      try {
        await navigator.clipboard.writeText(link);
        button.textContent = 'Скопировано';
        setTimeout(() => {
          button.textContent = 'Скопировать ссылку';
        }, 1500);
      } catch (error) {
        setStatus('Не удалось скопировать ссылку.');
      }
    });
  });
}

async function downloadFile(token, filename) {
  const jwt = ensureAuth();
  if (!jwt) {
    return;
  }
  try {
    const response = await fetch(`/api/download/${token}`, {
      headers: { 'Authorization': `Bearer ${jwt}` }
    });
    if (response.status === 401) {
      setToken(null);
      window.location.href = '/login';
      return;
    }
    if (!response.ok) {
      setStatus('Не удалось скачать файл.');
      return;
    }
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  } catch (error) {
    setStatus('Ошибка сети при скачивании.');
  }
}

function formatBytes(bytes) {
  if (bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const index = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${(bytes / Math.pow(1024, index)).toFixed(1)} ${units[index]}`;
}

function formatDate(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString('ru-RU');
}

function escapeHtml(value) {
  return value.replace(/[&<>"']/g, (char) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  }[char]));
}

function escapeAttr(value) {
  return escapeHtml(value).replace(/"/g, '&quot;');
}

ensureAuth();
fetchStats();
