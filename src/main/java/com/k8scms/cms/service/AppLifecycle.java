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

package com.k8scms.cms.service;

import com.k8scms.cms.Constants;
import com.k8scms.cms.mongo.MongoService;
import com.k8scms.cms.utils.Utils;
import com.mongodb.client.model.IndexOptions;
import com.k8scms.cms.CmsProperties;
import io.quarkus.runtime.StartupEvent;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.bind.JsonbConfig;
import java.util.Arrays;
import java.util.Optional;

@ApplicationScoped
public class AppLifecycle {
    private static final Logger log = LoggerFactory.getLogger(AppLifecycle.class);
    private static final String SPLASH = String.join("\n",
            "\n" +
                    "\n" +
                    "                       \n" +
                    "  ____   _____   ______\n" +
                    "_/ ___\\ /     \\ /  ___/\n" +
                    "\\  \\___|  Y Y  \\\\___ \\ \n" +
                    " \\___  >__|_|  /____  >\n" +
                    "     \\/      \\/     \\/ \n"
    );

    @Inject
    CmsProperties cmsProperties;

    @Inject
    MongoService mongoService;

    @Inject
    ModelService modelService;

    void onStart(@Observes StartupEvent ev) {
        JsonbConfig jsonbConfig = new JsonbConfig();
        jsonbConfig.setProperty(JsonbConfig.FORMATTING, true);
        log.info("{}{}", Constants.ANSI_YELLOW, SPLASH);
        log.info("The application is starting with properties\nCms: {}",
                Utils.stringify(cmsProperties));
        initModel();
    }

    void initModel() {
        Arrays.asList(
                "/model/cms_model_log.json",
                "/model/cms_model_model.json",
                "/model/cms_model_role.json",
                "/model/cms_model_user.json",
                "/model/cms_model_scheduler.json",
                "/model/cms_model_schedulerTask.json",
                "/model/testDB_model_test.json")
                .forEach(path -> {
                            Document model = Document.parse(Utils.fromResourcePathToString(path));
                            mongoService.put(
                                    cmsProperties.getDatabase(),
                                    cmsProperties.getCollectionModel(),
                                    new Document("database", model.getString("database")).append("collection", model.getString("collection")),
                                    model,
                                    true
                            ).await().atMost(cmsProperties.getMongoTimeoutDuration());
                        }
                );

        // create the indexes
        modelService.getModels().forEach((key, model) -> {
            if (model.getIndexes() != null) {
                model.getIndexes().forEach(modelIndex -> mongoService.createIndex(
                        model.getDatabase(),
                        model.getCollection(),
                        modelIndex.getIndex(),
                        new IndexOptions().unique(Optional.ofNullable(modelIndex.getOptions())
                                .map(document -> document.getBoolean("unique"))
                                .orElse(false))
                ).await().atMost(cmsProperties.getMongoTimeoutDuration()));
            }
        });
    }
}
