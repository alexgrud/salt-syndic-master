#!/bin/bash

for i in {1..9}
do
        cp template.yml node${i}.salt-syndic-master.bud-mk.local.yml
        sed -i -e "s/XXX-HOSTNAME-XXX/node${i}/g" node${i}.salt-syndic-master.bud-mk.local.yml
done
