var DEVICE_KEY = "travel_device_id";
var PENDING_GENERATION_KEY = "travel_pending_generation";
var AUTH_TOKEN_KEY = "travel_auth_token";

function ensureDeviceId() {
  var deviceId = wx.getStorageSync(DEVICE_KEY);
  if (!deviceId) {
    deviceId = "device-" + Date.now() + "-" + Math.random().toString(16).slice(2, 10);
    wx.setStorageSync(DEVICE_KEY, deviceId);
  }
  return deviceId;
}

function savePendingGeneration(payload) {
  wx.setStorageSync(PENDING_GENERATION_KEY, payload);
}

function getPendingGeneration() {
  return wx.getStorageSync(PENDING_GENERATION_KEY) || null;
}

function clearPendingGeneration() {
  wx.removeStorageSync(PENDING_GENERATION_KEY);
}

function getAuthToken() {
  return wx.getStorageSync(AUTH_TOKEN_KEY) || "";
}

function saveAuthToken(token) {
  if (!token) {
    clearAuthToken();
    return;
  }
  wx.setStorageSync(AUTH_TOKEN_KEY, token);
}

function clearAuthToken() {
  wx.removeStorageSync(AUTH_TOKEN_KEY);
}

module.exports = {
  ensureDeviceId: ensureDeviceId,
  savePendingGeneration: savePendingGeneration,
  getPendingGeneration: getPendingGeneration,
  clearPendingGeneration: clearPendingGeneration,
  getAuthToken: getAuthToken,
  saveAuthToken: saveAuthToken,
  clearAuthToken: clearAuthToken
};
