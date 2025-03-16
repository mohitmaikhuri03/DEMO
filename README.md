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


*********************************  non java project

pipeline {
    agent any
    environment {
        SONARQUBE_URL = 'http://localhost:9000' // Update with your SonarQube server URL
        SONAR_PROJECT_KEY = 'demo' // Update with your actual SonarQube project key
    }
    stages {
        stage('Checkout Code') {
            steps {
                git branch: 'main', url: 'https://github.com/OT-MICROSERVICES/frontend.git' // Update repo URL
            }
        }

        stage('SonarQube Analysis (CLI)') {
            steps {
                script {
                    def scannerHome = tool 'SonarQubeScanner'  // Use the installed scanner from Jenkins
                    withSonarQubeEnv('sonar') { // Ensure 'sonar' matches Jenkins settings
                        withCredentials([string(credentialsId: 'sonar', variable: 'SONARQUBE_TOKEN')]) {
                            sh """
                                ${scannerHome}/bin/sonar-scanner \
                                -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                                -Dsonar.host.url=${SONARQUBE_URL} \
                                -Dsonar.login=${SONARQUBE_TOKEN} \
                                -Dsonar.projectName=demo
                            """
                        }
                    }
                }
            }
        }
    }
}
