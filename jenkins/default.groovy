milestone()
stage ('Build Images') {
    // make sure we should continue
    if ( config.require_tag ) {
        try {
            sh 'docker/bin/check_if_tag.sh'
        } catch(err) {
            utils.ircNotification([stage: 'Git Tag Check', status: 'failure'])
            throw err
        }
    }
    utils.ircNotification([stage: 'Test & Deploy', status: 'starting'])
    lock ("bedrock-docker-${env.GIT_COMMIT}") {
        image_mode = config.demo ? 'demo' : 'prod'
        command = "docker/bin/build_images.sh --${image_mode}"
        if (config.smoke_tests || config.integration_tests) {
            command += ' --test'
        }
        try {
            sh command
        } catch(err) {
            utils.ircNotification([stage: 'Docker Build', status: 'failure'])
            throw err
        }
    }
}

if ( config.smoke_tests ) {
    milestone()
    stage ('Test Images') {
        parallel([
            smoke_tests: utils.integrationTestJob('smoke'),
            unit_tests: {
                node {
                    unstash 'scripts'
                    try {
                        sh 'docker/bin/run_tests.sh'
                    } catch(err) {
                        utils.ircNotification([stage: 'Unit Test', status: 'failure'])
                        throw err
                    }
                }
            },
        ])
    }
}

// test this way to default to true for undefined
if ( config.push_public_registry != false ) {
    milestone()
    stage ('Push Public Images') {
        try {
            if (config.demo) {
                utils.pushDockerhub('mozorg/bedrock_demo')
            }
            else {
                utils.pushDockerhub('mozorg/bedrock_base')
                utils.pushDockerhub('mozorg/bedrock_build')
                utils.pushDockerhub('mozorg/bedrock_code')
                utils.pushDockerhub('mozorg/bedrock_l10n', 'mozorg/bedrock')
            }
        } catch(err) {
            utils.ircNotification([stage: 'Dockerhub Push Failed', status: 'warning'])
        }
    }
}

/**
 * Do region first because deployment and testing should work like this:
 * region1:
 *   push image -> deploy app1 -> test app1 -> deploy app2 -> test app2
 * region2:
 *   push image -> deploy app1 -> test app1 -> deploy app2 -> test app2
 *
 * A failure at any step of the above should fail the entire job
 */
if ( config.apps ) {
    milestone()
    // default to usw only
    def regions = config.regions ?: ['usw']
    for (regionId in regions) {
        def region = global_config.regions[regionId]
        if (region.registry_port) {
            def stageName = "Private Push: ${region.name}"
            stage (stageName) {
                try {
                    utils.pushPrivateReg(region.registry_port, config.apps)
                } catch(err) {
                    utils.ircNotification([stage: stageName, status: 'failure'])
                    throw err
                }
            }
        }
        for (appname in config.apps) {
            appSuffix = config.app_name_suffix ?: ''
            if ( config.demo ) {
                appURL = utils.demoAppURL(appname, region)
            } else {
                appURL = "https://${appname}${appSuffix}.${region.name}.moz.works"
            }
            stageName = "Deploy ${appname}-${region.name}"
            // ensure no deploy/test cycle happens in parallel for an app/region
            lock (stageName) {
                milestone()
                stage (stageName) {
                    withEnv(["DEIS_PROFILE=${region.deis_profile}",
                             "DEIS_BIN=${region.deis_bin}",
                             "DOCKER_PRIVATE_REPO=${appname}",
                             "DEIS_APPLICATION=${appname}"]) {
                        try {
                            retry(3) {
                                if (config.demo) {
                                    withCredentials([[$class: 'StringBinding',
                                                      credentialsId: 'SENTRY_DEMO_DSN',
                                                      variable: 'SENTRY_DEMO_DSN']]) {
                                        sh 'docker/bin/prep_demo.sh'
                                    }
                                }
                                sh 'docker/bin/push2deis.sh'
                            }
                        } catch(err) {
                            utils.ircNotification([stage: stageName, status: 'failure'])
                            throw err
                        }
                    }
                }
                if ( config.integration_tests ) {
                    // queue up test closures
                    def allTests = [:]
                    for (filename in config.integration_tests) {
                        allTests[filename] = utils.integrationTestJob(filename, appURL)
                    }
                    stage ("Test ${appname}-${region.name}") {
                        try {
                            // wait for server to be ready
                            sleep(time: 10, unit: 'SECONDS')
                            parallel allTests
                        } catch(err) {
                            utils.ircNotification([stage: "Integration Tests ${appname}-${region.name}", status: 'failure'])
                            throw err
                        }
                    }
                }
                // huge success \o/
                utils.ircNotification([message: appURL, status: 'shipped'])
            }
        }
    }
}
