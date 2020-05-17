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
