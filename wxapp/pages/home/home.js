var api = require("../../utils/api.js");
var auth = require("../../utils/auth.js");
var storage = require("../../utils/storage.js");
var timeUtils = require("../../utils/time.js");
var AREA_REQUEST_TIMEOUT = 3000;
var fallbackAreaPackage = null;

var groupOptions = [
  { label: "单人", value: "solo" },
  { label: "情侣", value: "couple" },
  { label: "亲子", value: "family" },
  { label: "朋友", value: "friends" }
];

var budgetOptions = [
  { label: "低预算", value: "low" },
  { label: "中预算", value: "medium" },
  { label: "高预算", value: "high" }
];

var quickDurationOptions = [
  { key: "1d", label: "1天" },
  { key: "2d1n", label: "2天1夜" },
  { key: "3d2n", label: "3天2夜" },
  { key: "5d4n", label: "5天4夜" },
  { key: "7d", label: "7天" }
];

function buildFallbackAreaTree() {
  if (!fallbackAreaPackage) {
    fallbackAreaPackage = require("../../data/china-city-pack.json");
  }

  return ((fallbackAreaPackage && fallbackAreaPackage.items) || []).map(function(item) {
    return {
      province: item[0],
      code: "",
      cities: (item[1] || []).map(function(cityName) {
        return {
          name: cityName,
          code: ""
        };
      })
    };
  });
}

function requestWithTimeout(promise, timeoutMs, message) {
  return new Promise(function(resolve, reject) {
    var settled = false;
    var timer = setTimeout(function() {
      if (settled) {
        return;
      }
      settled = true;
      reject(new Error(message || "请求超时"));
    }, timeoutMs);

    promise.then(function(result) {
      if (settled) {
        return;
      }
      settled = true;
      clearTimeout(timer);
      resolve(result);
    }).catch(function(error) {
      if (settled) {
        return;
      }
      settled = true;
      clearTimeout(timer);
      reject(error);
    });
  });
}

function normalizeAreaTree(areaTree) {
  return (areaTree || [])
    .map(function(province) {
      return {
        province: province.name,
        code: province.code,
        cities: (province.children || [])
          .filter(function(city) {
            return city && city.name;
          })
          .map(function(city) {
            return {
              name: city.name,
              code: city.code
            };
          })
      };
    })
    .filter(function(item) {
      return item.province && item.cities.length;
    });
}

function buildCityColumns(areaTree, provinceIndex) {
  var safeTree = areaTree && areaTree.length ? areaTree : [];
  if (!safeTree.length) {
    return [[], []];
  }
  var safeProvinceIndex = safeTree[provinceIndex] ? provinceIndex : 0;

  return [
    safeTree.map(function(item) {
      return item.province;
    }),
    (safeTree[safeProvinceIndex].cities || []).map(function(item) {
      return item.name;
    })
  ];
}

function normalizeCityIndexes(areaTree, indexes) {
  var safeTree = areaTree && areaTree.length ? areaTree : [];
  if (!safeTree.length) {
    return [0, 0];
  }

  var provinceIndex = safeTree[indexes && indexes[0]] ? indexes[0] : 0;
  var cities = safeTree[provinceIndex].cities || [];
  var cityIndex = cities[indexes && indexes[1]] ? indexes[1] : 0;

  return [provinceIndex, cityIndex];
}

function buildSelectedCityInfo(areaTree, indexes) {
  var safeIndexes = normalizeCityIndexes(areaTree, indexes);
  var province = areaTree && areaTree[safeIndexes[0]] ? areaTree[safeIndexes[0]] : null;
  var city = province && province.cities[safeIndexes[1]] ? province.cities[safeIndexes[1]] : null;

  return {
    indexes: safeIndexes,
    province: province ? province.province : "",
    city: city ? city.name : ""
  };
}

function clampEndDateRange(startDate, endDate) {
  var maxEndDate = timeUtils.addDays(startDate, 9);
  var nextEndDate = endDate;
  var overMax = false;

  if (nextEndDate < startDate) {
    nextEndDate = startDate;
  }
  if (nextEndDate > maxEndDate) {
    nextEndDate = maxEndDate;
    overMax = true;
  }

  return {
    maxEndDate: maxEndDate,
    endDate: nextEndDate,
    overMax: overMax
  };
}

Page({
  data: {
    areaTree: [],
    cityColumns: [[], []],
    cityIndexes: [0, 0],
    pickerCityIndexes: [0, 0],
    selectedProvinceName: "",
    selectedCityName: "",
    groupOptions: groupOptions,
    budgetOptions: budgetOptions,
    quickDurationOptions: quickDurationOptions,
    groupIndex: 3,
    budgetIndex: 1,
    minStartDate: timeUtils.formatDate(new Date()),
    maxEndDate: timeUtils.addDays(timeUtils.formatDate(new Date()), 9),
    startDate: timeUtils.formatDate(new Date()),
    startTime: "09:00",
    endDate: timeUtils.addDays(timeUtils.formatDate(new Date()), 1),
    endTime: "18:00",
    timeOptions: timeUtils.buildTimeOptions(),
    timeIndexMap: {
      start: 6,
      end: 24
    },
    areaLoading: false,
    pageTransitioning: false,
    pageTransitionText: "页面跳转中..."
  },

  onLoad: function() {
    storage.ensureDeviceId();
    this.loadAreaTree();
  },

  onShow: function() {
    if (this.data.pageTransitioning) {
      this.setTransitionState(false);
    }
  },

  loadAreaTree: function() {
    var that = this;
    this.setData({
      areaLoading: true
    });

    requestWithTimeout(api.request({
      url: "/areaCode/tree",
      method: "POST",
      data: {
        level: 2
      },
      requireAuth: false
    }), AREA_REQUEST_TIMEOUT, "地区数据加载超时").then(function(result) {
      var normalizedTree = normalizeAreaTree(result);
      if (!normalizedTree.length) {
        throw new Error("地区数据为空");
      }

      that.setData({
        areaTree: normalizedTree,
        cityColumns: buildCityColumns(normalizedTree, 0),
        cityIndexes: [0, 0],
        pickerCityIndexes: [0, 0],
        selectedProvinceName: normalizedTree[0].province,
        selectedCityName: normalizedTree[0].cities[0] ? normalizedTree[0].cities[0].name : "",
        areaLoading: false
      });
    }).catch(function() {
      var fallbackTree = buildFallbackAreaTree();
      that.setData({
        areaTree: fallbackTree,
        cityColumns: buildCityColumns(fallbackTree, 0),
        cityIndexes: [0, 0],
        pickerCityIndexes: [0, 0],
        selectedProvinceName: fallbackTree[0] ? fallbackTree[0].province : "",
        selectedCityName: fallbackTree[0] && fallbackTree[0].cities[0] ? fallbackTree[0].cities[0].name : "",
        areaLoading: false
      });
    });
  },

  handleCityChange: function(event) {
    var selection = buildSelectedCityInfo(this.data.areaTree, event.detail.value);
    this.setData({
      cityIndexes: selection.indexes,
      pickerCityIndexes: selection.indexes,
      cityColumns: buildCityColumns(this.data.areaTree, selection.indexes[0]),
      selectedProvinceName: selection.province,
      selectedCityName: selection.city
    });
  },

  handleCityColumnChange: function(event) {
    var column = event.detail.column;
    var value = event.detail.value;
    var current = this.data.pickerCityIndexes.slice();
    var areaTree = this.data.areaTree && this.data.areaTree.length ? this.data.areaTree : [];

    if (!areaTree.length) {
      return;
    }

    current[column] = value;
    if (column === 0) {
      current[1] = 0;
      this.setData({
        cityColumns: buildCityColumns(areaTree, value),
        pickerCityIndexes: current
      });
      return;
    }

    this.setData({
      pickerCityIndexes: current
    });
  },

  handleCityCancel: function() {
    this.setData({
      pickerCityIndexes: this.data.cityIndexes.slice(),
      cityColumns: buildCityColumns(this.data.areaTree, this.data.cityIndexes[0] || 0)
    });
  },

  handleGroupChange: function(event) {
    this.setData({
      groupIndex: Number(event.detail.value)
    });
  },

  handleBudgetChange: function(event) {
    this.setData({
      budgetIndex: Number(event.detail.value)
    });
  },

  handleStartDateChange: function(event) {
    var startDate = event.detail.value;
    var rangeState = clampEndDateRange(startDate, this.data.endDate);
    this.setData({
      startDate: startDate,
      endDate: rangeState.endDate,
      maxEndDate: rangeState.maxEndDate
    });
  },

  handleEndDateChange: function(event) {
    var rangeState = clampEndDateRange(this.data.startDate, event.detail.value);
    this.setData({
      endDate: rangeState.endDate,
      maxEndDate: rangeState.maxEndDate
    });
    if (rangeState.overMax) {
      wx.showToast({
        title: "最长只能选择9天",
        icon: "none"
      });
    }
  },

  handleStartTimeChange: function(event) {
    this.setData({
      startTime: event.detail.value
    });
  },

  handleEndTimeChange: function(event) {
    this.setData({
      endTime: event.detail.value
    });
  },

  applyQuickDuration: function(event) {
    var key = event.currentTarget.dataset.key;
    var updated = timeUtils.applyQuickDuration({
      startDate: this.data.startDate,
      startTime: this.data.startTime,
      endDate: this.data.endDate,
      endTime: this.data.endTime
    }, key);
    this.setData({
      endDate: updated.endDate,
      endTime: updated.endTime
    });
  },

  createPayload: function() {
    var provinceIndex = this.data.cityIndexes[0];
    var cityIndex = this.data.cityIndexes[1];
    var areaTree = this.data.areaTree && this.data.areaTree.length ? this.data.areaTree : [];
    var province = areaTree[provinceIndex] || areaTree[0];
    var city = province && province.cities[cityIndex] ? province.cities[cityIndex] : null;

    return {
      province: province ? province.province : "",
      city: city ? city.name : "",
      startDate: this.data.startDate,
      startTime: this.data.startTime,
      endDate: this.data.endDate,
      endTime: this.data.endTime,
      travelGroup: groupOptions[this.data.groupIndex].value,
      budgetLevel: budgetOptions[this.data.budgetIndex].value
    };
  },

  setTransitionState: function(loading, text) {
    this.setData({
      pageTransitioning: !!loading,
      pageTransitionText: text || "页面跳转中..."
    });
  },

  openPage: function(url) {
    var that = this;
    wx.navigateTo({
      url: url,
      fail: function() {
        that.setTransitionState(false);
        wx.showToast({
          title: "打开页面失败，请稍后重试",
          icon: "none"
        });
      }
    });
  },

  ensureReadyThen: function(text, action) {
    var that = this;
    this.setTransitionState(true, text);
    return auth.ensureLogin().then(function() {
      action();
    }).catch(function(error) {
      that.setTransitionState(false);
      wx.showToast({
        title: (error && error.message) || "登录失败，请稍后重试",
        icon: "none"
      });
    });
  },

  generateRoute: function() {
    var payload = this.createPayload();
    if (!payload.city) {
      wx.showToast({
        title: "请选择城市",
        icon: "none"
      });
      return;
    }

    this.ensureReadyThen("正在进入路线生成页...", function() {
      storage.savePendingGeneration(payload);
      this.openPage("/pages/generate/generate");
    }.bind(this));
  },

  goHistory: function() {
    this.ensureReadyThen("正在打开历史路线...", function() {
      this.openPage("/pages/history/history");
    }.bind(this));
  },

  goFavorites: function() {
    this.ensureReadyThen("正在打开收藏路线...", function() {
      this.openPage("/pages/favorites/favorites");
    }.bind(this));
  }
});
