function pad(value) {
  return String(value).padStart(2, "0");
}

function formatDate(date) {
  return date.getFullYear() + "-" + pad(date.getMonth() + 1) + "-" + pad(date.getDate());
}

function addDays(dateString, days) {
  var date = new Date(dateString.replace(/-/g, "/"));
  date.setDate(date.getDate() + days);
  return formatDate(date);
}

function buildTimeOptions() {
  var list = [];
  for (var hour = 6; hour <= 23; hour += 1) {
    list.push(pad(hour) + ":00");
    if (hour !== 23) {
      list.push(pad(hour) + ":30");
    }
  }
  return list;
}

function applyQuickDuration(form, durationKey) {
  var result = Object.assign({}, form);
  if (durationKey === "1d") {
    result.endDate = result.startDate;
    result.endTime = "21:00";
  } else if (durationKey === "2d1n") {
    result.endDate = addDays(result.startDate, 1);
    result.endTime = "18:00";
  } else if (durationKey === "3d2n") {
    result.endDate = addDays(result.startDate, 2);
    result.endTime = "18:00";
  } else if (durationKey === "5d4n") {
    result.endDate = addDays(result.startDate, 4);
    result.endTime = "18:00";
  } else if (durationKey === "7d") {
    result.endDate = addDays(result.startDate, 6);
    result.endTime = "18:00";
  }
  return result;
}

module.exports = {
  formatDate: formatDate,
  addDays: addDays,
  buildTimeOptions: buildTimeOptions,
  applyQuickDuration: applyQuickDuration
};
