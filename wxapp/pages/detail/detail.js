var api = require("../../utils/api.js");
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

var PAGE_BOTTOM_THRESHOLD = 96;

function buildHeaderInfo(detail) {
  var safeDetail = detail || {};
  return {
    statusText: "路线文档已生成",
    province: safeDetail.province || "",
    city: safeDetail.city || "",
    startAt: safeDetail.startAt || "",
    endAt: safeDetail.endAt || "",
    travelGroupLabel: GROUP_LABEL_MAP[safeDetail.travelGroup] || "",
    budgetLabel: BUDGET_LABEL_MAP[safeDetail.budgetLevel] || "",
    summary: safeDetail.summary || ""
  };
}

Page({
  data: {
    routeId: "",
    source: "",
    detail: null,
    markdownText: "",
    loading: true,
    errorText: "",
    showRegenerate: true,
    showFavoriteButton: true,
    isNearBottom: true,
    isNearTop: true,
    headerInfo: buildHeaderInfo(null)
  },

  onLoad: function(options) {
    var routeId = options && options.routeId ? options.routeId : "";
    var source = options && options.source ? options.source : "";

    this.bottomCheckTimer = null;

    this.setData({
      routeId: routeId,
      source: source,
      showRegenerate: source !== "history" && source !== "favorite",
      showFavoriteButton: source !== "favorite"
    });
    this.loadDetail();
  },

  onUnload: function() {
    if (this.bottomCheckTimer) {
      clearTimeout(this.bottomCheckTimer);
      this.bottomCheckTimer = null;
    }
  },

  onShow: function() {
    if (this.data.routeId) {
      this.loadDetail();
    }
  },

  loadDetail: function() {
    var that = this;
    if (!this.data.routeId) {
      this.setData({
        loading: false,
        errorText: "缺少路线标识。",
        headerInfo: buildHeaderInfo(null)
      });
      return;
    }

    api.getRouteDetail(this.data.routeId).then(function(detail) {
      that.setData({
        detail: detail,
        markdownText: detail.contentMarkdown || "",
        loading: false,
        errorText: "",
        headerInfo: buildHeaderInfo(detail)
      }, function() {
        that.queueBottomStateCheck();
      });
    }).catch(function() {
      that.setData({
        loading: false,
        errorText: "详情加载失败，请稍后再试。"
      });
    });
  },

  queueBottomStateCheck: function() {
    var that = this;
    if (!this.data.markdownText) {
      this.setData({
        isNearBottom: true,
        isNearTop: true
      });
      return;
    }

    if (this.bottomCheckTimer) {
      return;
    }

    this.bottomCheckTimer = setTimeout(function() {
      that.bottomCheckTimer = null;
      that.updateBottomState();
    }, 16);
  },

  updateBottomState: function() {
    var that = this;
    wx.createSelectorQuery()
      .in(this)
      .selectViewport()
      .boundingClientRect()
      .select("#detail-bottom")
      .boundingClientRect(function(rect) {
        if (!rect) {
          return;
        }

        var viewportHeight = wx.getWindowInfo ? wx.getWindowInfo().windowHeight : 0;
        var nearBottom = viewportHeight > 0
          ? rect.bottom <= viewportHeight + PAGE_BOTTOM_THRESHOLD
          : true;

        that.setData({
          isNearBottom: nearBottom
        });
      })
      .exec();
  },

  jumpToTop: function() {
    var that = this;
    wx.pageScrollTo({
      selector: "#detail-top",
      duration: 220,
      success: function() {
        that.setData({
          isNearTop: true
        });
      }
    });
  },

  jumpToLatest: function() {
    var that = this;
    wx.pageScrollTo({
      selector: "#detail-bottom",
      duration: 220,
      success: function() {
        that.setData({
          isNearBottom: true
        });
      }
    });
  },

  onPageScroll: function(event) {
    var scrollTop = (event && event.scrollTop) || 0;
    this.setData({
      isNearTop: scrollTop <= 36
    });
    this.queueBottomStateCheck();
  },

  toggleFavorite: function() {
    var that = this;
    if (!this.data.detail) {
      wx.showToast({
        title: "路线详情尚未加载完成",
        icon: "none"
      });
      return;
    }

    var action = this.data.detail.favorite ? api.unfavoriteRoute : api.favoriteRoute;

    action(this.data.routeId).then(function(result) {
      that.setData({
        "detail.favorite": !!result.favorite
      });
      wx.showToast({
        title: result.favorite ? "已收藏路线" : "已取消收藏",
        icon: "none"
      });
    }).catch(function(error) {
      wx.showToast({
        title: (error && error.message) || "收藏失败，请稍后再试",
        icon: "none"
      });
    });
  },

  regenerate: function() {
    if (!this.data.detail || !this.data.showRegenerate) {
      return;
    }

    var startParts = (this.data.detail.startAt || "").split(" ");
    var endParts = (this.data.detail.endAt || "").split(" ");
    storage.savePendingGeneration({
      province: this.data.detail.province,
      city: this.data.detail.city,
      startDate: startParts[0] || "",
      startTime: startParts[1] || "",
      endDate: endParts[0] || "",
      endTime: endParts[1] || "",
      travelGroup: this.data.detail.travelGroup,
      budgetLevel: this.data.detail.budgetLevel
    });
    wx.navigateTo({
      url: "/pages/generate/generate"
    });
  }
});
