package com.k8scms.cms.model;

import java.util.List;

public class Model {
    private String database;
    private String collection;
    private List<Field> fields;
    private List<ModelIndex> indexes;

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public List<ModelIndex> getIndexes() {
        return indexes;
    }

    public void setIndexes(List<ModelIndex> indexes) {
        this.indexes = indexes;
    }
}
