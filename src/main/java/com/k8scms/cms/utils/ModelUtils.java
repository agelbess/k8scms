/*
 * MIT License
 *
 * Copyright (c) 2020 Alexandros Gelbessis
 *
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
 *
 */

package com.k8scms.cms.utils;

import com.k8scms.cms.CmsProperties;
import com.k8scms.cms.Constants;
import com.k8scms.cms.SecretProperties;
import com.k8scms.cms.model.Field;
import com.k8scms.cms.model.Model;
import com.k8scms.cms.mongo.MongoService;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModelUtils {

    private ModelUtils() {
    }

    public static Field findIdField(Model model) {
        return model.getFields()
                .stream()
                .filter(Field::isId)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(String.format("model %s.%s does not exist", model.getDatabase(), model.getCollection())));
    }

    private static Field findField(Model model, String name) {
        return model.getFields()
                .stream()
                .filter(field -> name.equals(field.getName()))
                .findAny()
                .orElse(Field.DEFAULT);
    }

    public static Document validate(Document document, Model model) {
        Document meta = new Document();
        for (Field field : model.getFields()) {
            if (field.getRegex() != null) {
                // use it also check for empty strings and nulls with the '\z' option
                Object value = document.get(field.getName());
                String valueS = value == null ? "" : value.toString();
                if (!valueS.matches(field.getRegex())) {
                    addMetaValidationError(meta, field.getName(), String.format("does not match '%s'", field.getRegex()));
                }
            }
        }
        for (Map.Entry<String, Object> entry : document.entrySet()) {
            Field field = ModelUtils.findField(model, entry.getKey());
            switch (field.getType()) {
                case Field.TYPE_MISSING:
                    addMetaValidationError(meta, entry.getKey(), "missing");
                    break;
                case Field.TYPE_BOOLEAN:
                    if (!(entry.getValue() instanceof Boolean)) {
                        addMetaValidationError(meta, field.getName(), "not boolean");
                    }
                    break;
                case Field.TYPE_DATE:
                    if (!(entry.getValue() instanceof Date)) {
                        addMetaValidationError(meta, field.getName(), "not date");
                    }
                    break;
                case Field.TYPE_JSON:
                    // on upload check for Map
                    if (!(entry.getValue() instanceof Map) && !(entry.getValue() instanceof Document) && !((entry.getValue()) instanceof List)) {
                        addMetaValidationError(meta, field.getName(), "not json");
                    }
                    if (entry.getValue() instanceof Map) {
                        Map<String, Object> entryEntry = (Map<String, Object>) entry.getValue();
                        if (field.getJson() != null) {
                            for (Map.Entry<String, Object> fieldEntry : field.getJson().entrySet()) {
                                if (!entryEntry.containsKey(fieldEntry.getKey())) {
                                    addMetaValidationError(meta, field.getName(), String.format("field '%s' is missing", fieldEntry.getKey()));
                                }
                            }
                        }
                    }
                    break;
                case Field.TYPE_INTEGER:
                    try {
                        Integer.parseInt(entry.getValue() + "");
                    } catch (NumberFormatException e) {
                        addMetaValidationError(meta, field.getName(), "not integer");
                    }
                    break;
                case Field.TYPE_DECIMAL:
                    try {
                        new BigDecimal(entry.getValue() + "");
                    } catch (NumberFormatException e) {
                        addMetaValidationError(meta, field.getName(), "not decimal");
                    }
                    break;
                case Field.TYPE_OID:
                    if (!(entry.getValue() instanceof ObjectId)) {
                        addMetaValidationError(meta, field.getName(), "not oid");
                    }
                    break;
                case Field.TYPE_SECRET1:
                case Field.TYPE_SECRET2:
                    if (!(entry.getValue() instanceof String)) {
                        addMetaValidationError(meta, field.getName(), "not secret");
                    }
                    break;
                case Field.TYPE_CRON:
                    if (!(entry.getValue() instanceof String)) {
                        addMetaValidationError(meta, field.getName(), "not cron");
                    } else {
                        if (!((String) entry.getValue()).matches(Constants.REGEX_CRON)) {
                            addMetaValidationError(meta, field.getName(), "not cron expression");
                        }
                    }
                    break;
                case Field.TYPE_EMAIL:
                    if (!((String) entry.getValue()).matches(Constants.REGEX_EMAIL)) {
                        addMetaValidationError(meta, field.getName(), "not email");
                    }

                default:
                    // do nothing
            }
        }
        document.put("_meta", meta);
        return document;
    }

    private static void addMetaValidationError(Document meta, String fieldName, String error) {
        Document validationErrors = (Document) meta.computeIfAbsent("validationErrors", k -> new Document());
        List<String> fieldErrors = (List<String>) validationErrors.computeIfAbsent(fieldName, k -> new ArrayList<String>());
        fieldErrors.add(error);
    }

    private static String toQuery(Object object) {
        if (object == null) {
            return "null";
        } else if (object instanceof List) {
            return ((List<Object>) object).stream()
                    .map(ModelUtils::toQuery)
                    .collect(Collectors.joining(","));
        } else if (object instanceof ObjectId) {
            return String.format("{'$oid': %s", ((ObjectId) object).toHexString());
        } else if (object instanceof Date) {
            return String.format("{'$date': %s", ((Date) object).toInstant().toString());
        } else if (object instanceof Decimal128) {
            return ((Decimal128) object).toString();
        }
        return String.format("'%s'", object.toString());
    }

    public static Document addRelations(Document document, Model model, MongoService mongoService, CmsProperties cmsProperties) {
        model.getFields().stream()
                .filter(field -> field.getRelation() != null)
                .forEach(field -> {
                    String[] tokens = field.getRelation().split(":", 3);
                    String database = tokens[0];
                    String collection = tokens[1];
                    String filter = tokens[2];
                    for (Field c : model.getFields()) {
                        if (c.getRelation() == null) {
                            if (filter.contains("{" + c.getName() + "}")) {
                                filter = filter.replaceAll("\\{" + c.getName() + "\\}", toQuery(document.get(c.getName())));
                            }
                        }
                    }
                    List<Document> relations = mongoService.get(database, collection, Document.parse(filter))
                            .collectItems()
                            .asList()
                            .await()
                            .atMost(cmsProperties.getMongoTimeoutDuration());
                    if (!relations.isEmpty()) {
                        document.put(
                                field.getName(),
                                relations
                        );
                    }
                });
        return document;
    }

    private static Object toWire(Field field, Object object, SecretProperties secretProperties) {
        switch (field.getType()) {
            case Field.TYPE_SECRET1:
                return "********";
            case Field.TYPE_SECRET2:
                return Utils.decrypt2(object.toString(), secretProperties.getSecretEncryptionKey());
            default:
                return toWire(object);
        }
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

    public static Document toWire(Document document, Model model, SecretProperties secretProperties) {
        for (Map.Entry<String, Object> entry : document.entrySet()) {
            Field field = ModelUtils.findField(model, entry.getKey());
            entry.setValue(toWire(field, entry.getValue(), secretProperties));
        }
        return document;
    }

    public static Document fromWire(Document document, Model model, SecretProperties secretProperties) {
        if (document != null) {
            for (Map.Entry<String, Object> entry : document.entrySet()) {
                Field field = ModelUtils.findField(model, entry.getKey());
                switch (field.getType()) {
                    case Field.TYPE_DATE:
                        try {
                            entry.setValue(Date.from(Instant.parse(entry.getValue().toString())));
                        } catch (DateTimeParseException e) {
                            // do nothing
                        }
                        break;
                    case Field.TYPE_OID:
                        entry.setValue(new ObjectId(entry.getValue().toString()));
                        break;
                    case Field.TYPE_INTEGER:
                        try {
                            entry.setValue(Integer.parseInt(entry.getValue().toString()));
                        } catch (NumberFormatException e) {
                            // do nothing
                            e.printStackTrace();
                        }
                        break;
                    case Field.TYPE_DECIMAL:
                        try {
                            entry.setValue(new BigDecimal(entry.getValue().toString()));
                        } catch (NumberFormatException e) {
                            // do nothing
                        }
                        break;
                    case Field.TYPE_SECRET1:
                        entry.setValue(Utils.encrypt1(entry.getValue().toString(), secretProperties.getSecretEncryptionKey()));
                        break;
                    case Field.TYPE_SECRET2:
                        entry.setValue(Utils.encrypt2(entry.getValue().toString(), secretProperties.getSecretEncryptionKey()));
                        break;
                    default:
                        // do nothing
                }
            }
        }
        return document;
    }
}
