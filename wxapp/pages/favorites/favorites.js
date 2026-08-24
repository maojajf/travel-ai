var api = require("../../utils/api.js");
var auth = require("../../utils/auth.js");
var labels = require("../../utils/labels.js");

Page({
  data: {
    list: [],
    loading: true,
    loadingMore: false,
    page: 1,
    pageSize: 10,
    hasNextPage: true,
    loadFinished: false
  },

  onShow: function() {
    this.reloadList();
  },

  onReachBottom: function() {
    this.loadNextPage();
  },

  reloadList: function() {
    this.setData({
      list: [],
      loading: true,
      loadingMore: false,
      page: 1,
      pageSize: 10,
      hasNextPage: true,
      loadFinished: false
    });
    this.loadPage(1, false);
  },

  loadNextPage: function() {
    if (this.data.loading || this.data.loadingMore || !this.data.hasNextPage) {
      return;
    }
    this.loadPage(this.data.page + 1, true);
  },

  loadPage: function(page, append) {
    var that = this;
    if (append) {
      this.setData({
        loadingMore: true
      });
    } else {
      this.setData({
        loading: true
      });
    }

    auth.ensureLogin().then(function() {
      return api.getFavorites(page, that.data.pageSize);
    }).then(function(result) {
      var nextList = append ? that.data.list.concat(labels.mapRouteList(result.list || [])) : labels.mapRouteList(result.list || []);
      that.setData({
        list: nextList,
        loading: false,
        loadingMore: false,
        page: result.page || page,
        hasNextPage: !!result.hasNextPage,
        loadFinished: !(result && result.hasNextPage)
      });
    }).catch(function(error) {
      that.setData({
        loading: false,
        loadingMore: false,
        hasNextPage: false,
        loadFinished: that.data.list.length > 0
      });
      wx.showToast({
        title: (error && error.message) || "加载失败，请稍后重试",
        icon: "none"
      });
    });
  },

  openDetail: function(event) {
    var routeId = event.currentTarget.dataset.id;
    wx.navigateTo({
      url: "/pages/detail/detail?routeId=" + routeId + "&source=favorite"
    });
  }
});
