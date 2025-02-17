#!/bin/bash

set -e
set -x 

cat <<EOF
##############################################################
####                          AWS                         ####
##############################################################

EOF

aws sts get-caller-identity
aws s3 ls s3://test.bitquant.cloud/
aws s3 ls s3://data.bitquant.cloud/

aws s3 cp s3://test.bitquant.cloud/crypto/1d/BTC.csv .
aws s3 cp s3://data.bitquant.cloud/crypto/1d/BTC.csv .

cat <<EOF

##############################################################

EOF


exit 1
