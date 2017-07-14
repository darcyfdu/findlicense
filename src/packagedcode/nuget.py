#
# Copyright (c) 2016 nexB Inc. and others. All rights reserved.
# http://nexb.com and https://github.com/nexB/scancode-toolkit/
# The ScanCode software is licensed under the Apache License version 2.0.
# Data generated with ScanCode require an acknowledgment.
# ScanCode is a trademark of nexB Inc.
#
# You may not use this software except in compliance with the License.
# You may obtain a copy of the License at: http://apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software distributed
# under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
# CONDITIONS OF ANY KIND, either express or implied. See the License for the
# specific language governing permissions and limitations under the License.
#
# When you publish or redistribute any data created with ScanCode or any ScanCode
# derivative work, you must accompany this data with the following acknowledgment:
#
#  Generated with ScanCode and provided on an "AS IS" BASIS, WITHOUT WARRANTIES
#  OR CONDITIONS OF ANY KIND, either express or implied. No content created from
#  ScanCode should be considered or used as legal advice. Consult an Attorney
#  for any legal advice.
#  ScanCode is a free software code scanning tool from nexB Inc. and others.
#  Visit https://github.com/nexB/scancode-toolkit/ for support and download.

from __future__ import absolute_import, print_function

from packagedcode import models
from packagedcode import xmlutils

"""
Handle nuget.org Nuget packages.
"""


class NugetPackage(models.Package):
    metafiles = ('[Content_Types].xml', '*.nuspec',)
    filetypes = ('zip archive', 'microsoft ooxml',)
    mimetypes = ('application/zip', 'application/octet-stream',)
    extensions = ('.nupkg',)
    repo_types = (models.repo_nuget,)

    type = models.StringType(default='Nuget')
    packaging = models.StringType(default=models.as_archive)

    @classmethod
    def recognize(cls, location):
        return parse(location)


nuspec_tags = [
    'id',
    'version',
    'title',
    'authors',
    'owners',
    'licenseUrl',
    'projectUrl',
    'requireLicenseAcceptance',
    'description',
    'summary',
    'releaseNotes',
    'copyright'
]


def parse_nuspec(location):
    """
    Return a dictionary of Nuget metadata from a .nuspec file at location.
    Return None if this is not a parsable nuspec.
    """
    if not location.endswith('.nuspec'):
        return
    return xmlutils.parse(location, _nuget_handler)


def _nuget_handler(xdoc):
    tags = {}
    for tag in nuspec_tags:
        xpath = xmlutils.namespace_unaware('/package/metadata/{}'.format(tag))
        values = xmlutils.find_text(xdoc, xpath)
        if values:
            value = u''.join(values)
            tags[tag] = value and value or None
    return tags


def parse(location):
    """
    Return a Nuget package from a nuspec file at location.
    Return None if this is not a parsable nuspec.
    """
    nuspec = parse_nuspec(location)
    if not nuspec:
        return
    asserted_license = models.AssertedLicense(url=nuspec.get('licenseUrl'))

    authors = [models.Party(name=nuspec.get('authors'))] if nuspec.get('authors') else []
    owners = [models.Party(name=nuspec.get('owners'))] if nuspec.get('owners') else []

    package = NugetPackage(
        location=location,

        name=nuspec.get('id'),
        version=nuspec.get('version'),

        summary=nuspec.get('title'),
        description=nuspec.get('description'),
        homepage_url=nuspec.get('projectUrl'),

        authors=authors,
        owners=owners,

        asserted_licenses=[asserted_license],
        copyrights=[nuspec.get('copyright')],
    )
    return package
