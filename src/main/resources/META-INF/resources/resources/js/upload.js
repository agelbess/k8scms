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

        const that = this;

        let json;
        let tableCellE;
        let uploadInputE;
        let clusterInputE;
        let databaseInputE;
        let collectionInputE;
        let putSwitch;
        let patchSwitch;
        let clearButtonE;

        const init = function () {
            console.debug('init upload');

            let rootE = $('#cms-upload').hide();
            let uploadCellE = $('<div>').addClass('mdl-cell mdl-cell--2-col').appendTo(rootE);
            let uploadGridE = $('<div>').addClass('mdl-grid').appendTo(uploadCellE);
            uploadInputE = $('<input type="file">')
                .addClass('mdl-button mdl-js-button mdl-js-ripple-effect mdl-button--raised mdl-button--colored mdl-cell mdl-cell--12-col')
                .mdl()
                .appendTo(uploadGridE);
            uploadInputE[0].addEventListener('change', handleFile, false);
            clusterInputE = createTextField('cms-upload-cluster', 'cluster', $.cms.context.resources.properties.cmsCluster)
                .addClass('mdl-cell mdl-cell--12-col')
                .appendTo(uploadGridE);
            databaseInputE = createTextField('cms-upload-database', 'database', $.cms.context.resources.properties.cmsDatabase)
                .addClass('mdl-cell mdl-cell--12-col')
                .appendTo(uploadGridE);
            collectionInputE = createTextField('cms-upload-collection', 'collection')
                .addClass('mdl-cell mdl-cell--12-col')
                .appendTo(uploadGridE);

            putSwitch = createSwitch('cms-form-upload-put', 'replace(put) by id');
            putSwitch.labelE
                .addClass('mdl-cell mdl-cell--12-col')
                .appendTo(uploadGridE);
            putSwitch.inputE.on('change', function () {
                patchSwitch.setValue(false);
            });
            patchSwitch = createSwitch('cms-form-upload-patch', 'update(patch) by id');
            patchSwitch.labelE
                .addClass('mdl-cell mdl-cell--12-col')
                .appendTo(uploadGridE);
            patchSwitch.inputE.on('change', function () {
                putSwitch.setValue(false);
            });
            let postButtonE = $('<button>').addClass('mdl-button mdl-js-button mdl-js-ripple-effect mdl-button--raised')
                .addClass('mdl-cell mdl-cell--12-col')
                .text('upload')
                .on('click', postJson)
                .mdl()
                .appendTo(uploadGridE);
            clearButtonE = $('<button>').addClass('mdl-button mdl-js-button mdl-js-ripple-effect mdl-button--raised')
                .addClass('mdl-cell mdl-cell--12-col')
                .text('clear')
                .on('click', clear)
                .mdl()
                .appendTo(uploadGridE);

            tableCellE = $('<div>').addClass('mdl-cell mdl-cell--10-col').appendTo(rootE);

            function handleFile(e) {
                let files = e.target.files;
                let f = files[0];
                if (f) {
                    // get database and collection from the filename
                    let fileTokens = f.name.split('.');
                    let filename = fileTokens[0];
                    let ext = fileTokens[1];
                    let filenameTokens = filename.split('_');
                    if (filenameTokens.length === 1) {
                        collectionInputE.find('input').val(filenameTokens[0]);
                    } else if (filenameTokens.length === 2) {
                        databaseInputE.find('input').val(filenameTokens[0]);
                        collectionInputE.find('input').val(filenameTokens[1]);
                    } else if (filenameTokens.length > 2) {
                        clusterInputE.find('input').val(filenameTokens[0]);
                        databaseInputE.find('input').val(filenameTokens[1]);
                        collectionInputE.find('input').val(filenameTokens[2]);
                    }

                    // after setting the database and collection
                    let model = getModel();

                    if (ext === 'json') {
                        console.debug('uploaded JSON');
                        let reader = new FileReader();
                        reader.onload = function (e) {
                            let json = JSON.parse(e.target.result);
                            setJson(json);
                        }
                        reader.readAsText(f);
                    } else {
                        console.debug('uploaded non JSON, try to parse as excel')
                        let reader = new FileReader();
                        reader.onload = function (e) {
                            let data = new Uint8Array(e.target.result);
                            // cellDates do not work correctly
                            // let workbook = XLSX.read(data, {type: 'array', cellDates: true});
                            let workbook = XLSX.read(data, {type: 'array'});

                            /* DO SOMETHING WITH workbook HERE */
                            let sheet = workbook.Sheets[workbook.SheetNames[0]];
                            let range = XLSX.utils.decode_range(sheet['!ref']);
                            let rows = [];
                            for (let rowNum = range.s.r; rowNum <= range.e.r; rowNum++) {
                                let row = [];
                                for (let colNum = range.s.c; colNum <= range.e.c; colNum++) {
                                    let nextCell = sheet[XLSX.utils.encode_cell({r: rowNum, c: colNum})];
                                    if (typeof nextCell === 'undefined') {
                                        row.push(void 0);
                                    } else {
                                        row.push(nextCell);
                                    }
                                }
                                rows.push(row);
                            }
                            let json = [];
                            let headers = rows[0];
                            for (let h = 0; h < headers.length; h++) {
                                if (!headers[h]) {
                                    $.cms.log.warn(`header on index ${h + 1} is empty...'`);
                                    $.cms.log.warn(`...all headers must have a proper field name value from the collection'`);
                                    return;
                                }
                            }
                            ;
                            for (let r = 1; r < rows.length; r++) {
                                let jsonObject = {};
                                let row = rows[r];
                                for (let c = 0; c < row.length; c++) {
                                    if (row[c]) {
                                        let cell = row[c];
                                        let field = $.cms.utils.findFieldByName(model.fields, headers[c].v);
                                        if (cell) {
                                            let value;
                                            if (field && (field.type === 'json' || field.type === 'array' || field.type === 'geoJson')) {
                                                try {
                                                    value = JSON.parse(cell.v);
                                                } catch (e) {
                                                    value = cell.v;
                                                }
                                            } else if (field && field.type === 'boolean') {
                                                value = Boolean(cell.v);
                                            }
                                            // check for date and exclude the currency formats
                                            else if (
                                                cell.t === 'n'
                                                && typeof cell.v === 'number'
                                                && (cell.v + '') !== cell.w
                                                // for double with formatting, e.g. v=35 w="35.00", or v=35.12345 w="35.12"
                                                && cell.w.indexOf('.') === -1
                                                && cell.w.indexOf('%') === -1
                                                && cell.w.indexOf('$') === -1
                                                && cell.w.indexOf('€') === -1) {
                                                // value = {'$date': new Date((cell.v - 25569) * 86400 * 1000).toISOString()};
                                                value = new Date((cell.v - 25569) * 86400 * 1000).toISOString();
                                            } else if (cell.v && field && (field.type === undefined || field.type === 'string')) {
                                                value = cell.v + '';
                                            } else {
                                                value = cell.v;
                                            }
                                            jsonObject[headers[c].v] = value;
                                        }
                                    }
                                }
                                json.push(jsonObject);
                            }
                            setJson(json);

                        };
                        reader.readAsArrayBuffer(f);
                    }
                } else {
                    clear();
                }
            }
        }

        const setJson = function (json) {
            that.json = json;
            let model = {...getModel()};
            model.fields = model.fields.filter(f => !f.relation && !f.virtual);
            if (!model.fields.length) {
                let fieldNames = [];
                let fields = [];
                for (let d of json) {
                    for (let c in d) {
                        if (!fieldNames.includes(c)) {
                            fieldNames.push(c);
                            fields.push({
                                name: c
                            });
                        }
                    }
                }
                model.fields = fields;
                $.cms.dialog.confirm({
                    timeout: 5000,
                    message: `Model ${model.cluster}.${model.database}.${model.collection} is missing, create it?`,
                    actionText: 'create it',
                    actionHandler: async () => {
                        let modelModel = {
                            cluster: $.cms.context.resources.properties.cluster,
                            database: $.cms.context.resources.properties.database,
                            collection: $.cms.context.resources.properties.collectionModel
                        }
                        await $.cms.api.post(modelModel, model);
                        $.cms.log.info(`generated model for ${model.cluster}.${model.database}.${model.collection}, please reload the page`);
                    }
                });
            } else {
                model.fields.unshift({
                    name: '_json',
                    type: 'json'
                });
                model.fields.unshift({
                    name: '_validationChanges',
                    type: 'json'
                });
                $.cms.api.validateAndFindValidMethods(model, json, (data) => {
                    let dataCopy = [...data];
                    dataCopy.forEach((d, i) => {
                        d._json = json[i];
                        d._validationChanges = d._meta.validationChanges;
                    });
                    let table = $.cms.table.create(model, dataCopy);
                    tableCellE.empty();
                    table.contentE.appendTo(tableCellE);
                    table.refresh();
                    $.cms.log.info(`loaded ${data.length} documents`)
                });
            }
        }

        const postJson = async function () {
            let method = 'post';
            if (putSwitch.getValue()) {
                method = 'put';
            }
            if (patchSwitch.getValue()) {
                method = 'patch';
            }
            let model = getModel();
            let fields = [];
            // to check for uniqueness
            let fieldsMap = {};
            let count = {
                inserted: 0,
                upserted: 0,
                matched: 0,
                modified: 0
            }
            $.cms.utils.loading();
            try {
                let triggerCounter = 10;
                $.cms.log.info(`uploading ${that.json.length}...`);
                for (let i = 0; i < that.json.length; i++) {
                    let data = that.json[i];
                    if (that.json.length > triggerCounter - 1 && i > 0 && i % triggerCounter == 0) {
                        $.cms.log.info(`...uploaded ${i} of ${that.json.length}`);
                    }
                    if (method === 'post') {
                        // POST returns void or throws errors
                        let result = await $.cms.api.post(model, data);
                        if (result.insertedId != null && result.insertedId != undefined) {
                            count.inserted++;
                        }
                    } else {
                        // PUT|PATCH
                        if (model.fields) {
                            let idFields = $.cms.utils.getIdFields(model);
                            if (idFields.length) {
                                // PUT contains result for updated records
                                let result;
                                let filter = $.cms.utils.filterObject(data, idFields.map(f => f.name));
                                if (method === 'put') {
                                    result = await $.cms.api.put(model, data, filter);
                                } else {
                                    result = await $.cms.api.patch(model, data, filter);
                                }
                                count.matched += result.matchedCount;
                                count.modified += result.modifiedCount;
                                if (result.upsertedId !== null && result.upsertedId !== undefined) {
                                    count.upserted++;
                                }
                            } else {
                                $.cms.log.info(`no 'id' field in model`);
                                break;
                            }
                        } else {
                            $.cms.log.info('no model found for collection');
                            break;
                        }
                    }
                }
                $.cms.log.info(`...done uploading ${that.json.length}`);
            } finally {
                let message =
                    `inserted ${count.inserted}, ` +
                    `upserted ${count.upserted}, ` +
                    `matched ${count.matched}, ` +
                    `modified ${count.modified}`;
                $.cms.log.info(message + '...')
                $.cms.log.info(`... out of ${that.json.length} documents in ${model.cluster}.${model.database}.${model.collection}`);
                $.cms.utils.loading(true);
                clear();
            }
        }

        const createTextField = function (id, label, value) {
            let divE = $('<div>').addClass('mdl-textfield mdl-js-textfield mdl-textfield--floating-label').mdl();
            let inputE = $('<input type="text">').addClass('mdl-textfield__input').attr('id', id).appendTo(divE);
            if (value) {
                inputE.val(value);
            }
            let labelE = $('<label>').addClass('mdl-textfield__label').attr('for', id).text(label).appendTo(divE);
            return divE;
        }

        const createSwitch = function (id, label) {
            let labelE = $('<label>')
                .attr('for', id)
                .addClass('mdl-switch mdl-js-switch mdl-js-ripple-effect').mdl();
            let inputE = $('<input type="checkbox">')
                .attr('id', id)
                .addClass('mdl-switch__input')
                .appendTo(labelE);
            let spanE = $('<div>').addClass('mdl-switch__label').text(label).appendTo(labelE);

            const getValue = function () {
                return labelE.hasClass('is-checked');
            }
            const setValue = function (value) {
                if (value) {
                    window.setTimeout(function () {
                        labelE.addClass('is-checked');
                    }, 0);
                } else {
                    labelE.removeClass('is-checked')
                }
            }

            return {
                labelE: labelE,
                inputE: inputE,
                getValue: getValue,
                setValue: setValue
            }
        }

        const getCluster = function () {
            return clusterInputE.find('input').val();
        }

        const getDatabase = function () {
            return databaseInputE.find('input').val();
        }

        const getCollection = function () {
            return collectionInputE.find('input').val();
        }

        const getModel = function () {
            let cluster = getCluster();
            let database = getDatabase();
            let collection = getCollection();
            let model = $.cms.context.resources.models.filter(
                m => m.cluster === cluster && m.database === database && m.collection === collection
            ).pop();
            if (!model) {
                model = {
                    cluster: cluster,
                    database: database,
                    collection: collection,
                    fields: []
                }
            }
            return model;
        }

        const start = function (params) {
            console.debug('start upload', params);

            if (params.cluster || params.database || params.collection) {
                let cluster = clusterInputE.find('input');
                let database = databaseInputE.find('input');
                let collection = collectionInputE.find('input');

                if (!cluster.val()) {
                    cluster.val(params.cluster).mdlNot().mdl();
                    clusterInputE.mdlNot().mdl();
                }
                if (!database.val()) {
                    database.val(params.database);
                    databaseInputE.mdlNot().mdl();
                }
                if (!collection.val()) {
                    collection.val(params.collection);
                    collectionInputE.mdlNot().mdl();
                }
            }
        }

        const clear = function () {
            uploadInputE.val('');
            tableCellE.empty();
            clusterInputE.find('input').val('');
            databaseInputE.find('input').val('');
            collectionInputE.find('input').val('');
        }

        return {
            init: init,
            start: start
        }
    }

    !$.cms ? $.cms = {} : null;

    jQuery.extend($.cms, {
        upload: Constructor()
    });
})();
