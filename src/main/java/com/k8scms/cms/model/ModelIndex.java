package com.k8scms.cms.model;

import org.bson.Document;

public class ModelIndex {
    private Document index;
    private Document options;

    public Document getIndex() {
        return index;
    }

    public void setIndex(Document index) {
        this.index = index;
    }

    public Document getOptions() {
        return options;
    }

    public void setOptions(Document options) {
        this.options = options;
    }
}
