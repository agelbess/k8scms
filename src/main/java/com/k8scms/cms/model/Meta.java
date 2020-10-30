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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Meta {

    public enum UPLOAD_RESULT {
        INSERT,
        UPDATE
    }

    public Map<String, List<String>> validationErrors;
    // UPLOADRESULT, {fieldName: message}
    public Map<UPLOAD_RESULT, Map<String, String>> uploadResults;

    public Map<String, String> relationFilters;

    public Map<String, List<String>> getValidationErrors() {
        return validationErrors;
    }

    public Map<String, List<String>> relationErrors;


    public void setValidationErrors(Map<String, List<String>> validationErrors) {
        this.validationErrors = validationErrors;
    }

    public Map<UPLOAD_RESULT, Map<String, String>> getUploadResults() {
        return uploadResults;
    }

    public void setUploadResults(Map<UPLOAD_RESULT, Map<String, String>> uploadResults) {
        this.uploadResults = uploadResults;
    }

    public Map<String, String> getRelationFilters() {
        return relationFilters;
    }

    public void setRelationFilters(Map<String, String> relationFilters) {
        this.relationFilters = relationFilters;
    }

    public Map<String, List<String>> getRelationErrors() {
        return relationErrors;
    }

    public void setRelationErrors(Map<String, List<String>> relationErrors) {
        this.relationErrors = relationErrors;
    }

    public int numOfErrors() {
        return Optional.ofNullable(validationErrors).map(stringListMap -> stringListMap.size()).orElse(0);
    }

    public static Comparator<Meta> comparator = new Comparator<Meta>() {
        @Override
        public int compare(Meta m1, Meta m2) {
            if (m1 == null) {
                return 1;
            } else if (m2 == null) {
                return -1;
            } else {
                if (m1.numOfErrors() == m2.numOfErrors()) {
                    return 0;
                } else {
                    return m1.numOfErrors() > m2.numOfErrors() ? 1 : -1;
                }
            }
        }
    };
}
