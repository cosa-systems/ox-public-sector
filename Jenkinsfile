@Library('pipeline-library') _
// please see for further information: https://gitlab.open-xchange.com/jenkins/pipeline-library#cipipeline

ciPipeline(
    omitBuildPattern: [
        '^open-xchange-appsuite-public-sector'
    ],
    obsCleanUp: false,
    build: false,
    fetch: false,
    integrationBuild: [
        fetchTests: '**/*element*.xml'
    ]
)
