pipeline {
    agent any

    stages {
        stage('Clean Workspace') {
            steps {
                deleteDir()
            }
        }
        stage('Checkout') {
            steps {
                // Replace 'your-repo-url' with your actual GitHub repository URL
                // Replace 'your-folder' with the desired folder name
                dir('ias') {
                    git url: 'https://github.com/IntegratedAlarmSystem-Group/ias.git', branch: 'develop'
                }
            }
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
}
