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

def slave_node = 'python'

if (common.validInputParam('SLAVE_NODE')) {
    slave_node = SLAVE_NODE
}

def deployMoMJob = 'deploy-heat-virtual_mcp11_aio'
if (common.validInputParam('MOM_JOB')) {
    deployMoMJob = MOM_JOB
}

def salt_overrides_list = SALT_OVERRIDES.tokenize('\n')

node(slave_node) {

    def momBuild
    def salt_mom_url
    def deploy_edges_infra = [:]
    def deploy_edges = [:]
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
    def EDGE_DEPLOY_SCHEMAS = '{os_ha_ovs: {deploy_job_name: "deploy-heat-os_ha_ovs", properties: {SLAVE_NODE: "python", STACK_INSTALL: "openstack,ovs", STACK_TEMPLATE: "os_ha_ovs", STACK_TYPE: "heat", FORMULA_PKG_REVISION: "testing", STACK_DELETE: false, STACK_CLUSTER_NAME: "os-ha-ovs-syndic"}}, k8s_ha_calico: {deploy_job_name: "deploy-heat-k8s_ha_calico", properties: {SLAVE_NODE: "python", STACK_INSTALL: "k8s,calico", STACK_TEMPLATE: "k8s_ha_calico", STACK_TYPE: "heat", FORMULA_PKG_REVISION: "testing", STACK_DELETE: false, STACK_CLUSTER_NAME: "k8s-ha-calico-syndic"}} }'

    def edge_deploy_schemas = readJSON text: EDGE_DEPLOY_SCHEMAS

    try {
        stage('Deploy MoM stack'){
            momBuild = build job: deployMoMJob, propagate: false, parameters: [
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
                node_name = "${momBuild.description.tokenize(' ')[2]}"
                salt_overrides_list.add("salt_syndic_master_address: ${momBuild.description.tokenize(' ')[1]}")
                common.infoMsg("Salt API is accessible via ${salt_mom_url}")
            } else {
                common.errorMsg("Deployment of MoM has been failed with result: " + momBuild.result)

            }

        }
        stage('Deploy edge clouds'){
            for (edge_deploy_schema in edge_deploy_schemas.keySet()) {
                def deploy_job
                def props

                deploy_job = edge_deploy_schemas[edge_deploy_schema]['deploy_job_name']

                common.infoMsg("edge cloud: ${edge_deploy_schema}")
                common.infoMsg("deploy job name: ${edge_deploy_schemas[edge_deploy_schema]['deploy_job_name']}")

                props = edge_deploy_schemas[edge_deploy_schema]['properties']

//                for (prop in edge_deploy_schemas[edge_deploy_schema]['properties'].keySet()) {
//                    common.infoMsg("prop: ${prop} value: ${edge_deploy_schemas[edge_deploy_schema]['properties'][prop]}")
//                }

                deploy_edges_infra["Deploy ${edge_deploy_schema} infra"] = {
                    node(slave_node) {
                        edgeBuilds["${edge_deploy_schema}-${props['STACK_TEMPLATE']}"] = build job: deploy_job, propagate: false, parameters: [
                            [$class: 'StringParameterValue', name: 'HEAT_STACK_ZONE', value: HEAT_STACK_ZONE],
                            [$class: 'StringParameterValue', name: 'OPENSTACK_API_PROJECT', value: OPENSTACK_API_PROJECT],
                            [$class: 'StringParameterValue', name: 'SLAVE_NODE', value: props['SLAVE_NODE']],
                            [$class: 'StringParameterValue', name: 'STACK_INSTALL', value: 'core'],
                            [$class: 'StringParameterValue', name: 'STACK_TEMPLATE', value: props['STACK_TEMPLATE']],
                            [$class: 'StringParameterValue', name: 'STACK_TEMPLATE_URL', value: 'https://github.com/ohryhorov/salt-syndic-master'],
                            [$class: 'StringParameterValue', name: 'STACK_TEMPLATE_BRANCH', value: 'master'],
                            [$class: 'StringParameterValue', name: 'STACK_TYPE', value: props['STACK_TYPE']],
                            [$class: 'StringParameterValue', name: 'FORMULA_PKG_REVISION', value: props['FORMULA_PKG_REVISION']],
                            [$class: 'StringParameterValue', name: 'STACK_CLUSTER_NAME', value: props['STACK_CLUSTER_NAME']],
                            [$class: 'StringParameterValue', name: 'STACK_TEST', value: ''],
                            [$class: 'BooleanParameterValue', name: 'TEST_DOCKER_INSTALL', value: false],
                            [$class: 'TextParameterValue', name: 'SALT_OVERRIDES', value: salt_overrides_list.join('\n')],
                            [$class: 'BooleanParameterValue', name: 'STACK_DELETE', value: props['STACK_DELETE'].toBoolean()],
                        ]
                    }
                }
            }

            parallel deploy_edges_infra

            for (k in deploy_edges_infra.keySet()) {
//                if (deploy_edges_infra[k].result == 'SUCCESS') {
                    common.infoMsg("${deploy_edges_infra[k]} ${deploy_edges_infra[k].description.tokenize(' ')[1]}")
//                    deploy_edges["${deploy_edges_infra[k]} with MoM"] = {
//                        node(slave_node) {
//                        }
//                    }
//
//                } else {
//                    common.successMsg("${k} : " + testBuilds[k].result)
//                    common.errorMsg("${k} : " + testBuilds[k].result)
//                }
            }

        }

    } catch (Exception e) {
        currentBuild.result = 'FAILURE'
        throw e
    } finally {
// Finally stage

    }
}
