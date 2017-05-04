#!/usr/bin/env python

from __future__ import absolute_import, unicode_literals, print_function

import os.path
import re
import sys

ENV_KEY_RE = re.compile(r'^[a-z][a-z0-9_]*$', flags=re.IGNORECASE)


# borrowed from https://github.com/willkg/everett/
def parse_env_file(envfile):
    """Parse the content of an iterable of lines as .env
    Return a dict of config variables.
    >>> parse_env_file(['DUDE=Abides'])
    {'DUDE': 'Abides'}
    """
    data = {}
    for line_no, line in enumerate(envfile):
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        if '=' not in line:
            raise RuntimeError('Env file line missing = operator (line %s)' % (line_no + 1))
        k, v = line.split('=', 1)
        k = k.strip()
        if not ENV_KEY_RE.match(k):
            raise RuntimeError(
                'Invalid variable name "%s" in env file (line %s)' % (k, (line_no + 1))
            )
        v = v.strip().strip('\'"')
        data[k] = v

    return data


def get_env_vars(filename):
    if os.path.isfile(filename):
        with open(filename) as envfile:
            return parse_env_file(envfile)
    else:
        return {}


def get_unique_vars(filenames):
    data = {}
    for fn in filenames:
        data.update(get_env_vars(fn))

    return data


def get_env_vars_oneline(filenames):
    output = []
    for pair in sorted(get_unique_vars(filenames).items()):
        output.append('%s=%s' % pair)

    return ' '.join(output)


if __name__ == '__main__':
    print(get_env_vars_oneline(sys.argv[1:]), end='')
