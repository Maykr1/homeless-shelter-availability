@Library('shared-jenkins-library') _

pipeline {
    // --- SETUP ---
    agent any
    options { timestamps(); disableConcurrentBuilds() }
    tools { maven 'maven-3.9.11' }

    environment {
        // --- APP ---
        APP_NAME            = "homeless-shelter-availability"

        // --- MAVEN ---
        NEXUS               = credentials('nexus-deploy')
        NEXUS_BASE          = "https://nexus.ethansclark.com"
        SNAPSHOT_REPO_ID    = "maven-snapshots"
        SNAPSHOT_REPO       = "${NEXUS_BASE}/repository/${SNAPSHOT_REPO_ID}/"

        // --- DOCKER ---
        DOCKER_BASE         = "localhost:8003"
    }

    stages {
        stage('Checkout Repo') {
            steps {
                checkout scm
            }
        }

        stage('Test') {
            steps {
                testApp('maven')
            }
        }

        stage('Build') {
            steps {
                buildApp('maven')
            }
        }

        stage('SonarQube') {
            steps {
                sonarApp('maven', APP_NAME)
            }
        }

        stage('Publish Snapshot') {
            steps {
                script {
                    env.SNAPSHOT_VERSION = getSnapshotVersion()
                }

                setVersion('maven', env.SNAPSHOT_VERSION)
                containerizeApp('maven', APP_NAME, SNAPSHOT_REPO, DOCKER_BASE, env.COMMIT_ID) // env.COMMIT_ID is set inside of getSnapshotVersion()
            }
        }
    }

    post {
        success {
            echo 'Build complete ✅' 
        }
        failure {
            echo 'Build failed ❌' 
        }
    }
}