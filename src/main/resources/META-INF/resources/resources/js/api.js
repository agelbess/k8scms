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

(function () {
        const Constructor = function () {

            const login = function (user) {
                return $.ajax({
                    type: 'POST',
                    contentType: 'application/json',
                    url: `/web-api/login`,
                    // TODO is stringify needed?
                    data: JSON.stringify(user),
                    error: $.cms.utils.ajaxError
                })
            }

            const logout = function (user) {
                return $.ajax({
                    type: 'GET',
                    url: `/web-api/logout`,
                    error: $.cms.utils.ajaxError
                })
            }

            const getProperties = function () {
                return $.ajax({
                    type: 'GET',
                    contentType: 'application/json',
                    url: '/web-api/properties',
                    error: $.cms.utils.ajaxError
                });
            }

            const getUser = function () {
                return $.ajax({
                    type: 'GET',
                    contentType: 'application/json',
                    url: '/web-api/user',
                    error: $.cms.utils.ajaxError
                });
            }

            const getModels = function () {
                return $.ajax({
                    type: 'GET',
                    contentType: 'application/json',
                    url: '/web-api/models',
                    error: $.cms.utils.ajaxError
                });
            }

            const get = function (model, filter) {
                return $.ajax({
                    type: 'GET',
                    contentType: 'application/json',
                    url: `/api/${model.database}/${model.collection}`,
                    data: filter,
                    error: $.cms.utils.ajaxError
                });
            }

            const getMeta = function (model) {
                return $.ajax({
                    type: 'GET',
                    contentType: 'application/json',
                    url: `/api/${model.database}/${model.collection}/meta`,
                    error: $.cms.utils.ajaxError
                });
            }

            const post = function (model, data) {
                return $.ajax({
                    type: 'POST',
                    contentType: 'application/json',
                    // TODO is that needed?
                    dataType: 'json',
                    url: `/api/${model.database}/${model.collection}`,
                    // TODO is stringify needed?
                    data: JSON.stringify(data),
                    error: $.cms.utils.ajaxError
                })
            }

            /** TODO
             * for some reason the POST that receives json only works with callbacks
             */
            const postGet = function (model, filter, callback) {
                $.ajax({
                    type: 'POST',
                    contentType: 'application/json',
                    dataType: 'json',
                    url: `/api/${model.database}/${model.collection}/GET`,
                    success: callback,
                    data: JSON.stringify(filter),
                    error: $.cms.utils.ajaxError
                })
            }

            const put = function (model, data, filter) {
                return $.ajax({
                    type: 'PUT',
                    contentType: 'application/json',
                    url: `/api/${model.database}/${model.collection}?${$.param(filter)}`,
                    data: JSON.stringify(data),
                    error: $.cms.utils.ajaxError
                })
            }

            const delete_ = function (model, filter) {
                return $.ajax({
                    type: 'DELETE',
                    contentType: 'application/json',
                    url: `/api/${model.database}/${model.collection}?${$.param(filter)}`,
                    error: $.cms.utils.ajaxError
                })
            }

            const validate = function (model, data, callback) {
                return $.ajax({
                    type: 'POST',
                    contentType: 'application/json',
                    dataType: 'json',
                    url: `/web-api/${model.database}/${model.collection}/validate`,
                    success: callback,
                    data: JSON.stringify({
                        data: data
                    }),
                    error: $.cms.utils.ajaxError
                })
            }

            return {
                login: login,
                logout: logout,
                getUser: getUser,
                getModels: getModels,
                getProperties: getProperties,
                get: get,
                post: post,
                postGet: postGet,
                put: put,
                delete: delete_,
                getMeta: getMeta,
                validate: validate,
            }
        }

        !$.cms ? $.cms = {} : null;

        jQuery.extend($.cms, {
            api: Constructor()
        });
    }
)();
