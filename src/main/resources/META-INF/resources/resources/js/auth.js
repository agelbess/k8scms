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

        const canAccess = function (database, collection, method) {
            let permissions = $.cms.context.resources.userPermissions;
            for (let permission of permissions) {
                let tokens = permission.split(':');
                if (
                    (!database || database.match(tokens[0])) &&
                    (!collection || collection.match(tokens[1])) &&
                    (!method || method.match(tokens[2]))) {
                    return true;
                }
            }
        }

        const hide = function (eE, database, collection, method) {
            if (!canAccess(database, collection, method)) {
                eE.hide();
            }
        }

        const remove = function (eE, database, collection, method) {
            if (!canAccess(database, collection, method)) {
                eE.remove();
            }
        }

        const ca = function (database, collection, method) {
            if (typeof database === 'string') {
                if (!canAccess(database, collection, method)) {
                    $(this).hide();
                }
            } else {
                // shift the arguments
                let model = database;
                let method = collection;
                if (!canAccess(model.database, model.collection, method)) {
                    $(this).hide();
                }
            }
            return $(this);
        }

        // register in $
        // can also be invoked as (model, method)
        $.fn.ca = ca;

        return {
            canAccess: canAccess
        }
    }

    !$.cms ? $.cms = {} : null;

    jQuery.extend($.cms, {
        auth: Constructor()
    });
})();
