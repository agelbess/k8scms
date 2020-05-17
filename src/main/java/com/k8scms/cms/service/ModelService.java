package com.k8scms.cms.service;

import com.k8scms.cms.CmsProperties;
import com.k8scms.cms.model.Model;
import com.k8scms.cms.mongo.MongoService;
import com.k8scms.cms.utils.Utils;
import io.quarkus.cache.CacheResult;
import org.bson.Document;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class ModelService {

    @Inject
    CmsProperties cmsProperties;

    @Inject
    MongoService mongoService;

    @CacheResult(cacheName = "models")
    public Map<String, Model> getModels() {
        return mongoService.get(cmsProperties.getDatabase(), cmsProperties.getCollectionModel(), new Document())
                .map(document -> Utils.fromJson(document.toJson(), Model.class))
                .collectItems()
                .asList()
                .await()
                .atMost(cmsProperties.getMongoTimeoutDuration())
                .stream()
                .collect(Collectors.toMap(
                        model -> model.getDatabase() + "." + model.getCollection(),
                        model -> model
                ));
    }

    public Model getModel(String database, String collection) {
        return getModels().get(database + "." + collection);
    }

}
