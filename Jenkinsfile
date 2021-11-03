node('Docker CI slave') {
    try {

        stage('checkout & env') {
            checkout scm
            //dev
            withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: 'jenkins-dev', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                sh "eval \$(aws ecr get-login --no-include-email --region us-east-1)"
            }
        }

        stage('Create and push docker image(s)') {
            dir("automation-logger") {
                env.DOCKER_CLI_EXPERIMENTAL = "enabled"
                sh "docker buildx create --use"
                sh "docker buildx build --progress plain --platform linux/arm64,linux/amd64 --push -t 931733775016.dkr.ecr.us-east-1.amazonaws.com/marketplace-sdk-automation-logger:latest ."
            }
        }

        stage('run kubectl to update relevant pod') {
            withCredentials([file(credentialsId: 'client_kubeconfig', variable: 'KUBECONFIG')]) {
                //get pods by - kubectl get --namespace automation pods -l app=automation-logger
                //re-deploy service - helm install --namespace automation logger ./autologger
                //for now, kill the pods matching the relevant app label, so the image will be pulled again.
                sh "curl -LO \"https://dl.k8s.io/release/\$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl\""
                sh "curl -LO \"https://dl.k8s.io/\$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl.sha256\""
                sh "echo \"\$(cat kubectl.sha256) kubectl\" | sha256sum --check"
                sh "chmod +x kubectl"
                sh "./kubectl delete --namespace automation pods -l app=automation-logger"
                sh "rm kubectl"
            }

        }


    } catch (e) {
        echo e
        throw e
    } finally {
        try {
            sh "gradle --stop"
        } catch (Exception ignored) {}
    }
}
