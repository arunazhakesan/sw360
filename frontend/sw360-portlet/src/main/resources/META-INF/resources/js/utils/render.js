/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
define('utils/render', [
	'jquery',
	'utils/escape'
], function($, escape) {
	function renderTrashIcon() {
		var $trashIcon = $('<svg>', {
			'class': 'delete lexicon-icon'
		});
		$trashIcon.append($('<title>'+Liferay.Language.get('delete')+'</title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>'));

		return $trashIcon;
	}

	function renderLinkTo(url, content, htmlContent) {
	    var $link = $("<a href='" + encodeURI(url) + "'/>");
	    if (typeof htmlContent == 'string' && htmlContent) {
	        $link.html(htmlContent);
	    } else if (typeof content == 'string' && content) {
	        $link.text(escape.decodeEntities(content));
	    } else {
	        $link.text(url);
	    }

	    return $link[0].outerHTML;
	}

	function renderUserEmail(user) {
	    if (typeof user == 'string') {
	        return renderLinkTo("mailto:" + user, user);
	    }

	    if (typeof user == 'object' && user.hasOwnProperty("email") && user.hasOwnProperty("givenname") && user.hasOwnProperty("lastname")) {
	        return renderLinkTo("mailto:" + user.email, user.givenname + " " + user.lastname);
	    } else {
	        return "N.A.";
	    }
	}

	function truncate(text, limit) {
		return (text.length > limit) ? text.substr(0, limit - 1) + '&hellip;' : text;
	}

    /* make the release & license urls expandable */
    function renderExpandableUrls(urls, urlType, truncate) {
        if (urls.length == 1 && extractDataFromHTMLElement(urls[0]).length < truncate) {
            return urls;
        }
        var delimiter = ', <br>';
        urls = urls.join(delimiter)

        var $container = $('<div/>', {
            style: 'display: flex;'
        }),
            $toggler = $('<div/>', {
                'class': 'Toggler' + urlType + 'List',
                'style': 'margin-right: 0.25rem; cursor: pointer;'
            }),
            $togglerOn = $('<div/>', {
                'class': 'Toggler_on'
            }).html('&#x25BC'),
            $togglerOff = $('<div/>', {
                'class': 'Toggler_off'
            }).html('&#x25BA'),
            $collapsed = $('<div/>', {
                'class': urlType + 'ListHidden'
            }).text(cutUrlList(urls, truncate, delimiter)),
            $expanded = $('<div/>', {
                'class': urlType + 'ListShown'
            }).html(urls);

        $togglerOn.hide();
        $expanded.hide();
        $toggler.append($togglerOff, $togglerOn);
        $container.append($toggler, $collapsed, $expanded);
        return $container[0].outerHTML;
    }

    function cutUrlList(urls, truncate, delimiter) {
        var firstUrl = extractDataFromHTMLElement(urls.split(delimiter)[0]);
        return firstUrl.substring(0, truncate) + "...";
    }

    function extractDataFromHTMLElement(link) {
        var dummyHTML = document.createElement('div');
        dummyHTML.innerHTML = link;
        return dummyHTML.textContent;
    }

    function toggleExpandableList(thisObj, type) {
        var toggler_off = thisObj.find('.Toggler_off'),
            toggler_on = thisObj.find('.Toggler_on'),
            parent = thisObj.parent(),
            listHidden = parent.find('.' + type + 'ListHidden'),
            listShown = parent.find('.' + type + 'ListShown');

        toggler_off.toggle();
        toggler_on.toggle();
        listHidden.toggle();
        listShown.toggle();
    }

    function toggleChildRow(thisObj, table) {
        let tr = thisObj.closest('tr'),
            row = table.row(tr);

        if (row.child.isShown()) {
            tr.find("td:first").html('&#x25BA');
            row.child.hide();
            tr.removeClass('shown');
            row.child().removeClass('active');
        } else {
            tr.find("td:first").html('&#x25BC');
            row.child(createChildRow(row.data())).show();
            tr.addClass('shown');
            row.child().addClass('active');
        }
    }

    /*
     * Define function for child row creation, which will contain additional data for a clicked table row
     */
    function createChildRow(rowData) {
        var childHtmlString = '' +
            '<div>' +
            '<span>' + rowData.text + '</span>' +
            '</div>';
        return childHtmlString;
    }

    function toggleAllChildRows(thisObj, table) {
        let textShown = thisObj.attr('data-show') === 'true';

        table.rows(':not(.orphan)').every(function (rowIdx, tableLoop, rowLoop) {
            let tr = $(this.node()),
                row = table.row(tr);

            if (textShown) {
                if (row.child.isShown()) {
                    $(tr).find('td:first').html('&#x25BA');
                    row.child.hide();
                    tr.removeClass('shown');
                    row.child().removeClass('active')
                }
            } else {
                if (!row.child.isShown()) {
                    $(tr).find('td:first').html('&#x25BC');
                    row.child(createChildRow(row.data())).show();
                    tr.addClass('shown');
                    row.child().addClass('active')
                }
            }
        });

        if (textShown) {
            thisObj.attr('data-show', 'false');
            thisObj.html('&#x25BA');
            thisObj.attr('title', 'Expand all');
        } else {
            thisObj.attr('data-show', 'true');
            thisObj.html('&#x25BC');
            thisObj.attr('title', 'Collapse all');
        }
    }

    function renderTimeToReadableFormat(timeInSeconds) {
        var date = new Date(Number(timeInSeconds));
        return date.toISOString().substring(0,10);
    }

    return {
		linkTo: renderLinkTo,
		userEmail: renderUserEmail,
		truncate: truncate,
		trashIcon: renderTrashIcon,
		renderExpandableUrls: renderExpandableUrls,
		toggleExpandableList: toggleExpandableList,
		toggleChildRow: toggleChildRow,
		toggleAllChildRows: toggleAllChildRows,
		renderTimestamp: renderTimeToReadableFormat
	};
});
