def call() {
    ansiblePlaybook(
                        credentialsId: '8d996de9-8b5a-4483-8b1a-4805fa3933df', 
                        disableHostKeyChecking: true,
                        installation: 'Ansible', 
                        inventory: 'resources/aws_ec2.yaml',
                        playbook: 'resources/vault.yaml', 
                        vaultTmpPath: ''
                    )

}
