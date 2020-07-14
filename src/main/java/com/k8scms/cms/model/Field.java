/*
 * MIT License
 * Copyright (c) 2020 Alexandros Gelbessis
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.k8scms.cms.model;

public class Field {

    public static final String TYPE_MISSING = "missing";

    public static final String TYPE_OID = "oid"; // $oid - HEX
    public static final String TYPE_DATE = "date"; // $date - ISO
    public static final String TYPE_STRING = "string";
    public static final String TYPE_INTEGER = "integer";
    public static final String TYPE_DECIMAL = "decimal";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_JSON = "json";
    public static final String TYPE_ARRAY = "array";
    public static final String TYPE_EMAIL = "email";
    public static final String TYPE_PHONE = "phone";
    public static final String ENCRYPTION_SECRET1 = "secret1"; // one way encryption
    public static final String ENCRYPTION_SECRET2 = "secret2"; // two way encryption
    public static final String TYPE_CRON = "cron"; // cron expression
    public static final String TYPE_GEO_JSON = "geoJson"; // GeoJSON

    public static final Field DEFAULT = new Field();

    static {
        DEFAULT.setType(TYPE_MISSING);
    }

    private boolean id;
    private String name;
    private String label;
    private boolean hidden;
    private boolean required;
    private String type = TYPE_STRING;
    // use it for validating arrays
    private String arrayType;
    private Relation relation;
    private String virtual;
    private String regex;
    private String charset;
    private boolean ignoreValidationChanges;
    private String encryption;

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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean getHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean getRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getArrayType() {
        return arrayType;
    }

    public void setArrayType(String arrayType) {
        this.arrayType = arrayType;
    }

    public Relation getRelation() {
        return relation;
    }

    public void setRelation(Relation relation) {
        this.relation = relation;
    }

    public String getVirtual() {
        return virtual;
    }

    public void setVirtual(String virtual) {
        this.virtual = virtual;
    }


    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public boolean getIgnoreValidationChanges() {
        return ignoreValidationChanges;
    }

    public void setIgnoreValidationChanges(boolean ignoreValidationChanges) {
        this.ignoreValidationChanges = ignoreValidationChanges;
    }

    public String getEncryption() {
        return encryption;
    }

    public void setEncryption(String encryption) {
        this.encryption = encryption;
    }
}
