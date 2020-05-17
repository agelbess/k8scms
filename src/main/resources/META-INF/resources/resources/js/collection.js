(function () {
    const Constructor = function () {

        let table;

        const init = function () {
            console.debug('init collection');

            let ca = $.cms.auth.canAccess;
            let properties = $.cms.context.resources.properties;
            let models = $.cms.context.resources.models;

            for (let model of models) {
                ca(model.database, model.collection, 'GET') ?
                    $.cms.header.appendDrawer(
                        `${model.database}.${model.collection}`,
                        `#COLLECTION&database=${model.database}&collection=${model.collection}`)
                    : null;
            }

            let lastParams;
            $(window).on('hashchange', function (event) {
                // On every hash change the render function is called with the new
                // hash.
                // This is how the navigation of our app happens.
                let params = $.cms.utils.getHashParams();
                if (lastParams && !$.cms.utils.equal(params, lastParams)) {
                    start(params);
                }
                lastParams = params;
            });
        }

        const start = function (params) {
            console.debug('start collection');
            let rootE = $('#cms-collection').empty();
            let model = $.cms.context.resources.models.filter(m => m.database === params.database && m.collection === params.collection).pop();

            table = $.cms.table.create(model);
            rootE.append(table.contentE);
            $('#cms-header-header-title').text(`collection ${model.database}.${model.collection}`);
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
