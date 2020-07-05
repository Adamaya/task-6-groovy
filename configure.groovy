job('job1_pull_repo_build_image') {
    steps {
      scm {
        git {
          remote {
            url('https://github.com/Adamaya/pipeline_implementation_with_k8s_-_jenkins.git')
          }
          branch('*/master')
        }
      }
      triggers {
        scm('* * * * *')
      }
      conditionalSteps {
            condition {
                shell("count=\$(ls | grep .php | wc -l); if [[ \$count -gt 0 ]]; then cp -vr * /home/php exit 0;else  exit 1; fi")
            }
            runner('DontRun')
            steps {
                dockerBuilderPublisher {
                  dockerFileDirectory("/home/php")
                  fromRegistry {
                    url("adamayasharma")
                    credentialsId("3f885629-0783-4229-8808-f2610c781c80")
                  }
                cloud("Local")
    
                tagsString("adamayasharma/-gphp-webserver")
                pushCredentialsId("3f885629-0783-4229-8808-f2610c781c80")
                pushOnSuccess(true)
                cleanImages(false)
                cleanupWithJenkinsJobDelete(false)
                noCache(false)
                pull(true)
                }    
            }
        }
      conditionalSteps {
            condition {
                shell("count=\$(ls | grep .php | wc -l); if [[ \$count -gt 0 ]]; then cp -vr * /home/php exit 0;else  exit 1; fi")
            }
            runner('DontRun')
            steps {
                dockerBuilderPublisher {
                  dockerFileDirectory("/home/http")
                  fromRegistry {
                    url("adamayasharma")
                    credentialsId("3f885629-0783-4229-8808-f2610c781c80")
                  }
                cloud("docker")
    
              tagsString("adamayasharma/-gapache-webserver")
              pushCredentialsId("3f885629-0783-4229-8808-f2610c781c80")
              pushOnSuccess(true)
              cleanImages(false)
              cleanupWithJenkinsJobDelete(false)
              noCache(false)
              pull(true)
              }    
            }
        }
    }
}

job('job2_deploy_on_k8s'){
  steps{
    triggers{
	  upstream("job1_pull_repo_build_image","SUCCESS")
    }
    shell("count=\$(ls /var/lib/jenkins/workspace/job1 | grep .php | wc -l);if [[ \$count -gt 0 ]];then if kubectl get deployment | grep php; then exit 0 ;else kubectl create -f /home/php-deployment.yml ;fi; else if kubectl get deployment | grep http; then exit 0; else kubectl create -f /home/http-deployment.yml; fi;fi")
  }
}

job('job3_test_web_app'){
  steps{
    triggers{
	  upstream("job2_deploy_on_k8s","SUCCESS")
    }
    shell('sleep 5;status=\$(curl -o /dev/null -s -w "%{http_code}" 192.168.99.102:30600); if [[ \$status == 200 ]]; then echo "web application working" ;else echo "web application is not working";fi')
  }
}

job('job4_redeploy'){
	triggers {
    upstream {
      upstreamProjects("job3_test_web_app")
      threshold("FAILURE")
    }
  }
  
  
  publishers {
    postBuildScripts {
      steps {
        downstreamParameterized {
  	  	  trigger("job1_pull_repo_build_image")
        }
      }
    }
  }
}
