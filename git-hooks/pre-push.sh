#!/bin/sh

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