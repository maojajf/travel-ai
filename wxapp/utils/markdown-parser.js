var LABEL_META = {
  "景点": { tone: "spot", iconType: "spot" },
  "美食": { tone: "food", iconType: "food" },
  "交通": { tone: "transport", iconType: "transport" },
  "建议": { tone: "tip", iconType: "tip" },
  "玩法": { tone: "tip", iconType: "tip" }
};

function createEmptyDocument() {
  return {
    documentTitle: "",
    overviewItems: [],
    days: [],
    reminders: []
  };
}

function normalizeLine(rawLine) {
  if (!rawLine) {
    return "";
  }
  return rawLine
    .replace(/\u00a0/g, " ")
    .replace(/[：﹕]/g, ":")
    .replace(/\t/g, " ")
    .trim();
}

function createItem(label, value) {
  var meta = LABEL_META[label] || { tone: "tip", iconType: "tip" };
  return {
    label: label,
    value: value,
    tone: meta.tone,
    iconType: meta.iconType
  };
}

function parseListLine(line) {
  var content = line.replace(/^[-*]\s*/, "").trim();
  if (!content) {
    return null;
  }

  var matched = content.match(/^([^:]+):\s*(.+)$/);
  if (!matched) {
    return createItem("建议", content);
  }

  return createItem(matched[1].trim(), matched[2].trim());
}

function ensureTimeSlot(day, title) {
  var slot = {
    title: title,
    items: []
  };
  day.timeSlots.push(slot);
  return slot;
}

function parseHeading(line) {
  var matched = line.match(/^(#{1,6})\s*(.+)$/);
  if (!matched) {
    return null;
  }
  return {
    level: matched[1].length,
    text: matched[2].trim()
  };
}

function parseRouteMarkdown(markdown) {
  var parsed = createEmptyDocument();
  if (!markdown) {
    return parsed;
  }

  var normalizedMarkdown = String(markdown)
    .replace(/\\r\\n/g, "\n")
    .replace(/\\n\\n/g, "\n\n")
    .replace(/\\n/g, "\n")
    .replace(/\\r/g, "\n")
    .replace(/\r\n/g, "\n")
    .replace(/\r/g, "\n");

  var lines = normalizedMarkdown.split("\n");
  var currentDay = null;
  var currentSlot = null;
  var inReminders = false;

  lines.forEach(function(rawLine) {
    var line = normalizeLine(rawLine);
    var heading = null;

    if (!line) {
      return;
    }

    heading = parseHeading(line);
    if (heading && heading.level === 1) {
      parsed.documentTitle = heading.text;
      return;
    }

    if (heading && heading.level === 2) {
      if (heading.text === "出行提醒") {
        inReminders = true;
        currentDay = null;
        currentSlot = null;
        return;
      }

      inReminders = false;
      currentDay = {
        title: heading.text,
        timeSlots: []
      };
      parsed.days.push(currentDay);
      currentSlot = null;
      return;
    }

    if (heading && heading.level === 3) {
      if (!currentDay) {
        return;
      }
      currentSlot = ensureTimeSlot(currentDay, heading.text);
      return;
    }

    if (!/^[-*]\s*/.test(line)) {
      return;
    }

    var item = parseListLine(line);
    if (!item) {
      return;
    }

    if (inReminders) {
      parsed.reminders.push(item.value || item.label);
      return;
    }

    if (currentDay && !currentSlot) {
      currentSlot = ensureTimeSlot(currentDay, "行程安排");
    }

    if (currentSlot) {
      currentSlot.items.push(item);
      return;
    }

    parsed.overviewItems.push(item);
  });

  parsed.days.forEach(function(day) {
    day.timeSlots.forEach(function(slot, index) {
      slot.isLast = index === day.timeSlots.length - 1;
    });
  });

  return parsed;
}

module.exports = {
  createEmptyDocument: createEmptyDocument,
  parseRouteMarkdown: parseRouteMarkdown
};
