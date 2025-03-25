pipeline {
    agent any

    options {
        // This is required if you want to clean before build
        skipDefaultCheckout(true)
    }
    
    environment {
        // Define your environment variables here
        //GRADLE_HOME = '/path/to/gradle'
        //JAVA_HOME = '/path/to/java'
        PATH = "${env.PATH}:/var/lib/jenkins/.local/bin"
        IAS_ROOT = "${env.WORKSPACE}/IasRoot"
    }

    stages {
        stage('Clean Workspace') {
            steps {
                cleanWs()
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
        stage('Install') {
            steps {
                dir('ias') {
                    sh './gradlew install'
                }
            }
        }
        stage('Compress IasRoot') {
            steps {
                sh 'tar -czf IasRoot.tgz -C IasRoot .'
            }
        }
    }

    post {
        success {
            archiveArtifacts artifacts: '**/*.tgz'
        }
    }
}
