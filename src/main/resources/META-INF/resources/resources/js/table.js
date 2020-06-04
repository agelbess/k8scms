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

        const create = function (model, data) {
            let table;
            const uid = new Date().getTime();
            const isStatic = data;

            let contentE = $('<div>').addClass('mdl-grid cms-table-grid');
            let tableCellE = $('<div>').addClass('mdl-cell mdl-cell--12-col').appendTo(contentE);

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
                        filter = JSON.parse(atob(filter64));
                    } catch (e) {
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
                    let pageCountersE = $('<span>').addClass('cms-table-counters-fromTo').appendTo(pageCountersCellE);
                    {
                        let from = parseInt(pageSliderE.val()) + 1;
                        let to = Math.min(from - 1 + parseInt(pageSizeSliderE.val()), documentCount);
                        pageCountersE.text(`${from}..${to} / ${documentCount}`);
                    }

                    // query
                    let id = `cms-table-query-${model.database}-${model.collection}`;
                    let queryGridE = $('<div>').addClass('mdl-grid').prependTo(tableCellE);
                    let queryE = $('<div>').addClass('mdl-cell--12-col').appendTo(queryGridE);
                    let queryExpandableE = $('<div>').addClass('mdl-textfield mdl-js-textfield mdl-textfield--expandable cms-table-query-expandable').css({width: '100%'}).appendTo(queryE);
                    let queryLabelE = $('<label>').addClass('mdl-button mdl-js-button mdl-button--icon').attr('for', id).mdl().appendTo(queryExpandableE);
                    let queryIconE = $('<span>').addClass('material-icons').text('search').appendTo(queryLabelE);
                    let queryHolderE = $('<div>').addClass('mdl-textfield__expandable-holder').css({
                        width: '100%',
                        maxWidth: '100%'
                    }).appendTo(queryExpandableE);
                    let queryHolderLabelE = $('<label>').addClass('mdl-textfield__label').attr('for', 'dummy').appendTo(queryHolderE);
                    let queryTimeoutId;
                    let queryInputE = $('<input type="text">').addClass('mdl-textfield__input').attr('id', id).css({width: '100%'}).appendTo(queryHolderE)
                        .on('keyup', () => {
                            try {
                                queryInputE.css({color: 'red'});
                                let json = JSON.parse(queryInputE.val());
                                queryInputE.css({color: 'inherit'});
                            } catch {
                            }
                            clearTimeout(queryTimeoutId);
                            queryTimeoutId = setTimeout(async () => {
                                try {
                                    let filter = queryInputE.val();
                                    if (filter) {
                                        JSON.parse(filter);
                                        $.cms.utils.setHashParam('filter', btoa(filter));
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
                        .ca(model, 'POST');
                    let deleteActionHeaderE = $('<span>')
                        .attr('id', 'cms-table-header-action-delete')
                        .addClass('material-icons mdl-badge mdl-badge--overlap cms-table-action')
                        .text('delete_sweep')
                        .attr('data-badge', 0)
                        .on('click', () => table.deleteSelected())
                        .appendTo(actionsHeaderE)
                        .ca(model, 'DELETE');
                    tableE.on('click', 'td,th label input[type="checkbox"]', function () {
                        window.setTimeout(function () {
                            let countSelected = table.tableE.find('tr.is-selected').length;
                            deleteActionHeaderE.attr('data-badge', countSelected);
                        }, 100)
                    });
                    let uploadActionHeaderE = $('<span>')
                        .addClass('material-icons cms-table-action')
                        .text('cloud_upload')
                        .on('click', () => window.location.href = `#UPLOAD&database=${model.database}&collection=${model.collection}`)
                        .appendTo(actionsHeaderE)
                        .ca(model, 'POST');
                    let downloadActionHeaderExcelE = $('<span>')
                        .addClass('material-icons cms-table-action')
                        .text('cloud_download')
                        .on('click', () => {
                            $.cms.download.xlsx(table.data, model);
                            $.cms.log.info(`downloaded ${table.data.length} documents from ${model.database}.${model.collection}`)
                        })
                        .appendTo(actionsHeaderE);
                }

                model.fields.forEach(c => {
                    let id = `cms-table-header-name-${c.name}`;
                    let headerE = $('<th>')
                        .attr('id', id)
                        .addClass(c.id ? 'cms-table-id' : '')
                        .appendTo(headerRowE);
                    $.cms.utils.getFieldIcon(c).addClass('cms-table-header-type').appendTo(headerE);
                    $('<span>').text(c.name).appendTo(headerE);
                    let tooltips = [];
                    c.regex ? tooltips.push('regex: ' + c.regex) : null;
                    c.relation ? tooltips.push('relation: ' + c.relation) : null;
                    if (tooltips.length) {
                        $('<div>').text(tooltips.join('\n'))
                            .addClass('mdl-tooltip mdl-tooltip--top')
                            .attr('for', id)
                            .mdl()
                            .appendTo(headerE);
                    }

                    let sortE;
                    let _sort = $.cms.utils.getHashParam('_sort');
                    if (_sort !== c.name) {
                        sortE = $('<span>').addClass('material-icons cms-table-header-sort').text('sort').appendTo(headerE);
                    } else {
                        sortE = $('<span>').addClass('mdl-badge cms-table-header-sort-selected').attr('data-badge', $.cms.utils.getHashParam('_sortDirection') > 0 ? '➚' : '➘').appendTo(headerE);
                    }
                    sortE.on('click', () => {
                        $.cms.utils.setHashParam('_sort', c.name);

                        let params = $.cms.utils.getHashParams();
                        let _sortDirection = params._sortDirection;
                        _sortDirection = _sortDirection ? _sortDirection * -1 : 1;
                        $.cms.utils.setHashParam('_sortDirection', _sortDirection);
                    });
                });
                let metaHeaderId = `cms-table-${model.collection}-meta-${uid}`;
                let metaHeaderE = $('<th>').attr('id', metaHeaderId).appendTo(headerRowE);
                let metaHeaderCounter = 0
                let tableBodyE = $('<tbody>');

                const setData = function (data, callback) {
                    table.data = data;
                    let i = 0;
                    data.forEach(async o => {
                            await $.cms.utils.sleep(0);
                            let rowE = $('<tr>').data('index', i++).appendTo(tableBodyE);
                            if (!isStatic) {
                                let actionsE = $('<td>').appendTo(rowE);
                                let editActionE = $('<span>')
                                    .addClass('material-icons cms-table-action')
                                    .text('edit')
                                    .on('click', () => showPutDialog(o))
                                    .appendTo(actionsE)
                                    .ca(model, 'POST');
                                let deleteActionE = $('<span>')
                                    .attr('id', 'cms-table-header-action-delete')
                                    .addClass('material-icons cms-table-action')
                                    .text('delete')
                                    .on('click', function () {
                                        let idField = $.cms.utils.getIdField(model);
                                        if (!idField) {
                                            $.cms.log.info(`cannot delete, no 'id' field`);
                                        } else {
                                            let data = {
                                                message: 'are you sure?',
                                                actionHandler: async () => {
                                                    await $.cms.api.delete(
                                                        table.model,
                                                        {[idField.name]: getIdValue(o)}
                                                    );
                                                    refresh();
                                                },
                                                actionText: 'delete'
                                            };
                                            $.cms.dialog.confirm(data);
                                        }
                                    })
                                    .appendTo(actionsE)
                                    .ca(model, 'DELETE');
                            }

                            model.fields.forEach(f => {
                                let value = o[f.name];
                                let id = `cms-table-cell-${i}-${f.name}`;
                                let dataE = $('<td>')
                                    .attr('id', id)
                                    .addClass(`cms-table-cell-field-type-${f.type}`)
                                    .appendTo(rowE);
                                let tooltip;
                                if (value != null) {
                                    switch (f.type) {
                                        case null:
                                        case undefined:
                                        case 'string':
                                        case 'oid':
                                        case 'date':
                                        case 'integer':
                                        case 'decimal':
                                        case 'boolean':
                                        case 'secret1':
                                        case 'secret2':
                                            dataE.text(value);
                                            break;
                                        case 'json':
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

                            let id = `cms-table-${model.collection}-${i}-meta-${uid}`;
                            let metaE = $('<td>')
                                .prop('id', id)
                                .appendTo(rowE);
                            if (o._meta && o._meta.validationErrors) {
                                let warnTooltips = [];
                                for (let field in o._meta.validationErrors) {
                                    let warns = o._meta.validationErrors[field];
                                    metaHeaderCounter += warns.length;
                                    warnTooltips.push(...warns.map(e => `${field}: ${e}`));

                                    let id = `cms-table-cell-${i}-${field}`;
                                    let cellE = $(`#${id}`).addClass('mdl-badge mdl-badge--overlap')
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
                                let tooltipE = $('<div>').addClass('mdl-tooltip mdl-tooltip--left').attr('for', id)
                                    .mdl()
                                    .html(warnTooltips.join('<br>'))
                                    .appendTo(metaE);
                            }
                            if (i === data.length) {
                                tableE.mdl();
                                callback ? callback() : null;
                                headerWarnE.attr('data-badge', metaHeaderCounter);
                                headerWarnTooltipE.text(metaHeaderCounter ? `${metaHeaderCounter} warnings` : 'no warnings')
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

                let headerWarnE = $('<div>')
                    .addClass('material-icons mdl-badge mdl-badge--overlap')
                    .attr('data-badge', metaHeaderCounter)
                    .text('warning')
                    .appendTo(metaHeaderE);
                let headerWarnTooltipE = $('<div>').addClass('mdl-tooltip mdl-tooltip--left').attr('for', metaHeaderId)
                    .text(metaHeaderCounter ? `${metaHeaderCounter} warnings` : 'no warnings')
                    .mdl()
                    .appendTo(metaHeaderE);

                // set it here, otherwise the table might not render correctly
                tableBodyE.appendTo(tableE);
                table.data = data;
                table.tableE = tableE;
                tableCellE.fadeIn();
            }

            const showPutDialog = function (data) {
                let form = $.cms.form.create(table.model, data);
                let actions = [];
                let saveAction = {
                    label: 'update',
                    action: async function () {
                        let updatedData = form.getValues() || {}; // added '|| {}' just for intelliJ's complaints
                        let idField = $.cms.utils.getIdField(model);
                        if (!idField) {
                            $.cms.log.warn(`cannot update, no 'id' field in collection`);
                        } else {
                            let idFieldName = idField.name;
                            let filter = {
                                // use the old id
                                [idFieldName]: data[idFieldName]
                            }
                            let count = await $.cms.api.put(model, updatedData, filter);
                            if (count.matchedCount === 0) {
                                $.cms.log.warn(`not matched any documents for update`)
                            }
                            if (count.modifiedCount === 0) {
                                $.cms.log.info(`not updated the document, did you apply any change?`)
                            } else {
                                $.cms.log.info(`updated document`)
                                refresh();
                            }
                        }
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
                let dialog = $.cms.dialog.create(`edit ${table.model.collection}`, form.contentE, actions);
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
                    let idField = $.cms.utils.getIdField(model);
                    if (!idField) {
                        $.cms.log.warn(`cannot delete, no 'id' field`);
                    } else {
                        $.cms.dialog.confirm({
                            message: 'are you sure?',
                            actionHandler: async () => {
                                $.cms.utils.loading();
                                for (const i of indexes) {
                                    await $.cms.api.delete(
                                        table.model,
                                        {[idField.name]: getIdValue(table.data[i])}
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

            const getIdValue = function (data) {
                const idCol = $.cms.utils.getIdField(model);
                return data[idCol.name];
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
