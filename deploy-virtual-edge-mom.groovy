
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

    def momBuild = [:]
    def salt_master_url

    try {
        stage('Deploy MoM stack'){
            momBuild[deployMoMJob] = build job: deployMoMJob, propagate: false, parameters: [
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

            // get salt master url
            salt_master_url = "http://${momBuild.description.tokenize(' ')[1]}:6969"
            node_name = "${momBuild.description.tokenize(' ')[2]}"
            common.infoMsg("Salt API is accessible via ${salt_master_url}")

        }

    } catch (Exception e) {
        currentBuild.result = 'FAILURE'
        throw e
    } finally {
// Finally stage

    }
}
