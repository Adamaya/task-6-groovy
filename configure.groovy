job('job-dsl-plugin') {
  steps{    
  scm {
        git {
          remote {
            url('https://github.com/jenkinsci/job-dsl-plugin.git')
          }
          branch('*/master')
        }
      }
  triggers {
        scm('* * * * *')
    }
  }
 
}
