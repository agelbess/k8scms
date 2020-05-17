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
