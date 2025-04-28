package com.hsbc.treasury.ecosystem.devops.action

import java.util.regex.Pattern
import com.hsbc.treasury.ecosystem.devops.model.Controller
import com.hsbc.treasury.ecosystem.devops.service.ChangeRequestService

class CrCreationAction extends CiAction {

    private final ChangeRequestService changeRequestService

    CrCreationAction(Script script, ChangeRequestService changeRequestService) {
        super(script)
        this.changeRequestService = changeRequestService
    }

    @Override
    def perform(Controller controller, Object config) {
        def jobUser = this.script.currentBuild.getBuildCauses()[0].userId
        this.script.echo("User id: ${jobUser}")
        // Make sure jobUser is a valid employee id
        def match = this.script.sh(returnStatus: true, script: "echo -n \"${jobUser}\" | grep -E \"^[0-9]+\\S\"")
        if (match != 0) {
            throw new Exception("${jobUser} is not a valid user name.")
        }
        this.script.maskPasswords(varPasswordPairs: [], varMaskRegexes: [
            [key: 'userPasswordRegex', value: Pattern.quote(this.script.params.CR_LOGIN_PASSWORD.toString().trim())],
            [key: 'basicAuthRegex', value: /Basic [^\s\n\"]+/],
            [key: 'bearerTokenRegex', value: /Bearer [^\s\n\"]+/]
        ]) {
            def authSecret = ""
            def loginPassword = this.script.params.CR_LOGIN_PASSWORD.toString().trim()

            // Choose auth type, Bearer or Basic
            if (!loginPassword.equals("")) {
                // loginPassword not empty → use basic auth
                this.script.echo("Use basicAuth username:password for ${jobUser}")
                def base64UsernamePassword = "${jobUser}:${loginPassword}".bytes.encodeBase64().toString()
                authSecret = "Basic ${base64UsernamePassword}"
            } else {
                // loginPassword empty → use bearer token
                def credentialsPrefix = config.getOrDefault('crMinionCredentialsPrefix', 'cr-minion-bearer-token-prod-')
                def credentialsId = "${credentialsPrefix}${jobUser}"
                this.script.echo("Use bearer token for ${jobUser} with credentials \"${credentialsId}\"")
                this.script.withCredentials([this.script.string(credentialsId: credentialsId, variable: 'bearerToken')]) {
                    def userToken = this.script.bearerToken
                    authSecret = "Bearer ${userToken}"
                }
            }
            Map crCreationResult = changeRequestService.createCr(config, authSecret)
            controller.crNumber = crCreationResult.get("crNumber")
            controller.crStartDateTime = crCreationResult.get("scheduleStartDateTime")
        }

        this.script.currentBuild.description += "<br><b>ICE pre-checker:</b> <a href='https://ice-prechecker.tooling.hsbc-9033616-ettooling-dev.dev.gcp.cloud.hk.hsbc/#/?crNumber=${controller.crNumber}'>${controller.crNumber}</a>"
    }

    @Override
    Closure getClosure() {
        return delegateClosure(this)
    }
}
