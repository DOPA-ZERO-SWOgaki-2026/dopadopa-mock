const USER_KEY = 'dopadopa-user';
const ACCOUNTS_KEY = 'dopadopa-accounts';
const DEVICE_ID_KEY = 'dopadopa-device-id';
const usernameEl = document.getElementById('accountUsername');
const totalPointsEl = document.getElementById('totalPointsValue');
const screenTimeValueEl = document.getElementById('screenTimeValue');
const digitalDetoxValueEl = document.getElementById('digitalDetoxValue');
const logoutButton = document.getElementById('logoutButton');
const viewDashboardButton = document.getElementById('viewDashboardButton');

const getCurrentUser = () => localStorage.getItem(USER_KEY) || '';

const getDeviceId = () => {
  let deviceId = localStorage.getItem(DEVICE_ID_KEY);
  if (!deviceId) {
    deviceId = `device-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`;
    localStorage.setItem(DEVICE_ID_KEY, deviceId);
  }
  return deviceId;
};

const getAccountProfiles = () => {
  try {
    return JSON.parse(localStorage.getItem(ACCOUNTS_KEY) || '{}');
  } catch (error) {
    console.error('Failed to parse account profiles', error);
    return {};
  }
};

const getCurrentDeviceMetrics = () => {
  const username = getCurrentUser();
  if (!username) {
    return { screenTimeSeconds: 0, digitalDetoxSeconds: 0 };
  }

  const profiles = getAccountProfiles();
  const account = profiles[username] || { devices: {} };
  const deviceId = getDeviceId();
  const stats = account.devices[deviceId];

  return stats || { screenTimeSeconds: 0, digitalDetoxSeconds: 0 };
};

const formatDuration = (totalSeconds) => {
  const roundedSeconds = Math.max(0, Math.round(totalSeconds));
  const hours = Math.floor(roundedSeconds / 3600);
  const minutes = Math.floor((roundedSeconds % 3600) / 60);
  const seconds = roundedSeconds % 60;

  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
};

const renderAccountPage = () => {
  const username = getCurrentUser();

  if (!username) {
    window.location.href = 'index.html';
    return;
  }

  if (usernameEl) {
    usernameEl.textContent = username;
  }

  const deviceStats = getCurrentDeviceMetrics();
  const digitalDetoxSeconds = Math.max(0, Number(deviceStats.digitalDetoxSeconds) || 0);
  const screenTimeSeconds = Math.max(0, Number(deviceStats.screenTimeSeconds) || 0);

  if (totalPointsEl) totalPointsEl.textContent = `${Math.floor(digitalDetoxSeconds / 60)}P`;
  if (screenTimeValueEl) screenTimeValueEl.textContent = formatDuration(screenTimeSeconds);
  if (digitalDetoxValueEl) digitalDetoxValueEl.textContent = formatDuration(digitalDetoxSeconds);
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
