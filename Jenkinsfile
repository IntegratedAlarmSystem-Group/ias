pipeline {
    agent any

    environment {
        // Define your environment variables here
        //GRADLE_HOME = '/path/to/gradle'
        //JAVA_HOME = '/path/to/java'
        PATH = "${env.PATH}:/var/lib/jenkins/.local/bin"
        IASROOT = "${env.WORKSPACE}/IasRoot"
    }

    stages {
        stage('Clean Workspace') {
            steps {
                deleteDir()
            }
        }
        stage('Create Folders') {
            steps {
                sh 'mkdir -p IasRoot'
                sh 'mkdir -p ias'
            }
        }
        stage('Checkout') {
            steps {
                dir('ias') {
                    git url: 'https://github.com/IntegratedAlarmSystem-Group/ias.git', branch: 'develop'
                }
            }
        }
        stage('Build') {
            steps {
                dir('ias') {
                    sh './gradlew build'
                }
            }
        }
    }
}
