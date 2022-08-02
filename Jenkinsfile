pipeline {
	agent any
	
	stages {
		stage('Checkout') {
			steps {
				checkout scm: [
					$class: 'GitSCM',
					branches: scm.branches
				]
			}
		}
	}
}

