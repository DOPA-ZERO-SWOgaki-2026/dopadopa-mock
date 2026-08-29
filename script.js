const STORAGE_KEY = 'dopadopa-state';
const DAILY_GOAL_SECONDS = 180 * 60;
const POINTS_PER_MINUTE = 1;

const state = {
  isTracking: false,
  phoneFreeSeconds: 0,
  lastStartedAt: null,
  isPowerOff: false,
  powerOffSeconds: 0,
  powerOffStartedAt: null,
  goalSeconds: DAILY_GOAL_SECONDS,
  dayKey: new Date().toISOString().slice(0, 10),
};

const toggleTrackingButton = document.getElementById('toggleTracking');
const togglePowerOffButton = document.getElementById('togglePowerOff');
const addTenMinutesButton = document.getElementById('addTenMinutes');
const pointsDisplay = document.getElementById('pointsDisplay');
const phoneFreeDisplay = document.getElementById('phoneFreeDisplay');
const powerOffDisplay = document.getElementById('powerOffDisplay');
const screenTimeDisplay = document.getElementById('screenTimeDisplay');
const digitalDetoxDisplay = document.getElementById('digitalDetoxDisplay');
const goalRatio = document.getElementById('goalRatio');
const ringProgress = document.getElementById('ringProgress');
const historyList = document.getElementById('historyList');
const weeklyPoints = document.getElementById('weeklyPoints');
const bestRecord = document.getElementById('bestRecord');
const dailyTarget = document.getElementById('dailyTarget');
const dailyProgressText = document.getElementById('dailyProgressText');
const goalSettingButton = document.getElementById('goalSettingButton');
const goalModal = document.getElementById('goalModal');
const goalMinutesInput = document.getElementById('goalMinutesInput');
const closeGoalModalButton = document.getElementById('closeGoalModal');
const cancelGoalSettingButton = document.getElementById('cancelGoalSetting');
const saveGoalSettingButton = document.getElementById('saveGoalSetting');

const saveState = () => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
};

const loadState = () => {
  const saved = localStorage.getItem(STORAGE_KEY);
  if (!saved) return;

  try {
    const parsed = JSON.parse(saved);
    Object.assign(state, parsed);
  } catch (error) {
    console.error('Failed to parse saved state', error);
  }
};

const formatDuration = (totalSeconds) => {
  const roundedSeconds = Math.max(0, Math.round(totalSeconds));
  const hours = Math.floor(roundedSeconds / 3600);
  const minutes = Math.floor((roundedSeconds % 3600) / 60);
  const seconds = roundedSeconds % 60;

  const hh = String(hours).padStart(2, '0');
  const mm = String(minutes).padStart(2, '0');
  const ss = String(seconds).padStart(2, '0');

  return `${hh}:${mm}:${ss}`;
};

const formatPoints = (value) => `${Math.floor(value)}P`;

const getGoalSeconds = () => state.goalSeconds || DAILY_GOAL_SECONDS;

const updateRing = () => {
  const goalSeconds = getGoalSeconds();
  const totalTrackedSeconds = state.phoneFreeSeconds + state.powerOffSeconds;
  const ratio = Math.min(totalTrackedSeconds / goalSeconds, 1);
  const circumference = 2 * Math.PI * 82;
  const dashOffset = circumference - circumference * ratio;
  ringProgress.style.strokeDasharray = `${circumference}`;
  ringProgress.style.strokeDashoffset = `${dashOffset}`;
  ringProgress.style.stroke = 'url(#ringGradient)';

  goalRatio.textContent = `${Math.round(ratio * 100)}%`;
};

const renderHistory = () => {
  const items = [
    { time: '08:15', event: '朝の通知を見ない時間を維持', points: '+30P' },
    { time: '12:40', event: '昼休みをスマホなしで過ごす', points: '+25P' },
    { time: '19:10', event: '夕食中のスマホチェックを回避', points: '+45P' },
  ];

  historyList.innerHTML = items
    .map(
      (item) => `
        <li>
          <span class="time">${item.time}</span>
          <span class="event">${item.event}</span>
          <span class="points">${item.points}</span>
        </li>
      `
    )
    .join('');
};

const updateUI = () => {
  const totalTrackedSeconds = Math.round(state.phoneFreeSeconds + state.powerOffSeconds);
  const totalPoints = Math.floor((totalTrackedSeconds / 60) * POINTS_PER_MINUTE);
  const goalSeconds = getGoalSeconds();
  const screenTimeSeconds = 3 * 60 * 60 + 40 * 60; // Android-like example value
  const digitalDetoxSeconds = 7 * 60 * 60 + 20 * 60; // example value for no-phone time

  pointsDisplay.textContent = formatPoints(totalPoints);
  phoneFreeDisplay.textContent = formatDuration(state.phoneFreeSeconds);
  powerOffDisplay.textContent = formatDuration(state.powerOffSeconds);
  screenTimeDisplay.textContent = formatDuration(screenTimeSeconds);
  digitalDetoxDisplay.textContent = formatDuration(digitalDetoxSeconds);
  dailyTarget.textContent = `${Math.floor(goalSeconds / 60)}分`;
  weeklyPoints.textContent = '2,480P';
  bestRecord.textContent = '4h 30m';

  const remainingSeconds = Math.max(goalSeconds - totalTrackedSeconds, 0);
  const remainingMinutes = Math.ceil(remainingSeconds / 60);
  const goalText = remainingSeconds === 0 ? '目標達成！' : `あと ${remainingMinutes}分`;
  dailyProgressText.textContent = goalText;

  const ratio = Math.min(totalTrackedSeconds / goalSeconds, 1);
  ringProgress.style.strokeDasharray = `${2 * Math.PI * 82}`;
  ringProgress.style.strokeDashoffset = `${2 * Math.PI * 82 - 2 * Math.PI * 82 * ratio}`;
  goalRatio.textContent = `${Math.round(ratio * 100)}%`;
};

const tick = () => {
  const now = Date.now();

  if (state.isTracking && state.lastStartedAt) {
    const elapsedSeconds = Math.max(1, Math.ceil((now - state.lastStartedAt) / 1000));
    state.phoneFreeSeconds += elapsedSeconds;
    state.lastStartedAt = now;
  }

  if (state.isPowerOff && state.powerOffStartedAt) {
    const elapsedSeconds = Math.max(1, Math.ceil((now - state.powerOffStartedAt) / 1000));
    state.powerOffSeconds += elapsedSeconds;
    state.powerOffStartedAt = now;
  }

  saveState();
  updateUI();
};

const startTracking = () => {
  state.isTracking = true;
  state.lastStartedAt = Date.now();
  toggleTrackingButton.textContent = '集中終了';
  saveState();
};

const stopTracking = () => {
  state.isTracking = false;
  state.lastStartedAt = null;
  toggleTrackingButton.textContent = '集中開始';
  saveState();
};

const toggleTracking = () => {
  if (state.isTracking) {
    stopTracking();
  } else {
    startTracking();
  }
};

const startPowerOff = () => {
  state.isPowerOff = true;
  state.powerOffStartedAt = Date.now();
  saveState();
  updateUI();
};

const stopPowerOff = () => {
  state.isPowerOff = false;
  state.powerOffStartedAt = null;
  saveState();
  updateUI();
};

const togglePowerOff = () => {
  if (state.isPowerOff) {
    stopPowerOff();
  } else {
    startPowerOff();
  }
};

const addTenMinutes = () => {
  state.phoneFreeSeconds += 10 * 60;
  if (state.isTracking && state.lastStartedAt) {
    state.lastStartedAt = Date.now();
  }
  saveState();
  updateUI();
};

const initializeSvgGradient = () => {
  const svgNS = 'http://www.w3.org/2000/svg';
  const svg = document.querySelector('.progress-ring svg');
  const defs = document.createElementNS(svgNS, 'defs');
  const linearGradient = document.createElementNS(svgNS, 'linearGradient');
  linearGradient.setAttribute('id', 'ringGradient');
  linearGradient.setAttribute('x1', '0%');
  linearGradient.setAttribute('y1', '0%');
  linearGradient.setAttribute('x2', '100%');
  linearGradient.setAttribute('y2', '100%');

  const stop1 = document.createElementNS(svgNS, 'stop');
  stop1.setAttribute('offset', '0%');
  stop1.setAttribute('stop-color', '#3d7bff');

  const stop2 = document.createElementNS(svgNS, 'stop');
  stop2.setAttribute('offset', '100%');
  stop2.setAttribute('stop-color', '#1ac29a');

  linearGradient.append(stop1, stop2);
  defs.appendChild(linearGradient);
  svg.prepend(defs);
};

const resetState = () => {
  state.isTracking = false;
  state.phoneFreeSeconds = 0;
  state.lastStartedAt = null;
  state.isPowerOff = false;
  state.powerOffSeconds = 0;
  state.powerOffStartedAt = null;
  state.goalSeconds = DAILY_GOAL_SECONDS;
  toggleTrackingButton.textContent = '集中開始';
  togglePowerOffButton.textContent = '電源オフ';
  saveState();
  updateUI();
};

const openGoalModal = () => {
  if (!goalModal || !goalMinutesInput) return;
  goalMinutesInput.value = String(Math.floor((state.goalSeconds || DAILY_GOAL_SECONDS) / 60));
  goalModal.classList.remove('hidden');
  goalModal.setAttribute('aria-hidden', 'false');
};

const closeGoalModal = () => {
  if (!goalModal) return;
  goalModal.classList.add('hidden');
  goalModal.setAttribute('aria-hidden', 'true');
};

const saveGoalTime = () => {
  if (!goalMinutesInput) return;

  const nextGoalMinutes = Number(goalMinutesInput.value);
  if (!Number.isFinite(nextGoalMinutes) || nextGoalMinutes <= 0) {
    return;
  }

  state.goalSeconds = nextGoalMinutes * 60;
  saveState();
  updateUI();
  closeGoalModal();
};

loadState();
initializeSvgGradient();
renderHistory();
updateUI();

if (state.isTracking) {
  toggleTrackingButton.textContent = '集中終了';
}
if (state.isPowerOff) {
  togglePowerOffButton.textContent = '電源オフ中';
}

setInterval(tick, 1000);

if (toggleTrackingButton) {
  toggleTrackingButton.addEventListener('click', toggleTracking);
}
if (togglePowerOffButton) {
  togglePowerOffButton.addEventListener('click', togglePowerOff);
}
if (addTenMinutesButton) {
  addTenMinutesButton.addEventListener('click', addTenMinutes);
}
if (goalSettingButton) {
  goalSettingButton.addEventListener('click', openGoalModal);
}
if (closeGoalModalButton) {
  closeGoalModalButton.addEventListener('click', closeGoalModal);
}
if (cancelGoalSettingButton) {
  cancelGoalSettingButton.addEventListener('click', closeGoalModal);
}
if (saveGoalSettingButton) {
  saveGoalSettingButton.addEventListener('click', saveGoalTime);
}
if (goalModal) {
  goalModal.addEventListener('click', (event) => {
    if (event.target === goalModal) {
      closeGoalModal();
    }
  });
}
window.addEventListener('beforeunload', saveState);
window.addEventListener('dblclick', resetState);
