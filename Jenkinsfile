pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                // Replace 'your-repo-url' with your actual GitHub repository URL
                // Replace 'your-folder' with the desired folder name
                git url: https://github.com/IntegratedAlarmSystem-Group/ias.git', branch: 'develop'
                dir('ias') {
                    checkout([$class: 'GitSCM', branches: [[name: '*/main']], userRemoteConfigs: [[url: 'https://github.com/your-username/your-repo-url']]])
                }
            }
        }
    }
}

