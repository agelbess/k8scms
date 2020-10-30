
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

package com.k8scms.cms.model;

import com.mongodb.bulk.BulkWriteError;

import java.util.List;

public class MethodResult {
    private Long modifiedCount;
    private Long deleteCount;
    private Long matchedCount;
    private List<String> insertedIds;
    private List<String> upsertedIds;
    private List<BulkWriteError> bulkWriteErrors;

    public Long getModifiedCount() {
        return modifiedCount;
    }

    public void setModifiedCount(Long modifiedCount) {
        this.modifiedCount = modifiedCount;
    }

    public Long getDeleteCount() {
        return deleteCount;
    }

    public void setDeleteCount(Long deleteCount) {
        this.deleteCount = deleteCount;
    }

    public Long getMatchedCount() {
        return matchedCount;
    }

    public void setMatchedCount(Long matchedCount) {
        this.matchedCount = matchedCount;
    }

    public List<String> getInsertedIds() {
        return insertedIds;
    }

    public void setInsertedIds(List<String> insertedIds) {
        this.insertedIds = insertedIds;
    }

    public List<String> getUpsertedIds() {
        return upsertedIds;
    }

    public void setUpsertedIds(List<String> upsertedIds) {
        this.upsertedIds = upsertedIds;
    }

    public List<BulkWriteError> getBulkWriteErrors() {
        return bulkWriteErrors;
    }

    public void setBulkWriteErrors(List<BulkWriteError> bulkWriteErrors) {
        this.bulkWriteErrors = bulkWriteErrors;
    }
}
