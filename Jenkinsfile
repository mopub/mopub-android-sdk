#!/usr/bin/env groovy
def androidAutomationProjectName = "mopub-android-automation"

pipeline {
    agent any
    environment {
        ANDROID_HOME = '/Users/jenkins/Library/Android/sdk'
        projectName = """${
            sh(script: 'IFS="/" read -ra TOKENS <<< "${JOB_NAME}"; echo ${TOKENS[0]}', returnStdout: true).trim()
        }"""
    }
    stages {
        stage('Tests') {
            steps {
                script {
                    PARSED_JOB_NAME = URLDecoder.decode(env.JOB_NAME, 'UTF-8')
                    if (projectName == androidAutomationProjectName) {
                        stage('Internal Android automation tests') {
                            echo "Internal Automation Tests are running - ${PARSED_JOB_NAME}"
                            sh '''
                                #!/bin/bash
                                chmod +x scripts/private/android.sh
                                scripts/private/android.sh
                            '''
                        }
                        stage('External Android automation tests') {
                            echo "External Automation Tests are running - ${PARSED_JOB_NAME}"
                            sh '''
                                #!/bin/bash
                                chmod +x scripts/private/androidRT.sh
                                scripts/private/androidRT.sh
                            '''
                        }
                    } else {
                        stage('Smoke tests') {
                            echo "Smoke Tests are running - ${PARSED_JOB_NAME}"
                            sh '''
                                #!/bin/bash
                                ./gradlew clean build
                            '''
                        }
                        stage('Sign apk') {
                            sh '$ANDROID_HOME/build-tools/29.0.2/zipalign -v -p 4 mopub-sample/build/outputs/apk/external/release/mopub-sample-external-release-unsigned.apk mopub-sample/build/outputs/apk/external/release/mopub-sample-external-release-unsigned-aligned.apk'
                            if (fileExists('mopub-sample/build/outputs/apk/internal/release/mopub-sample-internal-release-unsigned.apk')) {
                                sh '$ANDROID_HOME/build-tools/29.0.2/zipalign -v -p 4 mopub-sample/build/outputs/apk/internal/release/mopub-sample-internal-release-unsigned.apk mopub-sample/build/outputs/apk/internal/release/mopub-sample-internal-release-unsigned-aligned.apk'
                            }
                            withCredentials([string(credentialsId: 'android_store_key_pass', variable: 'JKS_PASS')]) {
                                sh '$ANDROID_HOME/build-tools/29.0.2/apksigner sign --ks ~/google_play_key.jks --ks-pass pass:$JKS_PASS --out mopub-sample/build/outputs/apk/external/release/mopub-sample-external-release-signed.apk mopub-sample/build/outputs/apk/external/release/mopub-sample-external-release-unsigned-aligned.apk'
                                if (fileExists('mopub-sample/build/outputs/apk/internal/release/mopub-sample-internal-release-unsigned-aligned.apk')) {
                                    sh '$ANDROID_HOME/build-tools/29.0.2/apksigner sign --ks ~/google_play_key.jks --ks-pass pass:$JKS_PASS --out mopub-sample/build/outputs/apk/internal/release/mopub-sample-internal-release-signed.apk mopub-sample/build/outputs/apk/internal/release/mopub-sample-internal-release-unsigned-aligned.apk'
                                }
                            }
                        }
                        stage('Archive') {
                            archiveArtifacts artifacts: 'mopub-sample/build/outputs/**/*.apk', excludes: 'mopub-sample/build/outputs/**/*unsigned.apk, mopub-sample/build/outputs/**/*aligned.apk', onlyIfSuccessful: true
                            archiveArtifacts artifacts: 'mopub-sdk/build/outputs/aar/mopub-sdk-*.aar', onlyIfSuccessful: true
                            archiveArtifacts artifacts: 'mopub-sdk/mopub-sdk-*/build/outputs/aar/mopub-sdk-*.aar', onlyIfSuccessful: true
                        }
                    }
                }
            }
        }
    }
    post {
        fixed {
            slackSend color: 'GREEN', message: "<${env.BUILD_URL}|${PARSED_JOB_NAME} #${env.BUILD_NUMBER}> has succeeded."
        }
        failure {
            slackSend color: 'RED', message: "Attention @here <${env.BUILD_URL}|${PARSED_JOB_NAME} #${env.BUILD_NUMBER}> has failed."
        }
    }
}
