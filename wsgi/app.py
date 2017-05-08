# flake8: noqa
import os

from decouple import config


try:
    import newrelic.agent
except ImportError:
    newrelic = False


if newrelic:
    newrelic_ini = config('NEWRELIC_PYTHON_INI_FILE', default=False)
    if newrelic_ini:
        newrelic.agent.initialize(newrelic_ini)
    else:
        newrelic = False

IS_HTTPS = os.environ.get('HTTPS', '').strip() == 'on'
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'bedrock.settings')

# must be imported after env var is set above.
from django.core.handlers.wsgi import WSGIRequest
from django.core.wsgi import get_wsgi_application
from raven.contrib.django.raven_compat.middleware.wsgi import Sentry


class WSGIHTTPSRequest(WSGIRequest):
    def _get_scheme(self):
        if IS_HTTPS:
            return 'https'

        return super(WSGIHTTPSRequest, self)._get_scheme()

application = get_wsgi_application()
application.request_class = WSGIHTTPSRequest
application = Sentry(application)

if newrelic:
    application = newrelic.agent.wsgi_application()(application)
