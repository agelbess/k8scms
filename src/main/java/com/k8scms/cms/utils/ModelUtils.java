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

package com.k8scms.cms.utils;

import com.k8scms.cms.Constants;
import com.k8scms.cms.SecretProperties;
import com.k8scms.cms.model.Field;
import com.k8scms.cms.model.GetOptions;
import com.k8scms.cms.model.Meta;
import com.k8scms.cms.model.Model;
import com.k8scms.cms.mongo.MongoService;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class ModelUtils {

    private ModelUtils() {
    }

    public static List<Field> findIdFields(Model model) {
        return model.getFields()
                .stream()
                .filter(Field::isId)
                .collect(Collectors.toList());
    }

    private static Field findField(Model model, String name) {
        return model.getFields()
                .stream()
                .filter(field -> name.equals(field.getName()))
                .findAny()
                .orElse(Field.DEFAULT);
    }

    public static List<Document> validate(List<Document> documents, Model model) {
        List<Map<String, Object>> ids = new ArrayList<>();
        List<Field> idFields = findIdFields(model);
        documents.forEach(document -> validate(document, model, idFields, ids));
        return documents;
    }

    private static Map<String, Object> filterDocument(Document document, List<Field> fields) {
        Map<String, Object> result = new HashMap<>();
        for (Field field : fields) {
            result.put(field.getName(), document.get(field.getName()));
        }
        return result;
    }

    private static Document validate(Document document, Model model, List<Field> idFields, List<Map<String, Object>> ids) {
        Meta meta = (Meta) document.getOrDefault("_meta", new Meta());
        Map<String, Object> documentIds = filterDocument(document, idFields);
        // Map already implements equals
        if (ids.contains(documentIds)) {
            if (idFields.size() > 1) {
                addMetaValidationError(meta, null, String.format("composite id '%s' already exists in collection's ids",
                        documentIds.values().stream().map(Object::toString).collect(Collectors.joining(","))));
            }
            for (String id : documentIds.keySet()) {
                addMetaValidationError(meta, id, String.format("id '%s' already exists in collection's ids", id));
            }
        } else {
            ids.add(documentIds);
        }
        for (Map.Entry<String, Object> entry : document.entrySet()) {
            Field field = com.k8scms.cms.utils.ModelUtils.findField(model, entry.getKey());
            // check regex
            if (field.getRegex() != null) {
                // use it also check for empty strings and nulls with the '\z' option
                Object value = document.get(field.getName());
                String valueS = value == null ? "" : value.toString();
                if (!valueS.matches(field.getRegex())) {
                    addMetaValidationError(meta, field.getName(), String.format("'%s' does not match '%s'", valueS, field.getRegex()));
                }
            }
            // check charset
            if (field.getCharset() != null) {
                try {
                    if (!Utils.isCharset(entry.getValue().toString().replace("€", ""), field.getCharset())) {
                        addMetaValidationError(meta, entry.getKey(), String.format("'%s' not in charset %s", entry.getValue(), field.getCharset()));
                    }
                } catch (UnsupportedEncodingException e) {
                    addMetaValidationError(meta, entry.getKey(), String.format("charset %s is not a known one", field.getCharset()));
                }
            }
            if (entry.getValue() != null) {
                // use the entry.key in case the field is not included in the model, otherwise the rest service returns list instead of map!!!
                validateEntry(meta, field.getType(), field.getArrayType(), entry.getKey(), entry.getValue());
            }
        }
        // not for relations and virtual fields. Relations error are reported in _meta.RelationErrors
        model.getFields().stream()
                .filter(field -> {
                    return field.getRequired() && field.getRelation() == null && field.getVirtual() == null;
                })
                .forEach(field -> {
                    if (document.get(field.getName()) == null) {
                        addMetaValidationError(meta, field.getName(), "required value");
                    }
                });
        document.putIfAbsent("_meta", meta);
        return document;
    }

    private static void validateEntry(Meta meta, String type, String arrayType, String fieldName, Object value) {
        switch (type) {
            case Field.TYPE_MISSING:
                addMetaValidationError(meta, fieldName, String.format("field %s is missing from the model", fieldName));
                break;
            case Field.TYPE_STRING:
                if (!(value instanceof String)) {
                    addMetaValidationError(meta, fieldName, String.format("'%s' is not a string", value));
                }
                break;
            case Field.TYPE_BOOLEAN:
                if (!(value instanceof Boolean)) {
                    addMetaValidationError(meta, fieldName, String.format("'%s' is not a boolean", value));
                }
                break;
            case Field.TYPE_DATE:
                if (!(value instanceof Date)) {
                    addMetaValidationError(meta, fieldName, String.format("'%s' is not a date", value));
                }
                break;
            case Field.TYPE_JSON:
                // on upload check for Map
                if (!(value instanceof Map) && !(value instanceof Document)) {
                    addMetaValidationError(meta, fieldName, "not json");
                }
                break;
            case Field.TYPE_ARRAY:
                if (arrayType != null && value instanceof List) {
                    for (Object listValue : (List) value) {
                        validateEntry(meta, arrayType, null, fieldName, listValue);
                    }
                }
                break;
            case Field.TYPE_INTEGER:
                if (!(value instanceof Integer)) {
                    addMetaValidationError(meta, fieldName, String.format("'%s' is not an integer is of type %s", value, value.getClass().getSimpleName()));
                }
                break;
            case Field.TYPE_DECIMAL:
                if (!(value instanceof Decimal128) && !(value instanceof BigDecimal) && !(value instanceof Double)) {
                    addMetaValidationError(meta, fieldName, String.format("'%s' is not a decimal, is of type %s", value, value.getClass().getSimpleName()));
                }
                break;
            case Field.TYPE_OID:
                if (!(value instanceof ObjectId)) {
                    addMetaValidationError(meta, fieldName, String.format("'%s' is not of type oid", value));
                }
                break;
            case Field.TYPE_CRON:
                if (!(value instanceof String)) {
                    addMetaValidationError(meta, fieldName, String.format("'%s' is not a cron expression", value));
                } else {
                    if (!((String) value).matches(Constants.REGEX_CRON)) {
                        addMetaValidationError(meta, fieldName, String.format("'%s' is not a cron expression", value));
                    }
                }
                break;
            case Field.TYPE_EMAIL:
                if (!((String) value).matches(Constants.REGEX_EMAIL)) {
                    addMetaValidationError(meta, fieldName, String.format("'%s' is not an email", value));
                }
                break;
            case Field.TYPE_PHONE:
                if (!((String) value).matches(Constants.REGEX_PHONE)) {
                    addMetaValidationError(meta, fieldName, String.format("'%s' is not a phone", value));
                }
                break;
            case Field.TYPE_GEO_JSON:
                // on upload check for Map
                if (!(value instanceof Map) && !(value instanceof Document) && !((value) instanceof List)) {
                    addMetaValidationError(meta, fieldName, String.format("'%s' is not a geoJson type", value));
                }
                if (value instanceof Map) {
                    Map<String, Object> entryEntry = (Map<String, Object>) value;
                    // e.g. { type: "Point", coordinates: [ 40, 5 ] }
                    if (entryEntry.get("type") == null) {
                        addMetaValidationError(meta, fieldName, "geoJson is missing 'type'");
                    }
                    if (entryEntry.get("coordinates") == null) {
                        addMetaValidationError(meta, fieldName, "geoJson is missing 'coordinates'");
                    }
                }
                break;
            default:
                // do nothing
        }
    }

    public static List<Document> findValidationChanges(MongoService mongoService, List<Document> documents, Model model, SecretProperties secretProperties) {
        List<Field> idFields = findIdFields(model);
        documents.forEach(document -> findValidationChanges(mongoService, document, model, idFields, secretProperties));
        return documents;
    }

    private static Document findValidationChanges(MongoService mongoService, Document document, Model model, List<Field> idFields, SecretProperties secretProperties) {
        Meta meta = (Meta) document.get("_meta");
        if (meta == null) {
            meta = new Meta();
        }

        if (idFields.isEmpty()) {
            // do nothing
        } else {
            Map<String, Object> documentIds = filterDocument(document, idFields);
            GetOptions getOptions = new GetOptions();
            getOptions.setLimit(1);
            Document old = mongoService.get(model.getCluster(), model.getDatabase(), model.getCollection(), new Document(documentIds), getOptions)
                    .collectItems().first().await().indefinitely();
            // decrypt secret2
            if (old == null) {
                // do nothing
            } else {
                decryptSecrets(old, model, secretProperties);
                Set<String> allKeys = new HashSet();
                allKeys.addAll(old.keySet());
                allKeys.addAll(document.keySet());
                allKeys.remove("_meta");

                for (String key : allKeys) {
                    // old contains the field
                    Object newValue = document.get(key);
                    Object oldValue = old.get(key);
                    boolean equal;
                    Field field = findField(model, key);
                    if (field.getIgnoreValidationChanges() || field.getVirtual() != null || field.getRelation() != null) {
                        // ignore the 'ignoreValidationChanges', 'virtual' and 'relation' ones
                    } else {
                        if (field.getType().equals(Field.TYPE_INTEGER) || field.getType().equals(Field.TYPE_DECIMAL)) {
                            equal = Objects.equals(oldValue == null ? null : oldValue.toString(), newValue == null ? null : newValue.toString());
                        } else {
                            equal = Objects.equals(oldValue, newValue);
                        }
                        if (!equal) {
                            if (field.getType().equals(Field.ENCRYPTION_SECRET1)) {
                                addValidationChange(meta, key, "one way secrets are always changing when persisted");
                            } else {
                                addValidationChange(meta, key, String.format("old: %s <> new: %s",
                                        oldValue == null ? "null" : oldValue.getClass().getSimpleName() + ": '" + oldValue + "'",
                                        newValue == null ? "null" : newValue.getClass().getSimpleName() + ": '" + newValue + "'"));
                            }
                        }
                    }
                }
            }
        }
        document.putIfAbsent("_meta", meta);
        return document;
    }

    private static void addValidationChange(Meta meta, String fieldName, String message) {
        if (meta.getValidationChanges() == null) {
            meta.setValidationChanges(new HashMap());
        }
        meta.getValidationChanges().put(fieldName, message);
    }

    private static void addMetaValidationError(Meta meta, String fieldName, String error) {
        if (meta.getValidationErrors() == null) {
            meta.setValidationErrors(new HashMap<String, List<String>>());
        }
        List<String> fieldErrors = meta.getValidationErrors().computeIfAbsent(fieldName, k -> new ArrayList<>());
        fieldErrors.add(error);
    }

    private static String toQuery(Object object) {
        if (object == null) {
            return "null";
        } else if (object instanceof List) {
            return ((List<Object>) object).stream()
                    .map(com.k8scms.cms.utils.ModelUtils::toQuery)
                    .collect(Collectors.joining(","));
        } else if (object instanceof ObjectId) {
            // need to escape $ for the String.replaceAll method
            return String.format("{'\\$oid': '%s'}", ((ObjectId) object).toHexString());
        } else if (object instanceof Date) {
            // need to escape $ for the String.replaceAll method
            return String.format("{'\\$date': '%s'}", ((Date) object).toInstant().toString());
        } else if (object instanceof Decimal128) {
            return ((Decimal128) object).toString();
        }
        return String.format("'%s'", object.toString());
    }

    public static Document addRelations(Document document, Model model, MongoService mongoService) {
        Meta meta = (Meta) document.get("_meta");
        if (meta == null) {
            meta = new Meta();
        }

        Map<String, List<Document>> relationsMap = new HashMap<>();
        for (Field field : model.getFields()) {
            if (field.getRelation() != null) {
                String cluster = field.getRelation().getCluster();
                String database = field.getRelation().getDatabase();
                String collection = field.getRelation().getCollection();
                String filter = field.getRelation().getFilter();
                for (Field c : model.getFields()) {
                    if (c.getRelation() == null) {
                        if (filter.contains("{" + c.getName() + "}")) {
                            filter = filter.replaceAll("\\{" + c.getName() + "\\}", toQuery(document.get(c.getName())));
                            addRelationFilter(meta, field.getName(), filter);
                        }
                    }
                }
                List<Document> relations = mongoService.get(cluster, database, collection, Document.parse(filter))
                        .collectItems()
                        .asList()
                        .await()
                        .indefinitely();
                if (!relations.isEmpty()) {
                    relationsMap.put(field.getName(), relations);
                    if (!field.getHidden()) {
                        document.put(
                                field.getName(),
                                relations
                        );
                    }
                }
                if (field.getRequired() && relations.isEmpty()) {
                    addMetaRelationError(meta, field.getName(), "relation required");
                }
            }
        }
        // add all the virtual fields
        model.getFields().stream()
                .filter(field -> field.getVirtual() != null)
                .forEach(field -> {
                    String[] tokens = field.getVirtual().split("\\.");
                    List values = ((List) relationsMap.get(tokens[0]));
                    if (values != null && !values.isEmpty()) {
                        Object value = values.get(0);
                        for (int i = 1; i < tokens.length; i++) {
                            if (value instanceof Map) {
                                value = ((Map<String, Object>) value).get(tokens[i]);
                            }
                        }
                        if (value != null) {
                            document.put(field.getName(), value);
                        }
                    }
                });
        return document;
    }

    private static void addRelationFilter(Meta meta, String fieldName, String filter) {
        if (meta.getRelationFilters() == null) {
            meta.setRelationFilters(new HashMap<>());
        }
        meta.getRelationFilters().put(fieldName, filter);
    }

    private static void addMetaRelationError(Meta meta, String fieldName, String error) {
        if (meta.getRelationErrors() == null) {
            meta.setRelationErrors(new HashMap<String, List<String>>());
        }
        List<String> fieldErrors = meta.getRelationErrors().computeIfAbsent(fieldName, k -> new ArrayList<>());
        fieldErrors.add(error);
    }

    private static Object toWire(Object object) {
        if (object instanceof List) {
            List<Object> list = new ArrayList<>();
            for (Object o : (List<Object>) object) {
                list.add(toWire(o));
            }
            return list;
        } else if (object instanceof ObjectId) {
            return ((ObjectId) object).toHexString();
        } else if (object instanceof Date) {
            return ((Date) object).toInstant().toString();
        } else if (object instanceof Document) {
            Document document = (Document) object;
            for (Map.Entry<String, Object> entry : document.entrySet()) {
                entry.setValue(toWire(entry.getValue()));
            }
        }
        return object;
    }

    public static Document toWire(Document document, Model model) {
        for (Map.Entry<String, Object> entry : document.entrySet()) {
            Field field = com.k8scms.cms.utils.ModelUtils.findField(model, entry.getKey());
            entry.setValue(toWire(entry.getValue()));
        }
        return document;
    }

    public static Document decryptSecrets(Document document, Model model, SecretProperties secretProperties) {
        for (Map.Entry<String, Object> entry : document.entrySet()) {
            Field field = com.k8scms.cms.utils.ModelUtils.findField(model, entry.getKey());
            entry.setValue(decryptSecrets(field, entry.getValue(), secretProperties));
        }
        return document;
    }

    private static Object decryptSecrets(Field field, Object object, SecretProperties secretProperties) {
        if (field.getEncryption() != null) {
            switch (field.getEncryption()) {
                case Field.ENCRYPTION_SECRET1:
                    return "********";
                case Field.ENCRYPTION_SECRET2:
                    if (object != null) {
                        return fromWire(field.getType(), field.getArrayType(), Utils.decrypt2(object.toString(), secretProperties.getSecretEncryptionKey()));
                    }
            }
        }
        return object;
    }

    public static Document encryptSecrets(Document document, Model model, SecretProperties secretProperties) {
        for (Map.Entry<String, Object> entry : document.entrySet()) {
            Field field = com.k8scms.cms.utils.ModelUtils.findField(model, entry.getKey());
            entry.setValue(encryptSecrets(field, entry.getValue(), secretProperties));
        }
        return document;
    }

    private static Object encryptSecrets(Field field, Object value, SecretProperties secretProperties) {
        Object result = value;
        if (field.getEncryption() != null && value != null) {
            switch (field.getEncryption()) {
                case Field.ENCRYPTION_SECRET1:
                    result = Utils.encrypt1(value.toString(), secretProperties.getSecretEncryptionKey());
                    break;
                case Field.ENCRYPTION_SECRET2:
                    result = Utils.encrypt2(value.toString(), secretProperties.getSecretEncryptionKey());
                    break;
            }
        }
        return result;
    }

    public static Document fromWire(Document document, Model model) {
        if (document != null) {
            for (Map.Entry<String, Object> entry : document.entrySet()) {
                Field field = com.k8scms.cms.utils.ModelUtils.findField(model, entry.getKey());
                entry.setValue(fromWire(field.getType(), field.getArrayType(), entry.getValue()));
            }
        }
        return document;
    }

    private static Object fromWire(String type, String arrayType, Object value) {
        Object result = value;
        if (value != null) {
            switch (type) {
                case Field.TYPE_DATE:
                    try {
                        result = Date.from(Instant.parse(value.toString()));
                    } catch (DateTimeParseException e) {
                        // do nothing
                    }
                    break;
                case Field.TYPE_OID:
                    try {
                        result = new ObjectId(value.toString());
                    } catch (IllegalArgumentException e) {
                        // do nothing
                    }
                    break;
                case Field.TYPE_INTEGER:
                    try {
                        result = Integer.parseInt(value.toString());
                    } catch (NumberFormatException e) {
                        // do nothing
                    }
                    break;
                case Field.TYPE_DECIMAL:
                    try {
                        result = new BigDecimal(value.toString());
                    } catch (NumberFormatException e) {
                        // do nothing
                    }
                    break;
                case Field.TYPE_ARRAY:
                    if (value instanceof List && arrayType != null) {
                        List<Object> newList = new ArrayList<>();
                        for (Object listValue : (List) value) {
                            newList.add(fromWire(arrayType, null, listValue));
                        }
                        result = newList;
                    }
                    break;
                default:
                    return value;
            }
        }
        return result;
    }
}
