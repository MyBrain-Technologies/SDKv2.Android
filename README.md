# JniBrainBox SDK
Documentation (SDK Overview & Projects using SDK) :

SDK Overview & Projects using SDK : https://mybrain.atlassian.net/wiki/spaces/SDKMO/pages/484377097/Projects+using+SDK

Import SDK in your project : https://mybrain.atlassian.net/wiki/spaces/SDKMO/pages/605224961/How+to+import+sources+of+SDK+library+dynamically+in+your+project

## Description

This Android SDK is designed to help users connect to Cyrebro Headsets and analyze the EEG data.
This SDK dependencies to BrainBox SDK

## Features

*   **EEG signal processing:** Process EEG and analyst data.
*   **Bluetooth connection:** manage connection to bluetooth.

## Environment

This project was developed and tested using the following environment:

*   **Java Version:** 17
*   **Gradle Version:** 7.4
*   **Android Studio Version:** Android Studio Ladybug | 2024.2.1 Patch 3
*   **Android Gradle Plugin Version:** 7.3.0
*   **Kotlin Version:** 1.7.20

**Note:** The project may be compatible with other versions, but these are the versions used during development.

## Build and publish
*   **Build:** `.\gradlew clean assemble`
*   **Publish:** `.\gradlew publish`


### Artifactory configuration

in local.properties provide these information:

    maven_username=[nexus-username]
    maven_password=[nexus-password]
in sdk-v3\build.gradle add these information:

        Properties properties = new Properties()
        properties.load(project.rootProject.file('local.properties').newDataInputStream())
        def mavenUsername = properties.getProperty('maven_username')
        def mavenPassword = properties.getProperty('maven_password')
        def releasesRepoUrl = 'https://package.mybraintech.com/repository/maven-2-releases/'
        def snapshotsRepoUrl = 'https://package.mybraintech.com/repository/maven-2-snapshots/'
        
        project.afterEvaluate {
            publishing {
                    repositories {
                        maven {
                        url = android.defaultConfig.versionName.endsWith('SNAPSHOT') ? snapshotsRepoUrl : releasesRepoUrl
                            credentials {
                            username mavenUsername
                            password mavenPassword
                            }
                        }
                    }
                    publications {
                    // Creates a Maven publication called "release".
                        release(MavenPublication) {
                            from components.release
                
                                groupId = 'com.mybraintech.android'
                                artifactId = 'sdk'
                        }

                        debug(MavenPublication) {
                            from components.debug
            
                            groupId = 'com.mybraintech.android'
                            artifactId = 'sdk-debug'
                            version = '3.5.1-SNAPSHOT'
                        }
                    }
            }
        }
### Installation

*   **Gradle:** implementation 'com.mybraintech.android:sdk:3.5.0.0'
