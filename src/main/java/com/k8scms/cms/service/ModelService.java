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
