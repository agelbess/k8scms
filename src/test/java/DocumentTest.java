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

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import java.time.Instant;

public class DocumentTest {
    static Jsonb jsonb;

    static {
        JsonbConfig jsonbConfig = new JsonbConfig();
        jsonbConfig.setProperty(JsonbConfig.FORMATTING, true);
        jsonb = JsonbBuilder.create(jsonbConfig);
    }

    @Test
    void testDate() {
        String s = "{'date': {'$date' : 1575024865000}}";
        Document document = Document.parse(s);
        System.out.println(jsonb.toJson(document));

        Object o = document.get("date");

        System.out.println(document.get("date"));
        // Instant.parse(document.get("date").toString());
    }

    @Test
    void testDate2() {
        String s = "{'date': '1575024865000'}";
        Document document = Document.parse(s);
        System.out.println(jsonb.toJson(document));
    }

    // parses as NumberDecimal
    @Test
    void testNumbers_whole_numbers() {
        String s = "{'number': 1}";
        Document document = Document.parse(s);
        Document result = Document.parse(document.toJson());

        System.out.println(s);
        System.out.println(jsonb.toJson(document));
        System.out.println(jsonb.toJson(result));
    }

    // parses as NumberDecimal
    @Test
    void testNumbers() {
        String s = "{'number': 0.1}";
        Document document = Document.parse(s);
        Document result = Document.parse(document.toJson());

        System.out.println(s);
        System.out.println(jsonb.toJson(document));
        System.out.println(jsonb.toJson(result));
    }

    // parses as Double
    @Test
    void testNumbersWith$In() {
        String s = "{'number': {'$in': [0.1]}}";
        Document document = Document.parse(s);
        Document result = Document.parse(document.toJson());

        System.out.println(s);
        System.out.println(jsonb.toJson(document));
        System.out.println(jsonb.toJson(result));
    }

    // parses as Integer
    @Test
    void testNumbersWith$In_whole_numbers() {
        String s = "{'number': {'$in': [1]}}";
        Document document = Document.parse(s);
        Document result = Document.parse(document.toJson());

        System.out.println(s);
        System.out.println(jsonb.toJson(document));
        System.out.println(jsonb.toJson(result));
    }
}
