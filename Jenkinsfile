pipeline {
    agent { label "gcp-bffpeak-jenkins-slave" }

    environment {
        SERVICENOW_INSTANCE_URL = "https://hsbcitidu.service-now.com"
        SERVICENOW_API_TABLE = "/api/now/table/cmdb_ci_automation"
        REQUESTED_BY = "uma.rao@noexternalmail.hsbc.com"
        ASSIGNMENT_GROUP = "ET-FINEX-BFF-PEAK-IT"
        SHORT_DESCRIPTION = "Automated CR via Groovy API"
        DESCRIPTION = "Created for deployment automation"
        RISK = "low"
        IMPACT = "low"
    }

    options {
        buildDiscarder(logRotator(daysToKeepStr: '7', numToKeepStr: '10'))
    }

    parameters {
        string(name: 'SERVICENOW_USERNAME', defaultValue: 'jenkins_service_account', description: 'ServiceNow Username')
        password(name: 'SERVICENOW_PASSWORD', defaultValue: '', description: 'ServiceNow Password')
        string(name: 'AUTOMATION_CI_NAME', defaultValue: 'Terraform Managed Automation CI', description: 'Automation CI Name')
        string(name: 'AUTOMATION_CI_SHORT_DESCRIPTION', defaultValue: 'Automation CI created by Groovy', description: 'Automation CI Short Description')
        string(name: 'CUSTOM_FIELD_VALUE', defaultValue: 'custom value', description: 'Custom field value')
    }

    stages {
        stage('Create Automation CI in ServiceNow') {
            steps {
                script {
                    def payload = [
                        name: params.AUTOMATION_CI_NAME,
                        short_description: params.AUTOMATION_CI_SHORT_DESCRIPTION,
                        operational_status: 1,
                        install_status: 1,
                        discovery_source: "Groovy",
                        assigned_to: null,
                        location: null,
                        department: null,
                        support_group: null,
                        u_custom_field: params.CUSTOM_FIELD_VALUE
                    ]

                    def jsonPayload = groovy.json.JsonOutput.toJson(payload)
                    
                    echo "Sending following payload to ServiceNow:\n${groovy.json.JsonOutput.prettyPrint(jsonPayload)}"

                    def response = httpRequest(
                        httpMode: 'POST',
                        url: "${env.SERVICENOW_INSTANCE_URL}${env.SERVICENOW_API_TABLE}",
                        authentication: 'servicenow-credentials', // You must create a Jenkins credential with ID 'servicenow-credentials'
                        customHeaders: [
                            [name: 'Content-Type', value: 'application/json']
                        ],
                        requestBody: jsonPayload,
                        validResponseCodes: '200:299'
                    )

                    def responseJson = readJSON text: response.content

                    echo "Response from ServiceNow: ${response.content}"

                    if (responseJson.result) {
                        env.AUTOMATION_CI_SYS_ID = responseJson.result.sys_id
                        env.AUTOMATION_CI_NUMBER = responseJson.result.number

                        echo "Automation CI created successfully!"
                        echo "Sys ID: ${env.AUTOMATION_CI_SYS_ID}"
                        echo "Number: ${env.AUTOMATION_CI_NUMBER}"
                    } else {
                        error "Failed to create Automation CI in ServiceNow. Response: ${response.content}"
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs()
            script {
                emailext(
                    body: """Pipeline '${env.JOB_NAME} [${env.BUILD_NUMBER}]' : ${currentBuild.result}

Check console output at ${env.BUILD_URL} to view the results.""",
                    subject: """${currentBuild.result == 'SUCCESS' ? 'SUCCESS:' : 'FAILURE:'} Pipeline '${env.JOB_NAME} [${env.BUILD_NUMBER}]'""",
                    to: "uma.rao@noexternalmail.hsbc.com"
                )
            }
        }
    }
}
