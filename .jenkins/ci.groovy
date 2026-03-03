@Library('add-ons-shared-libs@develop') _

node {
    continuousIntegrationPipeline(
        buildType: 'deploy',
        sonar: [
            enable: true,
            projectKey: "eclipse-kura_kura-gpio",
            tokenId: "sonarcloud-token-kura-gpio",
            exclusions: "tests/**/*,**/*.xml,**/*.yml,bundles/org.eclipse.kura.linux.gpio.libgpiod/src/main/java/org/eclipse/kura/linux/gpio/libgpiod1/LibGpiodV1Native.java,bundles/org.eclipse.kura.linux.gpio.libgpiod/src/main/java/org/eclipse/kura/linux/gpio/libgpiod2/LibGpiodV2Native.java",
            testExclusions: "**/*"
        ],
    )
}
