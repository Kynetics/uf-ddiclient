#!/bin/sh

#
# Copyright © 2017-2021  Kynetics  LLC
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#

puts "Checking kotlin code style..."

# stash any unstaged changes
git stash -q --keep-index

# check code respect kotlin style guide
./gradlew detekt

# store the last exit code in a variable
RESULT=$?

# unstash the unstashed changes
git stash pop -q

# return the './gradlew checkStyle' exit code
exit ${RESULT}