@Library('add-ons-shared-libs@develop') _

node {
    continuousIntegrationPipeline(
        buildType: 'deploy',
        sonar: [
            enable: true,
            projectKey: "eclipse-kura_kura-gpio",
            tokenId: "sonarcloud-token-kura-gpio",
            exclusions: "tests/**/*,**/*.xml,**/*.yml",
            testExclusions: "**/*"
        ],
    )
}
