# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

from selenium.webdriver.common.by import By

from pages.firefox.base import FirefoxBasePage
from pages.regions.download_button import DownloadButton


class FeaturesLandingPage(FirefoxBasePage):

    URL_TEMPLATE = '/{locale}/firefox/features/'

    _download_button_locator = (By.ID, 'features-header-download')
    _modern_features_locator = (By.CSS_SELECTOR, '.modern > .features-list-item')
    _privacy_features_locator = (By.CSS_SELECTOR, '.privacy > .features-list-item')
    _independence_features_locator = (By.CSS_SELECTOR, '.independence > .features-list-item')

    @property
    def download_button(self):
        el = self.find_element(*self._download_button_locator)
        return DownloadButton(self, root=el)

    @property
    def modern_features(self):
        els = [el for el in self.find_elements(*self._modern_features_locator)
               if el.is_displayed()]
        return els

    @property
    def privacy_features(self):
        els = [el for el in self.find_elements(*self._privacy_features_locator)
               if el.is_displayed()]
        return els

    @property
    def independence_features(self):
        els = [el for el in self.find_elements(*self._independence_features_locator)
               if el.is_displayed()]
        return els
