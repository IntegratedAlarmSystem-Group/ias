pipeline {
    agent any

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
    }
}
