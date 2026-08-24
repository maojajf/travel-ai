var LABEL_META = {
  "景点": { tone: "spot", badge: "景" },
  "美食": { tone: "food", badge: "食" },
  "交通": { tone: "transport", badge: "行" },
  "建议": { tone: "tip", badge: "贴" },
  "玩法": { tone: "play", badge: "玩" }
};

function createSegment(text, kind, key) {
  return {
    key: key,
    text: text,
    className: kind === "strong" ? "md-strong" : kind === "code" ? "md-code" : ""
  };
}

function parseInline(text, keyPrefix) {
  var input = String(text || "");
  var pattern = /(\*\*[^*]+\*\*|`[^`]+`)/g;
  var segments = [];
  var match = null;
  var cursor = 0;
  var index = 0;

  while ((match = pattern.exec(input))) {
    if (match.index > cursor) {
      segments.push(createSegment(input.slice(cursor, match.index), "text", keyPrefix + "-t" + index));
      index += 1;
    }

    if (match[0].indexOf("**") === 0) {
      segments.push(createSegment(match[0].slice(2, -2), "strong", keyPrefix + "-s" + index));
    } else {
      segments.push(createSegment(match[0].slice(1, -1), "code", keyPrefix + "-c" + index));
    }
    index += 1;
    cursor = match.index + match[0].length;
  }

  if (cursor < input.length) {
    segments.push(createSegment(input.slice(cursor), "text", keyPrefix + "-t" + index));
  }

  if (!segments.length) {
    segments.push(createSegment("", "text", keyPrefix + "-empty"));
  }

  return segments;
}

function createParagraph(text, keyPrefix) {
  return {
    key: keyPrefix,
    segments: parseInline(text, keyPrefix)
  };
}

function createTextBlock(text, keyPrefix) {
  return {
    key: keyPrefix,
    segments: parseInline(text, keyPrefix)
  };
}

function createGenericItem(content, key) {
  return {
    key: key,
    kind: "generic",
    segments: parseInline(content, key)
  };
}

function createSemanticItem(label, value, key) {
  var meta = LABEL_META[label];
  return {
    key: key,
    kind: "semantic",
    label: label,
    tone: meta.tone,
    badge: meta.badge,
    labelSegments: parseInline(label, key + "-label"),
    valueSegments: parseInline(value || "", key + "-value")
  };
}

function createDocument() {
  return {
    theme: null,
    overviewItems: [],
    overviewParagraphs: [],
    sections: [],
    looseParagraphs: []
  };
}

function normalizeMarkdown(markdown) {
  return String(markdown || "")
    .replace(/\r\n/g, "\n")
    .replace(/\r/g, "\n")
    .replace(/\u00a0/g, " ");
}

function parseListItem(content, key) {
  var matched = content.match(/^([^:：]+)\s*[:：]\s*(.*)$/);
  if (!matched) {
    return createGenericItem(content, key);
  }

  var label = matched[1].trim();
  var value = matched[2].trim();
  if (!LABEL_META[label]) {
    return createGenericItem(content, key);
  }

  return createSemanticItem(label, value, key);
}

function pushParagraph(target, text, key) {
  target.push(createParagraph(text, key));
}

function ensureSection(document, title, key) {
  var titleText = String(title || "").trim();
  var isReminder = titleText === "出行提醒";
  var section = {
    key: key,
    kind: isReminder ? "reminder" : "day",
    title: titleText,
    titleSegments: parseInline(titleText, key + "-title"),
    slots: [],
    items: [],
    paragraphs: []
  };
  document.sections.push(section);
  return section;
}

function ensureSlot(section, title, key) {
  var slot = {
    key: key,
    title: String(title || "").trim(),
    titleSegments: parseInline(title, key + "-title"),
    items: [],
    paragraphs: [],
    isLast: false
  };
  section.slots.push(slot);
  return slot;
}

function markLastSlots(document) {
  document.sections.forEach(function(section) {
    var slots = section.slots || [];
    slots.forEach(function(slot, index) {
      slot.isLast = index === slots.length - 1;
    });
  });
}

function parseMarkdown(markdown) {
  var normalized = normalizeMarkdown(markdown);
  var document = createDocument();
  var lines = normalized.split("\n");
  var currentSection = null;
  var currentSlot = null;

  lines.forEach(function(rawLine, index) {
    var line = rawLine.replace(/\t/g, "    ");
    var trimmed = line.trim();
    var headingMatch = null;
    var listMatch = null;
    var keyBase = "line-" + index;

    if (!trimmed) {
      return;
    }

    headingMatch = trimmed.match(/^(#{1,6})\s+(.+)$/);
    if (headingMatch) {
      if (headingMatch[1].length === 1) {
        document.theme = {
          key: keyBase + "-theme",
          title: headingMatch[2].trim(),
          titleSegments: parseInline(headingMatch[2].trim(), keyBase + "-theme-title")
        };
        currentSection = null;
        currentSlot = null;
        return;
      }

      if (headingMatch[1].length === 2) {
        currentSection = ensureSection(document, headingMatch[2], keyBase + "-section");
        currentSlot = null;
        return;
      }

      if (headingMatch[1].length >= 3) {
        if (!currentSection) {
          currentSection = ensureSection(document, "路线安排", keyBase + "-section-fallback");
        }
        currentSlot = ensureSlot(currentSection, headingMatch[2], keyBase + "-slot");
      }
      return;
    }

    listMatch = trimmed.match(/^-\s+(.+)$/);
    if (listMatch) {
      if (currentSlot) {
        currentSlot.items.push(parseListItem(listMatch[1].trim(), keyBase + "-item"));
        return;
      }

      if (currentSection) {
        currentSection.items.push(parseListItem(listMatch[1].trim(), keyBase + "-item"));
        return;
      }

      document.overviewItems.push(parseListItem(listMatch[1].trim(), keyBase + "-overview"));
      return;
    }

    if (currentSlot) {
      pushParagraph(currentSlot.paragraphs, trimmed, keyBase + "-paragraph");
      return;
    }

    if (currentSection) {
      pushParagraph(currentSection.paragraphs, trimmed, keyBase + "-paragraph");
      return;
    }

    if (document.theme || document.overviewItems.length) {
      pushParagraph(document.overviewParagraphs, trimmed, keyBase + "-overview-paragraph");
      return;
    }

    pushParagraph(document.looseParagraphs, trimmed, keyBase + "-loose");
  });

  markLastSlots(document);
  return document;
}

module.exports = {
  parseMarkdown: parseMarkdown
};
