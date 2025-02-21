pipeline {
	agent any
	
	stages {
		stage('Checkout') {
			steps {
				checkout scmGit(branches: [[name: '*/master']], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'ias'], cleanBeforeCheckout(deleteUntrackedNestedRepositories: true)], userRemoteConfigs: [[]])
			}
		}
	}
}

