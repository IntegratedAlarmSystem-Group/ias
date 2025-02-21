pipeline {
    agent any

    stages {
        stage('Clean Workspace') {
            steps {
                deleteDir()
            }
        }
        stage('Create IasRoot Folder') {
            steps {
                dir('IasRoot') {
                    // This will create the IasRoot directory
                    sh 'mkdir -p .'
                }
            }
        }
        stage('Checkout') {
            steps {
                dir('IasRoot/ias') {
                    git url: 'https://github.com/IntegratedAlarmSystem-Group/ias.git', branch: 'develop'
                }
            }
        }
    }
}
