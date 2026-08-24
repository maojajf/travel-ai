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

function mapTravelGroup(value) {
  return GROUP_LABEL_MAP[value] || value || "";
}

function mapBudgetLevel(value) {
  return BUDGET_LABEL_MAP[value] || value || "";
}

function mapRouteList(list) {
  return (list || []).map(function(item) {
    return Object.assign({}, item, {
      travelGroup: mapTravelGroup(item.travelGroup),
      budgetLevel: mapBudgetLevel(item.budgetLevel)
    });
  });
}

module.exports = {
  mapTravelGroup: mapTravelGroup,
  mapBudgetLevel: mapBudgetLevel,
  mapRouteList: mapRouteList
};
