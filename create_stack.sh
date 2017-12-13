#!/bin/bash

heat stack-create -f /usr/src/git/github/salt-syndic-master/template/virtual_salt_syndic.hot -e /usr/src/git/github/salt-syndic-master/env/virtual_salt_syndic.env salt-syndic-master
