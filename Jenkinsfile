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
            hipchatSend message: "${env.JOB_NAME} #${env.BUILD_NUMBER} has succeeded.", color: 'GREEN'
        }
        failure {
            hipchatSend message: "Attention @here ${env.JOB_NAME} #${env.BUILD_NUMBER} has failed.", color: 'RED'
        }
    }
}
