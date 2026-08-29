const USER_KEY = 'dopadopa-user';
const usernameEl = document.getElementById('accountUsername');
const logoutButton = document.getElementById('logoutButton');
const viewDashboardButton = document.getElementById('viewDashboardButton');

const getCurrentUser = () => localStorage.getItem(USER_KEY) || '';

const renderAccountPage = () => {
  const username = getCurrentUser();

  if (!username) {
    window.location.href = 'index.html';
    return;
  }

  if (usernameEl) {
    usernameEl.textContent = username;
  }
};

const logout = () => {
  localStorage.removeItem(USER_KEY);
  window.location.href = 'index.html';
};

if (viewDashboardButton) {
  viewDashboardButton.addEventListener('click', () => {
    window.location.href = 'index.html';
  });
}

if (logoutButton) {
  logoutButton.addEventListener('click', logout);
}

renderAccountPage();
