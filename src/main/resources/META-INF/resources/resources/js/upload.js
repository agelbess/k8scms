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

        const that = this;

        let json;
        let tableCellE;
        let uploadInputE;
        let databaseInputE;
        let collectionInputE;
        let updateSwitch;
        let clearButtonE;
        let postMessage = `creating documents on existing collections with an 'id' field can potentially generate duplication errors if the data contain 'id's`;

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
            databaseInputE = createTextField('cms-upload-database', 'database', $.cms.context.resources.properties.adminDatabase)
                .addClass('mdl-cell mdl-cell--12-col')
                .appendTo(uploadGridE);
            collectionInputE = createTextField('cms-upload-collection', 'collection')
                .addClass('mdl-cell mdl-cell--12-col')
                .appendTo(uploadGridE);

            updateSwitch = createSwitch('cms-form-upload-update', 'update by id');
            updateSwitch.labelE
                .addClass('mdl-cell mdl-cell--12-col')
                .appendTo(uploadGridE);
            updateSwitch.inputE.on('change', function () {
                window.setTimeout(function () {
                    if (updateSwitch.getValue()) {
                        $.cms.log.info(`updating documents with 'id's that do not exist in the collection...`);
                        $.cms.log.info(`...will NOT update them, the 'snackbar' contains the results`);
                    }
                })
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
                $.cms.log.info(postMessage);
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
                            for (let r = 1; r < rows.length; r++) {
                                let jsonObject = {};
                                let row = rows[r];
                                for (let c = 0; c < row.length; c++) {
                                    let cell = row[c];
                                    let field = $.cms.utils.findFieldByName(model.fields, headers[c].v);
                                    if (cell) {
                                        let value;
                                        if (field && field.type === 'json') {
                                            try {
                                                value = JSON.parse(cell.v);
                                            } catch (e) {
                                                value = cell.v;
                                            }
                                        } else if (field && field.type === 'boolean') {
                                            value = Boolean(cell.v);
                                        }
                                        // check for date and exclude the currency formats
                                        else if (cell.t === 'n' && typeof cell.v === 'number' && (cell.v + '') !== cell.w && cell.w.indexOf('%') === -1 && cell.w.indexOf('€') === -1) {
                                            // value = {'$date': new Date((cell.v - 25569) * 86400 * 1000).toISOString()};
                                            value = new Date((cell.v - 25569) * 86400 * 1000).toISOString();
                                        } else {
                                            value = cell.v;
                                        }
                                        jsonObject[headers[c].v] = value;
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
            model.fields = [...model.fields];
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
                    message: `Model ${model.database}.${model.collection} is missing, create it?`,
                    actionText: 'create it',
                    actionHandler: async () => {
                        let modelModel = {
                            database: $.cms.context.resources.properties.database,
                            collection: $.cms.context.resources.properties.collectionModel
                        }
                        await $.cms.api.post(modelModel, model);
                        $.cms.log.info(`generated model for ${model.database}.${model.collection}, please reload the page`);
                    }
                });
            } else {
                model.fields.unshift({
                    name: '_json',
                    label: '_json',
                    type: 'json'
                });
                $.cms.api.validate(model, json, (data) => {
                    let dataCopy = [...data];
                    dataCopy.forEach((d, i) => {
                        d._json = json[i];
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
            let post = !updateSwitch.getValue();
            let model = getModel();
            let fields = [];
            // to check for uniqueness
            let fieldsMap = {};
            let count = {
                matched: 0,
                modified: 0
            }
            $.cms.utils.loading();
            try {
                for (let data of that.json) {
                    // remove the meta
                    if (post) {
                        // POST returns void or throws errors
                        await $.cms.api.post(model, data);
                        count.modified++;
                    } else {
                        // PUT, get the model already saved
                        if (model.fields) {
                            let idField = $.cms.utils.getIdField(model);
                            if (idField) {
                                // PUT contains result for updated records
                                let result = await $.cms.api.put(model, data, {[idField.name]: data[idField.name]});
                                count.matched += result.matchedCount;
                                count.modified += result.modifiedCount;
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
            } finally {
                if (post) {
                    $.cms.log.info(`created ${count.modified}...`);
                    $.cms.log.info(`... out of ${that.json.length} documents in ${model.database}.${model.collection}`);
                } else {
                    $.cms.log.info(`matched ${count.matched}, updated ${count.modified}...`);
                    $.cms.log.info(`... out of ${that.json.length} documents in ${model.database}.${model.collection}`);
                }
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

        const getDatabase = function () {
            return databaseInputE.find('input').val();
        }

        const getCollection = function () {
            return collectionInputE.find('input').val();
        }

        const getModel = function () {
            let database = getDatabase();
            let collection = getCollection();
            let model = $.cms.context.resources.models.filter(
                m => m.database === database && m.collection === collection
            ).pop();
            if (!model) {
                model = {
                    database: getDatabase(),
                    collection: getCollection(),
                    fields: []
                }
            }
            return model;
        }

        const start = function (params) {
            console.debug('start upload', params);

            if (params.database || params.collection) {
                let database = databaseInputE.find('input');
                let collection = collectionInputE.find('input');

                if (!database.val()) {
                    database.val(params.database);
                }
                if (!collection.val()) {
                    collection.val(params.collection);
                }
            }
        }

        const clear = function () {
            uploadInputE.val(null);
            tableCellE.empty();
            databaseInputE.find('input').val(null);
            collectionInputE.find('input').val(null);
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
