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

package com.k8scms.cms.mongo;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.bulk.BulkWriteInsert;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.bulk.BulkWriteUpsert;

import java.util.List;

public class MongoBulkWriteExceptionBulkWriteResult extends BulkWriteResult {
    MongoBulkWriteException mongoBulkWriteException;

    MongoBulkWriteExceptionBulkWriteResult(MongoBulkWriteException mongoBulkWriteException) {
        this.mongoBulkWriteException = mongoBulkWriteException;
    }

    @Override
    public boolean wasAcknowledged() {
        return false;
    }

    @Override
    public int getInsertedCount() {
        return mongoBulkWriteException.getWriteResult().getInsertedCount();
    }

    @Override
    public int getMatchedCount() {
        return mongoBulkWriteException.getWriteResult().getMatchedCount();
    }

    @Override
    public int getDeletedCount() {
        return mongoBulkWriteException.getWriteResult().getDeletedCount();
    }

    @Override
    public int getModifiedCount() {
        return mongoBulkWriteException.getWriteResult().getModifiedCount();
    }

    @Override
    public List<BulkWriteInsert> getInserts() {
        return mongoBulkWriteException.getWriteResult().getInserts();
    }

    @Override
    public List<BulkWriteUpsert> getUpserts() {
        return mongoBulkWriteException.getWriteResult().getUpserts();
    }

    public MongoBulkWriteException getMongoBulkWriteException() {
        return mongoBulkWriteException;
    }

    public void setMongoBulkWriteException(MongoBulkWriteException mongoBulkWriteException) {
        this.mongoBulkWriteException = mongoBulkWriteException;
    }
}
