package com.hsbc.yourorg.devops.action   // src/com/hsbc/yourorg/devops/action/AutomationCiCreationAction.groovy

import com.hsbc.yourorg.devops.model.Controller
import groovy.json.JsonOutput

class AutomationCiCreationAction extends CiAction {

    AutomationCiCreationAction(Script script) {
        super(script)
    }

    @Override
    def perform(Controller controller, Object config) {
        this.script.maskPasswords(varPasswordPairs: [], varMaskRegexes: [
            [key: 'basicAuthRegex', value: /Basic [^\s\n\"]+/]
        ]) {
            def payload = [
                name: config.automationCiName,
                short_description: config.automationCiShortDescription,
                operational_status: 1,
                install_status: 1,
                discovery_source: "Groovy",
                assigned_to: null,
                location: null,
                department: null,
                support_group: null,
                u_custom_field: config.customFieldValue
            ]

            def jsonPayload = JsonOutput.toJson(payload)
            this.script.echo("Sending payload to ServiceNow:\n${JsonOutput.prettyPrint(jsonPayload)}")

            def response = this.script.httpRequest(
                httpMode: 'POST',
                url: "${config.servicenowInstanceUrl}${config.servicenowApiTable}",
                authentication: 'servicenow-credentials', // if you want to dynamically pass, replace this
                customHeaders: [
                    [name: 'Content-Type', value: 'application/json']
                ],
                requestBody: jsonPayload,
                validResponseCodes: '200:299'
            )

            def responseJson = this.script.readJSON text: response.content

            if (responseJson.result) {
                controller.automationCiSysId = responseJson.result.sys_id
                controller.automationCiNumber = responseJson.result.number

                this.script.currentBuild.description += "<br><b>Automation CI:</b> ${controller.automationCiNumber}"

                this.script.echo("Automation CI created successfully!")
                this.script.echo("Sys ID: ${controller.automationCiSysId}")
                this.script.echo("Number: ${controller.automationCiNumber}")
            } else {
                throw new Exception("Failed to create Automation CI in ServiceNow. Response: ${response.content}")
            }
        }
    }

    @Override
    Closure getClosure() {
        return delegateClosure(this)
    }
}
