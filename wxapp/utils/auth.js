var storage = require("./storage.js");

var loginPromise = null;

function getBaseUrl() {
  var app = typeof getApp === "function" ? getApp() : null;
  if (app && app.globalData && app.globalData.apiBaseUrl) {
    return app.globalData.apiBaseUrl;
  }
  return "http://127.0.0.1:8080/travel";
}

function wxLogin() {
  return new Promise(function(resolve, reject) {
    wx.login({
      success: function(result) {
        if (result && result.code) {
          resolve(result.code);
          return;
        }
        reject(new Error("微信登录失败"));
      },
      fail: reject
    });
  });
}

function requestToken(jsCode) {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: getBaseUrl() + "/user/regOrLogin",
      method: "POST",
      data: {
        jsCode: jsCode
      },
      header: {
        "Content-Type": "application/json",
        "X-Device-Id": storage.ensureDeviceId()
      },
      success: function(response) {
        var payload = response && response.data ? response.data : null;
        if (response.statusCode >= 200 && response.statusCode < 300 && payload && payload.code === 200 && payload.data) {
          resolve(String(payload.data));
          return;
        }

        var message = (payload && payload.msg) || "登录失败";
        var error = new Error(message);
        error.response = response;
        error.data = payload;
        reject(error);
      },
      fail: reject
    });
  });
}

function loginAndStoreToken(forceRefresh) {
  if (!forceRefresh) {
    var existingToken = storage.getAuthToken();
    if (existingToken) {
      return Promise.resolve(existingToken);
    }
  }

  if (loginPromise) {
    return loginPromise;
  }

  loginPromise = wxLogin()
    .then(function(jsCode) {
      return requestToken(jsCode);
    })
    .then(function(token) {
      storage.saveAuthToken(token);
      return token;
    })
    .catch(function(error) {
      storage.clearAuthToken();
      throw error;
    });

  return loginPromise.finally(function() {
    loginPromise = null;
  });
}

function ensureLogin() {
  var token = storage.getAuthToken();
  if (token) {
    return Promise.resolve(token);
  }
  return loginAndStoreToken(false);
}

function clearAuthToken() {
  storage.clearAuthToken();
}

module.exports = {
  ensureLogin: ensureLogin,
  loginAndStoreToken: loginAndStoreToken,
  clearAuthToken: clearAuthToken
};
