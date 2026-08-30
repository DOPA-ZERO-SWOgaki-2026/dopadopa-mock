const STORAGE_KEY = 'dopadopa-state';
const USER_KEY = 'dopadopa-user';
const ACCOUNTS_KEY = 'dopadopa-accounts';
const DEVICE_ID_KEY = 'dopadopa-device-id';

const DAILY_GOAL_SECONDS = 180 * 60;
const POINTS_PER_MINUTE = 1;

const state = {
screenTimeSeconds: 0,
digitalDetoxSeconds: 0,
lastActiveAt: null,
goalSeconds: DAILY_GOAL_SECONDS,
weeklyPoints: 0,
weeklyStartDate: null,
};

const pointsDisplay = document.getElementById('pointsDisplay');
const screenTimeDisplay = document.getElementById('screenTimeDisplay');
const digitalDetoxDisplay = document.getElementById('digitalDetoxDisplay');
const goalRatio = document.getElementById('goalRatio');
const ringProgress = document.getElementById('ringProgress');

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

const loginScreen = document.getElementById('loginScreen');
const appContainer = document.getElementById('appContainer');
const loginForm = document.getElementById('loginForm');
const usernameInput = document.getElementById('usernameInput');
const welcomeUser = document.getElementById('welcomeUser');
const logoutButton = document.getElementById('logoutButton');

const historyList = document.getElementById('historyList');

const getCurrentUser = () => {
return localStorage.getItem(USER_KEY) || '';
};

const getDeviceId = () => {
let deviceId = localStorage.getItem(DEVICE_ID_KEY);

if (!deviceId) {
deviceId = `device-${Date.now()}-${Math.random()
      .toString(16)
      .slice(2, 8)}`;


localStorage.setItem(DEVICE_ID_KEY, deviceId);


}

return deviceId;
};

const getAccountProfiles = () => {
try {
return JSON.parse(
localStorage.getItem(ACCOUNTS_KEY) || '{}'
);
} catch (error) {
console.error(
'Failed to parse account profiles:',
error
);


return {};


}
};

const saveAccountProfiles = (profiles) => {
localStorage.setItem(
ACCOUNTS_KEY,
JSON.stringify(profiles)
);
};

const ensureAccountProfile = (username) => {
const profiles = getAccountProfiles();
const safeUsername = String(username || '').trim();

if (!safeUsername) {
return null;
}

if (!profiles[safeUsername]) {
profiles[safeUsername] = {
devices: {},
};
}

saveAccountProfiles(profiles);

return profiles[safeUsername];
};

const formatDuration = (totalSeconds) => {
const seconds = Math.max(
0,
Math.round(Number(totalSeconds) || 0)
);

const hours = Math.floor(seconds / 3600);

const minutes = Math.floor(
(seconds % 3600) / 60
);

const remainingSeconds = seconds % 60;

return (
`${String(hours).padStart(2, '0')}:` +
`${String(minutes).padStart(2, '0')}:` +
`${String(remainingSeconds).padStart(2, '0')}`
);
};

const formatPoints = (value) => {
return `${Math.floor(
    Number(value) || 0
  ).toLocaleString()}P`;
};

const getTodayKey = () => {
return new Date().toISOString().slice(0, 10);
};

const getMonday = (date) => {
const result = new Date(date);

const day = result.getDay();

const diff = day === 0
? -6
: 1 - day;

result.setDate(
result.getDate() + diff
);

result.setHours(
0,
0,
0,
0
);

return result;
};

const getWeekKey = () => {
const monday = getMonday(
new Date()
);

return monday
.toISOString()
.slice(0, 10);
};

const loadState = () => {
const saved = localStorage.getItem(
STORAGE_KEY
);

if (!saved) {
state.lastActiveAt = Date.now();
state.weeklyStartDate = getWeekKey();
return;
}

try {
const parsed = JSON.parse(saved);


Object.assign(
  state,
  parsed
);

if (!state.weeklyStartDate) {
  state.weeklyStartDate = getWeekKey();
}

if (!state.lastActiveAt) {
  state.lastActiveAt = Date.now();
}

if (
  typeof state.screenTimeSeconds !== 'number'
) {
  state.screenTimeSeconds = 0;
}

if (
  typeof state.digitalDetoxSeconds !== 'number'
) {
  state.digitalDetoxSeconds = 0;
}

if (
  typeof state.weeklyPoints !== 'number'
) {
  state.weeklyPoints = Math.floor(
    state.digitalDetoxSeconds / 60
  );
}


} catch (error) {
console.error(
'Failed to load state:',
error
);


state.lastActiveAt = Date.now();
state.weeklyStartDate = getWeekKey();


}
};

const resetWeeklyPointsIfNeeded = () => {
const currentWeek = getWeekKey();

if (
state.weeklyStartDate !== currentWeek
) {
state.weeklyStartDate = currentWeek;
state.weeklyPoints = 0;
}
};

const syncCurrentDeviceMetrics = () => {
const username = getCurrentUser();

if (!username) {
return;
}

const profiles = getAccountProfiles();

if (!profiles[username]) {
profiles[username] = {
devices: {},
};
}

if (!profiles[username].devices) {
profiles[username].devices = {};
}

const deviceId = getDeviceId();

profiles[username].devices[deviceId] = {
screenTimeSeconds: Math.max(
0,
Math.round(
state.screenTimeSeconds || 0
)
),


digitalDetoxSeconds: Math.max(
  0,
  Math.round(
    state.digitalDetoxSeconds || 0
  )
),

weeklyPoints: Math.max(
  0,
  Math.floor(
    state.weeklyPoints || 0
  )
),


};

saveAccountProfiles(
profiles
);
};

const saveState = () => {
localStorage.setItem(
STORAGE_KEY,
JSON.stringify(state)
);

syncCurrentDeviceMetrics();
};

const getGoalSeconds = () => {
const goal = Number(
state.goalSeconds
);

if (
Number.isFinite(goal) &&
goal > 0
) {
return goal;
}

return DAILY_GOAL_SECONDS;
};

const initializeSvgGradient = () => {
const svgNS =
'http://www.w3.org/2000/svg';

const svg = document.querySelector(
'.progress-ring svg'
);

if (!svg) {
return;
}

if (
svg.querySelector(
'#ringGradient'
)
) {
return;
}

const defs =
document.createElementNS(
svgNS,
'defs'
);

const linearGradient =
document.createElementNS(
svgNS,
'linearGradient'
);

linearGradient.setAttribute(
'id',
'ringGradient'
);

linearGradient.setAttribute(
'x1',
'0%'
);

linearGradient.setAttribute(
'y1',
'0%'
);

linearGradient.setAttribute(
'x2',
'100%'
);

linearGradient.setAttribute(
'y2',
'100%'
);

const stop1 =
document.createElementNS(
svgNS,
'stop'
);

stop1.setAttribute(
'offset',
'0%'
);

stop1.setAttribute(
'stop-color',
'#3d7bff'
);

const stop2 =
document.createElementNS(
svgNS,
'stop'
);

stop2.setAttribute(
'offset',
'100%'
);

stop2.setAttribute(
'stop-color',
'#1ac29a'
);

linearGradient.append(
stop1,
stop2
);

defs.appendChild(
linearGradient
);

svg.prepend(defs);
};

const updateRing = () => {
if (!ringProgress) {
return;
}

const goalSeconds =
getGoalSeconds();

const detoxSeconds =
Math.max(
0,
Number(
state.digitalDetoxSeconds
) || 0
);

const ratio = Math.min(
detoxSeconds /
goalSeconds,
1
);

const circumference =
2 * Math.PI * 82;

const dashOffset =
circumference -
circumference * ratio;

ringProgress.style.strokeDasharray =
`${circumference}`;

ringProgress.style.strokeDashoffset =
`${dashOffset}`;

ringProgress.style.stroke =
'url(#ringGradient)';

if (goalRatio) {
goalRatio.textContent =
`${Math.round(
        ratio * 100
      )}%`;
}
};

const updateUI = () => {
resetWeeklyPointsIfNeeded();

const detoxSeconds =
Math.max(
0,
Math.round(
Number(
state.digitalDetoxSeconds
) || 0
)
);

const screenSeconds =
Math.max(
0,
Math.round(
Number(
state.screenTimeSeconds
) || 0
)
);

const points =
Math.floor(
detoxSeconds / 60
) * POINTS_PER_MINUTE;

const goalSeconds =
getGoalSeconds();

if (pointsDisplay) {
pointsDisplay.textContent =
formatPoints(points);
}

if (screenTimeDisplay) {
screenTimeDisplay.textContent =
formatDuration(
screenSeconds
);
}

if (digitalDetoxDisplay) {
digitalDetoxDisplay.textContent =
formatDuration(
detoxSeconds
);
}

if (weeklyPoints) {
weeklyPoints.textContent =
formatPoints(
state.weeklyPoints
);
}

if (dailyTarget) {
dailyTarget.textContent =
`${Math.floor(
        goalSeconds / 60
      )}分`;
}

const remainingSeconds =
Math.max(
goalSeconds -
detoxSeconds,
0
);

const remainingMinutes =
Math.ceil(
remainingSeconds / 60
);

if (dailyProgressText) {
if (
remainingSeconds === 0
) {
dailyProgressText.textContent =
'目標達成!';
} else {
dailyProgressText.textContent =
`あと ${remainingMinutes}分`;
}
}

if (bestRecord) {
const hours =
Math.floor(
detoxSeconds / 3600
);


const minutes =
  Math.floor(
    (detoxSeconds % 3600) /
      60
  );

bestRecord.textContent =
  `${hours}h ${minutes}m`;


}

updateRing();
};

const addDetoxTime = (
seconds
) => {
const safeSeconds =
Math.max(
0,
Math.floor(
Number(seconds) || 0
)
);

if (
safeSeconds <= 0
) {
return;
}

const previousPoints =
Math.floor(
state.digitalDetoxSeconds /
60
);

state.digitalDetoxSeconds +=
safeSeconds;

const newPoints =
Math.floor(
state.digitalDetoxSeconds /
60
);

const gainedPoints =
Math.max(
0,
newPoints -
previousPoints
);

if (
gainedPoints > 0
) {
resetWeeklyPointsIfNeeded();


state.weeklyPoints +=
  gainedPoints;


}
};

const calculateElapsedTime = () => {
const now = Date.now();

if (!state.lastActiveAt) {
state.lastActiveAt = now;
return;
}

const elapsedSeconds =
Math.floor(
(now -
state.lastActiveAt) /
1000
);

if (
elapsedSeconds <= 0
) {
return;
}

if (
document.visibilityState ===
'visible'
) {
state.screenTimeSeconds +=
elapsedSeconds;
} else {
addDetoxTime(
elapsedSeconds
);
}

state.lastActiveAt = now;
};

const tick = () => {
calculateElapsedTime();

saveState();

updateUI();
};

const handleVisibilityChange = () => {
const now = Date.now();

if (!state.lastActiveAt) {
state.lastActiveAt = now;
return;
}

const elapsedSeconds =
Math.floor(
(now -
state.lastActiveAt) /
1000
);

if (
elapsedSeconds > 0
) {
if (
document.visibilityState ===
'hidden'
) {
addDetoxTime(
elapsedSeconds
);
} else {
state.screenTimeSeconds +=
elapsedSeconds;
}
}

state.lastActiveAt = now;

saveState();

updateUI();
};

const handleLogin = (
event
) => {
event.preventDefault();

const username =
usernameInput?.value.trim();

if (!username) {
return;
}

ensureAccountProfile(
username
);

localStorage.setItem(
USER_KEY,
username
);

// 画面を表示に切り替える
showApp();
};

const handleLogout = () => {
calculateElapsedTime();

saveState();

localStorage.removeItem(
USER_KEY
);

window.location.href =
'index.html';
};

const showApp = () => {
if (loginScreen) {
loginScreen.classList.add(
'hidden'
);
}

if (appContainer) {
appContainer.classList.remove(
'hidden'
);
}

const username =
getCurrentUser();

if (
welcomeUser &&
username
) {
welcomeUser.textContent =
`${username} さんの記録`;
}
};

const showLogin = () => {
if (appContainer) {
appContainer.classList.add(
'hidden'
);
}

if (loginScreen) {
loginScreen.classList.remove(
'hidden'
);
}
};

const renderHistory = () => {
if (!historyList) {
return;
}

const items = [
{
time: '08:15',
event: '朝の通知を見ない時間を維持',
points: '+30P',
},
{
time: '12:40',
event: '昼休みをスマホなしで過ごす',
points: '+25P',
},
{
time: '19:10',
event: '夕食中のスマホチェックを回避',
points: '+45P',
},
];

historyList.innerHTML =
items
.map(
(item) => ` <li> <span class="time">
${item.time} </span>


        <span class="event">
          ${item.event}
        </span>

        <span class="points">
          ${item.points}
        </span>
      </li>
    `
  )
  .join('');


};

const openGoalModal = () => {
if (
!goalModal ||
!goalMinutesInput
) {
return;
}

goalMinutesInput.value =
String(
Math.floor(
getGoalSeconds() /
60
)
);

goalModal.classList.remove(
'hidden'
);

goalModal.setAttribute(
'aria-hidden',
'false'
);
};

const closeGoalModal = () => {
if (!goalModal) {
return;
}

goalModal.classList.add(
'hidden'
);

goalModal.setAttribute(
'aria-hidden',
'true'
);
};

const saveGoalTime = () => {
if (!goalMinutesInput) {
return;
}

const minutes =
Number(
goalMinutesInput.value
);

if (
!Number.isFinite(
minutes
) ||
minutes <= 0
) {
return;
}

state.goalSeconds =
minutes * 60;

saveState();

updateUI();

closeGoalModal();
};

loadState();

resetWeeklyPointsIfNeeded();

initializeSvgGradient();

renderHistory();

updateUI();

if (getCurrentUser()) {
showApp();
} else {
showLogin();
}

document.addEventListener(
'visibilitychange',
handleVisibilityChange
);

setInterval(
tick,
1000
);

if (goalSettingButton) {
goalSettingButton.addEventListener(
'click',
openGoalModal
);
}

if (closeGoalModalButton) {
closeGoalModalButton.addEventListener(
'click',
closeGoalModal
);
}

if (cancelGoalSettingButton) {
cancelGoalSettingButton.addEventListener(
'click',
closeGoalModal
);
}

if (saveGoalSettingButton) {
saveGoalSettingButton.addEventListener(
'click',
saveGoalTime
);
}

if (loginForm) {
loginForm.addEventListener(
'submit',
handleLogin
);
}

if (logoutButton) {
logoutButton.addEventListener(
'click',
handleLogout
);
}

if (goalModal) {
goalModal.addEventListener(
'click',
(event) => {
if (
event.target ===
goalModal
) {
closeGoalModal();
}
}
);
}

window.addEventListener(
'beforeunload',
() => {
calculateElapsedTime();
saveState();
}
);

