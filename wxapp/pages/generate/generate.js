var api = require("../../utils/api.js");
var auth = require("../../utils/auth.js");
var storage = require("../../utils/storage.js");

var GROUP_LABEL_MAP = {
  solo: "单人",
  couple: "情侣",
  family: "亲子",
  friends: "朋友"
};

var BUDGET_LABEL_MAP = {
  low: "低预算",
  medium: "中预算",
  high: "高预算"
};

var UI_TEXT = {
  navTitle: "漫游攻略",
  back: "返回",
  noPendingInput: "没有找到待生成的参数，请返回首页重新选择条件。",
  loginError: "登录失败，请稍后重试",
  streamInterrupted: "连接已中断，请稍后重新生成。",
  generateFailed: "生成失败，请稍后再试。",
  routeNotReady: "路线尚未生成完成",
  favoriteSuccess: "已收藏路线",
  unfavoriteSuccess: "已取消收藏",
  favoriteFailed: "收藏失败，请稍后再试",
  leaveTitle: "数据正在生成，确定要退出嘛",
  leaveDesc: "退出后数据将不会保留",
  leaveStay: "继续生成",
  leaveConfirm: "确定退出"
};

var MARKDOWN_FLUSH_INTERVAL = 50;
var SCROLL_RESUME_THRESHOLD = 80;
var SCROLL_TOP_THRESHOLD = 36;

function logGenerateRequestFailure(stage, detail) {
  console.error("[generate] request failed", {
    stage: stage || "",
    method: "POST",
    url: api.getBaseUrl() + "/routes/generate/stream",
    requestData: detail && detail.requestData,
    statusCode: detail && detail.statusCode,
    code: detail && detail.code,
    message: detail && detail.message,
    responseData: detail && detail.responseData,
    error: detail && detail.error
  });
}

function safeJsonParse(text) {
  try {
    return JSON.parse(text);
  } catch (error) {
    return null;
  }
}

function buildHeaderInfo(input, loading, summary) {
  var safeInput = input || {};
  return {
    statusText: loading ? "实时生成中" : "路线文档已生成",
    province: safeInput.province || "",
    city: safeInput.city || "",
    startAt: safeInput.startDate && safeInput.startTime ? safeInput.startDate + " " + safeInput.startTime : "",
    endAt: safeInput.endDate && safeInput.endTime ? safeInput.endDate + " " + safeInput.endTime : "",
    travelGroupLabel: GROUP_LABEL_MAP[safeInput.travelGroup] || "",
    budgetLabel: BUDGET_LABEL_MAP[safeInput.budgetLevel] || "",
    summary: summary || ""
  };
}

function getNavigationMetrics() {
  var windowInfo = wx.getWindowInfo ? wx.getWindowInfo() : {};
  var menuRect = wx.getMenuButtonBoundingClientRect ? wx.getMenuButtonBoundingClientRect() : null;
  var statusBarHeight = windowInfo.statusBarHeight || 20;
  var navTop = statusBarHeight;
  var navHeight = 44;
  var navBarHeight = statusBarHeight + 44;

  if (menuRect && menuRect.top && menuRect.height) {
    navTop = menuRect.top;
    navHeight = menuRect.height;
    navBarHeight = menuRect.bottom + menuRect.top - statusBarHeight;
  }

  return {
    navTop: navTop,
    navHeight: navHeight,
    navBarHeight: navBarHeight
  };
}

Page({
  data: {
    input: null,
    markdownText: "",
    loading: true,
    routeId: "",
    favorite: false,
    summary: "",
    createdAt: "",
    errorText: "",
    headerInfo: buildHeaderInfo(null, true, ""),
    autoFollow: true,
    scrollIntoView: "",
    scrollViewportHeight: 0,
    isNearBottom: true,
    isNearTop: true,
    generationFinished: false,
    leaveDialogVisible: false,
    navTop: 20,
    navHeight: 44,
    navBarHeight: 64,
    uiText: UI_TEXT
  },

  onLoad: function() {
    var metrics = getNavigationMetrics();

    this.decoder = typeof TextDecoder !== "undefined" ? new TextDecoder("utf-8") : null;
    this.sseBuffer = "";
    this.pendingMarkdownChunks = [];
    this.markdownTextBuffer = "";
    this.flushTimer = null;
    this.scrollTimer = null;
    this.lastScrollTop = 0;
    this.activeRequestId = 0;
    this.isLeaving = false;

    this.setData(metrics);

    var input = storage.getPendingGeneration();
    if (!input) {
      this.setData({
        loading: false,
        errorText: UI_TEXT.noPendingInput,
        headerInfo: buildHeaderInfo(null, false, "")
      });
      return;
    }

    this.setData({
      input: input,
      headerInfo: buildHeaderInfo(input, true, "")
    });
    this.startGeneration();
  },

  onReady: function() {
    this.measureScrollViewport();
  },

  onUnload: function() {
    this.clearAsyncState();
    this.abortRequest();
    if (this.isLeaving) {
      storage.clearPendingGeneration();
    }
  },

  clearAsyncState: function() {
    if (this.flushTimer) {
      clearTimeout(this.flushTimer);
      this.flushTimer = null;
    }
    if (this.scrollTimer) {
      clearTimeout(this.scrollTimer);
      this.scrollTimer = null;
    }
  },

  abortRequest: function() {
    if (this.requestTask && this.requestTask.abort) {
      this.requestTask.abort();
    }
    this.requestTask = null;
  },

  measureScrollViewport: function() {
    var that = this;
    wx.createSelectorQuery()
      .in(this)
      .select(".doc-scroll")
      .boundingClientRect(function(rect) {
        if (!rect || !rect.height) {
          return;
        }
        that.setData({
          scrollViewportHeight: rect.height
        });
      })
      .exec();
  },

  decodeChunk: function(arrayBuffer) {
    if (this.decoder) {
      return this.decoder.decode(arrayBuffer, { stream: true });
    }
    var bytes = new Uint8Array(arrayBuffer);
    var result = "";
    var index = 0;
    for (index = 0; index < bytes.length; index += 1) {
      result += String.fromCharCode(bytes[index]);
    }
    return decodeURIComponent(escape(result));
  },

  setGenerationTerminalState: function(patch, callback) {
    storage.clearPendingGeneration();
    this.setData(patch, callback);
  },

  startGeneration: function() {
    var that = this;
    this.isLeaving = false;
    this.setData({
      routeId: "",
      favorite: false,
      generationFinished: false,
      leaveDialogVisible: false
    });

    auth.ensureLogin().then(function() {
      return api.createNum();
    }).then(function() {
      that.startGenerationRequest(false);
    }).catch(function(error) {
      that.setGenerationTerminalState({
        loading: false,
        routeId: "",
        favorite: false,
        generationFinished: false,
        errorText: (error && error.message) || UI_TEXT.loginError,
        headerInfo: buildHeaderInfo(that.data.input, false, "")
      });
    });
  },

  startGenerationRequest: function(retriedLogin) {
    var that = this;
    var input = this.data.input;
    var requestId = this.activeRequestId + 1;

    this.abortRequest();

    this.activeRequestId = requestId;
    this.clearAsyncState();
    this.sseBuffer = "";
    this.pendingMarkdownChunks = [];
    this.markdownTextBuffer = "";
    this.lastScrollTop = 0;

    this.setData({
      markdownText: "",
      loading: true,
      routeId: "",
      favorite: false,
      summary: "",
      createdAt: "",
      errorText: "",
      autoFollow: true,
      scrollIntoView: "",
      isNearBottom: true,
      isNearTop: true,
      generationFinished: false,
      leaveDialogVisible: false,
      headerInfo: buildHeaderInfo(input, true, "")
    }, function() {
      that.measureScrollViewport();
    });

    api.getRequestHeaders({
      requireAuth: true,
      header: {
        Accept: "text/event-stream"
      }
    }).then(function(headers) {
      var requestTask = wx.request({
        url: api.getBaseUrl() + "/routes/generate/stream",
        method: "POST",
        enableChunked: true,
        responseType: "arraybuffer",
        data: input,
        header: headers,
        success: function(response) {
          var wrappedPayload;
          if (requestId !== that.activeRequestId || that.isLeaving) {
            return;
          }

          wrappedPayload = that.parseWrappedPayload(response.data);
          if (wrappedPayload) {
            that.handleWrappedStreamResponse(wrappedPayload, retriedLogin);
            return;
          }

          if (!requestTask.onChunkReceived && response.data) {
            that.handleChunk(response.data);
          }
          that.consumeSseBuffer(true);
        },
        fail: function() {
          if (requestId !== that.activeRequestId || that.isLeaving) {
            return;
          }
          logGenerateRequestFailure("network", {
            requestData: input
          });
          that.flushPendingMarkdown(true);
          that.setGenerationTerminalState({
            loading: false,
            routeId: "",
            favorite: false,
            errorText: UI_TEXT.streamInterrupted,
            autoFollow: true,
            isNearBottom: true,
            isNearTop: true,
            generationFinished: false,
            headerInfo: buildHeaderInfo(input, false, "")
          }, function() {
            that.scrollToBottom(true);
          });
        }
      });

      that.requestTask = requestTask;
      if (requestTask.onChunkReceived) {
        requestTask.onChunkReceived(function(event) {
          if (requestId !== that.activeRequestId || that.isLeaving) {
            return;
          }
          that.handleChunk(event.data);
        });
      }
    }).catch(function(error) {
      that.setGenerationTerminalState({
        loading: false,
        routeId: "",
        favorite: false,
        generationFinished: false,
        errorText: (error && error.message) || UI_TEXT.loginError,
        headerInfo: buildHeaderInfo(input, false, "")
      });
    });
  },

  parseWrappedPayload: function(arrayBuffer) {
    if (!arrayBuffer) {
      return null;
    }

    var text = this.decodeChunk(arrayBuffer);
    var trimmed = text ? text.trim() : "";
    if (!trimmed || trimmed.charAt(0) !== "{") {
      return null;
    }
    return safeJsonParse(trimmed);
  },

  handleWrappedStreamResponse: function(payload, retriedLogin) {
    var that = this;
    if (!payload) {
      return;
    }

    if (payload.code === 401 && !retriedLogin) {
      auth.loginAndStoreToken(true).then(function() {
        that.startGenerationRequest(true);
      }).catch(function(error) {
        logGenerateRequestFailure("login", {
          requestData: that.data.input,
          message: error && error.message,
          error: error
        });
        that.setGenerationTerminalState({
          loading: false,
          routeId: "",
          favorite: false,
          generationFinished: false,
          errorText: (error && error.message) || UI_TEXT.loginError,
          headerInfo: buildHeaderInfo(that.data.input, false, "")
        });
      });
      return;
    }

    logGenerateRequestFailure("wrapped-response", {
      requestData: this.data.input,
      statusCode: payload.statusCode,
      code: payload.code,
      message: payload.msg,
      responseData: payload
    });
    this.setGenerationTerminalState({
      loading: false,
      routeId: "",
      favorite: false,
      errorText: payload.msg || UI_TEXT.generateFailed,
      autoFollow: true,
      isNearBottom: true,
      isNearTop: true,
      generationFinished: false,
      headerInfo: buildHeaderInfo(this.data.input, false, "")
    }, this.scrollToBottom.bind(this, true));
  },

  handleChunk: function(arrayBuffer) {
    var text = this.decodeChunk(arrayBuffer).replace(/\r\n/g, "\n");
    this.sseBuffer += text;
    this.consumeSseBuffer(false);
  },

  consumeSseBuffer: function(flushRemainder) {
    var separatorIndex = this.sseBuffer.indexOf("\n\n");

    while (separatorIndex !== -1) {
      this.processEvent(this.sseBuffer.slice(0, separatorIndex).trim());
      this.sseBuffer = this.sseBuffer.slice(separatorIndex + 2);
      separatorIndex = this.sseBuffer.indexOf("\n\n");
    }

    if (flushRemainder && this.sseBuffer.trim()) {
      this.processEvent(this.sseBuffer.trim());
      this.sseBuffer = "";
    }
  },

  processEvent: function(rawEvent) {
    if (!rawEvent) {
      return;
    }

    var lines = rawEvent.split("\n");
    var eventName = "message";
    var dataLines = [];

    lines.forEach(function(line) {
      if (line.indexOf("event:") === 0) {
        eventName = line.slice(6).trim();
      }
      if (line.indexOf("data:") === 0) {
        dataLines.push(line.slice(5).trim());
      }
    });

    var payloadText = dataLines.join("\n");
    if (!payloadText || payloadText === "[DONE]") {
      return;
    }

    var payload = safeJsonParse(payloadText);
    if (!payload) {
      return;
    }

    if (eventName === "meta") {
      return;
    }

    if (eventName === "delta") {
      var nextChunk = String(payload.chunk || "")
        .replace(/\\r\\n/g, "\n")
        .replace(/\\n/g, "\n")
        .replace(/\\r/g, "\n")
        .replace(/\r\n/g, "\n")
        .replace(/\r/g, "\n");
      this.enqueueMarkdownChunk(nextChunk);
      return;
    }

    if (eventName === "done") {
      this.flushPendingMarkdown(true);
      this.setGenerationTerminalState({
        loading: false,
        routeId: payload.routeId || this.data.routeId || "",
        summary: payload.summary || "",
        createdAt: payload.createdAt || "",
        favorite: !!payload.favorite,
        autoFollow: true,
        isNearBottom: true,
        generationFinished: true,
        headerInfo: buildHeaderInfo(this.data.input, false, payload.summary || "")
      }, this.scrollToBottom.bind(this, true));
      return;
    }

    if (eventName === "error") {
      logGenerateRequestFailure("sse-event", {
        requestData: this.data.input,
        message: payload.message,
        responseData: payload
      });
      this.flushPendingMarkdown(true);
      this.setGenerationTerminalState({
        loading: false,
        routeId: "",
        favorite: false,
        errorText: payload.message || UI_TEXT.generateFailed,
        autoFollow: true,
        isNearBottom: true,
        generationFinished: false,
        headerInfo: buildHeaderInfo(this.data.input, false, "")
      }, this.scrollToBottom.bind(this, true));
    }
  },

  enqueueMarkdownChunk: function(chunk) {
    if (!chunk) {
      return;
    }

    this.pendingMarkdownChunks.push(chunk);
    if (this.flushTimer) {
      return;
    }

    var that = this;
    this.flushTimer = setTimeout(function() {
      that.flushTimer = null;
      that.flushPendingMarkdown();
    }, MARKDOWN_FLUSH_INTERVAL);
  },

  flushPendingMarkdown: function(forceScroll) {
    if (this.flushTimer) {
      clearTimeout(this.flushTimer);
      this.flushTimer = null;
    }

    if (!this.pendingMarkdownChunks.length) {
      if (forceScroll) {
        this.scrollToBottom(true);
      }
      return;
    }

    this.markdownTextBuffer += this.pendingMarkdownChunks.join("");
    this.pendingMarkdownChunks = [];

    var that = this;
    this.setData({
      markdownText: this.markdownTextBuffer
    }, function() {
      if (that.data.autoFollow || forceScroll) {
        that.scrollToBottom(!!forceScroll);
      }
    });
  },

  scrollToBottom: function(force) {
    var that = this;
    if (!this.data.autoFollow && !force) {
      return;
    }

    if (this.scrollTimer) {
      clearTimeout(this.scrollTimer);
    }

    this.setData({
      scrollIntoView: ""
    });

    this.scrollTimer = setTimeout(function() {
      that.setData({
        scrollIntoView: "markdown-bottom"
      });
    }, 0);
  },

  scrollToTop: function() {
    var that = this;
    if (this.scrollTimer) {
      clearTimeout(this.scrollTimer);
    }
    this.setData({
      autoFollow: false,
      scrollIntoView: ""
    });
    this.scrollTimer = setTimeout(function() {
      that.setData({
        scrollIntoView: "doc-top"
      });
    }, 0);
  },

  handleScroll: function(event) {
    var detail = event.detail || {};
    var scrollTop = detail.scrollTop || 0;
    var scrollHeight = detail.scrollHeight || 0;
    var viewportHeight = this.data.scrollViewportHeight || 0;
    var nearBottom = !viewportHeight || scrollTop + viewportHeight >= scrollHeight - SCROLL_RESUME_THRESHOLD;
    var nextState = {
      isNearBottom: !!nearBottom,
      isNearTop: scrollTop <= SCROLL_TOP_THRESHOLD
    };

    if (this.data.autoFollow && scrollTop + 8 < this.lastScrollTop && !nearBottom) {
      nextState.autoFollow = false;
    }

    this.lastScrollTop = scrollTop;
    this.setData(nextState);
  },

  jumpToLatest: function() {
    this.setData({
      autoFollow: true,
      isNearBottom: true
    }, this.scrollToBottom.bind(this, true));
  },

  jumpToTop: function() {
    this.setData({
      isNearTop: true
    }, this.scrollToTop.bind(this));
  },

  toggleFavorite: function() {
    var that = this;

    if (!this.data.generationFinished || !this.data.routeId) {
      wx.showToast({
        title: UI_TEXT.routeNotReady,
        icon: "none"
      });
      return;
    }

    var action = this.data.favorite ? api.unfavoriteRoute : api.favoriteRoute;

    action(this.data.routeId).then(function(result) {
      that.setData({
        favorite: !!result.favorite
      });
      wx.showToast({
        title: result.favorite ? UI_TEXT.favoriteSuccess : UI_TEXT.unfavoriteSuccess,
        icon: "none"
      });
    }).catch(function(error) {
      wx.showToast({
        title: (error && error.message) || UI_TEXT.favoriteFailed,
        icon: "none"
      });
    });
  },

  regenerate: function() {
    this.startGeneration();
  },

  handleBackTap: function() {
    if (this.data.loading) {
      this.setData({
        leaveDialogVisible: true
      });
      return;
    }

    this.leavePage();
  },

  hideLeaveDialog: function() {
    this.setData({
      leaveDialogVisible: false
    });
  },

  confirmLeave: function() {
    this.setData({
      leaveDialogVisible: false
    });
    this.leavePage();
  },

  leavePage: function() {
    var pages = getCurrentPages();
    var that = this;

    this.isLeaving = true;
    this.activeRequestId += 1;
    this.clearAsyncState();
    this.abortRequest();
    storage.clearPendingGeneration();

    if (pages.length > 1) {
      wx.navigateBack({
        delta: 1,
        fail: function() {
          that.isLeaving = false;
          wx.reLaunch({
            url: "/pages/home/home"
          });
        }
      });
      return;
    }

    wx.reLaunch({
      url: "/pages/home/home"
    });
  },

  noop: function() {
  }
});
