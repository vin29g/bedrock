/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

(function($) {
    'use strict';

    var client = window.Mozilla.Client;
    var $modalLink = $('#other-platforms-modal-link');

    var initOtherPlatformsModal = function() {
        // show the modal cta button
        $modalLink.removeClass('hidden');

        $modalLink.on('click', function(e) {
            e.preventDefault();
            Mozilla.Modal.createModal(this, $('#other-platforms'), {
                title: $(this).text()
            });

            window.dataLayer.push({
                'event': 'in-page-interaction',
                'eAction': 'link click',
                'eLabel': 'Download Firefox for another platform'
            });
        });
    };

    /**
     * Enable modal to optionally download Firefox for other platforms.
     * Don't show the modal for iOS or Android.
     */
    if ($modalLink.length && client.isDesktop) {
        initOtherPlatformsModal();
    }

    // trigger fade-in CSS animation
    $(function() {
        $('html').addClass('ready');
    });

})(window.jQuery);
