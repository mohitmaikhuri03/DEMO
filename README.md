pipeline {
    agent any

    environment {
        SONARQUBE_URL = 'http://localhost:9000' // Update this with your SonarQube server URL
        SONAR_PROJECT_KEY = 'demo' // Update this with your actual SonarQube project key
    }

    stages {
        stage('Checkout Code') {
            steps {
                git branch: 'main', url: 'https://github.com/jaiswaladi246/secretsanta-generator' // Update repo URL
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }

        stage('SonarQube Scan') {
            steps {
                withSonarQubeEnv('sonar') { // Ensure 'sonar' matches the SonarQube instance name in Jenkins settings
                    withCredentials([string(credentialsId: 'sonar', variable: 'SONARQUBE_TOKEN')]) {
                        sh """
                        mvn sonar:sonar \
                        -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                        -Dsonar.host.url=${SONARQUBE_URL} \
                        -Dsonar.login=${SONARQUBE_TOKEN}
                        """
                    }
                }
            }
        }


        }
    
}
