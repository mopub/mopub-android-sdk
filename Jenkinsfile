#!/usr/bin/env groovy
pipeline {
    agent any
    environment {
        ANDROID_HOME = '/Users/jenkins/Library/Android/sdk'
    }
    stages {
        stage('Build') {
            steps {
                sh './gradlew clean build'
            }
        }
    }
    post {
        success {
            slackSend color: 'GREEN', message: "<${env.BUILD_URL}|${env.JOB_NAME} #${env.BUILD_NUMBER}> has succeeded."
        }
        failure {
            slackSend color: 'RED', message: "Attention @here <${env.BUILD_URL}|${env.JOB_NAME} #${env.BUILD_NUMBER}> has failed."
        }
    }
}
