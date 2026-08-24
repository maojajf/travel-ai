var parser = require("./parser.js");

function cloneDocument(document) {
  return {
    theme: document.theme,
    overviewItems: document.overviewItems || [],
    overviewParagraphs: document.overviewParagraphs || [],
    sections: (document.sections || []).map(function(section) {
      return Object.assign({}, section, {
        paragraphs: section.paragraphs || [],
        items: section.items || [],
        slots: (section.slots || []).map(function(slot) {
          return Object.assign({}, slot, {
            paragraphs: slot.paragraphs || [],
            items: slot.items || []
          });
        })
      });
    }),
    looseParagraphs: document.looseParagraphs || []
  };
}

function buildDocument(markdown, enableCollapse, expandedState) {
  var parsed = parser.parseMarkdown(markdown);
  var document = cloneDocument(parsed);
  var state = expandedState || {};
  var collapseEnabled = !!enableCollapse;

  document.overviewExpanded = !collapseEnabled ? true : !!state.overviewExpanded;
  document.sections = document.sections.map(function(section) {
    var sectionExpanded = !collapseEnabled ? true : !!state[section.key];
    section.expanded = sectionExpanded;
    section.slots = section.slots.map(function(slot) {
      slot.expanded = true;
      return slot;
    });
    return section;
  });

  return document;
}

Component({
  properties: {
    markdown: {
      type: String,
      value: ""
    },
    enableCollapse: {
      type: Boolean,
      value: false
    }
  },

  data: {
    document: parser.parseMarkdown(""),
    expandedState: {}
  },

  observers: {
    markdown: function(markdown) {
      this.setData({
        expandedState: {}
      });
      this.refreshDocument(markdown, this.properties.enableCollapse, {});
    },
    enableCollapse: function(enableCollapse) {
      this.refreshDocument(this.properties.markdown, enableCollapse, this.data.expandedState);
    }
  },

  methods: {
    refreshDocument: function(markdown, enableCollapse, expandedState) {
      this.setData({
        document: buildDocument(markdown, enableCollapse, expandedState || this.data.expandedState)
      });
    },

    toggleOverview: function() {
      if (!this.properties.enableCollapse) {
        return;
      }

      var nextState = Object.assign({}, this.data.expandedState, {
        overviewExpanded: !this.data.document.overviewExpanded
      });
      this.setData({
        expandedState: nextState
      });
      this.refreshDocument(this.properties.markdown, this.properties.enableCollapse, nextState);
    },

    toggleSection: function(event) {
      if (!this.properties.enableCollapse) {
        return;
      }

      var key = event.currentTarget.dataset.key;
      var nextState = Object.assign({}, this.data.expandedState, {});
      nextState[key] = !nextState[key];
      this.setData({
        expandedState: nextState
      });
      this.refreshDocument(this.properties.markdown, this.properties.enableCollapse, nextState);
    }
  }
});
