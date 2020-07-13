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
import com.k8scms.cms.model.Model;
import com.k8scms.cms.mongo.MongoService;
import com.k8scms.cms.utils.Utils;
import io.quarkus.scheduler.Scheduled;
import org.bson.Document;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class ModelService {

    private Map<String, Model> models;

    @Inject
    CmsProperties cmsProperties;

    @Inject
    MongoService mongoService;

    @PostConstruct
    void postConstruct() {
        updateModels();
    }

    @Scheduled(every = "{cms.scheduler.model-service.every}")
    public void updateModels() {
        models = mongoService.get(cmsProperties.getCluster(), cmsProperties.getDatabase(), cmsProperties.getCollectionModel(), new Document())
                .map(document -> Utils.fromJson(document.toJson(), Model.class))
                .collectItems()
                .asList()
                .await()
                .indefinitely()
                .stream()
                .collect(Collectors.toMap(
                        model -> String.format("%s.%s.%s", Optional.ofNullable(model.getCluster()).orElse(cmsProperties.getCluster()), model.getDatabase(), model.getCollection()),
                        model -> model
                ));
    }

    public Map<String, Model> getModels() {
        return models;
    }

    public Model getModel(String cluster, String database, String collection) {
        Model model = models.get(String.format("%s.%s.%s", cluster, database, collection));
        if (model == null) {
            throw new NotFoundException(Response.status(Response.Status.NOT_FOUND).entity(String.format("Model %s.%s.%s not found", cluster, database, collection)).type(MediaType.TEXT_PLAIN).build());
        } else {
            return model;
        }
    }

}
