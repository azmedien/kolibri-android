node {
    // Mark the code checkout 'stage'....
    stage 'Stage Checkout'

    // Checkout code from repository and update any submodules
    checkout scm
    sh 'git submodule update --init'

    stage 'Stage Build & Test'

    //branch name from Jenkins environment variables
    echo "Branch is: ${env.BRANCH_NAME}"

    sh 'fastlane android test'
}