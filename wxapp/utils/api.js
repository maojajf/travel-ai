var auth = require("./auth.js");
var storage = require("./storage.js");

function getBaseUrl() {
  var app = typeof getApp === "function" ? getApp() : null;
  if (app && app.globalData && app.globalData.apiBaseUrl) {
    return app.globalData.apiBaseUrl;
  }
  return "http://127.0.0.1:8080/travel";
}

function createError(message, response) {
  var payload = response && response.data ? response.data : null;
  var error = new Error(message || (payload && payload.msg) || "请求失败");
  error.response = response || null;
  error.statusCode = response ? response.statusCode : 0;
  error.code = payload && typeof payload.code !== "undefined" ? payload.code : error.statusCode;
  error.data = payload;
  error.msg = payload && payload.msg ? payload.msg : error.message;
  return error;
}

function buildHeaders(extraHeaders) {
  var headers = Object.assign({
    "Content-Type": "application/json",
    "X-Device-Id": storage.ensureDeviceId()
  }, extraHeaders || {});
  var token = storage.getAuthToken();
  if (token) {
    headers["x-auth-token"] = token;
  }
  return headers;
}

function isWrappedResponse(payload) {
  return !!(payload && typeof payload === "object" && typeof payload.code !== "undefined" && Object.prototype.hasOwnProperty.call(payload, "data"));
}

function logRequestFailure(stage, options, detail) {
  var requestUrl = getBaseUrl() + ((options && options.url) || "");
  console.error("[api] request failed", {
    stage: stage || "",
    method: (options && options.method) || "GET",
    url: requestUrl,
    requestData: options && options.data,
    statusCode: detail && detail.statusCode,
    code: detail && detail.code,
    message: detail && detail.message,
    responseData: detail && detail.responseData,
    error: detail && detail.error
  });
}

function rawRequest(options) {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: getBaseUrl() + options.url,
      method: options.method || "GET",
      data: options.data,
      header: buildHeaders(options.header),
      success: resolve,
      fail: function(error) {
        logRequestFailure("network", options, {
          error: error,
          message: error && error.errMsg
        });
        reject(error);
      }
    });
  });
}

function request(options, retried) {
  var requireAuth = options.requireAuth !== false;
  var authFlow = requireAuth ? auth.ensureLogin() : Promise.resolve(storage.getAuthToken());

  return authFlow.then(function() {
    return rawRequest(options);
  }).then(function(response) {
    var payload = response && response.data;

    if (isWrappedResponse(payload)) {
      if (payload.code === 200) {
        return payload.data;
      }

      if (payload.code === 401 && !retried) {
        return auth.loginAndStoreToken(true).then(function() {
          return request(options, true);
        });
      }

      logRequestFailure("business", options, {
        statusCode: response.statusCode,
        code: payload.code,
        message: payload.msg,
        responseData: payload
      });
      throw createError(payload.msg, response);
    }

    if (response.statusCode === 401 && !retried) {
      return auth.loginAndStoreToken(true).then(function() {
        return request(options, true);
      });
    }

    if (response.statusCode >= 200 && response.statusCode < 300) {
      return payload;
    }

    logRequestFailure("http", options, {
      statusCode: response.statusCode,
      message: (payload && payload.msg) || "",
      responseData: payload
    });
    throw createError(null, response);
  });
}

function getRequestHeaders(options) {
  var requireAuth = !options || options.requireAuth !== false;
  var authFlow = requireAuth ? auth.ensureLogin() : Promise.resolve(storage.getAuthToken());
  return authFlow.then(function() {
    return buildHeaders(options && options.header);
  });
}

function getHistory(page, pageSize) {
  return request({
    url: "/routes/history",
    data: {
      page: page || 1,
      pageSize: pageSize || 10
    }
  });
}

function getFavorites(page, pageSize) {
  return request({
    url: "/routes/favorites",
    data: {
      page: page || 1,
      pageSize: pageSize || 10
    }
  });
}

function getRouteDetail(routeId) {
  return request({
    url: "/routes/" + routeId,
    requireAuth: false
  });
}

function createNum() {
  return request({
    url: "/routes/createNum",
    method: "POST"
  });
}

function favoriteRoute(routeId) {
  return request({
    url: "/routes/" + routeId + "/favorite",
    method: "POST"
  }).then(function() {
    return { favorite: true };
  });
}

function unfavoriteRoute(routeId) {
  return request({
    url: "/routes/" + routeId + "/favorite",
    method: "DELETE"
  }).then(function() {
    return { favorite: false };
  });
}

module.exports = {
  getBaseUrl: getBaseUrl,
  getRequestHeaders: getRequestHeaders,
  request: request,
  getHistory: getHistory,
  getFavorites: getFavorites,
  getRouteDetail: getRouteDetail,
  createNum: createNum,
  favoriteRoute: favoriteRoute,
  unfavoriteRoute: unfavoriteRoute
};
