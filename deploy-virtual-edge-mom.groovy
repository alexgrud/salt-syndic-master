/*
    {
        edge_cloud: {
            deploy_job_name: 'Deploy - os_ha_ovs heat',
            properties: {
                SLAVE_NODE: '',
                STACK_INSTALL: '',
                STACK_TEMPLATE: '',
                STACK_TYPE: ''
            }
        }
    }

 * OPENSTACK_API_PROJECT:
 * HEAT_STACK_ZONE:
 *
 */

common = new com.mirantis.mk.Common()
salt = new com.mirantis.mk.Salt()

def slave_node = 'python'

if (common.validInputParam('SLAVE_NODE')) {
    slave_node = SLAVE_NODE
}

def deployMoMJob = 'deploy-heat-virtual_mcp11_aio'
if (common.validInputParam('MOM_JOB')) {
    deployMoMJob = MOM_JOB
}

def salt_overrides_list = SALT_OVERRIDES.tokenize('\n')

def enableSyndic(saltMasterURL, nodeName, saltMasterCred, saltMoMIP) {

    def saltMaster
    saltMaster = salt.connection(saltMasterURL, saltMasterCred)

    // Set up test_target parameter on node level
    def fullnodename = salt.getMinions(saltMaster, nodeName).get(0)
    def saltMasterExpression = 'I@salt:master'
    def saltMasterTarget = ['expression': saltMasterExpression, 'type': 'compound']
    def result
    def conf_public_net = false


    def classes_to_add = ['system.salt.syndic.single']
    def params_to_add = ['salt_syndic_master_address': saltMoMIP]

    result = salt.runSaltCommand(saltMaster, 'local', saltMasterTarget, 'reclass.node_update', null, null, ['name': "${fullnodename}", 'classes': classes_to_add, 'parameters': params_to_add])
    salt.checkResult(result)

    common.infoMsg('Perform full refresh for all nodes')
    salt.fullRefresh(saltMaster, '*')

    if (salt.testTarget(saltMaster, 'I@salt:syndic:enabled:True')) {
        salt.enforceState(saltMaster, 'I@salt:syndic:enabled', 'salt.syndic', true, true, null, false, 180, 2)
    }


}

node(slave_node) {

    def momBuild
    def salt_mom_url
    def salt_mom_ip
    def deploy_edges_infra = [:]
    def deploy_edges = [:]
    def edgeBuildsInfra = [:]
    def edgeBuilds = [:]

    def OPENSTACK_API_PROJECT = 'mcp-oscore-ci'
    def HEAT_STACK_ZONE = 'mcp-oscore-ci'

/*    {
        edge_cloud: {
            deploy_job_name: 'Deploy - os_ha_ovs heat',
            properties: {
                HEAT_STACK_ZONE: 'mcp-oscore',
                OPENSTACK_API_PROJECT: 'mcp-oscore',
                SLAVE_NODE: 'python',
                STACK_INSTALL: 'core',
                STACK_TEMPLATE: 'os_ha_ovs',
                STACK_TYPE: 'heat',
                FORMULA_PKG_REVISION: 'testing',
                STACK_CLUSTER_NAME: 'os-ha-ovs-syndic'
                STACK_DELETE: false
            }
        }
    }
*/
//    def EDGE_DEPLOY_SCHEMAS = '{k8s_ha_calico: {deploy_job_name: "deploy-heat-k8s_ha_calico", properties: {SLAVE_NODE: "python", STACK_INSTALL: "k8s,calico", STACK_TEMPLATE: "k8s_ha_calico", STACK_TYPE: "heat", FORMULA_PKG_REVISION: "testing", STACK_DELETE: false, EXT: "and *ohryhorov-deploy-heat-k8s-ha-calico-59*", STACK_CLUSTER_NAME: "k8s-ha-calico"}} }'

    def EDGE_DEPLOY_SCHEMAS = '{os_ha_ovs: {deploy_job_name: "deploy-heat-os_ha_ovs", properties: {SLAVE_NODE: "python", STACK_INSTALL: "openstack,ovs", STACK_TEMPLATE: "os_ha_ovs", STACK_TYPE: "heat", FORMULA_PKG_REVISION: "testing", STACK_DELETE: false, STACK_CLUSTER_NAME: "os-ha-ovs"}}, k8s_ha_calico: {deploy_job_name: "deploy-heat-k8s_ha_calico", properties: {SLAVE_NODE: "python", STACK_INSTALL: "k8s,calico", STACK_TEMPLATE: "k8s_ha_calico", STACK_TYPE: "heat", FORMULA_PKG_REVISION: "testing", STACK_DELETE: false, STACK_CLUSTER_NAME: "k8s-ha-calico"}} }'

//    def EDGE_DEPLOY_SCHEMAS = '{os_ha_ovs: {deploy_job_name: "deploy-heat-os_ha_ovs", properties: {SALT_MASTER_URL: "http://172.17.49.56:6969", SLAVE_NODE: "python", STACK_INSTALL: "openstack,ovs", STACK_TEMPLATE: "os_ha_ovs", STACK_TYPE: "heat", FORMULA_PKG_REVISION: "testing", STACK_DELETE: false, STACK_CLUSTER_NAME: "os-ha-ovs-syndic"}}, k8s_ha_calico: {deploy_job_name: "deploy-heat-k8s_ha_calico", properties: {SALT_MASTER_URL: "http://172.17.49.52:6969", SLAVE_NODE: "python", STACK_INSTALL: "k8s,calico", STACK_TEMPLATE: "k8s_ha_calico", STACK_TYPE: "heat", FORMULA_PKG_REVISION: "testing", STACK_DELETE: false, STACK_CLUSTER_NAME: "k8s-ha-calico-syndic"}} }'

    def edge_deploy_schemas = readJSON text: EDGE_DEPLOY_SCHEMAS

//    try {
/*        stage('Deploy MoM stack'){
            momBuild = build job: deployMoMJob, propagate: true, parameters: [
                [$class: 'StringParameterValue', name: 'FORMULA_PKG_REVISION', value: 'testing'],
                [$class: 'StringParameterValue', name: 'STACK_CLUSTER_NAME', value: 'virtual-mcp11-aio'],
                [$class: 'StringParameterValue', name: 'STACK_INSTALL', value: 'core'],
                [$class: 'BooleanParameterValue', name: 'STACK_DELETE', value: STACK_DELETE.toBoolean()],
                [$class: 'StringParameterValue', name: 'STACK_RECLASS_ADDRESS', value: 'https://gerrit.mcp.mirantis.net/salt-models/mcp-virtual-aio'],
                [$class: 'StringParameterValue', name: 'STACK_RECLASS_BRANCH', value: "stable/queens"],
                [$class: 'StringParameterValue', name: 'OPENSTACK_API_PROJECT', value: 'mcp-oscore'],
                [$class: 'StringParameterValue', name: 'HEAT_STACK_ZONE', value: 'mcp-oscore'],
                [$class: 'StringParameterValue', name: 'STACK_TEMPLATE_URL', value: 'https://github.com/ohryhorov/salt-syndic-master'],
                [$class: 'StringParameterValue', name: 'STACK_TEMPLATE_BRANCH', value: 'master'],
                [$class: 'StringParameterValue', name: 'STACK_TEMPLATE', value: 'virtual_edge_mom'],
                [$class: 'StringParameterValue', name: 'STACK_TEST', value: ''],
                [$class: 'BooleanParameterValue', name: 'TEST_DOCKER_INSTALL', value: false],
                [$class: 'StringParameterValue', name: 'SLAVE_NODE', value: slave_node],
            ]

            if (momBuild.result == 'SUCCESS') {
                // get salt master url
                salt_mom_url = "http://${momBuild.description.tokenize(' ')[1]}:6969"
                salt_mom_ip = "${momBuild.description.tokenize(' ')[1]}"
                node_name = "${momBuild.description.tokenize(' ')[2]}"
                salt_overrides_list.add("salt_syndic_master_address: ${momBuild.description.tokenize(' ')[1]}")
                common.infoMsg("Salt API is accessible via ${salt_mom_url}")
                common.infoMsg("Enabling salt_syndic_enabled through overrides")
                salt_overrides_list.add("salt_syndic_enabled: true")
            } else {
                common.errorMsg("Deployment of MoM has been failed with result: " + momBuild.result)

            }

        } */
        stage('Deploy edge clouds'){
            salt_overrides_list.add("salt_syndic_enabled: true")
            salt_overrides_list.add("salt_syndic_master_address: 172.17.48.208")

            for (edge_deploy_schema in edge_deploy_schemas.keySet()) {
                def props
                def deploy_job
                def stack_name
                def ed = edge_deploy_schema

                deploy_job = edge_deploy_schemas[edge_deploy_schema]['deploy_job_name']

                common.infoMsg("edge cloud: ${edge_deploy_schema}")
                common.infoMsg("deploy job name: ${edge_deploy_schemas[edge_deploy_schema]['deploy_job_name']}")

                props = edge_deploy_schemas[edge_deploy_schema]['properties']

//                for (prop in edge_deploy_schemas[edge_deploy_schema]['properties'].keySet()) {
//                    common.infoMsg("prop: ${prop} value: ${edge_deploy_schemas[edge_deploy_schema]['properties'][prop]}")
//                }

                if (env.BUILD_USER_ID) {
                    stack_name = "${env.BUILD_USER_ID}-${edge_deploy_schema}-${BUILD_NUMBER}"
                } else {
                    stack_name = "replayed-${edge_deploy_schema}-${BUILD_NUMBER}"
                }
                deploy_edges_infra["Deploy ${ed} infra"] = {
                    node(slave_node) {
                        edgeBuildsInfra["${ed}"] = build job: deploy_job, propagate: false, parameters: [
                            [$class: 'StringParameterValue', name: 'HEAT_STACK_ZONE', value: HEAT_STACK_ZONE],
                            [$class: 'StringParameterValue', name: 'OPENSTACK_API_PROJECT', value: OPENSTACK_API_PROJECT],
                            [$class: 'StringParameterValue', name: 'SLAVE_NODE', value: props['SLAVE_NODE']],
                            [$class: 'StringParameterValue', name: 'STACK_INSTALL', value: 'core'],
                            [$class: 'StringParameterValue', name: 'STACK_NAME', value: stack_name],
                            [$class: 'StringParameterValue', name: 'STACK_TEMPLATE', value: props['STACK_TEMPLATE']],
                            [$class: 'StringParameterValue', name: 'STACK_TEMPLATE_URL', value: 'https://github.com/ohryhorov/salt-syndic-master'],
                            [$class: 'StringParameterValue', name: 'STACK_TEMPLATE_BRANCH', value: 'master'],
                            [$class: 'StringParameterValue', name: 'STACK_TYPE', value: 'heat'],
                            [$class: 'StringParameterValue', name: 'FORMULA_PKG_REVISION', value: props['FORMULA_PKG_REVISION']],
                            [$class: 'StringParameterValue', name: 'STACK_CLUSTER_NAME', value: props['STACK_CLUSTER_NAME']],
                            [$class: 'StringParameterValue', name: 'STACK_TEST', value: ''],
                            [$class: 'StringParameterValue', name: 'SALT_VERSION', value: ''],                            
                            [$class: 'BooleanParameterValue', name: 'TEST_DOCKER_INSTALL', value: false],
                            [$class: 'TextParameterValue', name: 'SALT_OVERRIDES', value: salt_overrides_list.join('\n')],
                            [$class: 'BooleanParameterValue', name: 'STACK_DELETE', value: props['STACK_DELETE'].toBoolean()],
                        ]
                    }
                }
            }

            parallel deploy_edges_infra

            for (k in edgeBuildsInfra.keySet()) {
                common.infoMsg("keyset1: ${[k]}")
                def ed_ = k
                def deploy_job
                def props_
                def current_salt_ip
                def extra_target
                def saltMasterURL

                if (edgeBuildsInfra[ed_].result == 'SUCCESS') {
//                    extra_target = "and *jopa"
                    extra_target = "and *${edgeBuildsInfra[ed_].description.tokenize(' ')[0]}*"
//                    current_salt_ip = edgeBuildsInfra[ed_].description.tokenize(' ')[1]
                    saltMasterURL = "http://${edgeBuildsInfra[ed_].description.tokenize(' ')[1]}:6969"

                    salt_mom_ip = "172.17.48.208"
                    salt_mom_url = "http://172.17.48.208:6969"

                    enableSyndic(saltMasterURL, 'cfg01*', SALT_MASTER_CREDENTIALS, salt_mom_ip)

                    props_ = edge_deploy_schemas[ed_]['properties']
                    deploy_job = edge_deploy_schemas[ed_]['deploy_job_name']


                    deploy_edges["Deploy ${ed_} with MoM"] = {
                       node(slave_node) {
                            edgeBuilds["${ed_}"] = build job: deploy_job, propagate: false, parameters: [
                                [$class: 'StringParameterValue', name: 'HEAT_STACK_ZONE', value: HEAT_STACK_ZONE],
                                [$class: 'StringParameterValue', name: 'OPENSTACK_API_PROJECT', value: OPENSTACK_API_PROJECT],
                                [$class: 'StringParameterValue', name: 'SLAVE_NODE', value: props_['SLAVE_NODE']],
                                [$class: 'StringParameterValue', name: 'STACK_INSTALL', value: props_['STACK_INSTALL']],
                                [$class: 'StringParameterValue', name: 'STACK_TEMPLATE', value: props_['STACK_TEMPLATE']],
                                [$class: 'StringParameterValue', name: 'STACK_TEMPLATE_URL', value: 'https://github.com/ohryhorov/salt-syndic-master'],
                                [$class: 'StringParameterValue', name: 'STACK_TEMPLATE_BRANCH', value: 'master'],
                                [$class: 'StringParameterValue', name: 'STACK_TYPE', value: 'physical'],
                                [$class: 'StringParameterValue', name: 'SALT_MASTER_URL', value: salt_mom_url],
                                [$class: 'StringParameterValue', name: 'EXTRA_TARGET', value: extra_target],
                                [$class: 'StringParameterValue', name: 'FORMULA_PKG_REVISION', value: props_['FORMULA_PKG_REVISION']],
                                [$class: 'StringParameterValue', name: 'STACK_CLUSTER_NAME', value: props_['STACK_CLUSTER_NAME']],
                                [$class: 'StringParameterValue', name: 'STACK_TEST', value: ''],
                                [$class: 'BooleanParameterValue', name: 'TEST_DOCKER_INSTALL', value: false],
                                [$class: 'TextParameterValue', name: 'SALT_OVERRIDES', value: salt_overrides_list.join('\n')],
                                [$class: 'BooleanParameterValue', name: 'STACK_DELETE', value: props_['STACK_DELETE'].toBoolean()],
                            ]
                        }
                    }
//                        common.infoMsg("${edgeBuildsInfra[k].description.tokenize(' ')[0]} ${edgeBuildsInfra[k].description.tokenize(' ')[1]}")
//                        deploy_edges["${deploy_edges_infra[k]} with MoM"] = {
//                                node(slave_node) {
//                                }
//                      }
                } else {
                    common.successMsg("${k} : " + edgeBuilds[k].result)
                    common.errorMsg("${k} : " + edgeBuilds[k].result)
                }
            }

            parallel deploy_edges

        }

//    } catch (Exception e) {
//        currentBuild.result = 'FAILURE'
//        throw e
//    } finally {
// Finally stage

//    }
}
