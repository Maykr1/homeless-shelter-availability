@Library('shared-jenkins-library') _

pipeline {
    agent any
    options { timestamps() }

    environment {
        // --- APP ---
        APP_NAME        = "homeless-shelter-availability"
        ACTIVE_PROFILE  = "docker"

        // --- DOCKER ---
        COMPOSE_DIR     = '/deploy'
        DOCKER_REG      = 'localhost:8003'
        DOCKER_REPO     = 'repository/docker-apps'

        IMAGE_TAG       = "${params.COMMIT_ID ?: 'latest'}"
        IMAGE           = "${DOCKER_REG}/${APP_NAME}:${IMAGE_TAG}"

        REG_CRED_ID     = 'nexus-deploy'
        PRUNE_MODE      = "${params.PRUNE_MODE ?: 'none'}"
    }

    parameters {
        string(
            name: 'COMMIT_ID',
            defaultValue: 'latest',
            description: 'Git commit ID to deploy (branch, tag, or sha)'
        )
        choice(name: 'PRUNE_MODE',
            choices: ['none', 'dangling', 'all'],
            description: 'Choose how aggressively to prune docker containers, volumes, etc.'
        )
    }

    stages {
        stage("Pull Secrets") {
            steps {
                pullSecrets(env.APP_NAME)
            }
        }

        stage('Login to Registry') {
            steps {
                login(env.REG_CRED_ID, env.DOCKER_REG)
            }
        }

        stage('Deploy Image') {
            steps {
                deployApp(env.IMAGE, env.COMPOSE_DIR, env.IMAGE_TAG, env.APP_NAME)
            }
        }

        stage('Cleanup') {
            when { expression { env.PRUNE_MODE != 'none' } }
            steps {
                cleanupServer(env.PRUNE_MODE)
            }
        }

        stage('Logout') {
            steps {
                logout(env.DOCKER_REG)
            }
        }
        
    }

    post {
        success { 
            echo "✅ Successfully deployed ${APP_NAME}:${env.IMAGE_TAG} image" 
        }

        failure { 
            echo "❌ Deployment failed for ${APP_NAME}:${env.IMAGE_TAG} image" 
        }
    }
}
