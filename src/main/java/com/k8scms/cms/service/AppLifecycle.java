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

package com.k8scms.cms.service;

import com.k8scms.cms.CmsProperties;
import com.k8scms.cms.Constants;
import com.k8scms.cms.mongo.MongoService;
import com.k8scms.cms.utils.Utils;
import com.mongodb.client.model.IndexOptions;
import io.quarkus.runtime.StartupEvent;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

import static java.nio.file.Files.readAllLines;

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

    void onStart(@Observes StartupEvent ev) throws IOException {
        log.info("{}{}", Constants.ANSI_YELLOW, SPLASH);
        log.info("The application is starting with properties\nCms: {}",
                Utils.stringify(cmsProperties));
        initModel();
    }

    void initModel() throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(cmsProperties.getModelsPath()))) {
            paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            String modelText = String.join("", readAllLines(path));
                            String replaceProperties = Utils.replaceProperties(modelText);
                            Document model = Document.parse(replaceProperties);
                            mongoService.put(
                                    cmsProperties.getCluster(),
                                    cmsProperties.getDatabase(),
                                    cmsProperties.getCollectionModel(),
                                    new Document("cluster", model.getString("cluster"))
                                            .append("database", model.getString("database"))
                                            .append("collection", model.getString("collection")),
                                    model,
                                    true
                            ).await().indefinitely();
                        } catch (IOException e) {
                            throw new RuntimeException("could not read model file", e);
                        }
                    });
        }

        // create the indexes
        modelService.getModels().forEach((key, model) -> {
            if (model.getIndexes() != null) {
                model.getIndexes().forEach(modelIndex -> mongoService.createIndex(
                        cmsProperties.getCluster(),
                        model.getDatabase(),
                        model.getCollection(),
                        modelIndex.getIndex(),
                        new IndexOptions()
                                .unique(Optional.ofNullable(modelIndex.getOptions())
                                        .map(document -> document.getBoolean("unique"))
                                        .orElse(false))
                                .background(Optional.ofNullable(modelIndex.getOptions())
                                        .map(document -> document.getBoolean("background"))
                                        .orElse(false)
                                )
                ).await().indefinitely());
            }
        });
    }
}
