#!/bin/bash

set -e

cat <<EOF
##############################################################
####                          AWS                         ####
##############################################################

EOF

aws sts get-caller-identity
aws s3 ls s3://test.bitquant.cloud/
aws s3 ls s3://data.bitquant.cloud/


cat <<EOF

##############################################################

EOF

