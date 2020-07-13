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

(function () {
    const Constructor = function () {

        let table;

        const init = function () {
            console.debug('init collection');

            let models = $.cms.context.resources.models.sort((a, b) => ('' + a.name).localeCompare(b.name));

            for (let model of models) {
                if ($.cms.auth.canAccess(model, 'GET')) {
                    $.cms.header.appendDrawer(
                        `${model.name}`,
                        `#COLLECTION&cluster=${model.cluster}&database=${model.database}&collection=${model.collection}`);
                }
            }

            let lastParams;
            $(window).on('hashchange', function (event) {
                // On every hash change the render function is called with the new
                // hash.
                // This is how the navigation of our app happens.
                let params = $.cms.utils.getHashParams();
                if (Object.keys(params).includes('COLLECTION') && lastParams && !$.cms.utils.equal(params, lastParams)) {
                    start(params);
                }
                lastParams = params;
            });
        }

        const start = function (params) {
            console.debug('start collection');
            let rootE = $('#cms-collection').empty();
            let model = $.cms.context.resources.models.filter(m => m.cluster === params.cluster && m.database === params.database && m.collection === params.collection).pop();

            table = $.cms.table.create(model);
            rootE.append(table.contentE);
            let title = `collection ${model.name}`;
            $('#cms-header-header-title').text(title);
            $('#cms-title').text(`${$.cms.context.resources.properties.projectName} ${title}`);
            table.refresh();
        }

        return {
            init: init,
            start: start
        }
    }

    !$.cms ? $.cms = {} : null;

    jQuery.extend($.cms, {
        collection: Constructor()
    });
})();
