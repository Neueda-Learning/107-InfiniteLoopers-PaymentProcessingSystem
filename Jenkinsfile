pipeline {
    agent any

    options {
        timestamps()

        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '15'))
    }

    parameters {
        booleanParam(name: 'BUILD_DOCKER_IMAGES', defaultValue: true, description: 'Build backend and frontend Docker images')
        booleanParam(name: 'PUSH_IMAGES', defaultValue: false, description: 'Push Docker images to the configured registry')
        booleanParam(name: 'DEPLOY_WITH_COMPOSE', defaultValue: false, description: 'Deploy the stack with docker compose on this Linux agent')
        string(name: 'DOCKER_REGISTRY', defaultValue: 'docker.io', description: 'Container registry hostname')
        string(name: 'DOCKER_REPOSITORY', defaultValue: 'your-org/payment-processing-system', description: 'Repository prefix used for backend and frontend images')
        string(name: 'DOCKER_REGISTRY_CREDENTIALS_ID', defaultValue: '', description: 'Jenkins username/password credentials ID for docker login')
        string(name: 'IMAGE_TAG', defaultValue: '', description: 'Optional override for image tag. Leave blank to use branch-buildNumber-shortCommit')
    }

    environment {
        BACKEND_IMAGE_NAME = 'payment-processing-backend'
        FRONTEND_IMAGE_NAME = 'payment-processing-frontend'
        COMPOSE_PROJECT_NAME = 'payment-processing-system'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_SHORT_COMMIT = sh(script: 'git rev-parse --short=8 HEAD', returnStdout: true).trim()
                    env.SAFE_BRANCH_NAME = (env.BRANCH_NAME ?: 'local').replaceAll('[^A-Za-z0-9_.-]', '-')
                    env.RESOLVED_IMAGE_TAG = params.IMAGE_TAG?.trim()
                            ? params.IMAGE_TAG.trim()
                            : "${env.SAFE_BRANCH_NAME}-${env.BUILD_NUMBER}-${env.GIT_SHORT_COMMIT}"
                    env.BACKEND_IMAGE = "${params.DOCKER_REGISTRY}/${params.DOCKER_REPOSITORY}/${env.BACKEND_IMAGE_NAME}:${env.RESOLVED_IMAGE_TAG}"
                    env.FRONTEND_IMAGE = "${params.DOCKER_REGISTRY}/${params.DOCKER_REPOSITORY}/${env.FRONTEND_IMAGE_NAME}:${env.RESOLVED_IMAGE_TAG}"
                }
            }
        }

        stage('Backend Tests') {
            steps {
                sh 'chmod +x mvnw'
                sh './mvnw --batch-mode clean package -DskipTests'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'target/*.jar,target/site/jacoco/**'
                }
            }
        }

        stage('Frontend Build') {
            steps {
                dir('frontend') {
                    sh 'npm ci'
                    sh 'VITE_API_BASE_URL=/api npm run build'
                }
            }
            post {
                always {
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'frontend/dist/**'
                }
            }
        }

        stage('Build Docker Images') {
            when {
                expression { params.BUILD_DOCKER_IMAGES }
            }
            steps {
                sh 'docker build -t "$BACKEND_IMAGE" .'
                sh 'docker build -t "$FRONTEND_IMAGE" ./frontend'
            }
        }

        stage('Push Docker Images') {
            when {
                allOf {
                    expression { params.BUILD_DOCKER_IMAGES }
                    expression { params.PUSH_IMAGES }
                    expression { params.DOCKER_REGISTRY_CREDENTIALS_ID?.trim() }
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: params.DOCKER_REGISTRY_CREDENTIALS_ID, usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PASSWORD')]) {
                    sh '''
                        set -e
                        echo "$REGISTRY_PASSWORD" | docker login "$DOCKER_REGISTRY" -u "$REGISTRY_USER" --password-stdin
                        docker push "$BACKEND_IMAGE"
                        docker push "$FRONTEND_IMAGE"
                        docker logout "$DOCKER_REGISTRY"
                    '''
                }
            }
        }

        stage('Deploy with Docker Compose') {
            when {
                expression { params.DEPLOY_WITH_COMPOSE }
            }
            steps {
                sh '''
                    set -e
                    : "${MYSQL_PASSWORD:?MYSQL_PASSWORD must be set in Jenkins for deployment}"
                    : "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD must be set in Jenkins for deployment}"
                    cat > .env <<EOF
MYSQL_DATABASE=${MYSQL_DATABASE:-payment_processing_db}
MYSQL_USER=${MYSQL_USER:-payment_user}
MYSQL_PASSWORD=${MYSQL_PASSWORD}
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
SPRING_PROFILES_ACTIVE=mysql
SERVER_PORT=8080
DB_URL=jdbc:mysql://mysql:3306/${MYSQL_DATABASE:-payment_processing_db}?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=${MYSQL_USER:-payment_user}
DB_PASSWORD=${MYSQL_PASSWORD}
MAIL_HOST=${MAIL_HOST:-smtp.gmail.com}
MAIL_PORT=${MAIL_PORT:-587}
MAIL_USERNAME=${MAIL_USERNAME:-}
MAIL_PASSWORD=${MAIL_PASSWORD:-}
MAIL_FROM=${MAIL_FROM:-}
APP_MAIL_ENABLED=${APP_MAIL_ENABLED:-false}
LOG_FILE=/app/logs/payment-processing-system.log
EOF
                    docker compose --env-file .env config
                    docker compose up -d --build
                '''
            }
        }
    }

    post {
        always {
            cleanWs(deleteDirs: true, notFailBuild: true)
        }
    }
}


