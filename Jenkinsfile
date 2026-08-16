pipeline {
    agent any

    triggers {
        githubPush()
    }

    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    environment {
        REGISTRY_HOST = 'ghcr.io'
        CI_COMPOSE_PROJECT = "wedding-share-ci-${BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    if (env.BRANCH_NAME && env.BRANCH_NAME != 'main') {
                        error('Only main branch builds are allowed.')
                    }
                    if (!env.REGISTRY_IMAGE_PREFIX?.trim()) {
                        error('REGISTRY_IMAGE_PREFIX must be configured in the Jenkins job.')
                    }

                    env.IMAGE_TAG = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                    env.BACKEND_IMAGE = "${env.REGISTRY_IMAGE_PREFIX}-backend:${env.IMAGE_TAG}"
                    env.FRONTEND_IMAGE = "${env.REGISTRY_IMAGE_PREFIX}-frontend:${env.IMAGE_TAG}"
                }
            }
        }

        stage('Backend test') {
            steps {
                dir('backend') {
                    sh 'chmod +x mvnw && ./mvnw test'
                }
            }
        }

        stage('Frontend npm ci + test') {
            steps {
                dir('frontend') {
                    sh 'npm ci && npm test'
                }
            }
        }

        stage('Docker image build') {
            steps {
                sh '''
                    docker build --pull -t "$BACKEND_IMAGE" backend
                    docker build --pull -t "$FRONTEND_IMAGE" frontend
                '''
            }
        }

        stage('Production Compose config validation') {
            steps {
                sh '''
                    set -eu
                    umask 077
                    mkdir -p "$WORKSPACE/.ci-tls"
                    cat > "$WORKSPACE/.ci-production.env" <<EOF
APP_DOMAIN=ci.example.test
TLS_CERT_DIR=$WORKSPACE/.ci-tls
POSTGRES_DB=ci_db
POSTGRES_USER=ci_user
POSTGRES_PASSWORD=ci_placeholder_password
ADMIN_EMAIL=admin@example.test
ADMIN_PASSWORD=ci_placeholder_password
JWT_SECRET=0123456789abcdef0123456789abcdef
CORS_ALLOWED_ORIGIN=https://ci.example.test
APP_PUBLIC_BASE_URL=https://ci.example.test
VITE_API_BASE_URL=https://ci.example.test
R2_ENDPOINT=https://example.r2.cloudflarestorage.com
R2_ACCESS_KEY_ID=ci-placeholder-access-key
R2_SECRET_ACCESS_KEY=ci-placeholder-secret-key
R2_BUCKET=ci-private-bucket
EOF
                    docker compose --project-name "$CI_COMPOSE_PROJECT" \
                      --env-file "$WORKSPACE/.ci-production.env" \
                      -f infrastructure/docker-compose.production.yml config -q
                '''
            }
        }

        stage('Nginx config validation') {
            steps {
                sh '''
                    set -eu
                    command -v openssl >/dev/null
                    openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
                      -keyout "$WORKSPACE/.ci-tls/privkey.pem" \
                      -out "$WORKSPACE/.ci-tls/fullchain.pem" \
                      -subj '/CN=ci.example.test' >/dev/null 2>&1
                    docker compose --project-name "$CI_COMPOSE_PROJECT" \
                      --env-file "$WORKSPACE/.ci-production.env" \
                      -f infrastructure/docker-compose.production.yml run --rm --no-deps nginx nginx -t
                '''
            }
        }

        stage('GHCR login') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'ghcr-registry', usernameVariable: 'GHCR_USERNAME', passwordVariable: 'GHCR_TOKEN')]) {
                    sh '''
                        set +x
                        printf '%s' "$GHCR_TOKEN" | docker login "$REGISTRY_HOST" --username "$GHCR_USERNAME" --password-stdin
                    '''
                }
            }
        }

        stage('GHCR push') {
            steps {
                sh '''
                    docker push "$BACKEND_IMAGE"
                    docker push "$FRONTEND_IMAGE"
                '''
            }
        }
    }

    post {
        always {
            sh '''
                set +e
                set +x
                docker compose --project-name "$CI_COMPOSE_PROJECT" \
                  --env-file "$WORKSPACE/.ci-production.env" \
                  -f infrastructure/docker-compose.production.yml down --volumes --remove-orphans
                docker logout "$REGISTRY_HOST"
                rm -rf "$WORKSPACE/.ci-production.env" "$WORKSPACE/.ci-tls"
            '''
            deleteDir()
        }
    }
}
