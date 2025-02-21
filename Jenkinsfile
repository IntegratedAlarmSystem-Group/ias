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
                dir('IasRoot') {
                    // This will create the IasRoot directory
                    sh 'mkdir -p .'
                }
                dir('ias') {
                    // This will create the ias directory
                    sh 'mkdir -p .'
                }
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
