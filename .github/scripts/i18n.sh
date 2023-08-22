#!/bin/bash -x

evalrc() {
  if [ "$1" -ne "0" ]; then
    echo "$2"
    exit $1
  fi
}

#Execute the gettext task
./gradlew gettext
evalrc $? "Gettext was not successful for branch $GIT_BRANCH."

files=$(git diff po/keys.pot | egrep -v -e '^( |\+#|\-#|@@|\+\+\+|\-\-\-|diff|index)' -e 'X-Generator' \
  -e 'POT-Creation-Date')

if [ ! -z "$files" ]; then
  #Code to commit the updated template file
  NEW_BRANCH=i18n/$(date '+%s')
  git checkout -b $NEW_BRANCH
  git add po/keys.pot
  evalrc $? " <== System return for git add file for branch $NEW_BRANCH."

  echo "Committing and pushing the template file."
  git -c "user.name=$GIT_AUTHOR_NAME" -c "user.email=$GIT_AUTHOR_EMAIL" commit -m "updated po/keys.pot template"
  evalrc $? " <== System return for git commit for branch $NEW_BRANCH."

  git push https://candlepin-bot:${I18N_TOKEN}@github.com/candlepin/candlepin $NEW_BRANCH
  evalrc $? " <== System return for git push for branch $NEW_BRANCH."
fi
