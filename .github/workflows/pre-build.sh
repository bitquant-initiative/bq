#!/bin/bash

set -e
set -x 

cat <<EOF
##############################################################
####                          AWS                         ####
##############################################################

EOF

aws sts get-caller-identity
aws s3 ls s3://${BQ_S3_BUCKET}/


cat <<EOF

##############################################################

EOF



