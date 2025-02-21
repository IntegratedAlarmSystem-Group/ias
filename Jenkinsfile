pipeline {
	agent any
	
	stages {
		stage('Checkout') {
			steps {
				dir('ias') {
					checkout scm: [
						$class: 'GitSCM',
						branches: scm.branches
					]
				}
			}
		}
	}
}

