#
# Copyright © 2017-2021  Kynetics  LLC
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#

# Tag last commit as 'latest'.

if [ "$TRAVIS_BRANCH" = "master" -a "$TRAVIS_PULL_REQUEST" = "false" ]; then
  git config --global user.email "hi@travis-ci.org"
  git config --global user.name "Sr. Travis"

  git remote add release "https://${GH_TOKEN}@github.com/Kynetics/uf-ddiclient.git"

  git push -d release latest
  git tag -d latest
  git tag -a "latest" -m "[Autogenerated] This is the latest version pushed to the master branch."
  git push release --tags
fi