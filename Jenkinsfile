pipeline {
    agent any

    stages {
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
}
