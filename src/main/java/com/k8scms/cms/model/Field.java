package com.k8scms.cms.model;

import java.util.Map;

public class Field {

    public static final String TYPE_MISSING = "missing";

    public static final String TYPE_OID = "oid"; // $oid - HEX
    public static final String TYPE_DATE = "date"; // $date - ISO
    public static final String TYPE_STRING = "string";
    public static final String TYPE_INTEGER = "integer";
    public static final String TYPE_DECIMAL = "decimal";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_JSON = "json";
    public static final String TYPE_EMAIL = "email";
    public static final String TYPE_PHONE = "phone";
    public static final String TYPE_SECRET1 = "secret1"; // one way encryption
    public static final String TYPE_SECRET2 = "secret2"; // two way encryption
    public static final String TYPE_CRON = "cron"; // two way encryption

    public static final Field DEFAULT = new Field();

    static {
        DEFAULT.setType(TYPE_MISSING);
    }

    private boolean id;
    private String name;
    private String type = TYPE_STRING;
    private String relation;
    private String regex;
    private Map<String, Object> json;

    public boolean isId() {
        return id;
    }

    public void setId(boolean id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public Map<String, Object> getJson() {
        return json;
    }

    public void setJson(Map<String, Object> json) {
        this.json = json;
    }
}
