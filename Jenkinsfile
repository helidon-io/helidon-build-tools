/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

pipeline {
  agent {
    label "linux"
  }
  options {
    parallelsAlwaysFailFast()
  }
  stages {
    stage('default') {
      parallel {
        stage('build'){
          steps {
            script {
              try {
                sh './etc/scripts/build.sh'
              } finally {
                archiveArtifacts artifacts: "**/target/surefire-reports/*.txt"
                junit testResults: '**/target/surefire-reports/*.xml'
              }
            }
          }
        }
        stage('build-windows'){
          agent {
            label "windows"
          }
          steps {
            script {
              try {
                bat '''
                    set JAVA_HOME=C:\\tools\\graalvm-ce-java11-20.2.0
                    set MAVEN_HOME=C:\\tools\\apache-maven-3.6.3
                    set PATH=%MAVEN_HOME%\\bin;%JAVA_HOME%\\bin;%PATH%
                    mvn clean install -Pnative-image
                '''
              } finally {
                archiveArtifacts artifacts: "**/target/surefire-reports/*.txt,helidon-cli/impl/target/helidon.exe"
                junit testResults: '**/target/surefire-reports/*.xml'
              }
            }
          }
        }
        stage('copyright'){
          steps {
            sh './etc/scripts/copyright.sh'
          }
        }
        stage('checkstyle'){
          steps {
            sh './etc/scripts/checkstyle.sh'
          }
        }
      }
    }
    stage('release') {
      when {
        branch '**/release-*'
      }
      environment {
        GITHUB_SSH_KEY = credentials('helidonrobot-github-ssh-private-key')
        MAVEN_SETTINGS_FILE = credentials('helidonrobot-maven-settings-ossrh')
        GPG_PUBLIC_KEY = credentials('helidon-gpg-public-key')
        GPG_PRIVATE_KEY = credentials('helidon-gpg-private-key')
        GPG_PASSPHRASE = credentials('helidon-gpg-passphrase')
      }
      steps {
        sh './etc/scripts/release.sh release_build'
      }
    }
  }
}
