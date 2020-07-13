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

        const create = function (model, data) {
            let table;
            const uid = new Date().getTime();
            const isStatic = data;

            let contentE = $('<div>').addClass('mdl-grid cms-table-grid');
            let tableCellE = $('<div>').addClass('mdl-cell mdl-cell--12-col').appendTo(contentE);
            let queryInputE;
            let pageCountersE;

            const refresh = function () {
                setTimeout(() => refresh_(), 100);
            }
            let autoRefreshId;
            const autoRefresh = function () {
                clearInterval(autoRefreshId);
                autoRefreshId = setInterval(() => refresh_(), 5000);
            }
            if ($.cms.utils.getHashParam('_autoRefresh')) {
                autoRefresh();
            }
            const refresh_ = async function () {
                let meta = await $.cms.api.getMeta(model);
                let filter64 = $.cms.utils.getHashParam('filter');
                let filter;
                if (filter64) {
                    try {
                        filter = JSON.parse(decodeURIComponent(atob(filter64)));
                    } catch (e) {
                        console.warn(`invalid filter`);
                    }
                }

                tableCellE.hide();
                tableCellE.empty();

                let tableE = $('<table>').addClass('mdl-data-table mdl-js-data-table mdl-shadow--2dp cms-margin-auto')
                    .addClass(isStatic ? '' : 'mdl-data-table--selectable')
                    .attr('id', `${table.model.collection}-${uid}`)
                    .appendTo(tableCellE);
                let tableHeaderE = $('<thead>').appendTo(tableE);
                let headerRowE = $('<tr>').appendTo(tableHeaderE);

                const updatePaging = function (documentCount) {
                    let skip = parseInt($.cms.utils.getHashParam('_skip')) || 0;
                    let from = skip + 1;
                    let limit = parseInt($.cms.utils.getHashParam('_limit')) || $.cms.context.resources.properties.pageSize;
                    let to = Math.min(skip + limit, documentCount);
                    pageCountersE.text(`${from}..${to} / ${documentCount}`);
                }

                if (!isStatic) {
                    // the pagination
                    let documentCount = meta.estimatedDocumentCount;
                    let limit = parseInt($.cms.utils.getHashParam('_limit')) || $.cms.context.resources.properties.pageSize;
                    let skip = parseInt($.cms.utils.getHashParam('_skip')) || 0;
                    let pages = Math.ceil(documentCount / limit);
                    let page = skip / limit + 1;
                    let defaultPageSize = $.cms.context.resources.properties.pageSize;
                    let slidersE = $('<div>').addClass('mdl-grid').prependTo(tableCellE);
                    let pageSliderCellE = $('<div>').addClass('mdl-cell--9-col').appendTo(slidersE);
                    let pageSizeSliderCellE = $('<div>').addClass('mdl-cell--2-col').appendTo(slidersE);
                    let pageCountersCellE = $('<div>').addClass('mdl-cell--1-col cms-table-counters').appendTo(slidersE);
                    let pageSizeSliderE = $('<input type="range">').addClass('mdl-slider mdl-js-slider')
                        .attr('id', 'cms-table-page-size-slider')
                        .attr('min', defaultPageSize)
                        .attr('max', Math.min(documentCount, $.cms.context.resources.properties.limit))
                        .attr('step', 1)
                        .attr('value', limit)
                        .mdl()
                        .appendTo(pageSizeSliderCellE)
                        .on('change', () => {
                            $.cms.utils.setHashParam('_skip', 0);
                            $.cms.utils.setHashParam('_limit', pageSizeSliderE.val());
                        });
                    let pageSizeSliderTooltipE = $('<div>').addClass('mdl-tooltip mdl-tooltip--top').attr('for', 'cms-table-page-size-slider')
                        .text(`page size ${limit}`)
                        .mdl()
                        .appendTo(pageSizeSliderCellE);
                    let pageSliderE = $('<input type="range" min="0">').addClass('mdl-slider mdl-js-slider')
                        .attr('id', 'cms-table-page-slider')
                        .attr('max', documentCount - 1)
                        .attr('step', pageSizeSliderE.val())
                        .attr('value', skip)
                        .mdl()
                        .appendTo(pageSliderCellE)
                        .on('change', () => {
                            $.cms.utils.setHashParam('_skip', pageSliderE.val());
                        });
                    let pageSliderTooltipE = $('<div>').addClass('mdl-tooltip mdl-tooltip--top').attr('for', 'cms-table-page-slider')
                        .text(`page ${page}/${pages}`)
                        .mdl()
                        .appendTo(pageSliderCellE);
                    let previousPageE = $('<span>').addClass('material-icons').text('skip_previous').appendTo(pageCountersCellE)
                        .on('click', () => {
                            let skip = parseInt(pageSliderE.val()) - parseInt(pageSizeSliderE.val());
                            if (skip > -1) {
                                $.cms.utils.setHashParam('_skip', skip);
                            }
                        });
                    let nextPageE = $('<span>').addClass('material-icons').text('skip_next').appendTo(pageCountersCellE)
                        .on('click', () => {
                            let skip = parseInt(pageSliderE.val()) + parseInt(pageSizeSliderE.val());
                            if (skip < documentCount) {
                                $.cms.utils.setHashParam('_skip', skip);
                            }
                        });
                    pageCountersE = $('<span>').addClass('cms-table-counters-fromTo').appendTo(pageCountersCellE);
                    updatePaging(documentCount);

                    // query
                    let id = `cms-table-query-${model.database}-${model.collection}-${uid}`;
                    let queryGridE = $('<div>').addClass('mdl-grid').prependTo(tableCellE);
                    let queryE = $('<div>').addClass('mdl-cell--12-col').appendTo(queryGridE);
                    let queryExpandableE = $('<div>').addClass('mdl-textfield mdl-js-textfield mdl-textfield--expandable cms-table-query-expandable').css({width: '100%'}).appendTo(queryE);
                    let queryLabelE = $('<label>').addClass('mdl-button mdl-js-button mdl-button--icon').attr('for', id).mdl().appendTo(queryExpandableE);
                    let queryIconE = $('<span>').addClass('material-icons').text('search_off')
                        .on('click', () => $.cms.utils.removeHashParam('filter'))
                        .appendTo(queryLabelE);
                    let queryHolderE = $('<div>').addClass('mdl-textfield__expandable-holder').css({
                        width: '100%',
                        maxWidth: '100%'
                    }).appendTo(queryExpandableE);
                    let queryHolderLabelE = $('<label>').addClass('mdl-textfield__label').attr('for', 'dummy').appendTo(queryHolderE);
                    let queryTimeoutId;
                    queryInputE = $('<input type="text">').addClass('mdl-textfield__input').attr('id', id).css({width: '100%'}).appendTo(queryHolderE)
                        .on('keyup', () => {
                            try {
                                queryInputE.css({color: 'red'});
                                let json = JSON.parse(queryInputE.val());
                                queryInputE.css({color: 'inherit'});
                            } catch (e) {
                                // do nothing
                            }
                            clearTimeout(queryTimeoutId);
                            queryTimeoutId = setTimeout(async () => {
                                try {
                                    let filter = queryInputE.val();
                                    if (filter) {
                                        JSON.parse(filter);
                                        $.cms.utils.removeHashParam('_skip');
                                        $.cms.utils.setHashParam('filter', btoa(encodeURIComponent(filter)));
                                    } else {
                                        $.cms.utils.setHashParam('filter', '');
                                    }
                                } catch (e) {
                                    console.log(e);
                                }
                            }, 1000);
                        });
                    queryInputE.val(JSON.stringify(filter));

                    // table
                    let actionsHeaderE = $('<th>').appendTo(headerRowE);
                    let refreshActionHeaderE = $('<span>')
                        .addClass('material-icons cms-table-action')
                        .text('refresh')
                        .on('click', refresh)
                        .appendTo(actionsHeaderE);
                    let autoRefreshHashParam = $.cms.utils.getHashParam('_autoRefresh');
                    let autoRefreshActionHeaderE = $('<span>')
                        .addClass('material-icons cms-table-action')
                        .addClass(autoRefreshHashParam ? 'cms-button-on' : '')
                        .text('sync')
                        .on('click', () => {
                            if (autoRefreshHashParam) {
                                $.cms.utils.setHashParam('_autoRefresh', '');
                            } else {
                                $.cms.utils.setHashParam('_autoRefresh', true);
                            }
                        })
                        .appendTo(actionsHeaderE);
                    let createActionHeaderE = $('<span>')
                        .addClass('material-icons cms-table-action')
                        .text('post_add')
                        .on('click', showPostDialog)
                        .appendTo(actionsHeaderE)
                        .caHide(model, 'POST');
                    let idFields = $.cms.utils.getIdFields(model);
                    if (idFields.length) {
                        let deleteActionHeaderE = $('<span>')
                            .attr('id', 'cms-table-header-action-delete')
                            .addClass('material-icons mdl-badge mdl-badge--overlap cms-table-action')
                            .text('delete_sweep')
                            .attr('data-badge', 0)
                            .on('click', () => table.deleteSelected())
                            .appendTo(actionsHeaderE)
                            .caHide(model, 'DELETE');
                        tableE.on('click', 'td,th label input[type="checkbox"]', function () {
                            window.setTimeout(function () {
                                let countSelected = table.tableE.find('tr.is-selected').length;
                                deleteActionHeaderE.attr('data-badge', countSelected);
                            }, 100)
                        });
                    }
                    let uploadActionHeaderE = $('<span>')
                        .addClass('material-icons cms-table-action')
                        .text('cloud_upload')
                        .on('click', () => window.location.href = `#UPLOAD&cluster=${model.cluster}&database=${model.database}&collection=${model.collection}`)
                        .appendTo(actionsHeaderE)
                        .caHide(model, 'POST');
                    let downloadActionHeaderExcelId = 'cms-table-download-excel';
                    let downloadActionHeaderExcelE = $('<span>')
                        .attr('id', downloadActionHeaderExcelId)
                        .addClass('material-icons cms-table-action')
                        .text('cloud_download')
                        .on('click', () => {
                            $.cms.download.xlsx(table.data, model);
                            $.cms.log.info(`downloaded ${table.data.length} documents from ${model.database}.${model.collection}`)
                        })
                        .appendTo(actionsHeaderE);
                    $('<div>')
                        .addClass('mdl-tooltip mdl-tooltip--top cms-table-tooltip-icon')
                        .attr('for', downloadActionHeaderExcelId)
                        .text('download excel')
                        .mdl()
                        .appendTo(downloadActionHeaderExcelE);
                    let downloadActionHeaderJsonId = 'cms-table-download-json';
                    let downloadActionHeaderJsonE = $('<span>')
                        .attr('id', downloadActionHeaderJsonId)
                        .addClass('material-icons cms-table-action')
                        .text('cloud_download')
                        .on('click', () => {
                            $.cms.download.json(table.data, model);
                            $.cms.log.info(`downloaded ${table.data.length} documents from ${model.database}.${model.collection}`)
                        })
                        .appendTo(actionsHeaderE);
                    $('<div>')
                        .addClass('mdl-tooltip mdl-tooltip--top cms-table-tooltip-icon')
                        .attr('for', downloadActionHeaderJsonId)
                        .text('download JSON')
                        .mdl()
                        .appendTo(downloadActionHeaderJsonE);
                }

                let metaHeaderId = `cms-table-${model.collection}-meta-${uid}`;
                let metaHeaderE = $('<th>').attr('id', metaHeaderId).appendTo(headerRowE);
                let metaHeaderCounter = 0;
                let metaValidationErrorsMap = {};
                let metaValidationChangesMap = {};
                let headerWarnE = $('<div>')
                    .addClass('material-icons mdl-badge mdl-badge--overlap')
                    .attr('data-badge', metaHeaderCounter)
                    .text('warning')
                    .on('click', () => {
                        if (Object.keys(metaValidationErrorsMap).length) {
                            $.cms.utils.copyToClipboard(JSON.stringify(metaValidationErrorsMap, undefined, 2));
                            $.cms.log.info('validation errors copied to clipboard');
                        }
                    })
                    .appendTo(metaHeaderE);
                let headerWarnTooltipE = $('<div>').addClass('mdl-tooltip mdl-tooltip--right cms-table-tooltip-warn').attr('for', metaHeaderId)
                    .text(metaHeaderCounter ? `${metaHeaderCounter} warnings` : 'no warnings')
                    .mdl()
                    .appendTo(metaHeaderE);
                if (!isStatic) {
                    let sortEId = 'cms-table-warn-sort';
                    let sortE = $('<span>').attr('id', sortEId);
                    let _sort = $.cms.utils.getHashParam('_sort');
                    if (_sort !== '_meta.validationErrors') {
                        sortE = sortE.addClass('material-icons cms-table-header-sort').text('sort');
                    } else {
                        sortE = sortE.addClass('mdl-badge cms-table-header-sort-selected').attr('data-badge', $.cms.utils.getHashParam('_sortDirection') > 0 ? '➚' : '➘');
                    }
                    sortE.appendTo(metaHeaderE);
                    sortE.on('click', () => {
                        let _sort = $.cms.utils.getHashParam('_sort');
                        let _sortDirection = $.cms.utils.getHashParam('_sortDirection');
                        let val = '_meta.validationErrors';
                        if (_sort !== val) {
                            $.cms.utils.setHashParam('_sort', val);
                            $.cms.utils.setHashParam('_sortDirection', -1);
                        } else if (_sortDirection == -1) {
                            $.cms.utils.setHashParam('_sort', val);
                            $.cms.utils.setHashParam('_sortDirection', 1);
                        } else {
                            // remove sorting
                            $.cms.utils.removeHashParam('_sort');
                            $.cms.utils.removeHashParam('_sortDirection');
                        }
                    });
                }

                model.fields.filter(f => !f.hidden).forEach(f => {
                    let id = `cms-table-header-name-${f.name}-${uid}`;
                    let headerE = $('<th>')
                        .attr('id', id)
                        .addClass(f.id ? 'cms-table-id' : '')
                        .appendTo(headerRowE);
                    $.cms.utils.getFieldIcon(f).addClass('cms-table-header-type').appendTo(headerE);
                    let headerNameE = $('<span>').text(f.label || f.name).appendTo(headerE);
                    if (f.virtual) {
                        headerNameE.addClass('cms-table-header-virtual');
                    }
                    if (f.relation) {
                        headerNameE.addClass('cms-table-header-relation');
                    }
                    if (!isStatic) {
                        if (!f.relation && !f.virtual) {
                            headerNameE.on('dblclick', () => {
                                switch (f.type) {
                                    case 'integer':
                                    case 'decimal':
                                        $.cms.log.info(`'$gte' means 'greater than or equal', try '$lte', '$ge', '$le' or replace '{"$gte":0}' with a number`);
                                        break;
                                }
                                // create a dialog
                                let formModel = {
                                    fields: [f]
                                }
                                let form = $.cms.form.create(formModel);
                                let action = function () {
                                    let values = form.getValues();
                                    // get the single value
                                    let value = values[Object.keys(values).pop()];
                                    switch (f.type) {
                                        case 'string':
                                        case 'phone':
                                        case 'email':
                                        case 'cron':
                                            value = {
                                                '$regex': value || '',
                                                '$options': 'i',
                                            };
                                            break;
                                        case 'integer':
                                        case 'decimal':
                                            value = {
                                                '$gte': value || 0
                                            }
                                            break;
                                        default:
                                            break;
                                    }
                                    let filter = {
                                        [f.name]: value
                                    }
                                    $.cms.utils.removeHashParam('_skip');
                                    $.cms.utils.setHashParam('filter', btoa(encodeURIComponent(JSON.stringify(filter))));
                                }
                                let formActions = [
                                    {
                                        label: 'apply',
                                        action: action,
                                        keyup: {
                                            key: 'Enter'
                                        }
                                    }
                                ];
                                let dialog = $.cms.dialog.create('filter', form.contentE, formActions);
                                dialog.show();
                            });
                        }
                    }
                    let tooltips = [];
                    if (f.regex) {
                        tooltips.push('regex: ' + f.regex);
                    }
                    if (f.relation) {
                        tooltips.push('relation: ' + JSON.stringify(f.relation));
                    }
                    if (tooltips.length) {
                        $('<div>').text(tooltips.join('\n'))
                            .addClass('mdl-tooltip mdl-tooltip--top')
                            .attr('for', id)
                            .mdl()
                            .appendTo(headerE);
                    }

                    if (f.name === '_validationChanges') {
                        headerE.addClass('cms-table-header-action')
                            .on('click', () => {
                                if (Object.keys(metaValidationChangesMap).length) {
                                    $.cms.utils.copyToClipboard(JSON.stringify(metaValidationChangesMap, undefined, 2));
                                    $.cms.log.info('validation errors copied to clipboard');
                                }
                            });
                    }

                    if (!isStatic && !f.relation && !f.virtual) {
                        let sortE;
                        let _sort = $.cms.utils.getHashParam('_sort');
                        if (_sort !== f.name) {
                            sortE = $('<span>').addClass('material-icons cms-table-header-sort').text('sort');
                        } else {
                            sortE = $('<span>').addClass('mdl-badge cms-table-header-sort-selected').attr('data-badge', $.cms.utils.getHashParam('_sortDirection') > 0 ? '➚' : '➘');
                        }
                        sortE.appendTo(headerE);
                        sortE.on('click', () => {
                            let _sort = $.cms.utils.getHashParam('_sort');
                            let _sortDirection = $.cms.utils.getHashParam('_sortDirection');
                            if (_sort !== f.name) {
                                $.cms.utils.setHashParam('_sort', f.name);
                                $.cms.utils.setHashParam('_sortDirection', 1);
                            } else if (_sortDirection == 1) {
                                $.cms.utils.setHashParam('_sort', f.name);
                                $.cms.utils.setHashParam('_sortDirection', -1);
                            } else {
                                // remove sorting
                                $.cms.utils.removeHashParam('_sort');
                                $.cms.utils.removeHashParam('_sortDirection');
                            }
                        });
                    }
                });

                let tableBodyE = $('<tbody>');

                const setData = function (data, callback) {
                    table.data = data;
                    let i = 0;
                    let hasSecret1Columns = model.fields.filter(c => c.type === 'secret1').length > 0;
                    if (hasSecret1Columns && !isStatic) {
                        $.cms.log.info(`model has 'secret1' field types, updating the document would corrupt the secret fields...`)
                        $.cms.log.info(`...editing the document is disabled, try editing each field individually`);
                    }
                    data.forEach(async o => {
                            await $.cms.utils.sleep(0);
                            let rowE = $('<tr>').data('index', i++).appendTo(tableBodyE);
                            if (!isStatic) {
                                let actionsE = $('<td>').appendTo(rowE);
                                // collections with secrets cannot be updated (put) because the secret data will be replaced by invalid values
                                let idFields = $.cms.utils.getIdFields(model);
                                if (idFields.length) {
                                    if (!hasSecret1Columns) {
                                        let editActionE = $('<span>')
                                            .addClass('material-icons cms-table-action')
                                            .text('edit')
                                            .on('click', () => showPutDialog(o, o))
                                            .appendTo(actionsE)
                                            .caHide(model, 'PUT');
                                    }
                                    let deleteActionE = $('<span>')
                                        .attr('id', 'cms-table-header-action-delete')
                                        .addClass('material-icons cms-table-action')
                                        .text('delete')
                                        .on('click', function () {
                                            let data = {
                                                message: 'are you sure?',
                                                actionHandler: async () => {
                                                    await $.cms.api.delete(
                                                        table.model,
                                                        $.cms.utils.filterObject(o, idFields.map(f => f.name))
                                                    );
                                                    refresh();
                                                },
                                                actionText: 'delete'
                                            };
                                            $.cms.dialog.confirm(data);
                                        })
                                        .appendTo(actionsE)
                                        .caHide(model, 'DELETE');
                                }
                            }

                            let id = `cms-table-${model.collection}-${i}-meta-${uid}`;
                            let metaE = $('<td>')
                                .prop('id', id)
                                .appendTo(rowE);
                            model.fields.filter(f => !f.hidden).forEach(f => {
                                let value = o[f.name];
                                let id = `cms-table-cell-${i}-${f.name}-${uid}`;
                                let dataE = $('<td>')
                                    .attr('id', id)
                                    .addClass(`cms-table-cell-field-type-${f.type}`)
                                    .appendTo(rowE);
                                if (f.relation && (o[f.name] || o[f.name] === 0)) {
                                    dataE.addClass('cms-table-cell-relation');
                                    let anchorE = $('<a>')
                                        .text('go')
                                        .appendTo(dataE);
                                    let idFields = $.cms.utils.getIdFields(model);
                                    // TODO need to property eval the filter from the relation.filter and replace any params
                                    let relationFilter = o._meta.relationFilters[f.name];
                                    // replace ' with "
                                    relationFilter = $.cms.utils.replaceAll(relationFilter, "'", '"');
                                    let filterHash = btoa(encodeURIComponent(JSON.stringify(JSON.parse(relationFilter))));
                                    let hash = '#COLLECTION&' +
                                        $.cms.utils.evalHash({
                                            cluster: f.relation.cluster,
                                            database: f.relation.database,
                                            collection: f.relation.collection,
                                            filter: filterHash
                                        });
                                    anchorE.attr('href', hash);
                                }
                                if (f.virtual) {
                                    dataE.addClass('cms-table-cell-virtual');
                                }
                                if (!isStatic && $.cms.auth.canAccess(model, 'PATCH')) {
                                    let idFields = $.cms.utils.getIdFields(model);
                                    if (idFields.length && !f.relation && !f.virtual) {
                                        dataE.on('dblclick', () => {
                                            let singleFieldData = {
                                                [f.name]: o[f.name]
                                            };
                                            showPatchDialog(o, singleFieldData);
                                        });
                                    }
                                }
                                let tooltip;
                                if (value != null) {
                                    switch (f.type) {
                                        case null:
                                        case undefined:
                                        case 'oid':
                                        case 'date':
                                        case 'integer':
                                        case 'decimal':
                                        case 'boolean':
                                        case 'secret1':
                                        case 'secret2':
                                            dataE.text(value);
                                            break;
                                        case 'string':
                                            if (value && typeof value === 'string' && value.length > 50) {
                                                dataE.append($('<span>').text(value.substring(0, 47) + '...'))
                                                    .append($.cms.utils.dynamicJson(null, [value], 0));
                                            } else {
                                                dataE.text(value);
                                            }
                                            break;
                                        case 'json':
                                        case 'array':
                                            try {
                                                dataE.append($.cms.utils.dynamicJson(null, value, 0));
                                            } catch (e) {
                                                dataE.text(value);
                                            }
                                            break;
                                        case 'geoJson':
                                            try {
                                                dataE.append($.cms.utils.dynamicJson(null, value, 0));
                                                if (value.type === 'Point' && Array.isArray(value.coordinates)) {
                                                    let iconE = $('<span>').addClass('material-icons').text('location_on');
                                                    let url = `https://www.google.com/maps/@${value.coordinates[0]},${value.coordinates[1]},15z`;
                                                    let gMapsE = $('<a>').attr('href', url).attr('target', '_blank').append(iconE).appendTo(dataE);
                                                }
                                            } catch (e) {
                                                dataE.text(value);
                                            }
                                            break;
                                        case 'email':
                                            dataE.append($(`<a href="mailto:${value}">`).text(value));
                                            break;
                                        case 'phone':
                                            dataE.append($(`<a href="tel:${value}">`).text(value));
                                            break;
                                        case 'cron':
                                            dataE.text(value);
                                            try {
                                                tooltip = cronstrue.toString(value);
                                            } catch (e) {
                                                // errors are reported from server validations
                                            }
                                            break;
                                        default:
                                            $.cms.log.error(`invalid field type '${f.type}'`);
                                    }
                                    if (tooltip) {
                                        $('<div>').addClass('mdl-tooltip mdl-tooltip--top')
                                            .attr('for', id)
                                            .text(tooltip)
                                            .mdl()
                                            .appendTo(dataE);
                                    }
                                }
                            });

                            if (o._meta) {
                                if (o._meta.validationErrors) {
                                    metaValidationErrorsMap[i] = o._meta.validationErrors;
                                    let warnTooltips = [];
                                    for (let field in o._meta.validationErrors) {
                                        let id = `cms-table-cell-${i}-${field}-${uid}`;
                                        let cellE = $(`#${id}`);
                                        let warns = o._meta.validationErrors[field];
                                        metaHeaderCounter += warns.length;
                                        warnTooltips.push(...warns.map(e => `${field}: ${e}`));

                                        cellE.addClass('mdl-badge mdl-badge--overlap')
                                            .attr('data-badge', warns.length);
                                        let tooltipE = $('<div>').addClass('mdl-tooltip mdl-tooltip--top cms-table-tooltip-warn').attr('for', id)
                                            .mdl()
                                            .html(warns.join('<br>'))
                                            .appendTo(cellE);

                                    }
                                    let warnE = $('<div>')
                                        .addClass('material-icons mdl-badge mdl-badge--overlap')
                                        .attr('data-badge', warnTooltips.length)
                                        .text('warning')
                                        .appendTo(metaE);
                                    let tooltipE = $('<div>').addClass('mdl-tooltip mdl-tooltip--right cms-table-tooltip-warn').attr('for', id)
                                        .mdl()
                                        .html(warnTooltips.join('<br>'))
                                        .appendTo(metaE);
                                }
                                if (o._meta.validationChanges) {
                                    for (let field in o._meta.validationChanges) {
                                        let change = o._meta.validationChanges[field];
                                        let id = `cms-table-cell-${i}-${field}-${uid}`;
                                        let cellE = $(`#${id}`);
                                        cellE.addClass('cms-table-cell-change');
                                        cellE.each(function () {
                                            if ($(this).text().trim() === '')
                                                $(this).text('<empty>');
                                        });
                                        let tooltipE = $('<div>').addClass('mdl-tooltip mdl-tooltip--bottom cms-table-tooltip-change').attr('for', id)
                                            .mdl()
                                            .text(change)
                                            .appendTo(cellE);
                                    }
                                }
                                if (o._meta.relationErrors) {
                                    for (let field in o._meta.relationErrors) {
                                        let error = o._meta.relationErrors[field];
                                        let id = `cms-table-cell-${i}-${field}-${uid}`;
                                        let cellE = $(`#${id}`);
                                        cellE.addClass('cms-table-cell-relationError');
                                        cellE.each(function () {
                                            if ($(this).text().trim() === '')
                                                $(this).text('<empty>');
                                        });
                                        let tooltipE = $('<div>').addClass('mdl-tooltip mdl-tooltip--right cms-table-tooltip-relationError').attr('for', id)
                                            .mdl()
                                            .text(error)
                                            .appendTo(cellE);
                                    }

                                }
                            }

                            if (i === data.length) {
                                tableE.mdl();
                                callback ? callback() : null;
                                headerWarnE.attr('data-badge', metaHeaderCounter);
                                if (metaHeaderCounter) {
                                    headerWarnE.addClass('cms-table-action');
                                }
                                headerWarnTooltipE.html((metaHeaderCounter ? `${metaHeaderCounter} warnings` : 'no warnings') + '<br>(sorting works only for fetch results, change page size if needed)' + '<br>click to copy');
                            }
                        }
                    );
                    if (!data.length) {
                        callback ? callback() : null;
                    }
                }
                if (!isStatic) {
                    let hashParams = $.cms.utils.getHashParams();
                    let getOptions = {};
                    getOptions._limit = parseInt(hashParams._limit) || $.cms.context.resources.properties.pageSize;
                    getOptions._skip = parseInt(hashParams._skip) || 0;
                    getOptions._sort = hashParams._sort;
                    getOptions._sortDirection = parseInt(hashParams._sortDirection) || 1;
                    let getData = {...filter, ...getOptions};
                    $.cms.utils.loading();
                    $.cms.api.postGet(model, getData, (data) => {
                        setData(data, () => $.cms.utils.loading(true));
                    });
                } else {
                    setData(data);
                }

                // set it here, otherwise the table might not render correctly
                tableBodyE.appendTo(tableE);
                table.data = data;
                table.tableE = tableE;
                tableCellE.fadeIn();
            }

            const showPutDialog = function (data, formData) {
                showPutPatchDialog('PUT', data, formData);
            }

            const showPatchDialog = function (data, formData) {
                showPutPatchDialog('PATCH', data, formData);
            }

            const showPutPatchDialog = function (method, data, formData) {
                let form;
                if (method === 'PUT') {
                    form = $.cms.form.create(model, formData);
                } else if (method === 'PATCH') {
                    let singleFieldModel = {...model};
                    // get the single field
                    singleFieldModel.fields = model.fields.filter(f => f.name === Object.keys(formData).pop());
                    form = $.cms.form.create(singleFieldModel, formData);
                } else {
                    $.log.error(`invalid method '${method}'`);
                }
                let dialog;
                let actions = [];
                let saveAction = {
                    label: 'update',
                    action: async function () {
                        let updatedData = form.getValues() || {}; // added '|| {}' just for intelliJ's complaints
                        let idFields = $.cms.utils.getIdFields(model);
                        if (!idFields.length) {
                            $.cms.log.warn(`cannot update, no 'id' fields in collection`);
                        } else {
                            let filter = $.cms.utils.filterObject(data, idFields.map(f => f.name));
                            let count;
                            if (method === 'PUT') {
                                count = await $.cms.api.put(model, updatedData, filter);
                            } else if (method === 'PATCH') {
                                count = await $.cms.api.patch(model, updatedData, filter);
                            }
                            if (count.matchedCount === 0) {
                                $.cms.log.warn(`not matched any documents for update`)
                            }
                            if (count.modifiedCount === 0) {
                                $.cms.log.info(`not updated the document, did you apply any change?`)
                            } else {
                                $.cms.log.info(`updated document`)
                                refresh();
                            }
                            dialog.close();
                        }
                    },
                    keyup: {
                        key: 'Enter'
                    }
                }
                let resetAction = {
                    label: 'reset',
                    action: function () {
                        form.setValues(data);
                    }
                }
                actions.push(resetAction);
                actions.push(saveAction);
                dialog = $.cms.dialog.create(`edit ${table.model.collection}`, form.contentE, actions);
                dialog.show();
            }

            const showPostDialog = function () {
                let form = $.cms.form.create(table.model);
                let actions = [];
                let saveAction = {
                    label: 'create',
                    action: async function () {
                        let data = form.getValues();
                        await $.cms.api.post(model, data);
                        $.cms.log.info(`created document from ${model.database}.${model.collection}`)
                        await refresh();
                        dialog.close();
                    }
                }
                actions.push(saveAction);
                let dialog = $.cms.dialog.create(`create ${table.model.collection}`, form.contentE, actions);
                dialog.show();
            }

            const deleteSelected = function () {
                const indexes = [];
                let selectedEs = table.tableE.find('tr.is-selected');
                if (selectedEs.length) {
                    selectedEs
                        .each(function (i) {
                            indexes.push($(this).data('index'));
                        })
                    // do not use lambda because you loss the $(this) reference
                    /*
                    .each(i => {
                        indexes.push($(this).data('index'));
                    });
                     */
                    let idFields = $.cms.utils.getIdFields(model);
                    if (!idFields.length) {
                        $.cms.log.warn(`cannot delete, no 'id' fields`);
                    } else {
                        $.cms.dialog.confirm({
                            message: 'are you sure?',
                            actionHandler: async () => {
                                $.cms.utils.loading();
                                for (const i of indexes) {
                                    await $.cms.api.delete(
                                        table.model,
                                        $.cms.utils.filterObject(table.data[i], idFields.map(f => f.name))
                                    );
                                }
                                $.cms.log.info(`deleted ${selectedEs.length} documents from ${model.database}.${model.collection}`)
                                await table.refresh();
                                $.cms.utils.loading(false);
                            },
                            actionText: 'delete'
                        });
                    }
                }
            }

            table = {
                contentE: contentE,
                model: model,
                refresh: refresh,
                deleteSelected: deleteSelected,
            }
            return table;
        }

        return {
            create: create
        }
    }

    !$.cms ? $.cms = {} : null;

    jQuery.extend($.cms, {
        table: Constructor()
    });
})();
