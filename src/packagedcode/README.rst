The purpose of `packagedcode` is to:

- detect a package, 
- determine its dependencies, 
- detect its asserted license (at the metadata level) vs. its actual licensing (as scanned).


1. **detect the presence of a package** in a codebase based on its manifest, its file
or archive type. Typically it is a third party package but it may be your own too.
Taking Python as a main example a package can exist in multiple forms:

    1.1. as a **source checkout** (or some source archive such as a source
    distribution or an `sdist`) where the presence of a `setup.py` or some
    `requirements.txt` file is the key marker for Python. For Maven it would be a
    `pom.xml` or a `build.gradle` file, for Ruby a `Gemfile` or `Gemfile.lock`, the
    presence of autotools files, and so on, with the goal to eventually covering all
    the packages formats/types that are out there and commonly used.

    1.2. as an **installable archive or binary** such as a Pypi wheel `.whl` or
    `.egg`, a Maven `.jar`, a Ruby `.gem`, a `.nupkg` for a Nuget, a `.rpm` or `.deb`
    Linux package, etc... Here the type, shape and name structure of an archive as
    well as some its files content are the key markers for detection. The metadata
    may also be included in that archive as a file or as some headers (e.g. RPMs)

    1.3. as an **installed packaged** such as when you `pip install` a Python package
    or `bundle install` Ruby gems or `npm install` node modules. Here the key markers
    may be some combo of a typical or conventional directory layout and presence of
    specific files such as the metadata installed with a Python `wheel`, a `vendor`
    directory for Ruby, some `node_modules` directory tree for npms, or a certain
    file type with metadata such as Windows DLLs. Additional markers may also include
    "namespaces" such as Java or Python imports, C/C++ namespace declarations.

2. **parse and collect the package manifest(s)** metadata. For Python, this means
extracting name, version, authorship, asserted licensing and declared dependencies as
found in the any of the package descriptor files (e.g. a `setup.py` file,
`requirements` file(s) or any of the `*-dist-info` or `*-egg-info` dir files such as
a `metadata.json`). Other package formats have their own metatada that may be more or
less comprehensive in the breadth and depth of information they offer (e.g.
`.nuspec`, `package.json`, `bower.json`, Godeps, etc...). These metadata include the
declared dependencies (and in some cases the fully resolved dependencies too such as
with Gemfile.lock). Finally, all the different packages formats and data are
normalized and stored in a common data structure abstracting the small differences of
naming and semantics that may exists between all the different package formats.

Once collected, these data are then injected in a `packages` section of the scan. 

What code in `packagedcode` is not meant to do:

A. **download packages** from a thirdparty repository: there is code upcomming code in
another tool that will be specifically dealing with this and also handles collecting
the metadata as served by a package repository (which are in most cases --but not
always-- the same as what is declared in the manifests). 

B. **resolve dependencies**: the focus here is on a purely static analysis that does not
rely on any network access at runtime by design. To scan for actually used
dependencies the process is to instead scan for an as-built or as-installed or as-
deployed codebase where the dependencies have already been provisioned and installed
and there ScanCode would detect these. 
There are also some upcomming prototype for a dynamic multi-package dependencies
resolver that actually runs live the proper tool to resolve and collect dependencies
(e.g. effectively running Maven, bundler, pip, npm, gradle, bower, go get/dep, etc).
This will be a tool separate from ScanCode as this requires having several/all
package managers installed (and possibly multiple versions of each) and may run code
from the codebase (e.g. a setup.py) and access the network for fetching or resolving
dependencies. It could be also exposed as a web service that can take in a manifest
and package and run safely the dep resolution in an isolated environment (e.g. a
chroot jail or docker container) and return the collected deps.

C. **match packages** (and files) to actual repositories or registries, e.g. given a
scan detecting packages matching would be looking them up in a remote package
repository or a local index and possibly using A. and/or B. additionally if needed.
Here again there is some upcomming code and tool that will deal specifically with
this aspect and would handle also building an index of actual registries/repositories
and matching using hashes and fingerprints.

An now some answer to questions originally by @sschuberth:

> More concretely, this does not download the source code of a Python package to run
ScanCode over it.

Correct. The assumption with ScanCode proper (aside of the other in progress tools
that I mentioned above) is that the deps have been fetched in the code you scan if
you want to scan for deps. Packages will be detected with their declared deps but the
deps will neither be resolved nor fetched. Though, as a second step we could also
verify that all the declared deps are also present in the scanned code as detected
packages. 

> This should be made very clear as this means cases where the license from the
metadata is wrong compared to the LICENSE file in the source code will not get
detected.

Both the metadata and the file level licenses (such as a header comment or a
`LICENSE` file of sorts) are detected by ScanCode here: the license scan detect the
licenses while the package scan collect the asserted licensing in the metadata. The
interesting thing thanks to this combo is that eventual conflicts (or incomplete
data) can then be analyzed and a deduction should be doable automatically: given a
scan for packages and licenses and copyrights, do the package metadata
asserted/declared license match the actual detected licenses? If not this could be
reported as some "error" condition... Furthermore, this could be refined based on
classification of the files: a package may assert a top level `MIT` license and use a
GPL-licensed build script. By knowing that the build script is indeed a build script,
we could then report that the GPL detected in such script is not conflicting with the
overall asserted MIT license of the package.  The same could be done with test
scripts/code, or documentation code (such as doxygen-generated docs)

> Moreover, licenses from transitive dependencies are not taking into account.

If the transitive dependencies have been resolved and their code present in the
codebase, then they would be caught by a static ScanCode scan and eventually scanned
both for package metadata and/or license detection. There are some caveats that would
need to be dealt with of course as some tools (e.g. Maven) may not store locally
(e.g. side-by-side with a given checkout) the corresponding artifacts/Jars and use
instead a `~/user` "global" dot directory to store a cache.

Beyond this, actual dependency resolution of a single package or a complete manifest
would the topic of another tool as mentioned above.
