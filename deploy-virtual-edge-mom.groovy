/*
    {
        edge_cloud: {
            deploy_job_name: 'Deploy - os_ha_ovs heat',
            properties: {
                HEAT_STACK_ZONE: '',
                OPENSTACK_API_PROJECT: '',
                SLAVE_NODE: '',
                STACK_INSTALL: '',
                STACK_TEMPLATE: '',
                STACK_TYPE: ''
            }
        }
    }

 *
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

node(slave_node) {

    def momBuild
    def salt_master_url
    def deploy_edges = [:]
    def edgeBuilds = [:]
    def props

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
                STACK_DELETE: false
            }
        }
    }
*/
//    def EDGE_DEPLOY_SCHEMAS = '{edge_cloud: {deploy_job_name: "Deploy - os_ha_ovs heat", properties: {HEAT_STACK_ZONE: "mcp-oscore", OPENSTACK_API_PROJECT: "mcp-oscore", SLAVE_NODE: "python", STACK_INSTALL: "core", STACK_TEMPLATE: "os_ha_ovs", STACK_TYPE: "heat", FORMULA_PKG_REVISION: "testing", STACK_DELETE: false}}, edge_cloud1: {deploy_job_name: "deploy job name1", properties: {property: "property1"}} }'
    def EDGE_DEPLOY_SCHEMAS = '{edge_cloud: {deploy_job_name: "deploy-heat-os_ha_ovs", properties: {HEAT_STACK_ZONE: "mcp-oscore", OPENSTACK_API_PROJECT: "mcp-oscore", SLAVE_NODE: "python", STACK_INSTALL: "core", STACK_TEMPLATE: "os_ha_ovs", STACK_TYPE: "heat", FORMULA_PKG_REVISION: "testing", STACK_DELETE: false}} }'

    def edge_deploy_schemas = readJSON text: EDGE_DEPLOY_SCHEMAS

    try {
        stage('Deploy MoM stack'){
/*            momBuild = build job: deployMoMJob, propagate: false, parameters: [
                [$class: 'StringParameterValue', name: 'FORMULA_PKG_REVISION', value: 'testing'],
                [$class: 'StringParameterValue', name: 'STACK_CLUSTER_NAME', value: 'virtual-mcp11-aio'],
                [$class: 'StringParameterValue', name: 'STACK_INSTALL', value: 'mom'],
                [$class: 'BooleanParameterValue', name: 'STACK_DELETE', value: STACK_DELETE.toBoolean()],
                [$class: 'StringParameterValue', name: 'STACK_RECLASS_ADDRESS', value: 'https://gerrit.mcp.mirantis.net/salt-models/mcp-virtual-aio'],
                [$class: 'StringParameterValue', name: 'STACK_RECLASS_BRANCH', value: "stable/queens"],
                [$class: 'StringParameterValue', name: 'OPENSTACK_API_PROJECT', value: 'mcp-oscore'],
                [$class: 'StringParameterValue', name: 'HEAT_STACK_ZONE', value: 'mcp-oscore'],
                [$class: 'StringParameterValue', name: 'STACK_TEMPLATE_URL', value: 'https://github.com/ohryhorov/salt-syndic-master'],
                [$class: 'StringParameterValue', name: 'STACK_TEMPLATE_BRANCH', value: 'master'],
                [$class: 'StringParameterValue', name: 'STACK_TEMPLATE', value: 'virtual_edge_mom'],
                [$class: 'StringParameterValue', name: 'SLAVE_NODE', value: slave_node],
            ]

            if (momBuild != null) {
                // get salt master url
                salt_master_url = "http://${momBuild.description.tokenize(' ')[1]}:6969"
                node_name = "${momBuild.description.tokenize(' ')[2]}"
                common.infoMsg("Salt API is accessible via ${salt_master_url}")
            }
*/
        }
        stage('Deploy edge clouds'){
            for (edge_deploy_schema in edge_deploy_schemas.keySet()) {

                common.infoMsg("edge cloud: ${edge_deploy_schema}")
                common.infoMsg("deploy job name: ${edge_deploy_schemas[edge_deploy_schema]['deploy_job_name']}")

                props = edge_deploy_schemas[edge_deploy_schema]['properties']

//                for (prop in edge_deploy_schemas[edge_deploy_schema]['properties'].keySet()) {
//                    common.infoMsg("prop: ${prop} value: ${edge_deploy_schemas[edge_deploy_schema]['properties'][prop]}")
//                }

                deploy_edges["Deploy ${edge_deploy_schema}"] = {
                    node(slave_node) {
                        edgeBuilds["${edge_deploy_schema}-${props['STACK_TEMPLATE']}"] = build job: edge_deploy_schemas[edge_deploy_schema]['deploy_job_name'], propagate: false, parameters: [
                            [$class: 'StringParameterValue', name: 'HEAT_STACK_ZONE', value: props['HEAT_STACK_ZONE']],
                            [$class: 'StringParameterValue', name: 'OPENSTACK_API_PROJECT', value: props['OPENSTACK_API_PROJECT']],
                            [$class: 'StringParameterValue', name: 'SLAVE_NODE', value: props['SLAVE_NODE']],
                            [$class: 'StringParameterValue', name: 'STACK_INSTALL', value: props['STACK_INSTALL']],
                            [$class: 'StringParameterValue', name: 'STACK_TEMPLATE', value: props['STACK_TEMPLATE']],
                            [$class: 'StringParameterValue', name: 'STACK_TYPE', value: props['STACK_TYPE']],
                            [$class: 'StringParameterValue', name: 'FORMULA_PKG_REVISION', value: props['FORMULA_PKG_REVISION']],
                            [$class: 'BooleanParameterValue', name: 'STACK_DELETE', value: props['STACK_DELETE'].toBoolean()],
                        ]
                    }
                }

            }

            parallel deploy_edges

        }

    } catch (Exception e) {
        currentBuild.result = 'FAILURE'
        throw e
    } finally {
// Finally stage

    }
}
