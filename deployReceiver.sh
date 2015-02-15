#!/bin/bash
#
# Update the file timestamp.js with a new version that contains the time this file was deployed
date=`date`
echo "Timestamp: $date"
cat receiver/timestamp_template.js | sed -e "s/TIMESTAMP/$date/" > receiver/timestamp.js

# This copies the receiver folder and it's files into a Google Drive folder that is shared publicly
# and the URL of that folder is the one we specify as the url of the receiver for the app in
# hence causing the custom receiver to run the code in the /receiver folder
cp -R receiver /Users/andrew/Google\ Drive/pongcast/
echo "Receiver deployed via Google Drive"
